# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify

app = Flask(__name__)


player_data = {} # Dizionario per contenere i dati di tutti i giocatori
agent_prediction = None  # Variabile per memorizzare la predizione dell'agente


# TODO Questa route era un Test e non rispetta i canoni RESTfull che le altre route invece hanno.
@app.route('/api/agent/action', methods=['POST'])
def execute_action():
    data = request.json
    # Logica per interagire con il tuo agente BDI
    action = data.get('action', 'No action provided')
    return jsonify({"result": "Action {} received".format(action)}), 200


@app.route('/api/agent/data', methods=['POST'])
def update_player_data():
    data = request.json
    player_id = data.get('player_id')

    if player_id:
        # Aggiorna o aggiungi i dati del giocatore
        player_data[player_id] = {
            "checkpoints": data.get('checkpoints'),
            "current_speed": data.get('current_speed'),
            "top_speed": data.get('top_speed'),
            "acceleration": data.get('acceleration'),
            "position": data.get('position'),
            "distance_to_front": data.get('distance_to_front'),
            "distance_to_back": data.get('distance_to_back'),
            "rank": data.get('rank'),
            "distance_to_finish": data.get('distance_to_finish')
        }

        return jsonify({
            "message": f"Data updated for player {player_id}",
            "player_data": player_data[player_id]
        }), 200
    else:
        return jsonify({"error": "Player ID not provided"}), 400

# Endpoint per ottenere i dati di un giocatore specifico (GET /data/{player_id})
@app.route('/api/agent/data/<string:player_id>', methods=['GET'])
def get_player_data(player_id):
    data = player_data.get(player_id)
    if data:
        return jsonify({
            "player_id": player_id,
            "player_data": data
        }), 200
    else:
        return jsonify({"error": f"No data found for player {player_id}"}), 404

# Endpoint per ottenere i dati di tutti i giocatori (GET /data)
@app.route('/api/agent/data', methods=['GET'])
def get_all_player_data():
    return jsonify({
        "all_player_data": player_data
    }), 200

# Endpoint per caricare la predizione dell'agente (POST /api/agent/prediction)
@app.route('/api/agent/prediction', methods=['POST'])
def set_agent_prediction():
    global agent_prediction
    data = request.json
    agent_prediction = data.get('prediction')

    if agent_prediction is not None:
        return jsonify({"message": "Prediction set successfully", "prediction": agent_prediction}), 200
    else:
        return jsonify({"error": "Prediction value not provided"}), 400

# Endpoint per ottenere la predizione dell'agente (GET /api/agent/prediction)
@app.route('/api/agent/prediction', methods=['GET'])
def get_agent_prediction():
    if agent_prediction is not None:
        return jsonify({"prediction": agent_prediction}), 200
    else:
        return jsonify({"error": "No prediction available"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)



#To test: run with Python
#then go to : http://localhost:5000/api/agent/action, you should see error 405, means the server is up waiting for an action.