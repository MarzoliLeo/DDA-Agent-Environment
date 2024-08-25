//pacman_agent.asl
/* Initial goals */
!start.

/* Plans */

+!start : true <-
    .print("Ciao sono pacman!!");
    !move(preferential).

+!move(preferential)[source(self)] <-
    .print("Attempting to move preferentially");
    move(preferential); // Questo viene letto dall'environment nelle action.(immaginalo come una send all'env).
    .wait(500);  // Wait for a second
    !move(preferential).

+!move(Direction) <-  // Handle move with specified direction
    .print("Moving in direction: ", Direction);
    move(Direction);
    .wait(500);  // Wait for a second
    !move(preferential).



