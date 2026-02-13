package Catan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiceSetTest {
    @Test
    void nextRollBetween2and12() {
        DiceSet set = new DiceSet();
        int result = set.nextRoll();
        assertTrue(result >= 2 && result <= 12, "sum of two dice should be between 2 and 12");
    }

    @Test
    void multipleRollsAreWithinRange() {
        DiceSet set = new DiceSet();
        for (int i = 0; i < 50; i++) {
            int r = set.nextRoll();
            assertTrue(r >= 2 && r <= 12);
        }
    }
}