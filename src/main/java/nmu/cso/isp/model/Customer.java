package nmu.cso.isp.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Persistence entity representing a broadband customer within the ISP system.
 * This class maps to the "customers" table and stores essential administrative
 * and technical data required for network diagnostics and billing.
 * * @author Muts Nazar
 * @version 1.0
 */
@Entity
@Table(name = "customers")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String contractNumber;
    private String phoneNumber; // +38000000000
    private String switchIp;
    private Integer portNumber;
    private Double balance;
    private String residence;
}
