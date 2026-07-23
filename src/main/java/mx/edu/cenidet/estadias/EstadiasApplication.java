package mx.edu.cenidet.estadias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EstadiasApplication {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "true");
		SpringApplication.run(EstadiasApplication.class, args);
	}

}
