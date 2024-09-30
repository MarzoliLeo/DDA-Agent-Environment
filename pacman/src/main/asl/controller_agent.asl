// Inizia il ciclo quando l'agente inizia
!update_checkpoint_belief_cycle.

// Definisci il ciclo
+!update_checkpoint_belief_cycle
  <- update_checkpoint_belief;
     .wait(1000);  // Tempo in ms.
     !send_player_data_to_ml_model;
     .wait(2000);
     train_the_model;
     .wait(1000);
     take_prediction_from_model;
     .wait(1000);
     //!balance_game;
     //.wait(1000);
     !update_checkpoint_belief_cycle.


// Aggiungi una nuova action per inviare i dati al modello di ML
+!send_player_data_to_ml_model
  <- .findall([PlayerId, Param, Val], player_data(PlayerId, Param, Val), B);  // Raccoglie tutti i beliefs relativi ai player_data
     .print(B);
     send_data_to_ml_model(B).  // Action.

// Goal che bilancia il gioco quando il modello predice uno squilibrio
+!balance_game: model_prediction(1)
  <- .print("Devo bilanciare il game...");
     !adjust_distance_to_front;
     !adjust_distance_to_back;
     !adjust_current_speed;
     !adjust_acceleration;
     !adjust_checkpoints;
     !adjust_top_speed.

+!adjust_distance_to_front: player_data(PlayerId, 'distance_to_front', Val) & Val > 1000
  <- .print("Riducendo distance_to_front per bilanciare il gioco.");
     !modifyBelief(PlayerId, 'distance_to_front', 100).

+!adjust_distance_to_back: player_data(PlayerId, 'distance_to_back', Val) & Val > 40
  <- .print("Riducendo distance_to_back per bilanciare il gioco.");
     !modifyBelief(PlayerId, 'distance_to_back', 40).

+!adjust_current_speed: player_data(PlayerId, 'current_speed', Val) & Val > 25
  <- .print("Riducendo current_speed per bilanciare il gioco.");
     !modifyBelief(PlayerId, 'current_speed', 25).

+!adjust_acceleration: player_data(PlayerId, 'acceleration', Val) & Val > 10
  <- .print("Riducendo acceleration per bilanciare il gioco.");
     !modifyBelief(PlayerId, 'acceleration', 10).

+!adjust_checkpoints: player_data(PlayerId, 'checkpoints', Val) & Val > 10
  <- .print("Riducendo checkpoints per bilanciare il gioco.");
     !modifyBelief(PlayerId, 'checkpoints', 10).

+!adjust_top_speed: player_data(PlayerId, 'top_speed', Val) & Val > 25
  <- .print("Riducendo top_speed per bilanciare il gioco.");
     !modifyBelief(PlayerId, 'top_speed', 25).


+!modifyBelief(PlayerId, Param, NewVal)
  <- -player_data(PlayerId, Param, _);  // Rimuove il belief esistente
     +player_data(PlayerId, Param, NewVal).  // Aggiunge il nuovo belief con il valore modificato


// Piani di fallback nel caso non ci sia un belief valido o la condizione non sia soddisfatta
+!adjust_distance_to_front
  <- .print("Nessuna modifica necessaria per distance_to_front.").
+!adjust_distance_to_back
  <- .print("Nessuna modifica necessaria per distance_to_back.").
+!adjust_current_speed
  <- .print("Nessuna modifica necessaria per current_speed.").
+!adjust_acceleration
  <- .print("Nessuna modifica necessaria per acceleration.").
+!adjust_checkpoints
  <- .print("Nessuna modifica necessaria per checkpoints.").
+!adjust_top_speed
<- .print("Nessuna modifica necessaria per top_speed.").
