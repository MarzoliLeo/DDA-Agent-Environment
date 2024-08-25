package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;
import java.util.logging.Logger;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Any Jason environment "entry point" should extend
 * jason.environment.Environment class to override methods init(),
 * updatePercepts() and executeAction().
 */

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

    // Aggiungi metodi per fornire percezioni all'agente, se necessario
}

