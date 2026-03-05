package nmu.cso.isp.service;

import nmu.cso.isp.bot.AdminBot;
import nmu.cso.isp.model.Ticket;
import nmu.cso.isp.repository.TicketRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background service responsible for monitoring and alerting about overdue support tickets.
 * This service implements a scheduling mechanism to ensure that no customer request
 * remains unaddressed for an extended period, maintaining the Service Level Agreement (SLA).
 *
 * @author Muts Nazar
 * @version 1.0
 */
@Service
public class TicketReminderService {

    private final TicketRepository ticketRepository;
    private final AdminBot adminBot;

    /**
     * Constructs the reminder service with required dependencies for ticket lookup
     * and administrative notifications.
     *
     * @param ticketRepository repository to query pending tickets
     * @param adminBot the administrative bot used to send urgent alerts
     */
    public TicketReminderService(TicketRepository ticketRepository, AdminBot adminBot) {
        this.ticketRepository = ticketRepository;
        this.adminBot = adminBot;
    }

    /**
     * Scheduled task that runs every 60 seconds (1 minute) to check for "stale" tickets.
     * A ticket is considered overdue if its status is "NEW" and it was created
     * more than 10 minutes ago.
     * *
     *
     * When overdue tickets are identified, the service triggers an urgent broadcast
     * message to the Admin Bot group to alert the technical staff.
     */
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