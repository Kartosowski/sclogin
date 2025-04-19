package pl.stellarcode.sCLogin.Handlers;

import pl.stellarcode.sCLogin.Log;
import pl.stellarcode.sCLogin.SCLogin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Bruteforce {
    public static Map<String, Instant> BlockedIPS = new HashMap<>();
    public static Map<String, Integer> BlockedAttempts = new HashMap<>();
    public static Map<String, Instant> BlockedAttemptIPS = new HashMap<>();


    public static boolean isIPBlocked(String IP) {
        if (BlockedIPS.containsKey(IP)) {
            if (Instant.now().isAfter(BlockedIPS.get(IP))) {
                BlockedIPS.remove(IP);
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static void addBlockAttempt(String IP) {
        if (!SCLogin.getConfig().node("security", "bruteforce", "auto-punishment").getBoolean()) {
            return;
        }

        if (SCLogin.getConfig().node("security", "bruteforce", "ignore-localhost").getBoolean()) {
            if (IP.equals("127.0.0.1") || IP.equals("localhost")) {
                SCLogin.getLogger().info("ignore");
                return;
            }
        }

        int MAX_ATTEMPTS = 3;

        if (BlockedAttempts.containsKey(IP)) {
            if (BlockedAttemptIPS.get(IP).isBefore(Instant.now())) {
                BlockedAttempts.remove(IP);
                BlockedIPS.remove(IP);
            } else {
                int attempt = BlockedAttempts.get(IP);
                attempt++;

                if (attempt >= MAX_ATTEMPTS) {
                    int punishmentDuration = SCLogin.getConfig().node("security", "bruteforce", "punishment-duration").getInt();
                    Log.msg("<gold>SCLOGIN</gold> <yellow>IP " + IP + " has been blocked for " + punishmentDuration + " minutes");
                    BlockedIPS.put(IP, Instant.now().plusSeconds(60 * punishmentDuration));
                    BlockedAttempts.remove(IP);
                    BlockedAttemptIPS.remove(IP);
                } else {
                    BlockedAttemptIPS.put(IP, Instant.now().plusSeconds(60 * 5));
                    BlockedAttempts.put(IP, attempt);
                }
                return;
            }
        }

        BlockedAttemptIPS.put(IP, Instant.now().plusSeconds(60 * 5));
        BlockedAttempts.put(IP, 1);
    }



}
