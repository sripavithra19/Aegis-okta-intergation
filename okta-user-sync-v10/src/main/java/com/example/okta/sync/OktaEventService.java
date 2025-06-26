package com.example.okta.sync;

import com.example.okta.sync.db.DatabaseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OktaEventService {
	private final String token;
	private final String orgUrl;
	private final String loadLastRunDate;

	public OktaEventService(String token, String orgUrl, String loadLastRunDate) {
		this.token = token;
		this.orgUrl = orgUrl;
		this.loadLastRunDate = loadLastRunDate;
	}

	public void fetchAndStoreEvents() {
		try {
			// Fetch only relevant events
			String filter = "eventType eq \"user.session.start\" or eventType eq \"user.session.end\" or eventType eq \"user.account.activated\" or eventType eq \"user.account.deactivated\"";
			String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
			String urlStr = orgUrl + "/api/v1/logs?filter=" + encodedFilter;
			// String sinceParam = "&since=" + java.net.URLEncoder.encode(loadLastRunDate,
			// "UTF-8");
			// String urlStr = orgUrl + "/api/v1/logs?since=" +
			// java.net.URLEncoder.encode(loadLastRunDate, "UTF-8");

			URL url = URI.create(urlStr).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "SSWS " + token);
			conn.setRequestProperty("Accept", "application/json");

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;

			while ((line = in.readLine()) != null) {
				response.append(line);
			}

			in.close();

			ObjectMapper mapper = new ObjectMapper();
			JsonNode events = mapper.readTree(response.toString());

			for (JsonNode event : events) {
				String eventId = UUID.randomUUID().toString();

				// Extract and convert timestamp
				String published = event.path("published").asText();
				Instant instant = Instant.parse(published);
				LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
				String formattedTimestamp = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

				String eventType = event.path("eventType").asText();
				String sessionId = event.path("authenticationContext").path("externalSessionId").asText();
				String actorId = event.path("actor").path("id").asText();
				String fullName = event.path("actor").path("displayName").asText();
				String email = event.path("actor").path("alternateId").asText();

				// Print to console
				System.out.println("Event ID: " + eventId);
				System.out.println("Timestamp: " + formattedTimestamp);
				System.out.println("Event Type: " + eventType);
				System.out.println("Session ID: " + sessionId);
				System.out.println("User ID: " + actorId);
				System.out.println("Name: " + fullName);
				System.out.println("Email: " + email);
				System.out.println("-------------------------------------------------");

				// Only insert if this is a login event and user doesn't exist
				if (eventType.equals("user.session.start")) {
					boolean exists = DatabaseService.userExists(email);
					if (!exists) {
						String userId = UUID.randomUUID().toString();
						DatabaseService.insertUser(userId, email, fullName, "", email);
						System.out.println("✅ New user added: " + email);
					} else {
						System.out.println("ℹ️ User already exists: " + email);
					}
				}

				// Insert the event (all types)
				DatabaseService.insertEvent(eventId, formattedTimestamp, sessionId, actorId, fullName, email,
						loadLastRunDate);
			}

		} catch (Exception e) {
			System.err.println("❌ Error fetching/storing events:");
			e.printStackTrace();
		}
	}
}