package pl.stellarcode.sCLogin.API;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MojangAPI {

    public static Boolean[] isPremium(String name, UUID uuid) {
        boolean api = false;
        boolean api2 = false;
        boolean api3 = false;

        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name.toLowerCase());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonObject jsonResponse = JsonParser.parseReader(reader).getAsJsonObject();
                String fetchedUUID = jsonResponse.get("id").getAsString();

                api = true;
                api2 = fetchedUUID.equals(uuid.toString().replace("-", ""));

            } else if (conn.getResponseCode() == 429) {
                api = false;
            } else if (conn.getResponseCode() == 404) {
                api = true;
                api3 = true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Boolean[]{api, api2, api3};
    }


}
