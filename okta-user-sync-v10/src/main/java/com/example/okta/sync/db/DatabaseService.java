package com.example.okta.sync.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseService {

	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/aegis_db";
	private static final String USER = "root";
	private static final String PASS = "system";

	// ✅ Insert user into DPS_USER table
	public static void insertUser(String id, String login, String firstName, String lastName, String email) {
		String sql = "INSERT IGNORE INTO DPS_USER (ID, LOGIN, FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, id);
			stmt.setString(2, login);
			stmt.setString(3, firstName);
			stmt.setString(4, lastName);
			stmt.setString(5, email);
			stmt.executeUpdate();

			System.out.println("✅ User inserted: " + login);

		} catch (SQLException e) {
			System.err.println("❌ Failed to insert user: " + login);
			e.printStackTrace();
		}
	}

	// ✅ Insert event into DSS_DPS_EVENT table
	// ✅ Insert event into DSS_DPS_EVENT table only if timestamp > 2025-06-23 and <=
	// current time
	public static void insertEvent(String id, String timestamp, String sessionId, String profileId, String name,
			String email, String loadLastRunDate) {
		String sql = "INSERT INTO DSS_DPS_EVENT (ID, TIMESTAMP, SESSIONID, PROFILEID, NAME, EMAIL) "
				+ "SELECT ?, ?, ?, ?, ?, ? " + "WHERE ? > ? AND ? <= NOW()";

		try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			// Bind insert values
			stmt.setString(1, id);
			stmt.setString(2, timestamp);
			stmt.setString(3, sessionId);
			stmt.setString(4, profileId);
			stmt.setString(5, name);
			stmt.setString(6, email);

			// Bind date conditions
			stmt.setString(7, timestamp);
			stmt.setString(8, loadLastRunDate);
			stmt.setString(9, timestamp);

			int rowsInserted = stmt.executeUpdate();
			if (rowsInserted > 0) {
				System.out.println("✅ Event inserted: " + id);
			} else {
				System.out.println("⏩ Event skipped (timestamp outside range): " + id);
			}

		} catch (SQLException e) {
			System.err.println("❌ Failed to insert event: " + id);
			e.printStackTrace();
		}
	}

	// ✅ Check if a user with the given login already exists
	public static boolean userExists(String login) {
		String sql = "SELECT 1 FROM DPS_USER WHERE LOGIN = ?";

		try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, login);
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next(); // true if user exists
			}

		} catch (SQLException e) {
			System.err.println("❌ Failed to check user existence: " + login);
			e.printStackTrace();
			return false;
		}
	}
}
