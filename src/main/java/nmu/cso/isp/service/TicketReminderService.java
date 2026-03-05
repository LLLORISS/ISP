package nmu.cso.isp.service;

import nmu.cso.isp.bot.AdminBot;
import nmu.cso.isp.model.Ticket;
import nmu.cso.isp.repository.TicketRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketReminderService {

    private final TicketRepository ticketRepository;
    private final AdminBot adminBot;

    public TicketReminderService(TicketRepository ticketRepository, AdminBot adminBot) {
        this.ticketRepository = ticketRepository;
        this.adminBot = adminBot;
    }

    @Scheduled(fixedRate = 60000)
    public void remindAboutOldTickets() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<Ticket> overdueTickets = ticketRepository.findByStatusAndCreatedAtBefore("NEW", threshold);

        if (!overdueTickets.isEmpty()) {
            for (Ticket ticket : overdueTickets) {
                String reminderText = String.format(
                        "🚨 **КРИТИЧНЕ НАГАДУВАННЯ: ЗАЯВКА №%d**\n" +
                                "━━━━━━━━━━━━━━\n" +
                                "⚠️ Ця заявка очікує вже понад 10 хвилин!\n" +
                                "📑 Договір: `%s`\n" +
                                "📞 Контакт: %s\n" +
                                "━━━━━━━━━━━━━━\n" +
                                "Майстри, візьміть у роботу негайно:",
                        ticket.getId(),
                        ticket.getContractNumber(),
                        ticket.getContactPhone()
                );

                adminBot.sendUrgentMessage(reminderText, ticket);
            }
        }
    }
}