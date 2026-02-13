package Catan;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiceTest {
    @Test
    void rollWithinBounds() {
        Dice d = new Dice();
        int value = d.roll();
        assertTrue(value >= 1 && value <= 6, "die result must be between 1 and 6");
    }

    @RepeatedTest(20)
    void rollRepeatedlyAlwaysValid() {
        Dice d = new Dice();
        int v = d.roll();
        assertTrue(v >= 1 && v <= 6);
    }
}