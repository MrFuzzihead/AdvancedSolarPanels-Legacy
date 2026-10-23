package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.mrfuzzihead.advancedsolarpanels.utils.MTRecipeConfig;

class MTRecipeConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesBomCommentsCrlfVersionAndSkipsMalformedLines() throws Exception {
        String originalVersion = MTRecipeConfig.configVersion;
        Path file = this.tempDir.resolve("recipes.txt");
        String content = "\uFEFFversion=9.9\r\n" + "# comment\r\n"
            + "not enough parts\r\n"
            + "a;b;not-a-number\r\n"
            + "a;b;-1\r\n";
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));

        try {
            assertTrue(
                MTRecipeConfig.parse(file.toFile())
                    .isEmpty());
            assertEquals("9.9", MTRecipeConfig.configVersion);
        } finally {
            MTRecipeConfig.configVersion = originalVersion;
        }
    }

    @Test
    void formatsOreDictionaryDefaultRecipes() {
        int before = MTRecipeConfig.defaultLines.size();
        MTRecipeConfig.addDefaultRecipe("ingotFoo", 1, "ingotBar", 1, 123);
        assertEquals(before + 2, MTRecipeConfig.defaultLines.size());
        assertTrue(
            MTRecipeConfig.defaultLines.get(before)
                .contains("[123 EU]"));
        assertTrue(
            MTRecipeConfig.defaultLines.get(before + 1)
                .contains("oredict:ingotFoo;"));
        assertTrue(
            MTRecipeConfig.defaultLines.get(before + 1)
                .contains("oredict:ingotBar;"));
    }

    @Test
    void rejectsMissingItemRegistryData() {
        assertThrows(IllegalArgumentException.class, () -> MTRecipeConfig.formatItemData(null));
    }
}
