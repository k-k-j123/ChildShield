package com.kaushik.ChildShield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChildShieldApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChildShieldApplication.class, args);
	}

}
