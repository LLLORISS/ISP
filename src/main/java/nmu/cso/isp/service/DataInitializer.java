package nmu.cso.isp.service;

import nmu.cso.isp.model.Customer;
import nmu.cso.isp.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final CustomerRepository customerRepository;

    public DataInitializer(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override public void run(String... args) {
        Customer c1 = new Customer();
        c1.setFullName("Муц Назар");
        c1.setContractNumber("101");
        c1.setResidence("Золочів");
        c1.setSwitchIp("192.168.10.5");
        c1.setPortNumber(12);
        c1.setBalance(150.0);
        customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setFullName("Тестовий Користувач");
        c2.setContractNumber("228");
        c2.setResidence("Тернопіль");
        c2.setSwitchIp("192.168.10.8");
        c2.setPortNumber(3);
        c2.setBalance(-50.0);
        customerRepository.save(c2);

        System.out.println("База даних ініціалізована тестовими даними!");
    }
}
