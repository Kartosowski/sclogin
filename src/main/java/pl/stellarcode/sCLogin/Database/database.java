package pl.stellarcode.sCLogin.Database;

import com.velocitypowered.api.proxy.Player;
import org.spongepowered.configurate.ConfigurationNode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.stellarcode.sCLogin.Listener.onPlayerPreLoginEvent;
import pl.stellarcode.sCLogin.SCLogin;
import pl.stellarcode.sCLogin.Util.parse;

import java.sql.*;
import java.util.UUID;


public class database {
    private Connection connection;
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public boolean createDefaultTables() throws SQLException {

        String type = SCLogin.typeDB;

        String sql;
        if (type.equalsIgnoreCase("MySQL")) {
            sql = """
            CREATE TABLE IF NOT EXISTS SClogin (
                id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                nickname TEXT NOT NULL,
                last_ip VARCHAR(36),
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                premium BOOLEAN,
                password TEXT
            )
            """;
        } else if (type.equalsIgnoreCase("SQLite")) {
            sql = """
            CREATE TABLE IF NOT EXISTS SClogin (
                id INTEGER PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                nickname TEXT NOT NULL,
                last_ip VARCHAR(36),
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                premium BOOLEAN,
                password TEXT
            )
            """;
        } else {
            sql = """
            CREATE TABLE IF NOT EXISTS SClogin (
                id INTEGER PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                nickname TEXT NOT NULL,
                last_ip VARCHAR(36),
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                premium BOOLEAN,
                password TEXT
            )
            """;
        }

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.executeUpdate();

        sql = """
        CREATE TABLE IF NOT EXISTS SClogin_data (
            uuid VARCHAR(36),
            nickname TEXT NOT NULL,
            premium BOOLEAN
        )
        """;

        preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.executeUpdate();

        return true;
    }

    public boolean InsertPlayer(UUID uuid, String name, String ip, Boolean ispremium, String password) throws SQLException {
        String sql = """
        INSERT INTO SClogin (
            uuid,
            nickname,
            last_ip,
            premium,
            password
        ) VALUES (?, ?, ?, ?, ?)
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, uuid.toString());
        preparedStatement.setString(2, name);
        preparedStatement.setString(3, ip);
        preparedStatement.setBoolean(4, ispremium);
        if (password != null) {
            String hashedPassword = passwordEncoder.encode(password);
            preparedStatement.setString(5, hashedPassword);
        } else {
            preparedStatement.setString(5, null);
        }

        preparedStatement.executeUpdate();
        return true;
    }



    public boolean UpdatePlayer(Player player) throws SQLException {
        String sql = """
        UPDATE SClogin SET nickname = ?, last_ip = ?, last_seen = CURRENT_TIMESTAMP WHERE uuid = ?
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, player.getUsername());
        preparedStatement.setString(2, player.getRemoteAddress().getAddress().getHostAddress());
        preparedStatement.setString(3, String.valueOf(player.getUniqueId()));

