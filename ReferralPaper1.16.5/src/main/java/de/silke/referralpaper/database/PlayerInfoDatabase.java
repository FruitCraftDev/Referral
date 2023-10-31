package de.silke.referralpaper.database;

import de.silke.referralpaper.Main;
import lombok.Data;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
public class PlayerInfoDatabase {
    private static final String HOST = Main.plugin.getConfig().getString("database.host");
    private static final String PORT = Main.plugin.getConfig().getString("database.port");
    private static final String DATABASE = Main.plugin.getConfig().getString("database.database");
    private static final String USERNAME = Main.plugin.getConfig().getString("database.username");
    private static final String PASSWORD = Main.plugin.getConfig().getString("database.password");
    private static final boolean useSSL = Main.plugin.getConfig().getBoolean("database.useSSL");
    private static final boolean autoReconnect = Main.plugin.getConfig().getBoolean("database.autoReconnect");
    private final ConnectionPoolManager connectionPool = new ConnectionPoolManager(
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=" + useSSL + "&autoReconnect=" + autoReconnect,
            USERNAME,
            PASSWORD,
            10
    );

    public PlayerInfoDatabase() {
        initializeDatabase().join();
    }

    private CompletableFuture<Void> initializeDatabase() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        createTable();
        future.complete(null);
        return future;
    }

    public CompletableFuture<Void> close() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        connectionPool.getConnection().thenAccept(connection -> {
            try {
                connection.close();
            } catch (SQLException e) {
                Main.log.warning("PLAYER_INFO_DATABASE: Ошибка при закрытии соединения с базой данных");
                future.completeExceptionally(e);
            }
        });
        future.complete(null);
        return future;
    }

    public CompletableFuture<Void> reconnect() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!connectionPool.isReconnecting()) {
            connectionPool.setReconnecting(true);
            connectionPool.getConnection().thenAccept(connection -> {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            Main.log.warning("PLAYER_INFO_DATABASE: Ошибка при закрытии соединения с базой данных");
                            future.completeExceptionally(e);
                        }
                        connectionPool.setReconnecting(false);
                    })
                    .thenRun(() -> {
                        Main.log.info("PLAYER_INFO_DATABASE: Переподключение к базе данных...");
                        initializeDatabase().join();
                        Main.log.info("PLAYER_INFO_DATABASE: Переподключение к базе данных завершено");
                    });
        }
        future.complete(null);
        return future;
    }

    private void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_information ("
                + "id INT(11) NOT NULL AUTO_INCREMENT,"
                + "uuid CHAR(36) PRIMARY KEY,"
                + "player_name VARCHAR(64),"
                + "declinedReferQuestion BOOLEAN,"
                + "referredPlayerUUID CHAR(36),"
                + "referredPlayerName VARCHAR(64),"
                + "luckPermsRole VARCHAR(64),"
                + "timePlayed BIGINT(20),"
                + "registrationDate DATE,"
                + "UNIQUE KEY id (id)"
                + ");";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
                statement.execute();
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при создании таблицы player_information");
            }
        });
    }

    public CompletableFuture<Boolean> containsPlayer(UUID uuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String sql = "SELECT * FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("uuid").equals(uuid.toString()));
                    } else {
                        future.complete(false);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при проверке наличия игрока в базе данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> addPlayer(UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "INSERT INTO player_information (uuid) VALUES (?)";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
//                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при добавлении игрока в базу данных");
                future.completeExceptionally(e);
            }

            String sql2 = "UPDATE player_information SET player_name = ? WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql2)) {
                statement.setString(1, Bukkit.getOfflinePlayer(uuid).getName());
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
//                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при добавлении игрока в базу данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setDeclinedReferQuestion(UUID uuid, boolean declinedReferQuestion) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET declinedReferQuestion = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, declinedReferQuestion);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения declinedReferQuestion");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setReferredPlayerUUID(String uuid, String referredPlayerUUID) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET referredPlayerUUID = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referredPlayerUUID);
                statement.setString(2, uuid);
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения referredPlayerUUID");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setReferredPlayerName(String uuid, String referredPlayerName) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET referredPlayerName = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referredPlayerName);
                statement.setString(2, uuid);
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения referredPlayerName");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setReferredPlayer(UUID uuid, UUID referredPlayerUUID) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET referredPlayerUUID = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referredPlayerUUID.toString());
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения referredPlayerUUID");
                future.completeExceptionally(e);
            }
        });

        String sql2 = "UPDATE player_information SET referredPlayerName = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql2)) {
                statement.setString(1, Bukkit.getOfflinePlayer(referredPlayerUUID).getName());
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения referredPlayerName");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setLuckPermsRole(UUID uuid, String luckPermsRole) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET luckPermsRole = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, luckPermsRole);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения luckPermsRole");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setTimePlayed(UUID uuid, long timePlayed) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET timePlayed = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, timePlayed);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения timePlayed");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> addTimePlayed(UUID playerUUID, long value) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET timePlayed = timePlayed + ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, value);
                statement.setString(2, playerUUID.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при добавлении значения timePlayed");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> setRegistrationDate(UUID uuid, java.sql.Date registrationDate) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE player_information SET registrationDate = ? WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDate(1, registrationDate);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при установке значения registrationDate");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        String sql = "SELECT uuid FROM player_information WHERE player_name = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(UUID.fromString(resultSet.getString("uuid")));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при получении значения uuid");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<String> getPlayerName(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String sql = "SELECT player_name FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("player_name"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при получении значения player_name");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> getDeclinedReferQuestion(UUID uuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String sql = "SELECT declinedReferQuestion FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getBoolean("declinedReferQuestion"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при получении значения declinedReferQuestion");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<String> getReferredPlayerUUID(String uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String sql = "SELECT referredPlayerUUID FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("referredPlayerUUID"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при получении значения referredPlayerUUID");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<String> getReferredPlayerName(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String sql = "SELECT referredPlayerName FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("referredPlayerName"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<String> getLuckPermsRole(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String sql = "SELECT luckPermsRole FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("luckPermsRole"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при получении значения luckPermsRole");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Long> getTimePlayed(UUID uuid) {
        CompletableFuture<Long> future = new CompletableFuture<>();

        String sql = "SELECT timePlayed FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        long timePlayed = resultSet.getLong("timePlayed");
                        if (timePlayed <= 0) {
                            future.complete(10L);
                            // Установить время игры в 10 миллисекунд, чтобы не было ошибки при конвертации в дни, часы, минуты и секунды
                            setTimePlayed(uuid, 10L).join();
                        } else {
                            Main.log.info("Время игры игрока " + Bukkit.getOfflinePlayer(uuid).getName() + " найдено в БД");
                            future.complete(timePlayed);
                        }
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Date> getRegistrationDate(UUID uuid) {
        CompletableFuture<Date> future = new CompletableFuture<>();

        String sql = "SELECT registrationDate FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getDate("registrationDate"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при получении значения registrationDate");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deletePlayer(String uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "DELETE FROM player_information WHERE uuid = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid);
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при удалении игрока из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> deleteAllPlayers() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "DELETE FROM player_information";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                Main.log.severe("PLAYER_INFO_DATABASE: Ошибка при удалении всех игроков из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}

