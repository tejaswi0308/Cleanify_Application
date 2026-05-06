package com.example.cleanify_application.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateTimeUtils {
    public static long parseIsoToMillis(String isoString) {
        if (isoString == null) return 0;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                return java.time.OffsetDateTime.parse(isoString).toInstant().toEpochMilli();
            } else {
                String cleaned = isoString.replace("Z", "+0000");
                if (cleaned.contains("+") && cleaned.lastIndexOf("+") > cleaned.lastIndexOf("T")) {
                    int lastPlus = cleaned.lastIndexOf("+");
                    String part1 = cleaned.substring(0, lastPlus);
                    String part2 = cleaned.substring(lastPlus).replace(":", "");
                    cleaned = part1 + part2;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
                return sdf.parse(cleaned).getTime();
            }
        } catch (Exception e) {
            return 0;
        }
    }
    public static String getCurrentIsoTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            return sdf.format(new java.util.Date());
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
