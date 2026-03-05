package nmu.cso.isp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "nmu.cso.isp")
public class IspApplication {

	public static void main(String[] args) {
		SpringApplication.run(IspApplication.class, args);
	}

}
