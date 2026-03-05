package nmu.cso.isp.repository;

import nmu.cso.isp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByContractNumber(String contractNumber);
}
