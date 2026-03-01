package CatanTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import Catan.Dice;

class DiceTest {
    @Test
    void roll_returnsValueBetween1And6() {
        Dice dice = new Dice();

        for (int i = 0; i < 100; i++) {
            int result = dice.roll();
            assertTrue(result >= 1 && result <= 6);
        }
    }

    @Test
    void multipleRolls_allWithinValidRange() {
        Dice dice = new Dice();

        for (int i = 0; i < 1000; i++) {
            int result = dice.roll();
            assertTrue(result >= 1 && result <= 6);
        }
    }

    @Test
    void roll_notAlwaysSameValue() {
        Dice dice = new Dice();

        int first = dice.roll();
        boolean differentFound = false;

        for (int i = 0; i < 100; i++) {
            if (dice.roll() != first) {
                differentFound = true;
                break;
            }
        }

        assertTrue(differentFound);
    }
}