package de.silke.referralpaper.database;

import de.silke.referralpaper.Main;
import lombok.Data;
import org.bukkit.entity.Player;

import java.sql.*;
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
                String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=" + useSSL + "&autoReconnect=" + autoReconnect;
                connection = DriverManager.getConnection(url, USERNAME, PASSWORD);
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
                        + "playersUsedReferralNick VARCHAR(64),"
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
                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
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
                statement.setString(3, "");
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
     * Автоматически установить значение для "referralOwnerName" и "referralOwnerUUID"
     * <p>referralOwnerName - Имя игрока, который пригласил игрока
     * <p>referralOwnerUUID - UUID игрока, который пригласил игрока
     *
     * @param player Игрок
     * @return CompletableFuture Завершение установки значения
     */
    public CompletableFuture<Void> setReferralOwner(Player player) {
        return CompletableFuture.runAsync(() -> {
            String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
            // Проверка на наличие игрока внутри БД
            // Если он существует, то в консоли будет ошибка
            try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
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
     * Добавить игрока к значению "playersUsedReferralNick"
     * <p>playersUsedReferralNick - Имена игроков, которые использовали реферальный ник
     *
     * @param player         Игрок
     * @param usedPlayerName Имя игрока, который использовал реферальный ник
     * @return CompletableFuture Завершение добавления значения
     */
    public CompletableFuture<Void> addPlayerToUsedReferralNick(Player player, String usedPlayerName) {
        return CompletableFuture.runAsync(() -> {
            String selectPlayerSQL = "SELECT * FROM referrals WHERE referralOwnerUUID = ?";
            // Проверка на наличие игрока внутри БД
            // Если он существует, то в консоли будет ошибка
            try (PreparedStatement statement = connection.prepareStatement(selectPlayerSQL)) {
                statement.setString(1, player.getUniqueId().toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
                    }
                }
            } catch (SQLException e) {
                Main.log.severe("Ошибка добавления игрока к значению \"playersUsedReferralNick\": " + e.getMessage());
            }

            // Добавление игрока к значению "playersUsedReferralNick"
            String updatePlayerSQL = "UPDATE referrals SET playersUsedReferralNick = ? WHERE referralOwnerUUID = ?";
            try (PreparedStatement statement = connection.prepareStatement(updatePlayerSQL)) {
                statement.setString(1, usedPlayerName);
                statement.setString(2, player.getUniqueId().toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                Main.log.severe("Ошибка добавления игрока к значению \"playersUsedReferralNick\": " + e.getMessage());
            }

            // Если игрок использовал рефералку, то скорее всего стоит добавить +1 к значению "amountOfTimesUsed"
            addAmountOfTimesUsed(player).join();
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
                        Main.log.severe("Игрок " + player.getName() + " уже существует в базе данных!");
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
}
