package nmu.cso.isp.bot;

import jdk.jshell.Diag;
import nmu.cso.isp.model.Ticket;
import nmu.cso.isp.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrative Telegram Bot component designed to manage service tickets.
 * This bot acts as the internal CRM interface for technicians to receive,
 * process, and close customer support requests.
 * * @author Muts Nazar
 * @version 1.0
 */
@Component
public class AdminBot extends TelegramLongPollingBot {
    private final TicketRepository ticketRepository;
    private final DiagnosticBot diagnosticBot;

    /**
     * Constructs a new AdminBot instance.
     *
     * @param ticketRepository repository for ticket data persistence
     * @param diagnosticBot reference to the client-facing bot for cross-bot communication
     */
    public AdminBot(TicketRepository ticketRepository, @org.springframework.context.annotation.Lazy DiagnosticBot diagnosticBot) {
        this.ticketRepository = ticketRepository;
        this.diagnosticBot = diagnosticBot;
    }

    @Value("${admin.bot.name}") private String botName;
    @Value("${admin.bot.token}") private String botToken;
    @Value("${admin.chat.id}") private String adminChatId;

    @Override public String getBotUsername() { return this.botName; }
    @Override public String getBotToken() { return this.botToken; }

    /**
     * Handles incoming updates from the Telegram API.
     * Primarily processes CallbackQueries triggered by inline buttons in the admin panel.
     * Updates ticket status (take, close, reopen) based on administrative actions.
     *
     * @param update the incoming update from Telegram
     */
    @Override public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            String masterTag = update.getCallbackQuery().getFrom().getUserName();
            if (masterTag == null) masterTag = update.getCallbackQuery().getFrom().getFirstName();

            String[] parts = callbackData.split("_");
            String action = parts[0];
            Long ticketId = Long.parseLong(parts[1]);

            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null) return;

            switch (action) {
                case "take" -> {
                    ticket.setStatus("IN_PROGRESS");
                    ticket.setProcessedBy("@" + masterTag);
                }
                case "close" -> {
                    ticket.setStatus("CLOSED");
                    notifyClientTicketClosed(ticket);
                }
                case "reopen" -> {
                    ticket.setStatus("NEW");
                    ticket.setProcessedBy(null);
                }
            }

            ticketRepository.save(ticket);
            updateTicketMessage(chatId, messageId, ticket);
        }
    }

    /**
     * Sends a notification to the administrator group about a new service ticket.
     * Includes ticket details and control buttons for status management.
     *
     * @param ticket the Ticket object containing request details
     */
    public void notifyNewTicket(Ticket ticket) {
        SendMessage message = new SendMessage();
        message.setChatId(adminChatId);
        message.setText(formatTicketText(ticket));
        message.setParseMode("HTML");
        message.setReplyMarkup(createButtons(ticket));
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    /**
     * Updates an existing ticket message in the Telegram chat to reflect status changes.
     *
     * @param chatId the unique identifier for the chat
     * @param messageId the identifier of the message to be edited
     * @param ticket the updated Ticket object
     */
    private void updateTicketMessage(long chatId, long messageId, Ticket ticket) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId((int) messageId);
        edit.setText(formatTicketText(ticket));
        edit.setParseMode("HTML");
        edit.setReplyMarkup(createButtons(ticket));
        try { execute(edit); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    /**
     * Formats ticket information into a stylized HTML string for Telegram display.
     * Uses emojis and standard formatting for improved readability.
     *
     * @param ticket the Ticket to format
     * @return a formatted HTML string
     */
    private String formatTicketText(Ticket ticket) {
        String statusEmoji = ticket.getStatus().equals("NEW") ? "🆕" : (ticket.getStatus().equals("CLOSED") ? "✅" : "🛠");

        String worker = ticket.getProcessedBy() != null ?
                "\n<b>Майстер:</b> " + ticket.getProcessedBy() : "";

        return String.format(
                "%s <b>ЗАЯВКА №%d</b> [%s]\n" +
                        "━━━━━━━━━━━━━━\n" +
                        "📑 <b>Договір:</b> <code>%s</code>\n" +
                        "📞 <b>Контакт:</b> %s\n" +
                        "⏰ <b>Створено:</b> %s%s\n" +
                        "━━━━━━━━━━━━━━",
                statusEmoji, ticket.getId(), ticket.getStatus(),
                ticket.getContractNumber(), ticket.getContactPhone(),
                ticket.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                worker
        );
    }

    /**
     * Generates a dynamic inline keyboard based on the ticket's current status.
     * Provides relevant actions like "Take in work", "Close", or "Reopen".
     *
     * @param ticket the Ticket used to determine appropriate actions
     * @return an InlineKeyboardMarkup containing action buttons
     */
    private InlineKeyboardMarkup createButtons(Ticket ticket) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        if ("NEW".equals(ticket.getStatus())) {
            row.add(createBtn("Взяти в роботу 🛠", "take_" + ticket.getId()));
            row.add(createBtn("Відхилити ❌", "close_" + ticket.getId()));
        } else if ("IN_PROGRESS".equals(ticket.getStatus())) {
            row.add(createBtn("Закрити ✅", "close_" + ticket.getId()));
            row.add(createBtn("Повернути 🔄", "reopen_" + ticket.getId()));
        }

        rows.add(row);
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Helper method to create an InlineKeyboardButton.
     *
     * @param text label for the button
     * @param callbackData data to be sent back when the button is pressed
     * @return a new InlineKeyboardButton instance
     */
    private InlineKeyboardButton createBtn(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }

    /**
     * Sends a notification to the user via DiagnosticBot that their ticket has been closed.
     * Includes a rating system (1-5 stars) for service quality feedback.
     *
     * @param ticket the Ticket that was just closed
     */
    private void notifyClientTicketClosed(Ticket ticket) {
        if (ticket.getUserChatId() != null) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(ticket.getUserChatId()));
            message.setText("✅ **Вашу заявку №" + ticket.getId() + " виконано!**\n\n" +
                    "Будь ласка, оцініть роботу нашого майстра:");
            message.setParseMode("Markdown");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(i + " ⭐");
                btn.setCallbackData("rate_" + ticket.getId() + "_" + i);
                row.add(btn);
            }

            rows.add(row);
            markup.setKeyboard(rows);
            message.setReplyMarkup(markup);

            try {
                diagnosticBot.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends an urgent broadcast message to the administrator group Chat ID.
     *
     * @param text the message content in Markdown format
     * @param ticket associated Ticket context for providing action buttons
     */
    public void sendUrgentMessage(String text, Ticket ticket) {
        SendMessage message = new SendMessage();
        message.setChatId(adminChatId);
        message.setText(text);
        message.setParseMode("Markdown");

        message.setReplyMarkup(createButtons(ticket));

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}