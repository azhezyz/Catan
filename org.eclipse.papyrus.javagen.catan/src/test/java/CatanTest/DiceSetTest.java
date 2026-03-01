package CatanTest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import Catan.DiceSet;

class DiceSetTest {

    @Test
    void nextRoll_returnsValueBetween2And12() {
        DiceSet diceSet = new DiceSet();

        for (int i = 0; i < 200; i++) {
            int result = diceSet.nextRoll();
            assertTrue(result >= 2 && result <= 12);
        }
    }
    @Test
    void multipleRolls_allWithinValidRange() {
        DiceSet diceSet = new DiceSet();

        for (int i = 0; i < 1000; i++) {
            int result = diceSet.nextRoll();
            assertTrue(result >= 2 && result <= 12);
        }
    }

    @Test
    void nextRoll_notAlwaysSameValue() {
        DiceSet diceSet = new DiceSet();

        int first = diceSet.nextRoll();
        boolean differentFound = false;

        for (int i = 0; i < 100; i++) {
            if (diceSet.nextRoll() != first) {
                differentFound = true;
                break;
            }
        }
        assertTrue(differentFound);
    }
}