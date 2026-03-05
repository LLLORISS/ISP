package nmu.cso.isp.bot;

import nmu.cso.isp.service.DiagnosticService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiagnosticBot extends TelegramLongPollingBot {
    private final DiagnosticService diagnosticService;
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, String> tempContract = new HashMap<>();

    @Value("${bot.name}")  private String botName;
    @Value("${bot.token}") private String botToken;

    public DiagnosticBot(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @Override public String getBotUsername() { return this.botName; }
    @Override public String getBotToken() { return this.botToken; }

    @Override public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if(text.equals("/start")) {
                sendText(chatId, "🤖 Вітаю! Я бот ISP для діагностики. \nЯ допоможу перевірити статус вашого інтернету та надати інформацію за договором.");
                sendText(chatId, "Почнемо авторизацію. Введіть номер вашого договору:");
                userState.put(chatId, "WAITING_CONTRACT");
            } else if ("WAITING_CONTRACT".equals(userState.get(chatId))) {
                tempContract.put(chatId, text);
                sendText(chatId, "Тепер введіть номер телефону, закріплений за цим договором (у форматі +380...):");
                userState.put(chatId, "WAITING_PHONE");
            } else if("WAITING_PHONE".equals(userState.get(chatId))) {
                String contract = tempContract.get(chatId);
                boolean isAuthenticated = diagnosticService.authenticate(contract, text);

                if(isAuthenticated) {
                    userState.put(chatId, "AUTHENTICATED");
                    sendMenu(chatId, "✅ Авторизація успішна! Оберіть дію:");
                } else {
                    sendText(chatId, "❌ Дані не збігаються. Спробуйте ще раз /start");
                    userState.remove(chatId);
                }
            } else if("AUTHENTICATED".equals(userState.get(chatId))) {
                String contract = tempContract.get(chatId);
                String lowerText = text.toLowerCase();

                if (lowerText.contains("інформація") || lowerText.contains("договір")) {
                    sendText(chatId, diagnosticService.getContractInfo(contract));
                } else if (lowerText.contains("діагностика")) {
                    sendText(chatId, diagnosticService.diagnoseCustomer(contract));
                } else {
                    sendMenu(chatId, "Будь ласка, оберіть пункт меню:");
                }
            }
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

    private void sendMenu(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("1. Інформація про договір");
        row.add("2. Діагностика");
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
}
