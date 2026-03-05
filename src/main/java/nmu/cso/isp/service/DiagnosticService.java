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

    public boolean authenticate(String contract, String phone) {
        Customer customer = customerRepository.findByContractNumber(contract);
        return customer != null && customer.getPhoneNumber().equals(phone);
    }

    public String diagnoseCustomer(String contractNumber) {
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

    public String getContractInfo(String contractNumber) {
        Customer customer = customerRepository.findByContractNumber(contractNumber);
        if (customer == null) return "Помилка: Дані втрачено. Спробуйте /start";

        return String.format(
                "📋 *Інформація про договір*:\n\n" +
                        "👤 Клієнт: %s\n" +
                        "📑 Договір №: %s\n" +
                        "📞 Телефон: %s\n" +
                        "🏠 Адреса: %s\n" +
                        "💰 Баланс: %.2f грн",
                customer.getFullName(),
                customer.getContractNumber(),
                customer.getPhoneNumber(),
                customer.getResidence(),
                customer.getBalance()
        );
    }
}
