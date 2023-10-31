package de.silke.referralpurpur.database;

import de.silke.referralpurpur.Main;
import lombok.Data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Data
public class ConnectionPoolManager {
    private String url;
    private String username;
    private String password;
    private int maxConnections;
    private Queue<Connection> connectionPool = new LinkedList<>();
    private Queue<CompletableFuture<Connection>> connectionQueue = new LinkedList<>();
    private boolean isReconnecting = false;

    public ConnectionPoolManager(String url, String username, String password, int maxConnections) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
    }

    public CompletableFuture<Connection> getConnection() {
        CompletableFuture<Connection> future = new CompletableFuture<>();

        if (connectionPool.isEmpty()) {
            if (connectionPool.size() < maxConnections) {
                // Если не достигнуто максимальное количество соединений, создаём новое соединение
                createConnectionAsync().thenAccept(future::complete);
            } else {
                // Если достигнуто максимальное количество соединений, добавляем в очередь
                connectionQueue.add(future);
            }
        } else {
            // Получить соединение из пула
            Connection connection = connectionPool.poll();
            future.complete(connection);
        }

        return future;
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            connectionPool.offer(connection);
            executeQueuedTasks();
        }
    }

    private CompletableFuture<Connection> createConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                Main.log.severe("CONNECTION_POOL: Ошибка при создании соединения с базой данных");
                return null;
            }
        });
    }

    private void executeQueuedTasks() {
        if (!isReconnecting && !connectionQueue.isEmpty()) {
            isReconnecting = true;
            createConnectionAsync().thenAccept(connection -> {
                if (connection != null) {
                    while (!connectionQueue.isEmpty()) {
                        CompletableFuture<Connection> future = connectionQueue.poll();
                        future.complete(connection);
                    }
                }
                isReconnecting = false;
            });
        }
    }

    public void close() {
        for (CompletableFuture<Connection> connection : connectionQueue) {
            connection.cancel(true);
        }
        for (Connection connection : connectionPool) {
            try {
                connection.close();
            } catch (SQLException e) {
                Main.log.severe("CONNECTION_POOL: Ошибка при закрытии соединения с базой данных");
            }
        }
    }
}

