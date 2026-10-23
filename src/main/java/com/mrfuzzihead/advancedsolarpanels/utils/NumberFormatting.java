package com.mrfuzzihead.advancedsolarpanels.utils;

/** Locale-independent digit grouping used by the machine GUIs. */
public final class NumberFormatting {

    private NumberFormatting() {}

    public static String groupDigits(String number) {
        if (number == null || number.isEmpty()) {
            return "";
        }
        String grouped = "";
        int count = 0;
        for (int i = number.length() - 1; i >= 0; i--) {
            if (count == 3) {
                grouped = " " + grouped;
                count = 0;
            }
            count++;
            grouped = number.charAt(i) + grouped;
        }
        return grouped;
    }
}
