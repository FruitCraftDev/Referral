package de.silke.referralpaper.database;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.utils.JsonSerializer;
import lombok.Data;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
public class ReferralsDatabase {
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

    public ReferralsDatabase() {
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
                Main.log.warning("REFERRALS_DATABASE: Ошибка при закрытии соединения с базой данных");
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
                            Main.log.warning("REFERRALS_DATABASE: Ошибка при закрытии соединения с базой данных");
                            future.completeExceptionally(e);
                        }
                        connectionPool.setReconnecting(false);
                    })
                    .thenRun(() -> {
                        Main.log.info("REFERRALS_DATABASE: Переподключение к базе данных...");
                        initializeDatabase().join();
                        Main.log.info("REFERRALS_DATABASE: Переподключение к базе данных завершено");
                    });
        }
        future.complete(null);
        return future;
    }

    private void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS referrals ("
                + "id INT(11) NOT NULL AUTO_INCREMENT,"
                + "referralOwnerUUID CHAR(36) PRIMARY KEY,"
                + "referralOwnerName VARCHAR(64),"
                + "playersUsedReferralNick JSON,"
                + "playerRewardsReceived JSON,"
                + "amountOfTimesUsed INT(11),"
                + "UNIQUE KEY id (id)"
                + ");";

        connectionPool.getConnection().thenAccept(connection -> {
            try {
                connection.createStatement().execute(createTableSQL);
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при создании таблицы в базе данных");
            }
        });
    }

    /**
     * Проверить наличие игрока в базе данных
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @return CompletableFuture Результат выполнения
     */
    public CompletableFuture<Boolean> containsPlayer(UUID referralOwnerUUID) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String sql = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("referralOwnerUUID").equals(referralOwnerUUID.toString()));
                    } else {
                        future.complete(false);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при проверке наличия игрока в базе данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> addPlayer(Player player) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "INSERT INTO referrals (referralOwnerUUID, referralOwnerName, playersUsedReferralNick, playerRewardsReceived, amountOfTimesUsed) VALUES (?, ?, ?, ?, ?)";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setString(3, JsonSerializer.serializeToJSON(new ArrayList<>()));
                statement.setString(4, JsonSerializer.serializeToJSON(new ArrayList<>()));
                statement.setInt(5, 0);
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при добавлении игрока в базу данных");
                future.completeExceptionally(e);
            }
        });
        future.complete(null);
        return future;
    }

    public CompletableFuture<Void> addPlayer(UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "INSERT INTO referrals (referralOwnerUUID, referralOwnerName, playersUsedReferralNick, playerRewardsReceived, amountOfTimesUsed) VALUES (?, ?, ?, ?, ?)";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, Main.plugin.getPlayerInfoDatabase().getPlayerName(uuid).join());
                statement.setString(3, JsonSerializer.serializeToJSON(new ArrayList<>()));
                statement.setString(4, JsonSerializer.serializeToJSON(new ArrayList<>()));
                statement.setInt(5, 0);
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при добавлении игрока в базу данных");
                future.completeExceptionally(e);
            }
        });
        future.complete(null);
        return future;
    }

    /**
     * Получить UUID игрока
     *
     * @param referralOwnerName Ник владельца реферала
     * @return CompletableFuture UUID игрока
     */
    public CompletableFuture<UUID> getPlayerUUID(String referralOwnerName) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        String sql = "SELECT referralOwnerUUID FROM referrals WHERE referralOwnerName = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(UUID.fromString(resultSet.getString("referralOwnerUUID")));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при получении UUID игрока из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Получить ник игрока
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @return CompletableFuture Ник игрока
     */
    public CompletableFuture<String> getPlayerName(UUID referralOwnerUUID) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String sql = "SELECT referralOwnerName FROM referrals WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getString("referralOwnerName"));
                    } else {
                        future.complete(null);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при получении ника игрока из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<List<String>> getReferralOwnersNames() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        String sql = "SELECT referralOwnerName FROM referrals";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                List<String> referralOwnersNames = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        referralOwnersNames.add(resultSet.getString("referralOwnerName"));
                    }
                }
                future.complete(referralOwnersNames);
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при получении ников игроков из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Добавить игрока в использованные ники
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @param playerName        Ник игрока
     * @return CompletableFuture Результат выполнения
     */
    public CompletableFuture<Void> addPlayerToUsedReferralNick(UUID referralOwnerUUID, String playerName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        List<String> nicks = getPlayersUsedReferralNick(referralOwnerUUID).join();
        nicks.add(playerName);

        setPlayersUsedReferralNick(referralOwnerUUID, nicks).join();
        future.complete(null);
        return future;
    }

    /**
     * Добавить игрока с полученной наградой
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @param playerName        Ник игрока
     * @return CompletableFuture Результат выполнения
     */
    public CompletableFuture<Void> addPlayerWithReward(UUID referralOwnerUUID, String playerName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        List<String> playersWithReward = getPlayerRewardsReceived(referralOwnerUUID).join();
        playersWithReward.add(playerName);

        setPlayerWithReward(referralOwnerUUID, playersWithReward).join();
        future.complete(null);
        return future;
    }

    /**
     * Проверить наличие игрока в использованных никах
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @return CompletableFuture Результат выполнения
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<String>> getPlayersUsedReferralNick(UUID referralOwnerUUID) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        String sql = "SELECT playersUsedReferralNick FROM referrals WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String nicksJson = resultSet.getString("playersUsedReferralNick");
                        List<String> nicksList = JsonSerializer.deserializeFromJSON(nicksJson);
                        future.complete(nicksList);
                    } else {
                        future.complete(new ArrayList<>());
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при получении использованных ников игрока из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Получить игроков с полученной наградой
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @return CompletableFuture Список игроков с полученной наградой
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<String>> getPlayerRewardsReceived(UUID referralOwnerUUID) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        String sql = "SELECT playerRewardsReceived FROM referrals WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String playersWithRewardJson = resultSet.getString("playerRewardsReceived");
                        List<String> playersWithRewardList = JsonSerializer.deserializeFromJSON(playersWithRewardJson);
                        future.complete(playersWithRewardList);
                    } else {
                        future.complete(new ArrayList<>());
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при получении игроков с наградой из базы данных: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }


    /**
     * Проверить наличие игрока в игроках с полученной наградой
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @param playerUUID        UUID игрока
     * @return CompletableFuture Результат выполнения
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> containsPlayerWithReward(UUID referralOwnerUUID, UUID playerUUID) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String sql = "SELECT playerRewardsReceived FROM referrals WHERE referralOwnerUUID = ?";
        final String nick = Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join();

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String playersWithRewardJson = resultSet.getString("playerRewardsReceived");
                        List<String> playersWithReward = JsonSerializer.deserializeFromJSON(playersWithRewardJson);
                        future.complete(playersWithReward.contains(nick));
                    } else {
                        future.complete(false);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при проверке наличия игрока с наградой в базе данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Установить игроков с приглашённым игроком
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @param nicks             Список игроков с приглашённым игроком
     * @return CompletableFuture Результат выполнения
     */
    public CompletableFuture<Void> setPlayersUsedReferralNick(UUID referralOwnerUUID, List<String> nicks) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE referrals SET playersUsedReferralNick = ? WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, JsonSerializer.serializeToJSON(nicks));
                statement.setString(2, referralOwnerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при обновлении использованных ников игрока в базе данных");
                future.completeExceptionally(e);
            }
        });
        future.complete(null);
        return future;
    }

    /**
     * Установить игроков с полученной наградой
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @param playersWithReward Список игроков с полученной наградой
     * @return CompletableFuture Результат выполнения
     */
    public CompletableFuture<Void> setPlayerWithReward(UUID referralOwnerUUID, List<String> playersWithReward) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE referrals SET playerRewardsReceived = ? WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, JsonSerializer.serializeToJSON(playersWithReward));
                statement.setString(2, referralOwnerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при обновлении игроков с наградой в базе данных");
                future.completeExceptionally(e);
            }
        });
        future.complete(null);
        return future;
    }

    /**
     * Получить количество использований реферала
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @return CompletableFuture Количество использований реферала
     */
    public CompletableFuture<Integer> getAmountOfTimesUsed(UUID referralOwnerUUID) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        String sql = "SELECT amountOfTimesUsed FROM referrals WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        future.complete(resultSet.getInt("amountOfTimesUsed"));
                    } else {
                        future.complete(0);
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при получении количества использований реферала из базы данных");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Добавить одно использование реферала
     *
     * @param referralOwnerUUID UUID владельца реферала
     * @return CompletableFuture Результат выполнения
     */
    public CompletableFuture<Void> addAmountOfTimesUsed(UUID referralOwnerUUID) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        String sql = "UPDATE referrals SET amountOfTimesUsed = amountOfTimesUsed + 1 WHERE referralOwnerUUID = ?";

        connectionPool.getConnection().thenAccept(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, referralOwnerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("REFERRALS_DATABASE: Ошибка при добавлении количества использований реферала в базе данных");
                future.completeExceptionally(e);
            }
        });
        future.complete(null);
        return future;
    }
}