package pl.stellarcode.sCLogin.Listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import pl.stellarcode.sCLogin.SCLogin;

import java.sql.SQLException;

public class onPlayerDisconnect {
    private final pl.stellarcode.sCLogin.Database.database database = SCLogin.getCurrentDatabase();

    @Subscribe
    public void onDisconnect(DisconnectEvent event) throws SQLException {
        if (database.isPlayerRegistered(event.getPlayer().getUniqueId())) {
            database.UpdatePlayer(event.getPlayer());
        }
    }
}
