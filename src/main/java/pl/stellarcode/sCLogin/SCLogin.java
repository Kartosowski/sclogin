package pl.stellarcode.sCLogin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.command.LimboCommandMeta;
import net.elytrium.limboapi.api.player.GameMode;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import pl.stellarcode.sCLogin.Commands.changepass;
import pl.stellarcode.sCLogin.Commands.premium;
import pl.stellarcode.sCLogin.Commands.sclogin;
import pl.stellarcode.sCLogin.Database.database;
import pl.stellarcode.sCLogin.Listener.*;
import pl.stellarcode.sCLogin.Util.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;

@Plugin(id = "sclogin", name = "SCLogin", version = "1.0", authors = {"Kartos"}, dependencies = {@Dependency(id = "limboapi")})
public class SCLogin {

    private final String Version = "TEST-BUILD-1.0";
    private static SCLogin plugin;
    private final ProxyServer server;
    private final Logger logger;
    private static database database = null;
    private Limbo limbo;
    private final LimboFactory limboFactory;
    private static ConfigurationNode Config;
    private static ConfigurationNode ConfigMessage;
    private final Path configPath = Paths.get("plugins", "SCLogin", "config.yml");
    private final Path configMessagePath = Paths.get("plugins", "SCLogin", "messages.yml");
    private static YamlConfigurationLoader configLoader;
    private static YamlConfigurationLoader configMessageLoader;
    private final Path dataDirectory;
    public static String typeDB;
    @Inject
    public SCLogin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.limboFactory = (LimboFactory) server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
        this.dataDirectory = dataDirectory;
        plugin = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws SQLException, IOException {
        long startTime = System.currentTimeMillis();
        server.getConsoleCommandSource().sendMessage(parse.colors("\n\n <green>SCLogin </green>" + Version+ "\n <dark_green>By Kartos</dark_green>" + "\n Loading...\n"));
        configintialize();

        typeDB = SCLogin.getConfig().node("database","type").getString("SQLite");

        database = new database();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Class.forName("org.sqlite.JDBC");

        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }

        if (!database.createDefaultTables()) {
            logger.warn("MYSQL Error could not create tables.");
        }
        VirtualWorld world = limboFactory.createVirtualWorld(Dimension.THE_END, 0, 0, 0, 0.0F, 0.0F);
        limbo = limboFactory.createLimbo(world).setName("SC-AUTH")
                .setGameMode(GameMode.SPECTATOR)
                .setShouldUpdateTags(false)
                .registerCommand(new LimboCommandMeta(Collections.singleton("login")))
                .registerCommand(new LimboCommandMeta(Collections.singleton("register")));

        server.getEventManager().register(this, new onPlayerPreLoginEvent());
        server.getEventManager().register(this, new onLoginLimboRegisterEvent());
        server.getEventManager().register(this, new onPlayerDisconnect());
        server.getEventManager().register(this, new onServerPreConnectEvent());
        server.getCommandManager().register(premium.createBrigadierCommand(server));
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("changepass").build(),
                new changepass()
        );
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("sclogin").build(),
                new sclogin()
        );

        long loadTime = System.currentTimeMillis() - startTime;
        logger.info("Loaded plugin in " + loadTime + "ms!");
    }


    public void configintialize() throws IOException {
        Path parentDir = configPath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        if (!Files.exists(configPath)) {
            copyConfig("config", parentDir);
        }
        configLoader = YamlConfigurationLoader.builder()
                .path(Paths.get("plugins", "SCLogin", "config.yml"))
                .build();


        Path parentDir2 = configMessagePath.getParent();
        if (!Files.exists(parentDir2)) {
            Files.createDirectories(parentDir2);
        }

        if (!Files.exists(configMessagePath)) {
            copyConfig("messages", parentDir2);
        }
        configMessageLoader = YamlConfigurationLoader.builder()
                .path(Paths.get("plugins", "SCLogin", "messages.yml"))
                .build();

        loadConfig();
    }

    public void loadConfig() throws ConfigurateException {
        Config = configLoader.load();
        ConfigMessage = configMessageLoader.load();
    }

    public static void reloadConfig() throws ConfigurateException {
        Config = configLoader.load();
        ConfigMessage = configMessageLoader.load();
    }

    private void copyConfig(String config, Path dir) throws IOException {
        try (InputStream in = SCLogin.class.getClassLoader().getResourceAsStream(config + ".yml")) {

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path outputPath = config.equals("config") ? configPath : configMessagePath;

            try (OutputStream out = Files.newOutputStream(outputPath)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }


    public static String getMessage(String path) {
        return plugin.ConfigMessage.node(path).getString("");
    }

    public static ConfigurationNode getConfig() { return plugin.Config; }

    public static Path getDataDirectory() { return plugin.dataDirectory; }

    public static Limbo getLimbo() {
        return plugin.limbo;
    }

    public static SCLogin getPlugin() {
        return plugin;
    }

    public static ProxyServer getServer() {
        return plugin.server;
    }

    public static Logger getLogger() {
        return plugin.logger;
    }

    public static database getCurrentDatabase() { return database; }

}
