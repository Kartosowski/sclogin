package pl.stellarcode.sCLogin.Listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import pl.stellarcode.sCLogin.API.MojangAPI;
import pl.stellarcode.sCLogin.Handlers.Bruteforce;
import pl.stellarcode.sCLogin.Log;
import pl.stellarcode.sCLogin.SCLogin;
import pl.stellarcode.sCLogin.Util.parse;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;


public class onPlayerPreLoginEvent {

    private final pl.stellarcode.sCLogin.Database.database database = SCLogin.getCurrentDatabase();
    public static HashMap<String, UUID> currentUUID = new HashMap<>();
    public static HashMap<UUID, Instant> NicknameInUse = new HashMap<>();
    public static HashSet<UUID> nonpremium = new HashSet<>();



    @Subscribe
    public void event(PreLoginEvent event) throws SQLException {
        currentUUID.put(event.getConnection().getRemoteAddress().getAddress().getHostAddress(), event.getUniqueId());

        if (Bruteforce.isIPBlocked(String.valueOf(event.getConnection().getRemoteAddress().getAddress().getHostAddress()))) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(SCLogin.getMessage("kick-blocked-ip"))));
            Log.msg("ip blocked");
            return;
        }



        if (NicknameInUse.containsKey(event.getUniqueId()) && !NicknameInUse.get(event.getUniqueId()).isAfter(Instant.now())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(SCLogin.getMessage("nickname-in-use"))));
            Log.msg("nickname in use!");
        }

        String playerName = database.getPlayerName(event.getUsername());

        if (playerName != null && !playerName.equals(event.getUsername())) {
            Log.msg("Nie zgadza sie nick");
            String msg = SCLogin.getMessage("wrong-nickname").replace("{nickname}", playerName);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(msg)));
            return;
        }

        if (nonpremium.contains(event.getUniqueId())) {
            Log.msg("HashSet juz wykryl ze ten gracz byl kiedys nonpremium here");
            return;
        }

        if (database.isPlayerInDatabaseByName(event.getUsername())) {
            if (database.isPremiumByName(event.getUsername())) {
                if (UUID.fromString(database.getUUIDByName(event.getUsername())).equals(event.getUniqueId())) {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                } else {
                    Log.msg("Nie zgadza sie uuid z premium");
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(SCLogin.getMessage("nickname-in-use"))));
                }
            }
        } else {
            Boolean[] api = MojangAPI.isPremium(event.getUsername(), event.getUniqueId());
            if (api[0]) {
                if (api[1]) {
                    database.InsertPlayer(event.getUniqueId(), event.getUsername(), event.getConnection().getRemoteAddress().getAddress().getHostAddress(), true, null);
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(SCLogin.getMessage("create-premium-rejoin"))));
                    Log.msg("<gold>SCLOGIN</gold> <yellow>Premium account was created for " + event.getUsername());
                } else {
                    if (!api[2]) {
                        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(SCLogin.getMessage("nickname-in-use"))));
                        NicknameInUse.put(event.getUniqueId(), Instant.now().plusSeconds(60 * 30));
                    }
                }
            } else {
                if (api[2]) {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(parse.colors(SCLogin.getMessage("nickname-in-use"))));
                    NicknameInUse.put(event.getUniqueId(), Instant.now().plusSeconds(60 * 30));
                }
            }
        }




    }
}



