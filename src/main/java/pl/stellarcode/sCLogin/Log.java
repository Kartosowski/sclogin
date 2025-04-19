package pl.stellarcode.sCLogin;

import pl.stellarcode.sCLogin.Util.parse;

public class Log {
    public static void msg(String string) {
        if (SCLogin.getConfig().node("logs", "enabled").getBoolean(true)) {
            SCLogin.getServer().getConsoleCommandSource().sendMessage(parse.colors(string));
        }
    }
}
