package nmu.cso.isp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The main entry point for the ISP Service Management Application.
 * This class initializes the Spring Boot framework, configures component scanning,
 * and enables background task scheduling for automated diagnostics and reminders.
 *
 *
 * @author Muts Nazar
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "nmu.cso.isp")
public class IspApplication {
    /**
     * Main method that serves as the entry point for the Java application.
     * It delegates the execution to Spring Boot's {@link SpringApplication#run} method
     * to launch the integrated web server and initialize the ApplicationContext.
     *
     * @param args command-line arguments passed to the application
     */
	public static void main(String[] args) {
		SpringApplication.run(IspApplication.class, args);
	}

}
