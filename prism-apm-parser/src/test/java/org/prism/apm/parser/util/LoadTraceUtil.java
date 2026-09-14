package org.prism.apm.parser.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class LoadTraceUtil {

    /**
     * Reads a test resource file from the classpath and returns its content as a String.
     *
     * @param resourcePath The path to the resource, relative to the 'resources' directory.
     * @return The content of the file.
     * @throws RuntimeException if the resource is not found or an I/O error occurs.
     */
    public static String loadTraceFromResource(String resourcePath) {
        try (InputStream is = LoadTraceUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading resource: " + resourcePath, e);
        }
    }

    /**
     * An example of how to use the loadTraceFromResource method.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // The path is now relative to the classpath's root (e.g., src/test/resources)
        String resourcePath = "sample-traces/simplified-error-trace";
        System.out.println("Reading resource from: " + resourcePath);

        String fileContent = loadTraceFromResource(resourcePath);

        System.out.println("\n--- Resource Content ---");
        System.out.println(fileContent);
        System.out.println("--- End of Resource Content ---\n");
    }
}
