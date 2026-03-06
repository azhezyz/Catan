package CatanTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import Catan.HumanGameLauncher;

class HumanGameLauncherTest {

    @SuppressWarnings("unchecked")
    private static List<String> invokePromptPlayerNames(Scanner scanner) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("promptPlayerNames", Scanner.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, scanner);
    }

    private static String invokeReadName(Scanner scanner, String label, String defaultName) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("readName", Scanner.class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, scanner, label, defaultName);
    }

    private static File invokeResolveVisualizeDir(Path statePath) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("resolveVisualizeDir", Path.class);
        method.setAccessible(true);
        return (File) method.invoke(null, statePath);
    }

    private static String invokeResolvePythonExecutable(File visualizeDir) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("resolvePythonExecutable", File.class);
        method.setAccessible(true);
        return (String) method.invoke(null, visualizeDir);
    }

    @Test
    void promptPlayerNamesUsesDefaultsOnBlankInput() throws Exception {
        List<String> names = invokePromptPlayerNames(new Scanner("\n\n\n\n"));
        assertEquals(List.of("Alice", "Bob", "Charlie", "Diana"), names);
    }

    @Test
    void readNameRejectsTooLongAndThenAcceptsValid() throws Exception {
        String tooLong = "a".repeat(41);
        String accepted = invokeReadName(new Scanner(tooLong + "\nNeo\n"), "Player 1", "Alice");
        assertEquals("Neo", accepted);
    }

    @Test
    void resolveVisualizeDirUsesStateParentWhenParentIsVisualize() throws Exception {
        Path tempRoot = Files.createTempDirectory("launcher-test");
        Path visualize = Files.createDirectory(tempRoot.resolve("visualize"));
        Path statePath = visualize.resolve("state.json");

        File resolved = invokeResolveVisualizeDir(statePath);
        assertEquals(visualize.toFile().getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void resolvePythonExecutableFindsWindowsVenvPath() throws Exception {
        Path tempRoot = Files.createTempDirectory("launcher-python");
        File visualizeDir = tempRoot.toFile();

        File scriptsDir = new File(visualizeDir, ".venv\\Scripts");
        assertTrue(scriptsDir.mkdirs() || scriptsDir.isDirectory());
        File python = new File(scriptsDir, "python.exe");
        assertTrue(python.createNewFile() || python.isFile());

        String resolved = invokeResolvePythonExecutable(visualizeDir);
        assertNotNull(resolved);
        assertTrue(resolved.endsWith("python.exe"));
    }
}
