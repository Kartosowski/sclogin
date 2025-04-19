package pl.stellarcode.sCLogin.Listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import pl.stellarcode.sCLogin.SCLogin;

public class onServerPreConnectEvent {

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!SCLogin.getConfig().node("login", "redirect-after-login", "last-server").getBoolean()) {
            String server = SCLogin.getConfig().node("login", "redirect-after-login", "redirect").getString("lobby2");

            if (event.getPlayer().getCurrentServer().isPresent() &&
                    event.getPlayer().getCurrentServer().get().getServerInfo().getName().equals(server)) {
                return;
            }
            
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(SCLogin.getServer().getServer(server).get()));
        }
    }
}
