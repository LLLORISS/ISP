package nmu.cso.isp.repository;

import nmu.cso.isp.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
}