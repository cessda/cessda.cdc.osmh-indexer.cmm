package eu.cessda.pasc.osmhhandler.oaipmh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "eu.cessda.pasc.osmhhandler.oaipmh")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
