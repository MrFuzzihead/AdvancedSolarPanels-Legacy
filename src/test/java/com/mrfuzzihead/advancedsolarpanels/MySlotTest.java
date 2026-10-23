package com.mrfuzzihead.advancedsolarpanels;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mrfuzzihead.advancedsolarpanels.utils.MySlot;

class MySlotTest {

    @Test
    void nullPermissionIsTreatedAsFalse() {
        assertFalse(new MySlot(null, 0, 0, 0, null).isItemValid(null));
        assertFalse(new MySlot(null, 0, 0, 0, false).isItemValid(null));
        assertTrue(new MySlot(null, 0, 0, 0, true).isItemValid(null));
    }
}
