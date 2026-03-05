package nmu.cso.isp.service;

import nmu.cso.isp.model.Customer;
import nmu.cso.isp.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class DiagnosticService {
    private final CustomerRepository customerRepository;

    public DiagnosticService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public String checkStatus(String contractNumber) {
        Customer customer = customerRepository.findByContractNumber(contractNumber);

        if(customer == null) {
            return "Помилка: Клієнта з номером " + contractNumber + " не знайдено";
        }

        if (customer.getBalance() < 0) {
            return "Статус: Заблоковано. Баланс: " + customer.getBalance() + " грн. Будь ласка, поповніть рахунок.";
        }

        return simulateNetworkCheck(customer);
    }

    private String simulateNetworkCheck(Customer customer) {
        boolean isUp = Math.random() > 0.2;

        if (isUp) {
            return "Статус: Онлайн. Обладнання за адресою " + customer.getSwitchIp() + " працює стабільно. Порт " + customer.getPortNumber() + " активний.";
        } else {
            return "Статус: Офлайн. Сигнал на порту " + customer.getPortNumber() + " відсутній. Можливий обрив кабелю.";
        }
    }
}
