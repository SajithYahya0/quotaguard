package com.sajith.quotagaurd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class QuotagaurdApplication {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	public static void main(String[] args) {

		SpringApplication.run(QuotagaurdApplication.class, args);
	}
}