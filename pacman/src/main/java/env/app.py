# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify

app = Flask(__name__)
file = Flask(__file__)
@app.route('/api/agent/action', methods=['POST'])
def execute_action():
    data = request.json
    # Qui si può inserire la logica per interagire con il tuo agente BDI
    action = data.get('action', 'No action provided')
    return jsonify({"result": "Action {} received".format(action)}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

#To test: run with Python
#then go to : http://localhost:5000/api/agent/action, you should see error 405, means the server is up waiting for an action.