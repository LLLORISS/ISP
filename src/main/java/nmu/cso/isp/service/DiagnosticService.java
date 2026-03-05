package nmu.cso.isp.service;

import nmu.cso.isp.model.Customer;
import nmu.cso.isp.model.Ticket;
import nmu.cso.isp.repository.CustomerRepository;
import nmu.cso.isp.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Core service responsible for executing network diagnostics and customer authentication.
 * This class orchestrates the technical logic for verifying billing status,
 * physical link availability, and optical signal levels (simulated for PON/FTTH).
 * * It serves as the primary bridge between the Telegram bot interfaces and
 * the customer data layer.
 *
 * @author Muts Nazar
 * @version 1.0
 */
@Service
public class DiagnosticService {
    private final CustomerRepository customerRepository;
    private final Random random = new Random();
    private final TicketRepository ticketRepository;

    /**
     * Constructs the diagnostic service with the required customer repository.
     *
     * @param customerRepository the repository for accessing subscriber data
     * @param ticketRepository the repository for accessing tickets data
     */
    public DiagnosticService(CustomerRepository customerRepository, TicketRepository ticketRepository) {
        this.customerRepository = customerRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Validates a customer's credentials based on their contract number and phone.
     *
     * @param contract the unique contract number string
     * @param phone the contact phone number registered in the system
     * @return true if credentials match a record in the database, false otherwise
     */
    public boolean authenticate(String contract, String phone) {
        Customer customer = customerRepository.findByContractNumber(contract);
        return customer != null && customer.getPhoneNumber().equals(phone);
    }

    /**
     * Performs a comprehensive multi-stage diagnostic check for a specific customer.
     * The process includes:
     * 1. Billing check (positive balance verification).
     * 2. Physical link simulation (UP/DOWN status).
     * 3. Optical signal level generation (dBm).
     * *
     *
     * @param contractNumber the contract number to diagnose
     * @return a detailed HTML-formatted diagnostic report for the user
     */
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
        report.append("🚀 **Висновок:** Проблем на стороні провайдера не виявлено. Мережа працює стабільно. \nЯкщо у вас залишились питання по роботі інтернету ви можете зв'язатися з нашою технічною підтримкою по номеру +380123456789 або залишити заявку на дзвінок від нас");

        return report.toString();
    }

    /**
     * Generates a formatted summary of the customer's contract details.
     *
     * @param contractNumber the identifier for the lookup
     * @return a formatted Markdown/HTML string with customer information
     */
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

    /**
     * Internal check to verify if the account has a positive balance.
     *
     * @param customer the customer entity to check
     * @return true if balance > 0
     */
    private boolean checkBalance(Customer customer) {
        return customer.getBalance() > 0;
    }

    /**
     * Simulates a check of the physical link status on the access switch.
     * Uses a probabilistic model where there is a 15% chance of a "Link Down" status.
     *
     * @return true if the link is simulated as active
     */
    private boolean checkPhysicalLink() {
        return random.nextDouble() > 0.15;
    }

    /**
     * Generates a realistic optical signal level for PON networks.
     * Typical range for healthy signal: -18.0 to -25.0 dBm.
     * Values below -27.0 dBm are considered critical.
     *
     * @return a simulated signal level in dBm
     */
    private double generateSignalLevel() {
        // Нормальний сигнал для PON: -18 до -25. Поганий: нижче -27.
        return -18.0 - (random.nextDouble() * 12.0);
    }

    /**
     * Logic for creating a new support ticket in the database.
     *
     * @param contract associated contract number
     * @param phone validated contact phone number
     * @return a formatted ticket identifier (e.g., TK-123)
     */
    public Ticket createTicket(String contract, String phone, Long chatId) {
        Ticket ticket = new Ticket();
        ticket.setContractNumber(contract);
        ticket.setContactPhone(phone);
        ticket.setUserChatId(chatId);
        ticket.setStatus("NEW");
        ticket.setCreatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }
}
