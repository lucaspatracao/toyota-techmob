package com.toyota.techmob.backend;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling liga o ColetorPlanta (leitura da bancada a cada
 * intervalo); @ConfigurationPropertiesScan registra o PlantaProperties.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class BackEndApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackEndApplication.class, args);
	}

	/** Relógio da aplicação: os horários dos pedidos saem daqui (nos testes, um relógio fixo). */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}

}
