package nmu.cso.isp.repository;

import nmu.cso.isp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Customer} entities.
 * This interface abstracts the underlying data access layer using Spring Data JPA,
 * providing standard CRUD operations and custom lookup methods for ISP subscribers.
 *
 * @author Muts Nazar
 * @version 1.0
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    /**
     * Retrieves a customer record based on their unique contract number.
     * This method is a key part of the authentication and diagnostic workflow,
     * allowing the system to link a Telegram user to a specific network profile.
     *
     * @param contractNumber the unique string identifier of the customer's contract
     * @return the {@link Customer} entity if found, or null otherwise
     */
    Customer findByContractNumber(String contractNumber);
}
