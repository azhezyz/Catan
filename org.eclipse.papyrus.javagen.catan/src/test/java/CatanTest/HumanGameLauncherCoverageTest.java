package CatanTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import Catan.AIPlayerStrategy;
import Catan.HumanGameLauncher;
import Catan.HumanPlayerStrategy;
import Catan.Player;
import Catan.PlayerStrategy;

class HumanGameLauncherCoverageTest {

    @Test
    void createPlayersBuildsExpectedStrategyTypesForEachControlMode() throws Exception {
        List<Player> players = invokeCreatePlayers(
                List.of("Alice", "Bob", "Charlie", "Diana"),
                List.of(
                        controlMode("HUMAN"),
                        controlMode("AI_OVER_SEVEN"),
                        controlMode("AI_CONNECT_ROADS"),
                        controlMode("AI_DEFEND_LONGEST_ROAD")
                )
        );

        assertEquals(4, players.size());
        assertInstanceOf(HumanPlayerStrategy.class, players.get(0).getStrategy());
        assertInstanceOf(AIPlayerStrategy.class, players.get(1).getStrategy());
        assertInstanceOf(AIPlayerStrategy.class, players.get(2).getStrategy());
        assertInstanceOf(AIPlayerStrategy.class, players.get(3).getStrategy());

        assertRuleChain(players.get(1).getStrategy(), "OverSevenCardsHandler", "ValueMaximizationHandler");
        assertRuleChain(players.get(2).getStrategy(), "ConnectRoadsHandler", "ValueMaximizationHandler");
        assertRuleChain(players.get(3).getStrategy(), "DefendLongestRoadHandler", "ValueMaximizationHandler");
    }

    @Test
    void createPlayersRejectsWrongInputSizes() throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("createPlayers", List.class, List.class);
        method.setAccessible(true);

        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(
                        null,
                        List.of("Alice"),
                        List.of(controlMode("HUMAN"))
                )
        );
        assertTrue(thrown.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void strategyForValueMaxUsesSingleHandlerWithoutChain() throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("strategyFor", playerControlModeClass());
        method.setAccessible(true);
        Object strategy = method.invoke(null, controlMode("AI_VALUE_MAX"));

        assertInstanceOf(AIPlayerStrategy.class, strategy);
        Object head = readFieldInHierarchy(strategy, "ruleChain");
        assertEquals("ValueMaximizationHandler", head.getClass().getSimpleName());
        assertNull(readFieldInHierarchy(head, "next"));
    }

    private static void assertRuleChain(PlayerStrategy strategy, String expectedHead, String expectedNext) throws Exception {
        AIPlayerStrategy aiStrategy = assertInstanceOf(AIPlayerStrategy.class, strategy);
        Object head = readFieldInHierarchy(aiStrategy, "ruleChain");
        assertEquals(expectedHead, head.getClass().getSimpleName());
        Object next = readFieldInHierarchy(head, "next");
        assertNotNull(next);
        assertEquals(expectedNext, next.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    private static Object controlMode(String name) throws Exception {
        Class<? extends Enum> enumClass = (Class<? extends Enum>) playerControlModeClass();
        return Enum.valueOf(enumClass, name);
    }

    @SuppressWarnings("unchecked")
    private static List<Player> invokeCreatePlayers(List<String> names, List<Object> modes) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("createPlayers", List.class, List.class);
        method.setAccessible(true);
        return (List<Player>) method.invoke(null, names, modes);
    }

    private static Class<?> playerControlModeClass() throws ClassNotFoundException {
        return Class.forName("Catan.HumanGameLauncher$PlayerControlMode");
    }

    private static Object readFieldInHierarchy(Object target, String fieldName) throws Exception {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}

