// Inizia il ciclo quando l'agente inizia
!update_checkpoint_belief_cycle.

// Definisci il ciclo
+!update_checkpoint_belief_cycle
  <- update_checkpoint_belief;
     .wait(1000);  // Aspetta 1000 millisecondi (1 secondo)
     !update_checkpoint_belief_cycle.  // Richiama l'azione per creare il ciclo