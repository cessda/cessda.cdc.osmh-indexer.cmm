package eu.cessda.pasc.oci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableMBeanExport;

@SpringBootApplication
@EnableMBeanExport
public class OCIApplication {

	public static void main(String[] args) {
		SpringApplication.run(OCIApplication.class, args);
	}
}
