package com.likelion.a1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class A1BackApplication {

	public static void main(String[] args) {
		SpringApplication.run(A1BackApplication.class, args);
	}

}
