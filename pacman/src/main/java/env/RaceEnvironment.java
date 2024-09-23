package env;

import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
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
        try
        {
            if (action.getFunctor().equals("update_checkpoint_belief")) {
                updatePlayerDataBelief(); //creo un loop di acquisizione dati.
                return true;
            }
            if (action.getFunctor().equals("send_to_ml_model")) {
                // Estrai l'argomento della lista di beliefs
                ListTerm beliefsList = (ListTerm) action.getTerm(0);

                // Crea un array per contenere i dati strutturati
                List<String[]> structuredData = new ArrayList<>();

                // Verifica che la lista di beliefs non sia vuota
                if (beliefsList.size() == 0) {
                    logger.warning("No beliefs to send to the ML model.");
                    return true;
                }

                // Itera sulla lista di beliefs
                for (Term term : beliefsList) {
                    if (term instanceof ListTerm) {
                        ListTerm tuple = (ListTerm) term;
                        String playerId = tuple.get(0).toString();
                        String parameter = tuple.get(1).toString();
                        String value = tuple.get(2).toString();

                        // Aggiungi i dati come un array strutturato
                        structuredData.add(new String[]{playerId, parameter, value});
                    }
                }

                // Ora invia queste informazioni al modello di ML, eventualmente tramite un metodo
                sendToMLModel(structuredData);
                logger.info( "HO APPENA INVIATO I DATI AL MODELLO");
                return true;
            }
        } catch (Exception e) {
            logger.severe("Error executing Action: " + e.getMessage());
        }

        return true;  // Assumi che l'azione sia stata eseguita con successo
    }

    // Metodo modificato per ottenere l'intero dictionary con i players_data.
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
                logger.info("Players data received: " + jsonResponse.toString());
                return jsonResponse;  // Restituisci l'intero JSONObject
            } else {
                logger.warning("GET request failed.");
            }
        } catch (Exception e) {
            logger.severe("Error retrieving data from Flask server: " + e.getMessage());
        }

        return null;  // Restituisci null in caso di errore
    }

    // Metodo per aggiornare i belief con i dati dei checkpoint
    public void updatePlayerDataBelief() {
        try {
            JSONObject playerData = getPlayersData();  // [GET /data] dal server Flask.
            if (playerData != null) {
                // Accedi all'oggetto all_player_data
                JSONObject allPlayerData = playerData.getJSONObject("all_player_data");

                // Itera attraverso il dizionario dei dati dei giocatori
                Iterator<String> keys = allPlayerData.keys();
                while (keys.hasNext()) {
                    String playerId = keys.next();  // Ottieni il playerId corrente
                    JSONObject playerInfo = allPlayerData.getJSONObject(playerId);  // Ottieni le informazioni del giocatore

                    //logger.info("Player Data appare nel seguente modo: " + playerInfo.toString());

                    // Estrai tutti i parametri del giocatore dal JSON
                    int checkpoints = playerInfo.getInt("checkpoints");
                    float currentSpeed = (float) playerInfo.getDouble("current_speed");
                    float topSpeed = (float) playerInfo.getDouble("top_speed");
                    float acceleration = (float) playerInfo.getDouble("acceleration");
                    JSONObject position = playerInfo.getJSONObject("position");
                    float posX = (float) position.getDouble("x");
                    float posY = (float) position.getDouble("y");
                    float posZ = (float) position.getDouble("z");


                    // Gestione di valori estremamente alti per distanza
                    float distanceToFront = (float) playerInfo.getDouble("distance_to_front");
                    if (distanceToFront > 1e30) {  // Soglia per considerare il valore come "infinito"
                        distanceToFront = Float.POSITIVE_INFINITY;  // Oppure un valore personalizzato come -1 per rappresentare "nessun giocatore davanti"
                    }

                    float distanceToBack = (float) playerInfo.getDouble("distance_to_back");
                    if (distanceToBack > 1e30) {  // Soglia per considerare il valore come "infinito"
                        distanceToBack = Float.POSITIVE_INFINITY;  // Oppure un valore personalizzato come -1 per rappresentare "nessun giocatore dietro"
                    }

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

                    logger.info("HO AGGIUNTO LE PERCEPTIONS!");
                }
            }
        } catch (JSONException e) {
            logger.severe("Error parsing player data JSON: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Error updating player data belief: " + e.getMessage());
        }
    }

    public void sendToMLModel(List<String[]> structuredData) {
        // Converti i dati strutturati in JSON
        JSONArray jsonArray = new JSONArray();
        for (String[] data : structuredData) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("player_id", data[0]);
            jsonObj.put("parameter", data[1]);
            jsonObj.put("value", data[2]);
            jsonArray.put(jsonObj);
        }

        // Invio dei dati tramite POST a Flask del modello.
        try {
            URL url = new URL("http://localhost:5001/submit_data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // Invia il JSON al server
            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonArray.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Legge la risposta dal server
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Response from ML model: " + response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
