package nmu.cso.isp.bot;

import nmu.cso.isp.model.Ticket;
import nmu.cso.isp.repository.TicketRepository;
import nmu.cso.isp.service.DiagnosticService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main customer-facing Telegram bot for ISP diagnostics and support.
 * This bot handles user authentication, automated diagnostics, speed testing via Web Apps,
 * and support ticket creation. It implements a simple state machine to manage
 * conversational flows.
 *
 * @author Muts Nazar
 * @version 1.1
 */
@Component
public class DiagnosticBot extends TelegramLongPollingBot {
    private final DiagnosticService diagnosticService;
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, String> tempContract = new HashMap<>();
    private final TicketRepository ticketRepository;
    private final AdminBot adminBot;

    private final String webAppUrl = "https://isp-production-7051.up.railway.app/speedtest.html";

    @Value("${bot.name}") private String botName;
    @Value("${bot.token}") private String botToken;

    /**
     * Initializes the DiagnosticBot with required services and repositories.
     *
     * @param diagnosticService service for contract validation and technical checks
     * @param ticketRepository repository for managing customer support tickets
     * @param adminBot reference to the administrative bot for ticket notifications
     */
    public DiagnosticBot(DiagnosticService diagnosticService, TicketRepository ticketRepository, AdminBot adminBot) {
        this.diagnosticService = diagnosticService;
        this.ticketRepository = ticketRepository;
        this.adminBot = adminBot;
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    /**
     * Processes all incoming updates from Telegram.
     * Handles text messages, Web App data, and callback queries for the rating system.
     * Implements logic for states such as START_MENU, WAITING_CONTRACT, and AUTHENTICATED.
     *
     * @param update the incoming Telegram update
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            processCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();

            if (update.getMessage().getWebAppData() != null) {
                String resultData = update.getMessage().getWebAppData().getData();
                sendText(chatId, "🚀 <b>Результат тесту отримано:</b>\n" + resultData);
                return;
            }

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                String currentState = userState.getOrDefault(chatId, "START_MENU");
                String lowerText = text.toLowerCase();

                if (text.equals("/start")) {
                    userState.put(chatId, "START_MENU");
                    sendStartMenu(chatId, "🤖 <b>Вітаю у сервісному боті ISP!</b>\nЯ допоможу вам з діагностикою та інформацією за договором.");
                    return;
                }

                if ("START_MENU".equals(currentState)) {
                    if (text.contains("Знайти договір") || text.contains("Авторизація")) {
                        sendText(chatId, "🔍 Будь ласка, введіть <b>номер вашого договору</b>:");
                        userState.put(chatId, "WAITING_CONTRACT");
                    } else if (!text.contains("Тест швидкості")) {
                        sendStartMenu(chatId, "Будь ласка, оберіть один з варіантів нижче:");
                    }
                } else if ("WAITING_CONTRACT".equals(currentState)) {
                    tempContract.put(chatId, text);
                    sendText(chatId, "📱 Тепер введіть <b>номер телефону</b>, закріплений за договором (+380...):");
                    userState.put(chatId, "WAITING_PHONE");
                } else if ("WAITING_PHONE".equals(currentState)) {
                    String contract = tempContract.get(chatId);
                    if (diagnosticService.authenticate(contract, text)) {
                        userState.put(chatId, "AUTHENTICATED");
                        sendMenu(chatId, "✅ <b>Авторизація успішна!</b>\nТепер ви можете переглянути дані або залишити заявку майстру.");
                    } else {
                        sendText(chatId, "❌ Дані не збігаються. Спробуйте ще раз /start");
                        userState.remove(chatId);
                    }
                } else if ("WAITING_TICKET_PHONE".equals(currentState)) {
                    saveAndNotifyTicket(chatId, text);
                } else if ("AUTHENTICATED".equals(currentState)) {
                    handleAuthenticatedFlow(chatId, lowerText);
                }
            }
        }
    }

    /**
     * Handles user interaction after successful authentication.
     *
     * @param chatId the user's unique chat ID
     * @param lowerText the lowercase message text for command matching
     */
    private void handleAuthenticatedFlow(long chatId, String lowerText) {
        String contract = tempContract.get(chatId);
        if (lowerText.contains("інформація") || lowerText.contains("договір")) {
            sendText(chatId, diagnosticService.getContractInfo(contract));
        } else if (lowerText.contains("діагностика")) {
            sendText(chatId, diagnosticService.diagnoseCustomer(contract));
            sendSupportButton(chatId, "🛠 Якщо проблема не вирішена, ви можете викликати майстра:");
        } else if (lowerText.contains("заявку") || lowerText.contains("майстру")) {
            sendText(chatId, "📞 Введіть <b>контактний номер</b>, за яким майстер зможе вам зателефонувати:");
            userState.put(chatId, "WAITING_TICKET_PHONE");
        } else if (lowerText.contains("головне меню")) {
            sendStartMenu(chatId, "Ви повернулися до головного меню.");
        } else {
            sendMenu(chatId, "Оберіть пункт меню:");
        }
    }

    /**
     * Saves a new support ticket to the database and notifies the Admin Bot.
     *
     * @param chatId the user's chat ID
     * @param phone the contact phone number provided for the ticket
     */
    private void saveAndNotifyTicket(long chatId, String phone) {
        Ticket ticket = diagnosticService.createTicket(tempContract.get(chatId), phone, chatId);

        adminBot.notifyNewTicket(ticket);

        sendText(chatId, "✅ <b>Заявка №" + ticket.getId() + " створена!</b>\nОчікуйте дзвінка протягом 15-30 хвилин.");
        userState.put(chatId, "AUTHENTICATED");
        sendMenu(chatId, "Бажаєте зробити щось ще?");
    }

    /**
     * Sends a plain text message to the specified chat with HTML formatting support.
     *
     * @param chatId the recipient's chat ID
     * @param text the message content
     */
    private void sendText(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends the main menu with persistent reply keyboard buttons for authenticated users.
     *
     * @param chatId the recipient's chat ID
     * @param text the message content
     */
    private void sendMenu(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("📄 Інформація");
        row.add("🛠 Діагностика");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a support-focused keyboard allowing users to either create a ticket or return to menu.
     *
     * @param chatId the recipient's chat ID
     * @param text the message content
     */
    private void sendSupportButton(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("🆘 Залишити заявку");
        row.add("🏠 Головне меню");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes callback queries from inline buttons, such as service rating stars.
     *
     * @param callbackQuery the callback query to process
     */
    private void processCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("rate_")) {
            String[] parts = data.split("_");
            Long ticketId = Long.parseLong(parts[1]);
            Integer score = Integer.parseInt(parts[2]);

            ticketRepository.findById(ticketId).ifPresent(ticket -> {
                ticket.setRating(score);
                ticketRepository.save(ticket);
            });

            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText("⭐⭐⭐⭐⭐\nДякуємо за оцінку! Ви поставили: " + score + " ⭐\nВаша думка важлива для нас.");

            try {
                execute(edit);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends the initial start menu featuring the Web App speed test button.
     *
     * @param chatId the recipient's chat ID
     * @param text the message content
     */
    private void sendStartMenu(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        KeyboardButton speedTestBtn = new KeyboardButton("🚀 Тест швидкості");
        speedTestBtn.setWebApp(new WebAppInfo(webAppUrl));

        row.add(speedTestBtn);
        row.add(new KeyboardButton("🔍 Знайти договір"));

        keyboard.add(row);
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}