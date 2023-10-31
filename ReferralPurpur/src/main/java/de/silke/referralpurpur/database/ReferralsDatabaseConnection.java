package de.silke.referralpurpur.database;

import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.utils.JsonSerializer;
import lombok.Data;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
@SuppressWarnings({"UnusedReturnValue"})
public class ReferralsDatabaseConnection {
    private static final String HOST = Main.plugin.getConfig().getString("database.host");
    private static final String PORT = Main.plugin.getConfig().getString("database.port");
    private static final String DATABASE = Main.plugin.getConfig().getString("database.database");
    private static final String USERNAME = Main.plugin.getConfig().getString("database.username");
    private static final String PASSWORD = Main.plugin.getConfig().getString("database.password");
    private static final boolean useSSL = Main.plugin.getConfig().getBoolean("database.useSSL");
    private static final boolean autoReconnect = Main.plugin.getConfig().getBoolean("database.autoReconnect");
    private DatabaseConnectionManager connectionManager;
    private Connection connection;

    public ReferralsDatabaseConnection() {
        if (connection == null) initializeDatabase().join();
    }

    /**
     * Инициализировать подключение к базе данных
     *
     * @return CompletableFuture Завершение инициализации
     */
    private CompletableFuture<Void> initializeDatabase() {
        return CompletableFuture.runAsync(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connectionManager = new DatabaseConnectionManager(
                        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=" + useSSL + "&autoReconnect=" + autoReconnect, USERNAME, PASSWORD,
                        900000);
                connection = connectionManager.getConnection();
                createTable().join();
            } catch (ClassNotFoundException | SQLException e) {
                Main.log.severe("Ошибка подключения к базе данных: " + e.getMessage());
            }
        });
    }

    /**
     * Закрыть соединение с базой данных
     *
     * @return CompletableFuture Завершение соединения
     */
    @SuppressWarnings("UnusedReturnValue")
    @SneakyThrows
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null) {
                    connectionManager.close();
                    connection.close();
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка закрытия соединения с базой данных: " + e.getMessage());
                reconnect().join();
            }
        });
    }

    /**
     * Переподключиться к базе данных
     *
     * @return CompletableFuture Переподключение
     */
    @SneakyThrows
    public CompletableFuture<Void> reconnect() {
        return CompletableFuture.runAsync(() -> {
            close().join();
            initializeDatabase().join();
        });
    }

    /**
     * Создать таблицу в базе данных
     *
     * @return CompletableFuture Завершение создания таблицы
     */
    private CompletableFuture<Void> createTable() {
        return CompletableFuture.runAsync(() -> {
            try {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS referrals ("
                        + "id INT(11) NOT NULL AUTO_INCREMENT,"
                        + "referralOwnerUUID CHAR(36) PRIMARY KEY,"
                        + "referralOwnerName VARCHAR(64),"
                        + "playersUsedReferralNick JSON,"
                        + "playerRewardsReceived JSON,"
                        + "amountOfTimesUsed INT(11),"
                        + "UNIQUE KEY id (id)"
                        + ");";

                connection.createStatement().executeUpdate(createTableSQL);
            } catch (SQLException e) {
                Main.log.severe("Ошибка создания таблицы в базе данных: " + e.getMessage());
                reconnect().join();
            }
        });
    }

    /**
     * Проверить наличие игрока в БД
     *
     * @param player Игрок
     * @return CompletableFuture Завершение проверки
     */
    @SneakyThrows
    public CompletableFuture<Boolean> containsPlayer(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next();
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка проверки наличия игрока в базе данных: " + e.getMessage());
                    reconnect().join();
                    return null;
                }
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Проверить наличие игрока в БД по его UUID
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение проверки
     */
    @SneakyThrows
    public CompletableFuture<Boolean> containsPlayer(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                if (playerUUID != null) {
                    String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                    try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                        statement.setString(1, playerUUID.toString());
                        try (ResultSet resultSet = statement.executeQuery()) {
                            return resultSet.next();
                        }
                    } catch (SQLException e) {
                        Main.log.severe("Ошибка проверки наличия игрока в базе данных: " + e.getMessage());
                        reconnect().join();
                        return null;
                    }
                } else {
//                Main.log.severe("Ошибка проверки наличия игрока в базе данных: Игрок не найден!");
                    return CompletableFuture.completedFuture(false).join();
                }
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Получить UUID игрока по его нику
     *
     * @param playerName Ник игрока
     * @return CompletableFuture Завершение получения UUID
     */
    @SneakyThrows
    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerName = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то возвращаем null
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения UUID игрока по его нику: " + e.getMessage());
                    reconnect().join();
                    return null;
                }

                // Получение UUID игрока по его нику
                String getPlayerSQL = "SELECT referralOwnerUUID FROM referrals WHERE referralOwnerName = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, playerName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return UUID.fromString(resultSet.getString("referralOwnerUUID"));
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения UUID игрока по его нику: " + e.getMessage());
                    reconnect().join();
                    return null;
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Добавить игрока в БД с определёнными значениями
     * <p>Если игрок уже существует, то в консоли будет ошибка
     *
     * @param player Игрок
     * @return CompletableFuture Завершение добавления игрока
     */
    @SneakyThrows
    public CompletableFuture<Void> addPlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
