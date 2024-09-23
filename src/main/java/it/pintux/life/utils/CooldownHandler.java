package it.pintux.life.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class CooldownHandler {

    private final DatabaseManager databaseManager;
    private final String tableName = "cooldowns";
    private HikariDataSource dataSource;

    public CooldownHandler(JavaPlugin plugin) {
        databaseManager = new DatabaseManager(plugin);
        setupDatabase();
    }

    private void setupDatabase() {
        try {
            if (dataSource != null) closeConnection();
            databaseManager.setup();
            dataSource = databaseManager.getDataSource();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dataSource != null) {
            createTable();
        }
    }

    private void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                                + "player_name VARCHAR(36) PRIMARY KEY, "
                                + "capture_cooldown BIGINT NOT NULL, "
                                + "place_cooldown BIGINT NOT NULL, "
                                + "capture_count INT NOT NULL DEFAULT 0, "
                                + "place_count INT NOT NULL DEFAULT 0)";
        try (Statement stmt = dataSource.getConnection().createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCooldown(String playerName, long captureCooldownMillis, long placeCooldownMillis) {
        if (getDataSource() == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long captureCooldownEndTime = currentTime + captureCooldownMillis;
        long placeCooldownEndTime = currentTime + placeCooldownMillis;

        String sql = "INSERT INTO " + tableName + " (player_name, capture_cooldown, place_cooldown) "
                     + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE capture_cooldown=?, place_cooldown=?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setLong(2, captureCooldownEndTime);
            pstmt.setLong(3, placeCooldownEndTime);
            pstmt.setLong(4, captureCooldownEndTime);
            pstmt.setLong(5, placeCooldownEndTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getCooldown(String playerName, String type) {
        if (getDataSource() == null) {
            return -1;
        }
        String sql = "SELECT capture_cooldown, place_cooldown FROM " + tableName + " WHERE player_name = ?";
        try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                long currentTime = System.currentTimeMillis();
                if ("capture".equals(type)) {
                    long captureCooldownEndTime = rs.getLong("capture_cooldown");
                    if (captureCooldownEndTime > currentTime) {
                        return captureCooldownEndTime - currentTime;
                    }
                } else if ("place".equals(type)) {
                    long placeCooldownEndTime = rs.getLong("place_cooldown");
                    if (placeCooldownEndTime > currentTime) {
                        return placeCooldownEndTime - currentTime;
                    }
                }
            }
            removeCooldown(playerName, type);
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void removeCooldown(String playerName, String type) {
        if (getDataSource() == null) {
            return;
        }
        String sql = "UPDATE " + tableName + " SET " + type + "_cooldown = 0 WHERE player_name = ?";
        try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementCaptureCount(String playerName) {
        if (getDataSource() == null) {
            return;
        }
        String sql = "UPDATE " + tableName + " SET capture_count = capture_count + 1 WHERE player_name = ?";
        try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementPlaceCount(String playerName) {
        if (getDataSource() == null) {
            return;
        }
        String sql = "UPDATE " + tableName + " SET place_count = place_count + 1 WHERE player_name = ?";
        try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getCaptureCount(String playerName) {
        if (getDataSource() == null) {
            return 0;
        }
        String sql = "SELECT capture_count FROM " + tableName + " WHERE player_name = ?";
        try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("capture_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getPlaceCount(String playerName) {
        if (getDataSource() == null) {
            return 0;
        }
        String sql = "SELECT place_count FROM " + tableName + " WHERE player_name = ?";
        try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("place_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void closeConnection() {
        databaseManager.close();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}

