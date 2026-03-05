package nmu.cso.isp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence entity representing a support ticket created by a customer.
 * This class tracks the lifecycle of a service request, from initial creation
 * in the Diagnostic Bot to resolution and final rating by the user.
 * * @author Muts Nazar
 * @version 1.1
 */
@Entity
@Table(name="tickets")
@Data
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contractNumber;
    private String contactPhone;
    private String status;
    private LocalDateTime createdAt;
    private String processedBy;
    private Long userChatId;
    private Integer rating;
}
