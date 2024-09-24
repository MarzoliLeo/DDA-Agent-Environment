# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify
import threading
import json
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier

app = Flask(__name__)

# Global variables to store incoming data and ML model state
data_storage = []  # List of all received data records
model = None  # Placeholder for the ML model
lock = threading.Lock()  # Lock to handle thread-safe operations

@app.route('/send_data', methods=['POST'])
def receive_data():
    """
    Receive game data via POST request and append it to data_storage.
    Each entry should be in the format: {"player_id": "1", "parameter": "speed", "value": 45.0}
    """
    global data_storage
    incoming_data = request.get_json()

    # Append the incoming data to the data storage
    with lock:
        data_storage.append(incoming_data)

    return jsonify({"status": "data received"}), 200

def preprocess_data():
    """
    Preprocess the data_storage for training the model.
    All parameters associated with a player_id will be processed.
    Missing or unused parameters will be imputed or transformed intelligently.
    """
    # Parameters provided in the data
    all_parameters = ['checkpoints', 'current_speed', 'top_speed', 'acceleration',
                      'position_x', 'position_y', 'position_z', 'distance_to_front',
                      'distance_to_back', 'rank', 'distance_to_finish']

    # Dictionary to aggregate data for each player
    player_data = {}

    with lock:
        for record in data_storage:
            player_id = record['player_id']
            parameter = record['parameter']
            value = record['value']

            if player_id not in player_data:
                player_data[player_id] = {param: np.nan for param in all_parameters}

            # Handle position as a list of [posX, posY, posZ]
            if parameter == 'position':
                pos_values = value.strip('[]').split(',')
                player_data[player_id]['position_x'] = float(pos_values[0])
                player_data[player_id]['position_y'] = float(pos_values[1])
                player_data[player_id]['position_z'] = float(pos_values[2])
            else:
                player_data[player_id][parameter] = value

    # Preprocessing: handling missing parameters with imputation (mean or other strategies)
    processed_data = []
    for player_id, params in player_data.items():
        for param, value in params.items():
            if np.isnan(value):
                params[param] = np.mean([p[param] for p in player_data.values() if not np.isnan(p[param])])

        processed_data.append([player_id] + list(params.values()))

    return np.array(processed_data)

def classify_balance(data):
    """
    Determine if a game is balanced or unbalanced based on the provided parameters.
    Example strategy: if the difference between top_speed or distance_to_finish exceeds a threshold, the game is unbalanced.
    """
    speed_diff_threshold = 20  # Example threshold for speed difference
    distance_diff_threshold = 100  # Example threshold for distance to finish line

    speeds = data[:, all_parameters.index('current_speed')]
    distances = data[:, all_parameters.index('distance_to_finish')]

    if max(speeds) - min(speeds) > speed_diff_threshold or max(distances) - min(distances) > distance_diff_threshold:
        return 1  # Unbalanced
    else:
        return 0  # Balanced

@app.route('/train_model', methods=['POST'])
def train_model():
    """
    Train the machine learning model using all available parameters.
    No parameters will be excluded; missing ones will be handled.
    """
    global model

    # Preprocess the data
    training_data = preprocess_data()

    # Generate labels based on the classification of balance
    labels = np.array([classify_balance(training_data) for _ in training_data])

    # Split data into training and testing sets
    X_train, X_test, y_train, y_test = train_test_split(training_data[:, 1:], labels, test_size=0.2)

    # Train the model (using Random Forest as an example)
    model = RandomForestClassifier()
    model.fit(X_train, y_train)

    return jsonify({"status": "model trained"}), 200

@app.route('/predict', methods=['GET'])
def predict():
    """
    Make predictions using the trained model.
    Ensure the model is trained before allowing predictions.
    """
    global model

    if model is None:
        return jsonify({"error": "Model is not trained yet."}), 400

    # Example data for prediction (use the last player data available)
    with lock:
        if len(data_storage) == 0:
            return jsonify({"error": "No data available for prediction."}), 400

        # Use the last player's data for prediction
        player_data = preprocess_data()[-1, 1:]

    # Perform prediction
    prediction = model.predict([player_data])

    return jsonify({"prediction": int(prediction[0])}), 200

if __name__ == '__main__':
    app.run(debug=True, port=5001)