        preparedStatement.executeUpdate();
        return true;
    }

    public boolean UpdatePremium(Player player, Boolean premium) throws SQLException {
        String sql = """
        UPDATE SClogin SET uuid = ?, premium = ? WHERE uuid = ?
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, onPlayerPreLoginEvent.currentUUID.get(player.getRemoteAddress().getAddress().getHostAddress()).toString());
        preparedStatement.setBoolean(2, premium);
        preparedStatement.setString(3, player.getUniqueId().toString());
        preparedStatement.executeUpdate();

        return true;
    }


    public boolean UpdatePlayerPassword(UUID uuid, String password) throws SQLException {
        String hashedPassword = passwordEncoder.encode(password);

        String sql = """
        UPDATE SClogin SET password = ? WHERE uuid = ?
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, hashedPassword);
        preparedStatement.setString(2, uuid.toString());

        preparedStatement.executeUpdate();
        return true;
    }


    public boolean isPlayerInDatabase(UUID uuid) throws SQLException {
        String sql = """
            SELECT uuid FROM SClogin WHERE uuid = ?;
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, uuid.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    public boolean isPlayerInDatabaseByName(String name) throws SQLException {
        String sql = """
            SELECT nickname FROM SClogin WHERE nickname = ?;
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, name);
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    public boolean PasswordMatch(UUID uuid, String password) throws SQLException {
        String sql = "SELECT password FROM SClogin WHERE uuid = ?";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String storedPassword = resultSet.getString(1);
                    return passwordEncoder.matches(password, storedPassword);
                } else {
                    return false;
                }
            }
        }
    }

    public boolean PasswordMatchesOld(UUID uuid, String password) throws SQLException {
        String sql = "SELECT password FROM SClogin WHERE uuid = ?";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String storedPassword = resultSet.getString(1);
                    return passwordEncoder.matches(password, storedPassword);
                } else {
                    return false;
                }
            }
        }
    }


    public String getUUIDByName(String name) throws SQLException {
        String sql = "SELECT uuid FROM SClogin WHERE nickname = ?";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, name);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                } else {
                    return null;
                }
            }
        }
    }

    public boolean checkIfPasswordIsNull(UUID uuid) throws SQLException {
        String sql = "SELECT password FROM SClogin WHERE uuid = ?";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, uuid.toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("password") == null;
                }
                return true;
            }
        }
    }


    public boolean isPlayerRegistered(UUID uuid) throws SQLException {
        String sql = """
            SELECT uuid FROM SClogin WHERE uuid = ?;
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, uuid.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    public String getPlayerName(String name) throws SQLException {
        String type = SCLogin.typeDB;

        String sql = null;
        if (type.equals("SQLite")) {
            sql = """
            SELECT nickname FROM SClogin WHERE nickname = ? COLLATE NOCASE;
            """;
        } else if (type.equals("MySQL")) {
            sql = """
            SELECT nickname FROM SCLogin WHERE nickname = ?;
            """;
        }


        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, name);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getString(1);
        } else {
            return null;
        }
    }

    public boolean isPremiumByUUID(UUID uuid) throws SQLException {
        String sql = """
            SELECT premium FROM SClogin WHERE uuid = ?;
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, uuid.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getBoolean(1);
        }

        return false;
    }

    public boolean isPremiumByName(String name) throws SQLException {
        String sql = """
            SELECT premium FROM SClogin WHERE nickname = ?;
        """;

        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, name);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getBoolean(1);
        }

        return false;
    }

    public Connection getConnection() throws SQLException {

        if (this.connection != null) {
            return this.connection;
        }

        ConfigurationNode config = SCLogin.getConfig();
        String type = SCLogin.typeDB;

        if (type.equalsIgnoreCase("mysql")) {
            String host = config.node("database", "hostname").getString("localhost");
            String database = config.node("database", "database").getString("database");
            int port = config.node("database", "port").getInt(3306);
            String user = config.node("database", "username").getString("root");
            String password = config.node("database", "password").getString("");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?allowPublicKeyRetrieval=true&useSSL=false";
            SCLogin.getLogger().info("Using MySQL as database");
            this.connection = DriverManager.getConnection(url, user, password);
        } else if (type.equalsIgnoreCase("sqlite")) {
            SCLogin.getLogger().info("Using SQLite as database");
            String url = "jdbc:sqlite:"+SCLogin.getDataDirectory().getParent()+"/SCLogin/database.db";
            this.connection = DriverManager.getConnection(url);

        } else {
            SCLogin.getServer().getConsoleCommandSource().sendMessage(parse.colors("<red>There was an error in the Configuration. Using SQLite as database</red>"));
            String url = "jdbc:sqlite:"+SCLogin.getDataDirectory().getParent()+"/SCLogin/database.db";
            this.connection = DriverManager.getConnection(url);
        }


        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

}
