package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class NamespaceResourceTest {

    private static final Path ROOT = Paths.get("")
        .toAbsolutePath();
    private static final Path SOURCE = ROOT.resolve("src/main/java");
    private static final Path LANG = ROOT.resolve("src/main/resources/assets/advancedsolarpanel/lang");

    @Test
    void sourceDoesNotContainTheLegacyPackage() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE)) {
            for (Path file : files.filter(
                path -> path.toString()
                    .endsWith(".java"))
                .collect(Collectors.toList())) {
                assertFalse(
                    new String(Files.readAllBytes(file), StandardCharsets.UTF_8).contains("advsolar"),
                    "Legacy package reference in " + file);
            }
        }
    }

    @Test
    void gradleModIdMatchesTheModAnnotation() throws IOException {
        String gradleProperties = new String(
            Files.readAllBytes(ROOT.resolve("gradle.properties")),
            StandardCharsets.UTF_8);
        assertTrue(gradleProperties.contains("modId = AdvancedSolarPanel"));

        String modSource = new String(
            Files.readAllBytes(SOURCE.resolve("com/mrfuzzihead/advancedsolarpanels/AdvancedSolarPanel.java")),
            StandardCharsets.UTF_8);
        assertTrue(modSource.contains("modid = \"AdvancedSolarPanel\""));
    }

    @Test
    void englishAndRussianLanguageKeysMatch() throws IOException {
        assertEquals(readKeys(LANG.resolve("en_US.lang")), readKeys(LANG.resolve("ru_RU.lang")));
    }

    @Test
    void everyLanguageKeyIsNonEmpty() throws IOException {
        for (Path file : new Path[] { LANG.resolve("en_US.lang"), LANG.resolve("ru_RU.lang") }) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                assertTrue(separator > 0, "Malformed language line in " + file + ": " + line);
                assertFalse(
                    trimmed.substring(separator + 1)
                        .trim()
                        .isEmpty(),
                    "Empty translation in " + file + ": " + line);
            }
        }
    }

    private static Set<String> readKeys(Path file) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator > 0) {
                keys.add(trimmed.substring(0, separator));
            }
        }
        return keys;
    }
}
