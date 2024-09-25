// Inizia il ciclo quando l'agente inizia
!update_checkpoint_belief_cycle.

// Definisci il ciclo
+!update_checkpoint_belief_cycle
  <- update_checkpoint_belief;
     .wait(1000);  // Aspetta 1000 millisecondi (1 secondo)
     !send_player_data_to_ml_model;
     .wait(2000);
     train_the_model;
     .wait(1000);
     take_prediction_from_model;
     .wait(1000);
     !update_checkpoint_belief_cycle.


// Aggiungi una nuova action per inviare i dati al modello di ML
+!send_player_data_to_ml_model
  <- .findall([PlayerId, Param, Val], player_data(PlayerId, Param, Val), B);  // Raccoglie tutti i beliefs relativi ai player_data
     .print(B);  // Stampa i beliefs raccolti
     send_data_to_ml_model(B).  // Chiama l'azione Java per inviare i dati al modello.

