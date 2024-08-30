package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

/**
 * Any Jason environment "entry point" should extend
 * jason.environment.Environment class to override methods init(),
 * updatePercepts() and executeAction().
 */

//TODO se esegui "./gradlew runPacmanMas" con il server Flask attivo, invierà la richiesta correttamente.
// ATTENZIONE!: E' un agente e sta mandando richieste HTTP... Gestire molto attentamente quante richieste manda...
// (per ora ne manda poche.. ma bisognerà rallentarle se non mandarne addirittura solo una).
public class RaceEnvironment extends Environment {

    static Logger logger = Logger.getLogger(RaceEnvironment.class.getName());

    @Override
    public void init(final String[] args) {
        // Inizializzazione se necessaria
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
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

    public int getCheckpointCount() {
        try {
            URL url = new URL("http://localhost:5000/api/agent/response");
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

                // Parsifica il JSON ricevuto
                JSONObject jsonResponse = new JSONObject(content.toString());
                int totalCheckpoints = jsonResponse.getInt("total_checkpoints");
                logger.info("Total checkpoints received: " + totalCheckpoints);
                return totalCheckpoints;
            } else {
                logger.warning("GET request failed.");
            }
        } catch (Exception e) {
            logger.severe("Error retrieving checkpoints from Flask server: " + e.getMessage());
        }

        return -1;  // Restituisci un valore di errore se qualcosa va storto
    }

    // Aggiungi metodi per fornire percezioni all'agente, se necessario
}


