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

    private static Process invokeStartVisualizerProcess(Path statePath) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("startVisualizerProcess", Path.class);
        method.setAccessible(true);
        return (Process) method.invoke(null, statePath);
    }

    private static void invokeStopVisualizer(Process process) throws Exception {
        Method method = HumanGameLauncher.class.getDeclaredMethod("stopVisualizer", Process.class);
        method.setAccessible(true);
        method.invoke(null, process);
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
    void resolveVisualizeDirFallsBackToUserDirVisualize() throws Exception {
        Path tempRoot = Files.createTempDirectory("launcher-fallback");
        Path nonVisualizeParent = Files.createDirectory(tempRoot.resolve("states"));
        Path statePath = nonVisualizeParent.resolve("state.json");

        File resolved = invokeResolveVisualizeDir(statePath);
        File expected = new File(System.getProperty("user.dir"), "visualize");
        assertEquals(expected.getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void resolvePythonExecutableFindsUnixVenvPath() throws Exception {
        Path tempRoot = Files.createTempDirectory("launcher-python");
        File visualizeDir = tempRoot.toFile();

        File binDir = new File(new File(visualizeDir, ".venv"), "bin");
        assertTrue(binDir.mkdirs() || binDir.isDirectory());
        File python = new File(binDir, "python");
        assertTrue(python.createNewFile() || python.isFile());

        String resolved = invokeResolvePythonExecutable(visualizeDir);
        assertNotNull(resolved);
        assertTrue(resolved.endsWith("python"));
    }

    @Test
    void resolvePythonExecutableReturnsNullWhenNoVenv() throws Exception {
        Path tempRoot = Files.createTempDirectory("launcher-no-python");
        String resolved = invokeResolvePythonExecutable(tempRoot.toFile());
        assertNull(resolved);
    }

    @Test
    void startVisualizerProcessReturnsNullWhenDirectoryMissing() throws Exception {
        Path tempRoot = Files.createTempDirectory("launcher-no-venv");
        Path visualize = Files.createDirectory(tempRoot.resolve("visualize"));
        Path statePath = visualize.resolve("state.json");
        Process process = invokeStartVisualizerProcess(statePath);
        if (process != null) {
            process.destroyForcibly();
        }
        assertNull(process);
    }

    @Test
    void stopVisualizerHandlesNullAndAliveProcess() throws Exception {
        invokeStopVisualizer(null);

        Process fakeAlive = new Process() {
            private boolean destroyed;

            @Override
            public java.io.OutputStream getOutputStream() {
                return java.io.OutputStream.nullOutputStream();
            }

            @Override
            public java.io.InputStream getInputStream() {
                return java.io.InputStream.nullInputStream();
            }

            @Override
            public java.io.InputStream getErrorStream() {
                return java.io.InputStream.nullInputStream();
            }

            @Override
            public int waitFor() {
                return 0;
            }

            @Override
            public int exitValue() {
                return 0;
            }

            @Override
            public void destroy() {
                destroyed = true;
            }

            @Override
            public boolean isAlive() {
                return !destroyed;
            }
        };

        invokeStopVisualizer(fakeAlive);
        assertFalse(fakeAlive.isAlive());
    }
}
