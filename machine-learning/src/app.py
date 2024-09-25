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
# Aggiungi questa lista di parametri all'inizio del tuo codice
all_parameters = ['player_id', 'current_speed', 'distance_to_finish', 'checkpoints',
                  'acceleration', 'position_x', 'position_y', 'position_z', 'rank']


@app.route('/send_data', methods=['POST'])
def receive_data():
    """
    Receive game data via POST request and append it to data_storage.
    Each entry should be in the format: {"player_id": "1", "parameter": "speed", "value": 45.0}
    """
    global data_storage
    incoming_data = request.get_json()

    # Pulisci i dati rimuovendo le virgolette singole
    for record in incoming_data:
        record['player_id'] = record['player_id'].replace("'", "")
        record['parameter'] = record['parameter'].replace("'", "")
        # Se 'value' è una stringa rappresentante una lista, convertila in un oggetto Python
        if record['parameter'] == 'position':
            record['value'] = json.loads(record['value'])  # Converti la stringa in una lista

    # Append the cleaned data to the data storage
    with lock:
        data_storage.extend(incoming_data)

    return jsonify({"status": "data received"}), 200


def preprocess_data():
    global data_storage
    processed_data = []

    for record in data_storage:
        # Esegui il parsing del valore
        player_id = record['player_id']
        parameter = record['parameter']

        # Assicurati che il valore sia numerico
        value = record['value']

        # Gestisci i valori 'position' (che è una lista) e altri valori
        if parameter == 'position':
            # Assicurati di gestire il caso della lista
            value = [float(v) for v in value]  # Converte i valori della lista in float
        else:
            try:
                value = float(value)  # Converti il valore in float
            except ValueError:
                # Se non è un numero, gestiscilo come preferisci
                continue  # Ignora o gestisci diversamente

        # Aggiungi i dati elaborati a processed_data solo per parametri numerici
        if parameter in all_parameters:
            processed_data.append(value)

    return np.array(processed_data)  # Restituisci un array NumPy con solo i valori numerici


def classify_balance(data):
    """
    Determine if a game is balanced or unbalanced based on the provided parameters.
    This version uses a more complex strategy involving multiple parameters.
    """
    # Define thresholds for imbalance detection
    speed_diff_threshold = 10  # Speed difference threshold
    distance_diff_threshold = 100  # Distance to finish threshold
    checkpoint_diff_threshold = 2  # Max checkpoint difference
    acceleration_diff_threshold = 10  # Max acceleration difference
    position_diff_threshold = 200  # Max position distance (Euclidean)
    rank_diff_threshold = 2  # Max difference in rank

    # Extract data for each parameter
    speeds = data[:, all_parameters.index('current_speed')]
    distances_to_finish = data[:, all_parameters.index('distance_to_finish')]
    checkpoints = data[:, all_parameters.index('checkpoints')]
    accelerations = data[:, all_parameters.index('acceleration')]
    positions_x = data[:, all_parameters.index('position_x')]
    positions_y = data[:, all_parameters.index('position_y')]
    positions_z = data[:, all_parameters.index('position_z')]
    ranks = data[:, all_parameters.index('rank')]

    # Calculate differences for each parameter
    speed_diff = max(speeds) - min(speeds)
    distance_diff = max(distances_to_finish) - min(distances_to_finish)
    checkpoint_diff = max(checkpoints) - min(checkpoints)
    acceleration_diff = max(accelerations) - min(accelerations)
    rank_diff = max(ranks) - min(ranks)

    # Calculate Euclidean distances between players' positions
    position_diffs = []
    for i in range(len(positions_x)):
        for j in range(i+1, len(positions_x)):
            pos_diff = np.sqrt((positions_x[i] - positions_x[j])**2 +
                               (positions_y[i] - positions_y[j])**2 +
                               (positions_z[i] - positions_z[j])**2)
            position_diffs.append(pos_diff)

    max_position_diff = max(position_diffs) if position_diffs else 0

    # Check if any of the differences exceed the thresholds
    if (speed_diff > speed_diff_threshold or
            distance_diff > distance_diff_threshold or
            checkpoint_diff > checkpoint_diff_threshold or
            acceleration_diff > acceleration_diff_threshold or
            max_position_diff > position_diff_threshold or
            rank_diff > rank_diff_threshold):
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

    # Verifica se ci sono dati sufficienti per l'addestramento
    if len(training_data) == 0:
        return jsonify({"error": "No training data available."}), 400

    # Generate labels based on the classification of balance
    labels = np.array([classify_balance(training_data) for _ in range(len(training_data))])

    # Split data into training and testing sets
    X_train, X_test, y_train, y_test = train_test_split(training_data[:, 1:], labels, test_size=0.2)

    # Train the model (using Random Forest as an example)
    model = RandomForestClassifier()
    model.fit(X_train, y_train)

    return jsonify({"status": "model trained"}), 200

@app.route('/prediction', methods=['GET'])
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




