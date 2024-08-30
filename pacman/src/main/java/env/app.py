# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify

app = Flask(__name__)

# Inizializza il contatore dei checkpoint
checkpoint_counter = 0

@app.route('/api/agent/action', methods=['POST'])
def execute_action():
    data = request.json
    # Logica per interagire con il tuo agente BDI
    action = data.get('action', 'No action provided')
    return jsonify({"result": "Action {} received".format(action)}), 200

@app.route('/api/agent/checkpoint', methods=['POST'])
def update_checkpoint():
    global checkpoint_counter
    data = request.json
    checkpoints = data.get('checkpoints', 0)

    # Aggiorna il contatore dei checkpoint
    checkpoint_counter += checkpoints
    #print(f"Checkpoint updated: {checkpoints}. Total now: {checkpoint_counter}")
    return jsonify({"message": "Checkpoint count updated", "total_checkpoints": checkpoint_counter}), 200

@app.route('/api/agent/response', methods=['GET'])
def get_checkpoint_count():
    global checkpoint_counter
    return jsonify({"total_checkpoints": checkpoint_counter}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)


#To test: run with Python
#then go to : http://localhost:5000/api/agent/action, you should see error 405, means the server is up waiting for an action.