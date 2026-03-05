package nmu.cso.isp.bot;

import nmu.cso.isp.service.DiagnosticService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class DiagnosticBot extends TelegramLongPollingBot {
    private final DiagnosticService diagnosticService;


    @Value("${bot.name}")  private String botName;
    @Value("${bot.token}") private String botToken;

    public DiagnosticBot(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @Override public String getBotUsername() { return this.botName; }
    @Override public String getBotToken() { return this.botToken; }

    @Override public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String response;
            if(messageText.equals("/start")) {
                response = "🤖 ISP Analyzator готовий! Введіть номер договору:";
            } else {
                response = diagnosticService.diagnoseCustomer(messageText);
            }
            sendText(chatId, response);
        }

    }

    private void sendText(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
