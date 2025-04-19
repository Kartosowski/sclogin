package pl.stellarcode.sCLogin.Commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import pl.stellarcode.sCLogin.API.MojangAPI;
import pl.stellarcode.sCLogin.Database.database;
import pl.stellarcode.sCLogin.Listener.onPlayerPreLoginEvent;
import pl.stellarcode.sCLogin.SCLogin;
import pl.stellarcode.sCLogin.Util.parse;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class premium {

    private static final HashMap<UUID, Instant> alreadyChecked = new HashMap<>();

    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy) {
        LiteralCommandNode<CommandSource> premiumNode = BrigadierCommand.literalArgumentBuilder("premium")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (!(source instanceof Player)) {
                        source.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    Player player = (Player) source;
                    UUID playerUUID = player.getUniqueId();
                    Component message = Component.text("An unknown error occurred.", NamedTextColor.RED);

                    if (alreadyChecked.containsKey(playerUUID) && alreadyChecked.get(playerUUID).isAfter(Instant.now())) {
                        message = parse.colors(SCLogin.getMessage("cooldown"));
                    } else {
                        player.sendMessage(parse.colors(SCLogin.getMessage("verifying")));
                        alreadyChecked.put(playerUUID, Instant.now().plusSeconds(30));
                        database db = SCLogin.getCurrentDatabase();

                        try {
                            if (db.isPremiumByUUID(playerUUID)) {
                                message = parse.colors(SCLogin.getMessage("already-premium"));
                            } else {
                                String ip = player.getRemoteAddress().getAddress().getHostAddress();
                                UUID currentUUID = onPlayerPreLoginEvent.currentUUID.get(ip);
                                Boolean[] apiResult = MojangAPI.isPremium(player.getUsername(), currentUUID);

                                if (apiResult[0]) {
                                    if (apiResult[1]) {
                                        onPlayerPreLoginEvent.nonpremium.remove(playerUUID);
                                        player.disconnect(parse.colors(SCLogin.getMessage("premium-rejoin")));
                                        db.UpdatePremium(player, true);
                                        return Command.SINGLE_SUCCESS;
                                    } else {
                                        message = parse.colors(SCLogin.getMessage("nonpremium"));
                                    }
                                } else {
                                    message = parse.colors(SCLogin.getMessage("error"));
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            message = Component.text("A database error occurred.", NamedTextColor.RED);
                        }
                    }

                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .build();

        return new BrigadierCommand(premiumNode);
    }
}
