package pl.stellarcode.sCLogin.Listener;

import com.velocitypowered.api.event.Subscribe;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import pl.stellarcode.sCLogin.Handlers.AuthHandlerSession;
import pl.stellarcode.sCLogin.SCLogin;

import java.sql.SQLException;

public class onLoginLimboRegisterEvent {

    private final pl.stellarcode.sCLogin.Database.database database = SCLogin.getCurrentDatabase();

    @Subscribe
    public void event(LoginLimboRegisterEvent event) throws SQLException {
        boolean autologin = SCLogin.getConfig().node("login", "premium-auto-login").getBoolean(false);
        if (autologin) {
            if (database.isPremiumByUUID(event.getPlayer().getUniqueId())) {
                database.UpdatePlayer(event.getPlayer());
            } else {
                event.addCallback(() -> SCLogin.getLimbo().spawnPlayer(event.getPlayer(), new AuthHandlerSession()));
            }
        } else {
            event.addCallback(() -> SCLogin.getLimbo().spawnPlayer(event.getPlayer(), new AuthHandlerSession()));
        }
    }


}