//                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
                            return;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления игрока в базу данных: " + e.getMessage());
                    reconnect().join();
                }

                // Добавление игрока в БД
                String insertPlayerSQL = "INSERT INTO referrals (referralOwnerUUID, referralOwnerName, playersUsedReferralNick, playerRewardsReceived, amountOfTimesUsed) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setString(2, player.getName());
                    statement.setString(3, JsonSerializer.serializeToJSON(new ArrayList<>()));
                    statement.setString(4, JsonSerializer.serializeToJSON(new ArrayList<>()));
                    statement.setInt(5, 0);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления игрока в базу данных: " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Удалить игрока из БД
     * <p>Если игрока не существует, то в консоли будет ошибка
     *
     * @param player Игрок
     * @return CompletableFuture Завершение удаления игрока
     */
    @SneakyThrows
    public CompletableFuture<Void> removePlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных!");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка удаления игрока из базы данных: " + e.getMessage());
                    reconnect().join();
                }

                // Удаление игрока из БД
                String deletePlayerSQL = "DELETE FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(deletePlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка удаления игрока из базы данных: " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Получить ник игрока по его UUID
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение получения ника игрока
     */
    @SneakyThrows
    public CompletableFuture<String> getPlayerName(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
//                        Main.log.severe("Игрок с UUID " + playerUUID + " не существует в базе данных!");
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения ника игрока по его UUID: " + e.getMessage());
                    reconnect().join();
                    return null;
                }

                // Получение ника игрока по его UUID
                String getPlayerSQL = "SELECT referralOwnerName FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getString("referralOwnerName");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения ника игрока по его UUID: " + e.getMessage());
                    reconnect().join();
                    return null;
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значения для "referralOwnerName"
     * <p>referralOwnerName - Имя игрока, который пригласил игрока
     *
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<List<String>> getReferralOwnerNames() {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                List<String> names = new ArrayList<>();
                String selectNamesSQL = "SELECT referralOwnerName FROM referrals";
                try (PreparedStatement statement = connection.prepareStatement(selectNamesSQL)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            names.add(resultSet.getString("referralOwnerName"));
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения \"referralOwnerName\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }
                return names;
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Автоматически установить значение для "referralOwnerName" и "referralOwnerUUID"
     * <p>referralOwnerName - Имя игрока, который пригласил игрока
     * <p>referralOwnerUUID - UUID игрока, который пригласил игрока
     *
     * @param player Игрок
     * @return CompletableFuture Завершение установки значения
     */
    private CompletableFuture<Void> setReferralOwner(Player player) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
//                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
                            return;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referralOwnerName\" и \"referralOwnerUUID\": " + e.getMessage());
                    reconnect().join();
                }

                // Установка значения для "referralOwnerName" и "referralOwnerUUID"
                String updatePlayerSQL = "UPDATE referrals SET referralOwnerName = ?, referralOwnerUUID = ? WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setString(1, player.getName());
                    statement.setString(2, player.getUniqueId().toString());
                    statement.setString(3, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referralOwnerName\" и \"referralOwnerUUID\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Добавить игрока по UUID к значению "playersUsedReferralNick"
     * <p>playersUsedReferralNick - Имена игроков, которые использовали реферальный ник
     *
     * @param UUID        UUID игрока
     * @param newNickname Имя игрока, который использовал реферальный ник
     * @return CompletableFuture Завершение добавления значения
     */
    @SneakyThrows
    public CompletableFuture<Void> addPlayerToUsedReferralNick(UUID UUID, String newNickname) {
        return CompletableFuture.runAsync(() -> {
            List<String> nicknames = getPlayersUsedReferralNick(UUID).join();

            nicknames.add(newNickname);

            setPlayersUsedReferralNick(UUID, nicknames);
        });
    }

    /**
     * Добавить игрока по UUID к значению "playerRewardsReceived"
     * <p>playerRewardsReceived - Имена игроков, которые использовали реферальный ник
     *
     * @param UUID        UUID игрока
     * @param newNickname Имя игрока, который использовал реферальный ник
     * @return CompletableFuture Завершение добавления значения
     */
    @SneakyThrows
    public CompletableFuture<Void> addPlayerToPlayerRewardsReceived(UUID UUID, String newNickname) {
        return CompletableFuture.runAsync(() -> {
            List<String> nicknames = getPlayerRewardsReceived(UUID).join();

            nicknames.add(newNickname);

            setPlayerRewardsReceived(UUID, nicknames);
        });
    }

    /**
     * Получить значение для "playersUsedReferralNick"
     *
     * @param player Игрок (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<List<String>> getPlayersUsedReferralNick(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                List<String> nicknames = new ArrayList<>();
                String selectNicknamesSQL = "SELECT playersUsedReferralNick FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectNicknamesSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            String nicknamesJson = resultSet.getString("playersUsedReferralNick");
                            // Десериализация JSON в список ников
                            nicknames = JsonSerializer.deserializeFromJSON(nicknamesJson);
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения \"playersUsedReferralNick\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }
                return nicknames;
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Получить значение для "playersUsedReferralNick" по UUID
     *
     * @param UUID UUID игрока (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<List<String>> getPlayersUsedReferralNick(UUID UUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                List<String> nicknames = new ArrayList<>();
                String selectNicknamesSQL = "SELECT playersUsedReferralNick FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectNicknamesSQL)) {
                    statement.setString(1, UUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            String nicknamesJson = resultSet.getString("playersUsedReferralNick");
                            // Десериализация JSON в список ников
                            nicknames = JsonSerializer.deserializeFromJSON(nicknamesJson);
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения \"playersUsedReferralNick\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }
                return nicknames;
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Получить значение для "playerRewardsReceived"
     *
     * @param UUID UUID игрока (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<List<String>> getPlayerRewardsReceived(UUID UUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                List<String> nicknames = new ArrayList<>();
                String selectNicknamesSQL = "SELECT playerRewardsReceived FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectNicknamesSQL)) {
                    statement.setString(1, UUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            String nicknamesJson = resultSet.getString("playerRewardsReceived");
                            // Десериализация JSON в список ников
                            nicknames = JsonSerializer.deserializeFromJSON(nicknamesJson);
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения \"playerRewardsReceived\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }
                return nicknames;
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Проверить, есть ли ник игрока, определённого по UUID в списке "playerRewardsReceived"
     *
     * @param UUID UUID игрока (владелец реферального ника)
     * @param UUID UUID игрока, который может быть в списке "playerRewardsReceived"
     * @return CompletableFuture Завершение проверки
     */
    @SneakyThrows
    public CompletableFuture<Boolean> containsPlayerInPlayerRewardsReceived(UUID UUID, UUID targetUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                List<String> nicknames = getPlayerRewardsReceived(UUID).join();

                String nickname = Main.plugin.getPlayerInfoDatabase().getPlayerName(targetUUID).join();
                return nicknames.contains(nickname);
            } else {
                reconnect().join();
                return null;
            }
        });
    }

    /**
     * Установить значение для "playersUsedReferralNick"
     *
     * @param playerUUID UUID игрока (владелец реферального ника)
     * @param nicknames  Имена игроков, которые использовали реферальный ник
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setPlayersUsedReferralNick(UUID playerUUID, List<String> nicknames) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, String.valueOf(playerUUID));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            String updatePlayerSQL = "UPDATE referrals SET playersUsedReferralNick = ? WHERE referralOwnerUUID = ?";
                            try (PreparedStatement statement2 = connection.prepareStatement(updatePlayerSQL)) {
                                String nicknamesJson = JsonSerializer.serializeToJSON(nicknames);
                                statement2.setString(1, nicknamesJson);
                                statement2.setString(2, String.valueOf(playerUUID));
                                statement.executeUpdate();
                            } catch (SQLException e) {
                                Main.log.severe("Ошибка установки значения для \"playersUsedReferralNick\": " + e.getMessage());
                                reconnect().join();
                            }
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referralOwnerName\" и \"referralOwnerUUID\": " + e.getMessage());
                    reconnect().join();
                }

                String updatePlayerSQL = "UPDATE referrals SET playersUsedReferralNick = ? WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    String nicknamesJson = JsonSerializer.serializeToJSON(nicknames);
                    statement.setString(1, nicknamesJson);
                    statement.setString(2, String.valueOf(playerUUID));
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"playersUsedReferralNick\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Установить значение для "playerRewardsReceived"
     *
     * @param playerUUID UUID игрока (владелец реферального ника)
     * @param nicknames  Имена игроков, которые использовали реферальный ник
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setPlayerRewardsReceived(UUID playerUUID, List<String> nicknames) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String updatePlayerSQL = "UPDATE referrals SET playerRewardsReceived = ? WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    String nicknamesJson = JsonSerializer.serializeToJSON(nicknames);
                    statement.setString(1, nicknamesJson);
                    statement.setString(2, String.valueOf(playerUUID));
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"playerRewardsReceived\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Добавить +1 к значению для "amountOfTimesUsed"
     * <p>amountOfTimesUsed - количество раз, когда игроки использовали реферальный ник
     *
     * @param player Игрок (владелец реферального ника)
     * @return CompletableFuture Завершение добавления к значению
     */
    @SneakyThrows
    public CompletableFuture<Void> addAmountOfTimesUsed(Player player) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
