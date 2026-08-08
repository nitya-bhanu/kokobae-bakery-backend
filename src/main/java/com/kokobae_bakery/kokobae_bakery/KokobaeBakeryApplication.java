package com.kokobae_bakery.kokobae_bakery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KokobaeBakeryApplication {

	public static void main(String[] args) {
		SpringApplication.run(KokobaeBakeryApplication.class, args);
	}
}
