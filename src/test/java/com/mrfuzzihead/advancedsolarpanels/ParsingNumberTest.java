package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.NumberFormatting;

class ParsingNumberTest {

    @Test
    void groupsThousandsAndHandlesEmptyInput() {
        assertEquals("", NumberFormatting.groupDigits(null));
        assertEquals("", NumberFormatting.groupDigits(""));
        assertEquals("0", NumberFormatting.groupDigits("0"));
        assertEquals("999", NumberFormatting.groupDigits("999"));
        assertEquals("1 000", NumberFormatting.groupDigits("1000"));
        assertEquals("1 234 567", NumberFormatting.groupDigits("1234567"));
    }
}
