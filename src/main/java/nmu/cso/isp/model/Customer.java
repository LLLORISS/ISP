package nmu.cso.isp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "customers")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String contractNumber;
    private String switchIp;
    private Integer portNumber;
    private Double balance;
    private String residence;
}
