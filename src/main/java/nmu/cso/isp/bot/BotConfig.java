package nmu.cso.isp.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {
    @Bean public TelegramBotsApi telegramBotsApi(DiagnosticBot diagnosticBot, AdminBot adminBot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);

        try {
            api.registerBot(diagnosticBot);
            api.registerBot(adminBot);
            System.out.println("Боти успішно запущені!");
        } catch (TelegramApiException e) {
            System.err.println("Помилка при реєстрації ботів: " + e.getMessage());
            throw e;
        }
        return api;
    }
}