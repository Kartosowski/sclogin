package pl.stellarcode.sCLogin.Commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import pl.stellarcode.sCLogin.SCLogin;
import pl.stellarcode.sCLogin.Util.parse;

public class sclogin implements RawCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("sclogin.commands")) {
            source.sendMessage(parse.colors(SCLogin.getMessage("no-permission")));
            return;
        }
        String[] args = invocation.arguments().split("\\s+");

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            long startTime = System.currentTimeMillis();

            try {
                SCLogin.reloadConfig();
                long loadTime = System.currentTimeMillis() - startTime;
                source.sendMessage(parse.colors("<yellow> Config and messages have been reloaded in</yellow> <gold>" + loadTime + "ms!</gold>\n<gray> Some features were not reloaded by the command;\n you will have to restart the server."));
            } catch (Exception e) {
                source.sendMessage(Component.text("Failed to reload config!", NamedTextColor.RED));
                e.printStackTrace();
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("help")) { {
            source.sendMessage(parse.colors("\n <gold>SCLogin - Commands</gold> \n <click:suggest_command:'/sclogin reload'><hover:show_text:'Click here to suggest command'>/sclogin reload</hover></click> <gray>Reloads config</gray>\n <click:suggest_command:'/premium'><hover:show_text:'Click here to suggest command'>/premium</hover></click> <gray>Change state of user account to premium</gray>\n <click:suggest_command:'/changepass'><hover:show_text:'Click here to suggest command'>/changepass</hover></click> <gray>Change user's password</gray> \n <yellow>Discord: <white>https://discord.gg/dvET3MzDC4</yellow>\n"));
        }
        } else {
            source.sendMessage(parse.colors("<red>Unknown arguments!</red> <gray>Check <white>/sclogin help</white> for commands!</gray>"));
        }
    }
}
