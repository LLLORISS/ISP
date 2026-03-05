package nmu.cso.isp.service;

import nmu.cso.isp.model.Customer;
import nmu.cso.isp.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data seeding component that populates the database with initial testing records.
 * This class implements {@link CommandLineRunner}, ensuring it executes immediately
 * after the Spring ApplicationContext is fully initialized.
 * * It is primarily used during the development phase to provide a consistent
 * set of mock customers for authentication and diagnostic testing.
 *
 * @author Muts Nazar
 * @version 1.0
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private final CustomerRepository customerRepository;

    /**
     * Constructs the initializer with required repository access.
     *
     * @param customerRepository the repository used to persist initial customer data
     */
    public DataInitializer(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Executes the data seeding logic.
     * It clears the existing customer table and inserts predefined mock records,
     * including technical parameters like Switch IP, Port numbers, and financial balances.
     *
     * @param args command line arguments passed to the application (not used)
     */
    @Override public void run(String... args) {
        customerRepository.deleteAll();

        Customer c1 = new Customer();
        c1.setFullName("Муц Назар");
        c1.setContractNumber("101");
        c1.setPhoneNumber("+380983958180");
        c1.setResidence("Золочів");
        c1.setSwitchIp("192.168.10.5");
        c1.setPortNumber(12);
        c1.setBalance(150.0);
        customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setFullName("Тестовий Користувач");
        c2.setContractNumber("228");
        c2.setPhoneNumber("+380507654321");
        c2.setResidence("Тернопіль");
        c2.setSwitchIp("192.168.10.8");
        c2.setPortNumber(3);
        c2.setBalance(-50.0);
        customerRepository.save(c2);

        System.out.println("База даних ініціалізована тестовими даними!");
    }
}