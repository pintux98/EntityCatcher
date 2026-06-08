package it.pintux.life.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;
    private boolean sqlite;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite");
        String host = config.getString("database.mysql.host");
        int port = config.getInt("database.mysql.port");
        String database = config.getString("database.mysql.database");
        String username = config.getString("database.mysql.username");
        String password = config.getString("database.mysql.password");
        initialize(type, host, port, database, username, password);
    }

    public void initialize(String dbType, String host, int port, String database, String username, String password) {
        HikariConfig hikariConfig = new HikariConfig();

        if (dbType != null && dbType.equalsIgnoreCase("sqlite")) {
            this.sqlite = true;
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            // The SQLite driver does not auto-register through Hikari's URL detection in
            // every relocated environment, so load it explicitly first.
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {
            }
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            // SQLite is a single-writer database; one connection avoids "database is locked".
            hikariConfig.setMaximumPoolSize(1);
        } else if (dbType != null && dbType.equalsIgnoreCase("mysql")) {
            this.sqlite = false;
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
        } else {
            plugin.getLogger().severe("Unsupported database type '" + dbType + "'! Use 'sqlite' or 'mysql'.");
            return;
        }

        this.dataSource = new HikariDataSource(hikariConfig);
        plugin.getLogger().info("Database pool created (" + dbType + ")");
    }

    public boolean isSQLite() {
        return sqlite;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
