package de.silke.referralpaper.database;

import de.silke.referralpaper.Main;
import lombok.Data;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
@SuppressWarnings({"UnusedReturnValue"})
public class PlayerInfoDatabaseConnection {
    private static final String HOST = Main.plugin.getConfig().getString("database.host");
    private static final String PORT = Main.plugin.getConfig().getString("database.port");
    private static final String DATABASE = Main.plugin.getConfig().getString("database.database");
    private static final String USERNAME = Main.plugin.getConfig().getString("database.username");
    private static final String PASSWORD = Main.plugin.getConfig().getString("database.password");
    private static final boolean useSSL = Main.plugin.getConfig().getBoolean("database.useSSL");
    private static final boolean autoReconnect = Main.plugin.getConfig().getBoolean("database.autoReconnect");
    private DatabaseConnectionManager connectionManager;
    private Connection connection;

    public PlayerInfoDatabaseConnection() {
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
//            close().join();
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

                connection.createStatement().executeUpdate(createTableSQL);
            } catch (SQLException e) {
                Main.log.severe("Ошибка создания таблицы в базе данных: " + e.getMessage());
                reconnect().join();
            }
        });
    }

    public CompletableFuture<ResultSet> executeQuery(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                statement.execute();
                try (ResultSet resultSet = statement.getResultSet()) {
                    if (resultSet.next()) {
                        return resultSet;
                    } else {
                        return null;
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка выполнения запроса: " + e.getMessage());
                return null;
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
                if (player != null) {
                    String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                    try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                        statement.setString(1, player.getUniqueId().toString());
                        try (ResultSet resultSet = statement.executeQuery()) {
                            return resultSet.next();
                        }
                    } catch (SQLException e) {
                        Main.log.severe("Ошибка проверки наличия игрока в базе данных: " + e.getMessage());
                        reconnect().join();
                    }
                } else {
//                Main.log.severe("Ошибка проверки наличия игрока в базе данных: Игрок не найден!");
                    return CompletableFuture.completedFuture(false).join();
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
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
                    String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                    try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                        statement.setString(1, playerUUID.toString());
                        try (ResultSet resultSet = statement.executeQuery()) {
                            return resultSet.next();
                        }
                    } catch (SQLException e) {
                        Main.log.severe("Ошибка проверки наличия игрока в базе данных: " + e.getMessage());
                        reconnect().join();
                    }
                } else {
                    Main.log.severe("Ошибка проверки наличия игрока в базе данных: Игрок не найден!");
                    return CompletableFuture.completedFuture(false).join();
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
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления игрока в базу данных: " + e.getMessage());
                    reconnect().join();
                }

                // Добавление игрока в БД
                String insertPlayerSQL = "INSERT INTO player_information (uuid, player_name, declinedReferQuestion, referredPlayerUUID, referredPlayerName, luckPermsRole, timePlayed, registrationDate) VALUES (?, ?, false, ?, ?, ?, 0, ?);";
                try (PreparedStatement statement = connection.prepareStatement(insertPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setString(2, player.getName());
                    statement.setString(3, null);
                    statement.setString(4, null);
                    statement.setString(5, null);
                    statement.setDate(6, new Date(System.currentTimeMillis()));
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
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных! (removePlayer)");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка удаления игрока из базы данных: " + e.getMessage());
                    reconnect().join();
                }

                // Удаление игрока из БД
                String deletePlayerSQL = "DELETE FROM player_information WHERE uuid = ?";
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
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
//                        Main.log.severe("Игрок с UUID " + playerUUID.toString() + " не существует в базе данных! (getPlayerName)");
                            return null;
                        }

                        return resultSet.getString("player_name");
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения ника игрока: " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Установить значение для "declinedReferQuestion"
     * <p>declinedReferQuestion - отказался ли игрок от вопроса об игроке, который его пригласил
     *
     * @param player Игрок
     * @param value  Значение
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setDeclinedReferQuestion(Player player, boolean value) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных! (setDeclinedReferQuestion)");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"declinedReferQuestion\": " + e.getMessage());
                    reconnect().join();
                }

                // Установка значения для "declinedReferQuestion"
                String updatePlayerSQL = "UPDATE player_information SET declinedReferQuestion = ? WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setBoolean(1, value);
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"declinedReferQuestion\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Автоматически установить значения для "referredPlayerUUID" и "referredPlayerName"
     * <p>referredPlayerUUID - UUID игрока, который пригласил игрока
     * <p>referredPlayerName - Имя игрока, который пригласил игрока
     *
     * @param player         Игрок
     * @param referredPlayer Игрок, которого игрок отметил как пригласившего его
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setReferredPlayer(Player player, Player referredPlayer) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных! (setReferredPlayer)");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referredPlayerUUID\" и \"referredPlayerName\": " + e.getMessage());
                    reconnect().join();
                }

                // Установка значения для "referredPlayerUUID" и "referredPlayerName"
                String updatePlayerSQL = "UPDATE player_information SET referredPlayerUUID = ?, referredPlayerName = ? WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setString(1, referredPlayer.getUniqueId().toString());
                    statement.setString(2, referredPlayer.getName());
                    statement.setString(3, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referredPlayerUUID\" и \"referredPlayerName\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Автоматически установить значения для "referredPlayerUUID" и "referredPlayerName"
     * <p>referredPlayerUUID - UUID игрока, который пригласил игрока
     * <p>referredPlayerName - Имя игрока, который пригласил игрока
     *
     * @param player             Игрок
     * @param referredPlayerUUID UUID игрока, который пригласил игрока
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setReferredPlayer(Player player, UUID referredPlayerUUID) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных! (setReferredPlayer)");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referredPlayerUUID\" и \"referredPlayerName\": " + e.getMessage());
                    reconnect().join();
                }

                // Установка значения для "referredPlayerUUID" и "referredPlayerName"
                String updatePlayerSQL = "UPDATE player_information SET referredPlayerUUID = ?, referredPlayerName = ? WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setString(1, referredPlayerUUID.toString());
                    statement.setString(2, Bukkit.getOfflinePlayer(referredPlayerUUID).getName());
                    statement.setString(3, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"referredPlayerUUID\" и \"referredPlayerName\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Установить значение для "luckPermsRole"
     * <p>luckPermsRole - роль игрока в LuckPerms
     *
     * @param player Игрок
     * @param value  Значение
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setLuckPermsRole(Player player, String value) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (setLuckPermsRole)!");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"luckPermsRole\": " + e.getMessage());
                    reconnect().join();
                }

                // Установка значения для "luckPermsRole"
                String updatePlayerSQL = "UPDATE player_information SET luckPermsRole = ? WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setString(1, value);
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"luckPermsRole\": " + e.getMessage());
                    reconnect().join();
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Добавить значение к значению "timePlayed"
     * <p>timePlayed - время, которое игрок провёл на всех серверах (миллисекунды)
     *
     * @param player Игрок
     * @param value  Long значение (миллисекунды)
     * @return CompletableFuture Завершение добавления значения
     */
    @SneakyThrows
    public CompletableFuture<Void> addTimePlayed(Player player, long value) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {

                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (addTimePlayed)!");
                            return;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления значения для \"timePlayed\": " + e.getMessage());
                    reconnect().join();
                }

                // Добавление значения для "timePlayed"
                String updatePlayerSQL = "UPDATE player_information SET timePlayed = timePlayed + ? WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setLong(1, value);
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка добавления значения для \"timePlayed\": " + e.getMessage());
                }
            } else {
                reconnect().join();
            }
        });
    }

    /**
     * Установить значение для "registrationDate"
     * <p>registrationDate - дата регистрации игрока
     *
     * @param player Игрок
     * @param value  Дата регистрации
     * @return CompletableFuture Завершение установки значения
     */
    @SneakyThrows
    public CompletableFuture<Void> setRegistrationDate(Player player, Date value) {
        return CompletableFuture.runAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (setRegistrationDate)!");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"registrationDate\": " + e.getMessage());
                }

                // Установка значения для "registrationDate"
                String updatePlayerSQL = "UPDATE player_information SET registrationDate = ? WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                    statement.setDate(1, value);
                    statement.setString(2, player.getUniqueId().toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    Main.log.severe("Ошибка установки значения для \"registrationDate\": " + e.getMessage());
                }
            } else {
                reconnect().join();
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
                String selectPlayerSQL = "SELECT * FROM player_information WHERE player_name = ?";
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
                String getPlayerSQL = "SELECT uuid FROM player_information WHERE player_name = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, playerName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return UUID.fromString(resultSet.getString("uuid"));
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения UUID игрока по его нику: " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "declinedReferQuestion"
     * <p>declinedReferQuestion - отказался ли игрок от вопроса об игроке, который его пригласил
     *
     * @param player Игрок
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<Boolean> getDeclinedReferQuestion(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (getDeclinedReferQuestion)!");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"declinedReferQuestion\": " + e.getMessage());
                }

                // Получение значения для "declinedReferQuestion"
                String getPlayerSQL = "SELECT declinedReferQuestion FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getBoolean("declinedReferQuestion");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"declinedReferQuestion\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return false;
        });
    }

    /**
     * Получить значение "referredPlayerUUID"
     * <p>referredPlayerUUID - UUID игрока, который пригласил игрока
     *
     * @param player Игрок
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<String> getReferredPlayerUUID(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
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
                    Main.log.severe("Ошибка получения значения для \"referredPlayerUUID\": " + e.getMessage());
                }

                // Получение значения для "referredPlayerUUID"
                String getPlayerSQL = "SELECT referredPlayerUUID FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getString("referredPlayerUUID");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"referredPlayerUUID\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "referredPlayerName"
     * <p>referredPlayerName - Имя игрока, который пригласил игрока
     *
     * @param player Игрок
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<String> getReferredPlayerName(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он не существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (getReferredPlayerName)!");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"referredPlayerName\": " + e.getMessage());
                }

                // Получение значения для "referredPlayerName"
                String getPlayerSQL = "SELECT referredPlayerName FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getString("referredPlayerName");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"referredPlayerName\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "referredPlayerUUID" по UUID игрока
     * <p>referredPlayerUUID - UUID игрока, который пригласил игрока
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<String> getReferredPlayerName(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"referredPlayerName\": " + e.getMessage());
                }

                // Получение значения для "referredPlayerUUID"
                String getPlayerSQL = "SELECT referredPlayerName FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getString("referredPlayerName");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"referredPlayerName\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "luckPermsRole"
     * <p>luckPermsRole - роль игрока в LuckPerms
     *
     * @param player Игрок
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<String> getLuckPermsRole(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                // Если он существует, то в консоли будет ошибка
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (getLuckPermsRole)!");
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"luckPermsRole\": " + e.getMessage());
                }

                // Получение значения для "luckPermsRole"
                String getPlayerSQL = "SELECT luckPermsRole FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getString("luckPermsRole");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"luckPermsRole\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "timePlayed" по UUID игрока
     * <p>timePlayed - время, которое игрок провёл на всех серверах (миллисекунды)
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<Long> getTimePlayed(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
//                        Main.log.severe("Игрок с UUID " + playerUUID.toString() + " не существует в базе данных (getTimePlayed)!");
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"timePlayed\": " + e.getMessage());
                }

                // Получение значения для "timePlayed"
                String getPlayerSQL = "SELECT timePlayed FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getLong("timePlayed");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"timePlayed\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "registrationDate"
     * <p>registrationDate - дата регистрации игрока
     *
     * @param player Игрок
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<Date> getRegistrationDate(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {

                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок " + player.getName() + " не существует в базе данных (getRegistrationDate)!");
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"registrationDate\": " + e.getMessage());
                }

                // Получение значения для "registrationDate"
                String getPlayerSQL = "SELECT registrationDate FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, player.getUniqueId().toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getDate("registrationDate");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"registrationDate\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }

    /**
     * Получить значение "registrationDate" по UUID игрока
     * <p>registrationDate - дата регистрации игрока
     *
     * @param playerUUID UUID игрока
     * @return CompletableFuture Завершение получения значения
     */
    @SneakyThrows
    public CompletableFuture<Date> getRegistrationDate(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (connection != null) {
                String selectPlayerSQL = "SELECT * FROM player_information WHERE uuid = ?";
                // Проверка на наличие игрока внутри БД
                try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        // Если игрока нет в БД, то возвращаем null
                        if (!resultSet.next()) {
                            Main.log.severe("Игрок с UUID " + playerUUID + " не существует в базе данных (getRegistrationDate)!");
                            return null;
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"registrationDate\": " + e.getMessage());
                }

                // Получение значения для "registrationDate"
                String getPlayerSQL = "SELECT registrationDate FROM player_information WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(getPlayerSQL)) {
                    statement.setString(1, playerUUID.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getDate("registrationDate");
                        }
                    }
                } catch (SQLException e) {
                    Main.log.severe("Ошибка получения значения для \"registrationDate\": " + e.getMessage());
                }
            } else {
                reconnect().join();
                return null;
            }
            return null;
        });
    }
}