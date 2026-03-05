package nmu.cso.isp.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Configuration class for initializing and registering Telegram bots within the Spring context.
 * This class ensures that both the Diagnostic and Administrative bots are correctly
 * connected to the Telegram API upon application startup.
 * * @author Muts Nazar
 * @version 1.0
 */
@Configuration
public class BotConfig {
    /**
     * Creates and configures the TelegramBotsApi bean.
     * This method handles the registration of multiple bot instances (Diagnostic and Admin)
     * using a long-polling session.
     *
     * @param diagnosticBot the customer-facing diagnostic bot instance
     * @param adminBot the internal administrative bot instance
     * @return a configured TelegramBotsApi instance
     * @throws TelegramApiException if an error occurs during bot registration or session initialization
     */
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