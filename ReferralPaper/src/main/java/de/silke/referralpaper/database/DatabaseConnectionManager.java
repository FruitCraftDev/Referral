package de.silke.referralpaper.database;

import de.silke.referralpaper.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DatabaseConnectionManager {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maxIdleTime; // Максимальное время простоя соединения с базой данных
    private final ScheduledExecutorService connectionCleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Connection, Long> activeConnections = new ConcurrentHashMap<>();

    public DatabaseConnectionManager(String jdbcUrl, String username, String password, int maxIdleTime) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maxIdleTime = maxIdleTime;

        connectionCleanupExecutor.scheduleAtFixedRate(this::cleanupIdleConnections, maxIdleTime, maxIdleTime, TimeUnit.MILLISECONDS);
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        activeConnections.put(connection, System.currentTimeMillis());
        return connection;
    }

    private void cleanupIdleConnections() {
        long currentTime = System.currentTimeMillis();
        for (Connection connection : activeConnections.keySet()) {
            long lastUsedTime = activeConnections.get(connection);
            if (currentTime - lastUsedTime > maxIdleTime) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    Main.log.warning("Ошибка при очистке неиспользуемых соединений с базой данных");
                }
                activeConnections.remove(connection);
            }
        }
    }

    public void close() {
        connectionCleanupExecutor.shutdownNow();
        activeConnections.keySet().forEach(connection -> {
            try {
                connection.close();
            } catch (SQLException e) {
                Main.log.warning("Ошибка при закрытии соединения с базой данных");
            }
        });
    }
}