package nmu.cso.isp.bot;

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
    private final TicketRepository ticketRepository;
    private final AdminBot adminBot;

    @Value("${bot.name}")  private String botName;
    @Value("${bot.token}") private String botToken;

    public DiagnosticBot(DiagnosticService diagnosticService, TicketRepository ticketRepository, AdminBot adminBot) {
        this.diagnosticService = diagnosticService;
        this.ticketRepository = ticketRepository;
        this.adminBot = adminBot;
    }

    @Override public String getBotUsername() { return this.botName; }
    @Override public String getBotToken() { return this.botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            processCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String currentState = userState.getOrDefault(chatId, "");

            if (text.equals("/start")) {
                sendText(chatId, "🤖 Вітаю! Я бот ISP для діагностики. \nЯ допоможу перевірити статус вашого інтернету та надати інформацію за договором.");
                sendText(chatId, "Почнемо авторизацію. Введіть номер вашого договору:");
                userState.put(chatId, "WAITING_CONTRACT");
            }
            else if ("WAITING_CONTRACT".equals(currentState)) {
                tempContract.put(chatId, text);
                sendText(chatId, "Тепер введіть номер телефону, закріплений за цим договором (у форматі +380...):");
                userState.put(chatId, "WAITING_PHONE");
            }
            else if ("WAITING_PHONE".equals(currentState)) {
                String contract = tempContract.get(chatId);
                boolean isAuthenticated = diagnosticService.authenticate(contract, text);

                if (isAuthenticated) {
                    userState.put(chatId, "AUTHENTICATED");
                    sendMenu(chatId, "✅ Авторизація успішна! Оберіть дію:");
                } else {
                    sendText(chatId, "❌ Дані не збігаються. Спробуйте ще раз /start");
                    userState.remove(chatId);
                }
            }
            else if ("WAITING_TICKET_PHONE".equals(currentState)) {
                String contract = tempContract.get(chatId);
                String phoneForMaster = text;

                nmu.cso.isp.model.Ticket ticket = new nmu.cso.isp.model.Ticket();
                ticket.setContractNumber(contract);
                ticket.setContactPhone(phoneForMaster);
                ticket.setStatus("NEW");
                ticket.setCreatedAt(java.time.LocalDateTime.now());
                ticket.setUserChatId(chatId);

                ticket = ticketRepository.save(ticket);
                adminBot.notifyNewTicket(ticket);

                sendText(chatId, "✅ Заявка №" + ticket.getId() + " успішно створена!");
                sendText(chatId, "⏳ Очікуйте дзвінка на номер " + phoneForMaster + " протягом декількох хвилин.");

                userState.put(chatId, "AUTHENTICATED");
                sendMenu(chatId, "Оберіть наступну дію:");
            }
            else if ("AUTHENTICATED".equals(currentState)) {
                String contract = tempContract.get(chatId);
                String lowerText = text.toLowerCase();

                if (lowerText.contains("інформація") || lowerText.contains("договір")) {
                    sendText(chatId, diagnosticService.getContractInfo(contract));
                } else if (lowerText.contains("діагностика")) {
                    sendText(chatId, diagnosticService.diagnoseCustomer(contract));
                    sendSupportButton(chatId, "🛠 Якщо проблема не вирішена, натисніть кнопку нижче:");
                } else if (lowerText.contains("заявку") || lowerText.contains("майстру")) {
                    sendText(chatId, "📞 Будь ласка, введіть контактний номер телефону, за яким служба підтримки зможе з вами зв'язатися:");
                    userState.put(chatId, "WAITING_TICKET_PHONE");
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

    private void sendSupportButton(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("🆘 Залишити заявку");
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
}
