package pl.stellarcode.sCLogin.Commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.configurate.ConfigurationNode;
import pl.stellarcode.sCLogin.Database.database;
import pl.stellarcode.sCLogin.SCLogin;
import pl.stellarcode.sCLogin.Util.parse;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class changepass implements RawCommand {
    private static final HashMap<UUID, Instant> cooldown = new HashMap<>();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments().split("\\s+");

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return;
        }

        if (cooldown.containsKey(player.getUniqueId()) && cooldown.get(player.getUniqueId()).isAfter(Instant.now())) {
            source.sendMessage(parse.colors(SCLogin.getMessage("cooldown")));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(parse.colors(SCLogin.getMessage("correct-usage-of-changepass")));
            return;
        }

        String oldPassword = args[0];
        String newPassword = args[1];
        database database = SCLogin.getCurrentDatabase();

        try {
            if (database.checkIfPasswordIsNull(player.getUniqueId())) {
                source.sendMessage(parse.colors(SCLogin.getMessage("can-not-change-password-premium")));
                return;
            }

            if (!database.PasswordMatch(player.getUniqueId(), oldPassword)) {
                source.sendMessage(parse.colors(SCLogin.getMessage("incorrect-password")));
                return;
            }

            ConfigurationNode config = SCLogin.getConfig();
            int minLength = config.node("security", "password", "small").getInt(6);
            int maxLength = config.node("security", "password", "large").getInt(32);

            if (newPassword.length() < minLength || newPassword.length() > maxLength) {
                source.sendMessage(parse.colors(SCLogin.getMessage("password-length")));
                return;
            }

            if (database.PasswordMatchesOld(player.getUniqueId(), newPassword)) {
                source.sendMessage(parse.colors(SCLogin.getMessage("new-password-matches-old-password")));
                return;
            }

            database.UpdatePlayerPassword(player.getUniqueId(), newPassword);
            source.sendMessage(parse.colors(SCLogin.getMessage("password-changed")));

            cooldown.put(player.getUniqueId(), Instant.now().plusSeconds(15));

        } catch (SQLException e) {
            source.sendMessage(parse.colors(SCLogin.getMessage("error")));
            e.printStackTrace();
        }
    }
}
