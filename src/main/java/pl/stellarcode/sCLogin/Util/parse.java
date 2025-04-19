package pl.stellarcode.sCLogin.Util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class parse {
    public static Component colors(String string) {
        MiniMessage miniMessage = MiniMessage.miniMessage();

        return miniMessage.deserialize(string);
    }
}
