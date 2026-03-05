package nmu.cso.isp.service;

import nmu.cso.isp.model.Customer;
import nmu.cso.isp.model.Ticket;
import nmu.cso.isp.repository.CustomerRepository;
import nmu.cso.isp.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class DiagnosticService {
    private final CustomerRepository customerRepository;
    private final Random random = new Random();
    private TicketRepository ticketRepository;

    public DiagnosticService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public boolean authenticate(String contract, String phone) {
        Customer customer = customerRepository.findByContractNumber(contract);
        return customer != null && customer.getPhoneNumber().equals(phone);
    }

    public String diagnoseCustomer(String contractNumber) {
        Customer customer = customerRepository.findByContractNumber(contractNumber);
        if (customer == null) return "❌ Помилка: Клієнта не знайдено.";

        StringBuilder report = new StringBuilder();
        report.append("🔍 **Запущено глибоку діагностику лінії...**\n\n");

        if (!checkBalance(customer)) {
            report.append("🛑 **Зупинено:** Доступ обмежено через заборгованість (")
                    .append(customer.getBalance()).append(" грн).\n")
                    .append("💳 Будь ласка, поповніть рахунок для відновлення послуги.");
            return report.toString();
        }
        report.append("✅ Білінг: Доступ дозволено.\n");

        if (!checkPhysicalLink()) {
            report.append("❌ **Порт:** Статус Down. Сигнал на порту ")
                    .append(customer.getPortNumber()).append(" відсутній.\n")
                    .append("⚠️ Можливий обрив кабелю або вимкнення живлення роутера.");
            return report.toString();
        }
        report.append("✅ Лінк: Активний (1000 Mbps).\n");

        double signal = generateSignalLevel();
        report.append("📡 Рівень сигналу: ").append(String.format("%.2f", signal)).append(" дБм\n");
        if (signal < -27.0) {
            report.append("⚠️ Попередження: Критично низький рівень сигналу!\n");
        }

        report.append("🌐 IP-адреса: ").append(customer.getSwitchIp()).append("\n\n");
        report.append("🚀 **Висновок:** Проблем на стороні провайдера не виявлено. Мережа працює стабільно. \nЯкщо у вас все ще залишились питання по роботі інтернету ви можете зв'язатися з нашою технічною підтримкою по номеру +380123456789 або залишити заявку на дзвінок від нас");

        return report.toString();
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

    private boolean checkBalance(Customer customer) {
        return customer.getBalance() > 0;
    }

    private boolean checkPhysicalLink() {
        return random.nextDouble() > 0.15;
    }

    private double generateSignalLevel() {
        // Нормальний сигнал для PON: -18 до -25. Поганий: нижче -27.
        return -18.0 - (random.nextDouble() * 12.0);
    }

    public String createTicket(String contract, String phone) {
        Ticket ticket = new Ticket();
        ticket.setContractNumber(contract);
        ticket.setContactPhone(phone);
        ticket.setStatus("NEW");
        ticket.setCreatedAt(LocalDateTime.now());

        ticketRepository.save(ticket);

        return "TK-" + ticket.getId();
    }
}
