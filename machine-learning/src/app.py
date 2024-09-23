from flask import Flask, request, jsonify
from collections import defaultdict

app = Flask(__name__)

# In-memory data storage for player data
player_data = defaultdict(lambda: defaultdict(list))

# Route to handle POST requests from Agent's sendToMLModel method
@app.route('/submit_data', methods=['POST'])
def submit_data():
    try:
        # Parse incoming JSON data
        data = request.get_json()
        if not data:
            return jsonify({'error': 'Invalid data format'}), 400

        # Loop through the received data and store it
        for entry in data:
            player_id = entry.get('player_id')
            parameter = entry.get('parameter')
            value = entry.get('value')

            if player_id and parameter and value is not None:
                # Store value for each player and parameter
                player_data[player_id][parameter].append(value)
            else:
                return jsonify({'error': 'Incomplete data entry'}), 400

        return jsonify({'status': 'Data received successfully'}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Route to retrieve grouped player data for model processing
@app.route('/get_data', methods=['GET'])
def get_data():
    try:
        # Return stored data grouped by player and parameter
        return jsonify(player_data), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, port=5001)
