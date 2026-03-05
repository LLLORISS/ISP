package nmu.cso.isp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name="tickets")
@Data
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contractNumber;
    private String contactPhone;
    private String status; // "NEW", "IN_PROGRESS", "CLOSED"
    private LocalDateTime createdAt;
    private String processedBy;
    private Long userChatId;
}
