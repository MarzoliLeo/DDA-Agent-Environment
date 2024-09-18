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
        updateCheckpointBelief();
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        if (action.getFunctor().equals("update_checkpoint_belief")) {
            updateCheckpointBelief(); //creo un loop di acquisizione dati.
            return true;
        }

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
    public JSONObject getCheckpointData() {
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
    public void updateCheckpointBelief() {
        try {
            JSONObject checkpointData = getCheckpointData();  //[GET /data] dal server Flask.
            if (checkpointData != null) {
                // Itera attraverso il dizionario dei checkpoint
                Iterator<String> keys = checkpointData.keys();
                while (keys.hasNext()) {
                    String playerId = keys.next();
                    int checkpoints = checkpointData.getInt(playerId);

                    // Aggiungi un belief per ogni giocatore e il loro conteggio dei checkpoint
                    addPercept(Literal.parseLiteral("checkpoints('" + playerId + "', " + checkpoints + ")"));
                }
            }
        } catch (JSONException e) {
            logger.severe("Error parsing checkpoint JSON: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Error updating checkpoint belief: " + e.getMessage());
        }
    }

    // Aggiungi metodi per fornire percezioni all'agente, se necessario
}
