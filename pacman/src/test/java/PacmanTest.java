import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

public class PacmanTest {
    private PacmanModel pacmanModel;
    private int[][] gridMatrix;

    @BeforeEach
    public void setUp() {
        gridMatrix = new int[25][25];
        for (int i = 0; i < 25; i++) {
            for (int j = 0; j < 25; j++) {
                gridMatrix[i][j] = 1; // All cells are not walls
            }
        }
        pacmanModel = new PacmanModel(gridMatrix);
    }

    @Test
    public void testGridSize() {
        System.out.println("Inizio testGridSize");
        Assertions.assertEquals(25, pacmanModel.getGridSize());
        System.out.println("Fine testGridSize");
    }

    @Test
    public void testCellSize() {
        System.out.println("Inizio testCellSize");
        Assertions.assertEquals(25, pacmanModel.getCellSize());
        System.out.println("Fine testCellSize");
    }

    @Test
    public void testScore() {
        Assertions.assertEquals(0, pacmanModel.getScore());
        pacmanModel.addScore(10);
        Assertions.assertEquals(10, pacmanModel.getScore());
    }


    @Test
    public void testPowerUp() {
        System.out.println("Inizio testPowerUp");
        Assertions.assertFalse(pacmanModel.consumePowerUp(0, 0));
        System.out.println("Fine testPowerUp");
    }

    @Test
    public void testPacmanSphere() {
        System.out.println("Inizio testPacmanSphere");
        Point pacmanSphere = pacmanModel.getPacmanSphere();
        Assertions.assertNotNull(pacmanSphere);
        Assertions.assertTrue(pacmanSphere.x >= 0 && pacmanSphere.x < 25);
        Assertions.assertTrue(pacmanSphere.y >= 0 && pacmanSphere.y < 25);
        System.out.println("Fine testPacmanSphere");
    }
}
