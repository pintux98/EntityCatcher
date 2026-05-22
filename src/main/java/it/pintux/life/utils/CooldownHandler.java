package it.pintux.life.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class CooldownHandler {

    private final DatabaseManager databaseManager;
    private final String tableName = "cooldowns";
    private final String historyTable = "capture_history";
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;

    public CooldownHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        databaseManager = new DatabaseManager(plugin);
        setupDatabase();
    }

    private void setupDatabase() {
        try {
            if (dataSource != null) closeConnection();
            databaseManager.setup();
            dataSource = databaseManager.getDataSource();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to setup database", e);
        }
        if (dataSource != null) {
            createTable();
            createHistoryTable();
        }
    }

    private void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                                + "player_uuid VARCHAR(36) PRIMARY KEY, "
                                + "capture_cooldown BIGINT NOT NULL DEFAULT 0, "
                                + "place_cooldown BIGINT NOT NULL DEFAULT 0, "
                                + "capture_count INT NOT NULL DEFAULT 0, "
                                + "place_count INT NOT NULL DEFAULT 0, "
                                + "last_capture_time BIGINT NOT NULL DEFAULT 0, "
                                + "last_place_time BIGINT NOT NULL DEFAULT 0)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create cooldowns table", e);
        }
    }

    public void setCooldown(UUID playerUUID, long captureCooldownMillis, long placeCooldownMillis) {
        if (dataSource == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long captureCooldownEndTime = currentTime + captureCooldownMillis;
        long placeCooldownEndTime = currentTime + placeCooldownMillis;

        String sql = "INSERT INTO " + tableName + " (player_uuid, capture_cooldown, place_cooldown) "
                     + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE capture_cooldown=?, place_cooldown=?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setLong(2, captureCooldownEndTime);
            pstmt.setLong(3, placeCooldownEndTime);
            pstmt.setLong(4, captureCooldownEndTime);
            pstmt.setLong(5, placeCooldownEndTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set cooldown for {0}", playerUUID);
        }
    }

    public long getCooldown(UUID playerUUID, String type) {
        if (dataSource == null) {
            return -1;
        }
        String sql = "SELECT capture_cooldown, place_cooldown FROM " + tableName + " WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
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
            return -1;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get cooldown for {0}", playerUUID);
            return -1;
        }
    }

    public void removeCooldown(UUID playerUUID, String type) {
        if (dataSource == null) {
            return;
        }
        String column = type + "_cooldown";
        String sql = "UPDATE " + tableName + " SET " + column + " = 0 WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove cooldown for {0}", playerUUID);
        }
    }

    /**
     * Records the current time as the last capture/place action for cross-server anti-exploit.
     */
    public void recordActionTime(UUID playerUUID, String action) {
        if (dataSource == null) {
            return;
        }
        String column = "last_" + action + "_time";
        String sql = "INSERT INTO " + tableName + " (player_uuid, " + column + ") "
                     + "VALUES (?, ?) ON DUPLICATE KEY UPDATE " + column + "=?";
        long now = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setLong(2, now);
            pstmt.setLong(3, now);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to record action time for {0}", playerUUID);
        }
    }

    /**
     * Checks if the player performed the same action recently (cross-server aware).
     * Returns true if the action is too recent (within the threshold).
     */
    public boolean isActionTooRecent(UUID playerUUID, String action, long thresholdMillis) {
        if (dataSource == null) {
            return false;
        }
        String column = "last_" + action + "_time";
        String sql = "SELECT " + column + " FROM " + tableName + " WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long lastTime = rs.getLong(column);
                return (System.currentTimeMillis() - lastTime) < thresholdMillis;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check action time for {0}", playerUUID);
        }
        return false;
    }

    public void incrementCaptureCount(UUID playerUUID) {
        if (dataSource == null) {
            return;
        }
        String sql = "UPDATE " + tableName + " SET capture_count = capture_count + 1 WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to increment capture count for {0}", playerUUID);
        }
    }

    public void incrementPlaceCount(UUID playerUUID) {
        if (dataSource == null) {
            return;
        }
        String sql = "UPDATE " + tableName + " SET place_count = place_count + 1 WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to increment place count for {0}", playerUUID);
        }
    }

    public int getCaptureCount(UUID playerUUID) {
        if (dataSource == null) {
            return 0;
        }
        String sql = "SELECT capture_count FROM " + tableName + " WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("capture_count");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get capture count for {0}", playerUUID);
        }
        return 0;
    }

    public int getPlaceCount(UUID playerUUID) {
        if (dataSource == null) {
            return 0;
        }
        String sql = "SELECT place_count FROM " + tableName + " WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("place_count");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get place count for {0}", playerUUID);
        }
        return 0;
    }

    public void closeConnection() {
        databaseManager.close();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    private void createHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + historyTable + " ("
                     + "id INT AUTO_INCREMENT PRIMARY KEY, "
                     + "player_uuid VARCHAR(36) NOT NULL, "
                     + "entity_type VARCHAR(64) NOT NULL, "
                     + "entity_name VARCHAR(256), "
                     + "catcher_type VARCHAR(64) NOT NULL, "
                     + "world VARCHAR(64), "
                     + "captured_at BIGINT NOT NULL, "
                     + "INDEX idx_player (player_uuid))";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create capture_history table", e);
        }
    }

    public void recordCapture(UUID playerUUID, String entityType, String entityName, String catcherType, String world) {
        if (dataSource == null) return;
        String sql = "INSERT INTO " + historyTable + " (player_uuid, entity_type, entity_name, catcher_type, world, captured_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, entityType);
            pstmt.setString(3, entityName != null ? entityName : "");
            pstmt.setString(4, catcherType);
            pstmt.setString(5, world);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to record capture history for {0}", playerUUID);
        }
    }

    public List<CaptureRecord> getCaptureHistory(UUID playerUUID, int limit) {
        if (dataSource == null) return Collections.emptyList();
        String sql = "SELECT entity_type, entity_name, catcher_type, world, captured_at FROM " + historyTable
                     + " WHERE player_uuid = ? ORDER BY captured_at DESC LIMIT ?";
        List<CaptureRecord> records = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new CaptureRecord(
                        rs.getString("entity_type"),
                        rs.getString("entity_name"),
                        rs.getString("catcher_type"),
                        rs.getString("world"),
                        rs.getLong("captured_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get capture history for {0}", playerUUID);
        }
        return records;
    }

    public static class CaptureRecord {
        public final String entityType;
        public final String entityName;
        public final String catcherType;
        public final String world;
        public final long capturedAt;

        public CaptureRecord(String entityType, String entityName, String catcherType, String world, long capturedAt) {
            this.entityType = entityType;
            this.entityName = entityName;
            this.catcherType = catcherType;
            this.world = world;
            this.capturedAt = capturedAt;
        }

        public String getFormattedTime() {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(capturedAt));
        }
    }
}
