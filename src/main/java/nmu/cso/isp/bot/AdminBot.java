package nmu.cso.isp.bot;

import nmu.cso.isp.model.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class AdminBot extends TelegramLongPollingBot {
    @Value("${admin.bot.name}")
    private String botName;

    @Value("${admin.bot.token}")
    private String botToken;

    @Value("${admin.chat.id}")
    private String adminChatId;

    @Override
    public String getBotUsername() {
        return this.botName;
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getChatId());
    }

    public void notifyNewTicket(Ticket ticket) {
        String text = String.format(
                "🆕 **НОВА ЗАЯВКА №%d**\n" +
                        "━━━━━━━━━━━━━━\n" +
                        "📑 **Договір:** `%s`\n" +
                        "📞 **Контакт:** %s\n" +
                        "⏰ **Час:** %s\n" +
                        "━━━━━━━━━━━━━━",
                ticket.getId(),
                ticket.getContractNumber(),
                ticket.getContactPhone(),
                ticket.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        );

        SendMessage message = new SendMessage();
        message.setChatId(adminChatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
