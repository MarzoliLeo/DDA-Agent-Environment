package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Iterator;

/**
 * Any Jason environment "entry point" should extend
 * jason.environment.Environment class to override methods init(),
 * updatePercepts() and executeAction().
 */

public class RaceEnvironment extends Environment {

    static Logger logger = Logger.getLogger(RaceEnvironment.class.getName());

    @Override
    public void init(final String[] args) {
        // Aggiornamento iniziale dei checkpoint come belief
        updatePlayerDataBelief();
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        if (action.getFunctor().equals("update_checkpoint_belief")) {
            updatePlayerDataBelief(); //creo un loop di acquisizione dati.
            return true;
        }
        // Questo era solo un TEST INIZIALE.
        try {
            // Converti l'azione in JSON
            String actionJson = "{\"action\": \"" + action.toString() + "\"}";

            // URL del server Flask
            URL url = new URL("http://localhost:5000/api/agent/action");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Invia la richiesta
            try (OutputStream os = conn.getOutputStream()) {
                os.write(actionJson.getBytes());
                os.flush();
            }

            // Leggi la risposta
            int responseCode = conn.getResponseCode();
            logger.info("POST Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Risposta corretta
                logger.info("Action sent successfully.");
            } else {
                logger.warning("POST request failed.");
            }

        } catch (Exception e) {
            logger.severe("Error sending action to Flask server: " + e.getMessage());
        }

        return true;  // Assumi che l'azione sia stata eseguita con successo
    }

    // Metodo modificato per ottenere l'intero dizionario di checkpoint
    public JSONObject getPlayersData() {
        try {
            // URL del nuovo endpoint Flask che restituisce il dizionario
            URL url = new URL("http://localhost:5000/api/agent/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                // Parsifica il JSON ricevuto come un oggetto JSONObject
                JSONObject jsonResponse = new JSONObject(content.toString());
                logger.info("Checkpoint data received: " + jsonResponse.toString());
                return jsonResponse;  // Restituisci l'intero JSONObject
            } else {
                logger.warning("GET request failed.");
            }
        } catch (Exception e) {
            logger.severe("Error retrieving checkpoint data from Flask server: " + e.getMessage());
        }

        return null;  // Restituisci null in caso di errore
    }

    // Metodo per aggiornare i belief con i dati dei checkpoint
    public void updatePlayerDataBelief() {
        try {
            JSONObject playerData = getPlayersData();  // [GET /data] dal server Flask.
            if (playerData != null) {
                // Itera attraverso il dizionario dei dati dei giocatori
                Iterator<String> keys = playerData.keys();
                while (keys.hasNext()) {
                    String playerId = keys.next();
                    JSONObject playerInfo = playerData.getJSONObject(playerId);

                    // Estrai tutti i parametri del giocatore dal JSON
                    int checkpoints = playerInfo.getInt("checkpoints");
                    float currentSpeed = (float) playerInfo.getDouble("current_speed");
                    float topSpeed = (float) playerInfo.getDouble("top_speed");
                    float acceleration = (float) playerInfo.getDouble("acceleration");
                    JSONObject position = playerInfo.getJSONObject("position");
                    float posX = (float) position.getDouble("x");
                    float posY = (float) position.getDouble("y");
                    float posZ = (float) position.getDouble("z");
                    float distanceToFront = (float) playerInfo.getDouble("distance_to_front");
                    float distanceToBack = (float) playerInfo.getDouble("distance_to_back");
                    int rank = playerInfo.getInt("rank");
                    float distanceToFinish = (float) playerInfo.getDouble("distance_to_finish");

                    // Aggiungi un belief per ogni parametro del giocatore
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'checkpoints', " + checkpoints + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'current_speed', " + currentSpeed + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'top_speed', " + topSpeed + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'acceleration', " + acceleration + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'position', [" + posX + ", " + posY + ", " + posZ + "])"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'distance_to_front', " + distanceToFront + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'distance_to_back', " + distanceToBack + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'rank', " + rank + ")"));
                    addPercept(Literal.parseLiteral("player_data('" + playerId + "', 'distance_to_finish', " + distanceToFinish + ")"));
                }
            }
        } catch (JSONException e) {
            logger.severe("Error parsing player data JSON: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Error updating player data belief: " + e.getMessage());
        }
    }


    // Aggiungi metodi per fornire percezioni all'agente, se necessario
}
