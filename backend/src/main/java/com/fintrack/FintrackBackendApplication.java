package com.fintrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FintrackBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FintrackBackendApplication.class, args);
	}

}
