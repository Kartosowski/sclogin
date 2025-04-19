package pl.stellarcode.sCLogin.Handlers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import pl.stellarcode.sCLogin.Log;
import pl.stellarcode.sCLogin.SCLogin;
import pl.stellarcode.sCLogin.Util.parse;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class AuthHandlerSession implements LimboSessionHandler {
    private LimboPlayer player;
    private final Logger logger = SCLogin.getLogger();
    private final pl.stellarcode.sCLogin.Database.database database = SCLogin.getCurrentDatabase();
    private ScheduledTask task;
    private boolean logged;
    private boolean needToRegister;
    private boolean needToUpdatePassword;
    private int attempts = 0;
    private int max_attempts = SCLogin.getConfig().node("security", "bruteforce", "max-login-tries").getInt();
    @Override
    public void onChat(String chat) {
        if (logged) {
            return;
        }

        String[] args = chat.split(" ");
        if (args.length != 0) {
            if (args[0].equals("/login")) {
                if (args.length >= 2) {
                    try {
                        if (database.PasswordMatch(player.getProxyPlayer().getUniqueId(), args[1])) {
                            logged = true;
                            task.cancel();
                            player.getProxyPlayer().sendActionBar(Component.text(""));
                            player.disconnect();
                        } else {
                            attempts++;
                            if (attempts >= max_attempts) {
                                Bruteforce.addBlockAttempt(player.getProxyPlayer().getRemoteAddress().getAddress().getHostAddress().toString());
                                player.getProxyPlayer().disconnect(parse.colors(SCLogin.getMessage("kick-too-many-attempts")));
                            }
                            player.getProxyPlayer().sendMessage(parse.colors(SCLogin.getMessage("incorrect-password")));
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    player.getProxyPlayer().sendMessage(parse.colors(SCLogin.getMessage("msg-login")));
                }

            }

            if (!needToUpdatePassword) {
                if (!needToRegister) {
                    return;
                }
            }

            if (args[0].equals("/register")) {
                ConfigurationNode config = SCLogin.getConfig();
                if (args.length >= 3) {
                    if (args[1].length() >= config.node("security","password","small").getInt(6) && args[1].length() <= config.node("security","password","large").getInt(32)) {
                        if (args[1].equals(args[2])) {
                            try {
                                if (needToUpdatePassword) {
                                    database.UpdatePlayerPassword(player.getProxyPlayer().getUniqueId(), args[1]);
                                    player.getProxyPlayer().sendActionBar(Component.text(""));
                                    logged = true;
                                    task.cancel();
                                } else {
                                    Player playerr = player.getProxyPlayer();
                                    database.InsertPlayer(playerr.getUniqueId(), playerr.getUsername(), playerr.getRemoteAddress().getAddress().getHostAddress(), database.isPremiumByUUID(player.getProxyPlayer().getUniqueId()), args[1]);
                                    player.getProxyPlayer().sendActionBar(Component.text(""));
                                    logged = true;
                                    Log.msg("<gold>SCLOGIN</gold> <yellow>Created non-premium account for " + player.getProxyPlayer().getUsername());
                                    task.cancel();
                                }

                                player.disconnect();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            player.getProxyPlayer().sendMessage(parse.colors(SCLogin.getMessage("password-dont-match")));
                        }
                    } else {
                        player.getProxyPlayer().sendMessage(parse.colors(SCLogin.getMessage("password-length")));
                    }
                } else {
                    player.getProxyPlayer().sendMessage(parse.colors(SCLogin.getMessage("msg-register")));
                }
            }
        }
    }
    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        logged = false;
        Player proxiedPlayer = player.getProxyPlayer();
        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(60), Duration.ZERO);
        Repeat(player);
        try {
            if (database.isPlayerRegistered(proxiedPlayer.getUniqueId())) {
                if (database.isPremiumByUUID(proxiedPlayer.getUniqueId())) {
                    if (database.checkIfPasswordIsNull(proxiedPlayer.getUniqueId())) {
                        needToUpdatePassword = true;
                    } else {
                        needToRegister = false;
                    }
                }
            } else {
                needToRegister = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (needToRegister || needToUpdatePassword) {
            sendTitle(proxiedPlayer, SCLogin.getMessage("title-register"), SCLogin.getMessage("subtitle-register"), times);
            proxiedPlayer.sendMessage(parse.colors(SCLogin.getMessage("msg-register")));
        } else {
            sendTitle(proxiedPlayer, SCLogin.getMessage("title-login"), SCLogin.getMessage("subtitle-login"), times);
            proxiedPlayer.sendMessage(parse.colors(SCLogin.getMessage("msg-login")));
        }




    }


    public void sendTitle(Player player, String title, String subtitle, Title.Times times) {
        player.sendTitlePart(TitlePart.TIMES, times);
        player.sendTitlePart(TitlePart.TITLE, parse.colors(title));
        player.sendTitlePart(TitlePart.SUBTITLE, parse.colors(subtitle));
    }

    public void Repeat(LimboPlayer player) {
        int countermax = SCLogin.getConfig().node("login", "time-to-login").getInt(30);
        task = SCLogin.getServer().getScheduler().buildTask(SCLogin.getPlugin(), new Runnable() {

            int counter = 0;

            @Override
            public void run() {
                if (counter >= countermax) {
                    if (!logged) {
                        player.getProxyPlayer().disconnect(parse.colors(SCLogin.getMessage("kick-time-expired")));
                    }

                    task.cancel();
                    return;
                }

                counter++;
                String msg = SCLogin.getMessage("time-left").replace("{time}", String.valueOf(countermax - counter + 1));
                player.getProxyPlayer().sendActionBar(parse.colors(msg));
            }
        }).repeat(1, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void onDisconnect() {
        if (SCLogin.getConfig().node("login", "clean-chat-after-login").getBoolean(true)) {
            player.getProxyPlayer().sendMessage(Component.text("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"));
        }

        try {
            database.UpdatePlayer(player.getProxyPlayer());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        task.cancel();
    }



}