//                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
                            return;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления +1 к значению для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                }

                // Добавление +1 к значению для "amountOfTimesUsed"
                String updatePlayerSQL = "UPDATE referrals SET amountOfTimesUsed = amountOfTimesUsed + 1 WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления +1 к значению для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Добавить +1 к значению для "amountOfTimesUsed" по UUID
     * <p>amountOfTimesUsed - количество раз, когда игроки использовали реферальный ник
     *
     * @param UUID UUID игрока (владелец реферального ника)
     * @return CompletableFuture Завершение добавления к значению
     */
    @SneakyThrows
    public CompletableFuture<Void> addAmountOfTimesUsed(UUID UUID) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, UUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления +1 к значению для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                }

                // Добавление +1 к значению для "amountOfTimesUsed"
                String updatePlayerSQL = "UPDATE referrals SET amountOfTimesUsed = amountOfTimesUsed + 1 WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setString(1, UUID.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления +1 к значению для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Получить значение для "amountOfTimesUsed"
     *
     * @param player Игрок (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows(NullPointerException.class)
    public CompletableFuture<Integer> getAmountOfTimesUsed(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
//                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
                            return CompletableFuture.completedFuture(0).join();
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }

                // Получение значения для "amountOfTimesUsed"
                String selectAmountOfTimesUsedSQL = "SELECT amountOfTimesUsed FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectAmountOfTimesUsedSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt("amountOfTimesUsed");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }
            } else {
                reconnect().join();
                return null;
            }
            return 0;
        });
    }

    /**
     * Получить значение для "amountOfTimesUsed" по UUID
     *
     * @param UUID UUID игрока (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<Integer> getAmountOfTimesUsed(UUID UUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, UUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            return CompletableFuture.completedFuture(0).join();
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }

                // Получение значения для "amountOfTimesUsed"
                String selectAmountOfTimesUsedSQL = "SELECT amountOfTimesUsed FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectAmountOfTimesUsedSQL)) {
                    statement.setString(1, UUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getInt("amountOfTimesUsed");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"amountOfTimesUsed\": " + e.getMessage());
                    reconnect().join();
                    return null;
                }
            } else {
                reconnect().join();
                return null;
            }
            return 0;
        });
    }
}
