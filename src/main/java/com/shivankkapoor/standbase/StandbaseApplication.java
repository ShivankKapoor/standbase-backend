package com.shivankkapoor.standbase;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StandbaseApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		System.setProperty("DB_URL", dotenv.get("DB_URL"));
		System.setProperty("DB_USER", dotenv.get("DB_USER"));
		System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
		String serverPort = dotenv.get("SERVER_PORT");
		System.setProperty("SERVER_PORT", serverPort != null ? serverPort : "8080");
		String discordWebhook = dotenv.get("DISCORD_WEBHOOK");
		if (discordWebhook != null) System.setProperty("DISCORD_WEBHOOK", discordWebhook);
		SpringApplication.run(StandbaseApplication.class, args);
	}

}
