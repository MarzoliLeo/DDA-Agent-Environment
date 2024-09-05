# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify

app = Flask(__name__)

# Inizializza un dizionario per tenere traccia dei checkpoint per ogni giocatore
checkpoint_counter = {}

# TODO Questa route era un Test e non rispetta i canoni RESTfull che le altre route invece hanno.
@app.route('/api/agent/action', methods=['POST'])
def execute_action():
    data = request.json
    # Logica per interagire con il tuo agente BDI
    action = data.get('action', 'No action provided')
    return jsonify({"result": "Action {} received".format(action)}), 200

# Endpoint per aggiornare o aggiungere i checkpoint di un giocatore specifico
@app.route('/api/agent/checkpoint', methods=['POST'])
def update_checkpoint():
    data = request.json
    player_id = data.get('player_id')
    checkpoints = data.get('checkpoints', 0)

    if player_id not in checkpoint_counter:
        checkpoint_counter[player_id] = 0

    # Aggiorna il contatore dei checkpoint per il giocatore specifico
    checkpoint_counter[player_id] += checkpoints
    return jsonify({
        "message": f"Checkpoint count updated for player {player_id}",
        "total_checkpoints": checkpoint_counter[player_id]
    }), 200

# Endpoint per ottenere i checkpoint di un giocatore specifico (GET /checkpoint/{player_id})
@app.route('/api/agent/checkpoint/<string:player_id>', methods=['GET'])
def get_checkpoint_count(player_id):
    total_checkpoints = checkpoint_counter.get(player_id, 0)
    return jsonify({
        "player_id": player_id,
        "total_checkpoints": total_checkpoints
    }), 200

# Endpoint per ottenere tutti i checkpoint di tutti i giocatori (GET /checkpoints)
@app.route('/api/agent/checkpoints', methods=['GET'])
def get_all_checkpoints():
    return jsonify({
        "checkpoint_counter": checkpoint_counter
    }), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)



#To test: run with Python
#then go to : http://localhost:5000/api/agent/action, you should see error 405, means the server is up waiting for an action.