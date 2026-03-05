package nmu.cso.isp.bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {
    @Bean public TelegramBotsApi telegramBotsApi(DiagnosticBot diagnosticBot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        diagnosticBot.execute(new org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook(true));
        api.registerBot(diagnosticBot);
        System.out.println("Бот запущений!");
        return api;
    }
}

