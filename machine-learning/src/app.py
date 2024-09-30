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

all_parameters_to_evaluate_for_classification = ['player_id', 'current_speed', 'distance_to_finish', 'checkpoints',
                                                 'acceleration', 'position_x', 'position_y', 'position_z', 'rank']
all_parameters_sent_via_Json = [
    'checkpoints', 'current_speed', 'top_speed',
    'acceleration','position', 'distance_to_front',
    'distance_to_back', 'rank', 'distance_to_finish'
]


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

    #print("Data Storage ha questo contenuto: ", data_storage)
    return jsonify({"status": "data received"}), 200


def preprocess_data():
    global data_storage
    processed_data = {}

    for record in data_storage:
        player_id = record['player_id']
        parameter = record['parameter']
        value = record['value']

        # Se il player_id non è già nel dizionario, aggiungilo con valori None
        if player_id not in processed_data:
            processed_data[player_id] = [None] * len(all_parameters_sent_via_Json)

        # Gestione della posizione come oggetto separato
        if parameter == 'position':
            try:
                value_x = float(value[0])
                value_y = float(value[1])
                value_z = float(value[2])

                # Aggiungi le coordinate x, y, z nei rispettivi indici
                processed_data[player_id].extend([value_x, value_y, value_z])

            except (ValueError, IndexError):
                # Gestione dell'errore se la posizione non è valida
                print(f"Errore nella conversione della posizione per player_id: {player_id}.")
                continue  # Salta alla prossima iterazione del loop
        else:
            try:
                value = float(value)  # Converti altri valori in float
            except ValueError:
                continue  # Ignora se il valore non è numerico

            # Inserisci il valore del parametro nel posto giusto
            if parameter in all_parameters_sent_via_Json:
                index = all_parameters_sent_via_Json.index(parameter)
                processed_data[player_id][index] = value

    # Rimuovi eventuali None dall'array finale, tranne se appartengono a parametri significativi
    for player, params in processed_data.items():
        processed_data[player] = [value for value in params if value is not None]

    # Converti il dizionario in una lista di liste (ogni lista rappresenta un giocatore)
    return np.array(list(processed_data.values()))  # Restituisce un array bidimensionale




def classify_balance(data):
    """
    Determine if a game is balanced or unbalanced based on the provided parameters.
    This version uses a more complex strategy involving multiple parameters.
    """
    # Define thresholds for imbalance detection
    speed_diff_threshold = 10  # Speed difference threshold
    distance_diff_threshold = 100  # Distance to finish threshold # 50
    checkpoint_diff_threshold = 2  # Max checkpoint difference #5
    acceleration_diff_threshold = 5  # Max acceleration difference
    position_diff_threshold = 200  # Max position distance (Euclidean)
    rank_diff_threshold = 15  # Max difference in rank #15 è inutile temporaneamente ,prima era 2.

    #print("DATA IN CLASSIFY ", data)

    # Extract data for each parameter
    speeds = data[:, all_parameters_to_evaluate_for_classification.index('current_speed')]
    distances_to_finish = data[:, all_parameters_to_evaluate_for_classification.index('distance_to_finish')]
    checkpoints = data[:, all_parameters_to_evaluate_for_classification.index('checkpoints')]
    accelerations = data[:, all_parameters_to_evaluate_for_classification.index('acceleration')]
    positions_x = data[:, all_parameters_to_evaluate_for_classification.index('position_x')]
    positions_y = data[:, all_parameters_to_evaluate_for_classification.index('position_y')]
    positions_z = data[:, all_parameters_to_evaluate_for_classification.index('position_z')]
    ranks = data[:, all_parameters_to_evaluate_for_classification.index('rank')]

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
    global model

    # Preprocess the data
    training_data = preprocess_data()

    print("TRAINING DATA E' QUESTO: ", training_data)
    # Verifica se ci sono dati sufficienti per l'addestramento
    #if len(training_data) == 0 or training_data.shape[1] != len(all_parameters_sent_via_Json):
    #    return jsonify({"error": "Insufficient or malformed training data."}), 400

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




