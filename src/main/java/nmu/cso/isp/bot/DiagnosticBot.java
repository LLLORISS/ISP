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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
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

    private final String webAppUrl = "https://isp-production-7051.up.railway.app/speedtest.html";

    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;

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
                    nmu.cso.isp.model.Ticket ticket = new nmu.cso.isp.model.Ticket();
                    ticket.setContractNumber(tempContract.get(chatId));
                    ticket.setContactPhone(text);
                    ticket.setStatus("NEW");
                    ticket.setCreatedAt(java.time.LocalDateTime.now());
                    ticket.setUserChatId(chatId);
                    ticket = ticketRepository.save(ticket);
                    adminBot.notifyNewTicket(ticket);

                    sendText(chatId, "✅ <b>Заявка №" + ticket.getId() + " створена!</b>\nОчікуйте дзвінка протягом 15-30 хвилин.");
                    userState.put(chatId, "AUTHENTICATED");
                    sendMenu(chatId, "Бажаєте зробити щось ще?");
                } else if ("AUTHENTICATED".equals(currentState)) {
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
            }
        }
    }

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