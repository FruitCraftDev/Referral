package de.silke.referralpaper.database;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.utils.JsonSerializer;
import lombok.Data;
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
                        60000);
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
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null) {
                    connectionManager.close();
                    connection.close();
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка закрытия соединения с базой данных: " + e.getMessage());
            }
        });
    }

    /**
     * Переподключиться к базе данных
     *
     * @return CompletableFuture Переподключение
     */
    public CompletableFuture<Void> reconnect() {
        return CompletableFuture.runAsync(() -> {
            close().join();
            initializeDatabase().join();
        });
    }

    // We store:
    // - Referral Owner's UUID
    // - Referral Owner's Name
    // - Names of players, that used the referral nick
    // - Amount of times the referral nick was used

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
                        + "amountOfTimesUsed INT(11),"
                        + "UNIQUE KEY id (id)"
                        + ");";

                connection.createStatement().executeUpdate(createTableSQL);
            } catch (SQLException e) {
                Main.log.severe("Ошибка создания таблицы в базе данных: " + e.getMessage());
            }
        });
    }

    /**
     * Проверить наличие игрока в БД
     *
     * @param player Игрок
     * @return CompletableFuture Завершение проверки
     */
    public CompletableFuture<Boolean> containsPlayer(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка проверки наличия игрока в базе данных: " + e.getMessage());
            }
            return false;
        });
    }

    /**
     * Проверить наличие игрока в БД по его UUID
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение проверки
     */
    public CompletableFuture<Boolean> containsPlayer(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (playerUUID != null) {
                String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next();
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка проверки наличия игрока в базе данных: " + e.getMessage());
                }
            } else {
//                Main.log.severe("Ошибка проверки наличия игрока в базе данных: Игрок не найден!");
                return CompletableFuture.completedFuture(false).join();
            }
            return null;
        });
    }

    /**
     * Получить UUID игрока по его нику
     *
     * @param playerName Ник игрока
     * @return CompletableFuture Завершение получения UUID
     */
    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
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
    public CompletableFuture<Void> addPlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
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
            }

            // Добавление игрока в БД
            String insertPlayerSQL = "INSERT INTO referrals (referralOwnerUUID, referralOwnerName, playersUsedReferralNick, amountOfTimesUsed) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setString(3, JsonSerializer.serializeToJSON(new ArrayList<>()));
                statement.setInt(4, 0);
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка добавления игрока в базу данных: " + e.getMessage());
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
    public CompletableFuture<Void> removePlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
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
            }

            // Удаление игрока из БД
            String deletePlayerSQL = "DELETE FROM referrals WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(deletePlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка удаления игрока из базы данных: " + e.getMessage());
            }
        });
    }

    /**
     * Получить ник игрока по его UUID
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение получения ника игрока
     */
    public CompletableFuture<String> getPlayerName(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
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
    public CompletableFuture<List<String>> getReferralOwnerNames() {
        return CompletableFuture.supplyAsync(() -> {
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
            }
            return names;
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
    public CompletableFuture<Void> addPlayerToUsedReferralNick(UUID UUID, String newNickname) {
        return CompletableFuture.runAsync(() -> {
            List<String> nicknames = getPlayersUsedReferralNick(UUID).join();

            nicknames.add(newNickname);

            setPlayersUsedReferralNick(UUID, nicknames);
        });
    }

    /**
     * Получить значение для "playersUsedReferralNick"
     *
     * @param player Игрок (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    public CompletableFuture<List<String>> getPlayersUsedReferralNick(Player player) {
        return CompletableFuture.supplyAsync(() -> {
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
            }
            return nicknames;
        });
    }

    /**
     * Получить значение для "playersUsedReferralNick" по UUID
     *
     * @param UUID UUID игрока (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    public CompletableFuture<List<String>> getPlayersUsedReferralNick(UUID UUID) {
        return CompletableFuture.supplyAsync(() -> {
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
            }
            return nicknames;
        });
    }

    /**
     * Установить значение для "playersUsedReferralNick"
     *
     * @param playerUUID UUID игрока (владелец реферального ника)
     * @param nicknames  Имена игроков, которые использовали реферальный ник
     * @return CompletableFuture Завершение установки значения
     */
    public CompletableFuture<Void> setPlayersUsedReferralNick(UUID playerUUID, List<String> nicknames) {
        return CompletableFuture.runAsync(() -> {
            // Check if the player exists inside the database
            String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                statement.setString(1, String.valueOf(playerUUID));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        // Set the value for "playersUsedReferralNick"
                        String updatePlayerSQL = "UPDATE referrals SET playersUsedReferralNick = ? WHERE referralOwnerUUID = ?";
                        try (PreparedStatement statement2 = connection.prepareStatement(updatePlayerSQL)) {
                            // Serialize the list of nicknames to a JSON array (use a JSON library)
                            String nicknamesJson = JsonSerializer.serializeToJSON(nicknames);
                            statement2.setString(1, nicknamesJson);
                            statement2.setString(2, String.valueOf(playerUUID));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            Main.log.severe("Error setting the value for \"playersUsedReferralNick\": " + e.getMessage());
                        }
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка установки значения для \"referralOwnerName\" и \"referralOwnerUUID\": " + e.getMessage());
            }

            // Set the value for "playersUsedReferralNick"
            String updatePlayerSQL = "UPDATE referrals SET playersUsedReferralNick = ? WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                // Serialize the list of nicknames to a JSON array (use a JSON library)
                String nicknamesJson = JsonSerializer.serializeToJSON(nicknames);
                statement.setString(1, nicknamesJson);
                statement.setString(2, String.valueOf(playerUUID));
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Error setting the value for \"playersUsedReferralNick\": " + e.getMessage());
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
    public CompletableFuture<Void> addAmountOfTimesUsed(Player player) {
        return CompletableFuture.runAsync(() -> {
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
            }

            // Добавление +1 к значению для "amountOfTimesUsed"
            String updatePlayerSQL = "UPDATE referrals SET amountOfTimesUsed = amountOfTimesUsed + 1 WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка добавления +1 к значению для \"amountOfTimesUsed\": " + e.getMessage());
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
    public CompletableFuture<Void> addAmountOfTimesUsed(UUID UUID) {
        return CompletableFuture.runAsync(() -> {
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
            }

            // Добавление +1 к значению для "amountOfTimesUsed"
            String updatePlayerSQL = "UPDATE referrals SET amountOfTimesUsed = amountOfTimesUsed + 1 WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                statement.setString(1, UUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка добавления +1 к значению для \"amountOfTimesUsed\": " + e.getMessage());
            }
        });
    }

    /**
     * Получить значение для "amountOfTimesUsed"
     *
     * @param player Игрок (владелец реферального ника)
     * @return CompletableFuture Завершение получения значения
     */
    public CompletableFuture<Integer> getAmountOfTimesUsed(Player player) {
        return CompletableFuture.supplyAsync(() -> {
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
    public CompletableFuture<Integer> getAmountOfTimesUsed(UUID UUID) {
        return CompletableFuture.supplyAsync(() -> {
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
            }
            return 0;
        });
    }
}
