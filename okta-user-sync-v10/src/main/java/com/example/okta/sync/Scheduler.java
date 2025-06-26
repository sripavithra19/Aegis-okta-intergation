package com.example.okta.sync;

import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

	public static void main(String[] args) {
		try {
			// ✅ Load Okta properties
			Properties props = new Properties();
			InputStream input = Scheduler.class.getClassLoader().getResourceAsStream("application.properties");

			if (input == null) {
				throw new RuntimeException("❌ 'okta.properties' file not found in classpath!");
			}

			props.load(input);

			String token = props.getProperty("okta.client.token");
			String orgUrl = props.getProperty("okta.client.orgUrl");
			String loadLastRunDate = loadLastRunDate();

			if (token == null || orgUrl == null) {
				throw new RuntimeException("❌ 'token' or 'orgUrl' missing in okta.properties");
			}

			// ✅ Build Okta Client
			Client client = Clients.builder().setOrgUrl(orgUrl).setClientCredentials(() -> token).build();

			// ✅ Services
			OktaUserService userService = new OktaUserService(client);
			OktaEventService eventService = new OktaEventService(token, orgUrl, loadLastRunDate);

			// ✅ Schedule every 2 minutes
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

			scheduler.scheduleAtFixedRate(() -> {
				System.out.println("🔁 Sync started at: " + java.time.LocalDateTime.now());

				try {
					userService.syncUsersToDatabase(); // Insert users
					eventService.fetchAndStoreEvents(); // Print + Insert events

					System.out.println("✅ Sync completed.\n");

				} catch (Exception e) {
					System.err.println("❌ Sync failed:");
					e.printStackTrace();
				}

			}, 0, 2, TimeUnit.MINUTES);

		} catch (Exception e) {
			System.err.println("❌ Scheduler setup failed:");
			e.printStackTrace();
		}
	}

	public static String loadLastRunDate() throws IOException {
		Properties props = new Properties();
		try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {
			props.load(input);
		}
		return props.getProperty("okta.client.lastRunDate");
	}

}