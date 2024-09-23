package it.pintux.life.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type");
        String host = config.getString("database.mysql.host");
        int port = config.getInt("database.mysql.port");
        String database = config.getString("database.mysql.database");
        String username = config.getString("database.mysql.username");
        String password = config.getString("database.mysql.password");
        initialize(type, host, port, database, username, password);
    }

    public void initialize(String dbType, String host, int port, String database, String username, String password) {

        if (dbType.equals("mysql")) {
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);

            this.dataSource = new HikariDataSource(hikariConfig);
            Bukkit.getLogger().info("Hikari pool created");
        } else {
            Bukkit.getLogger().severe("Unsupported database type! Please use either 'mysql' or 'h2'.");
        }
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
