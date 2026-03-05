package nmu.cso.isp.repository;

import nmu.cso.isp.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer interface for {@link Ticket} entities.
 * This repository manages the lifecycle of support requests, enabling the
 * administrative bot to query and update ticket information in the database.
 *
 * @author Muts Nazar
 * @version 1.0
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    /**
     * Finds tickets that match a specific status and were created before a given timestamp.
     * This method is primarily used for identifying overdue or "stale" tickets that
     * require immediate administrative attention (SLA monitoring).
     *
     * @param status the current processing status of the ticket (e.g., "NEW", "IN_PROGRESS")
     * @param dateTime the cutoff point for ticket creation time
     * @return a list of {@link Ticket} entities that satisfy both criteria
     */
    List<Ticket> findByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
}