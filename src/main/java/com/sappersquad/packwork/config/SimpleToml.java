package com.sappersquad.packwork.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately tiny TOML reader for the one file Packwork writes itself
 * ({@code config/packwork-server.toml}). Handles exactly the subset the generated
 * file uses - comments, {@code [section]} / {@code [section.sub]} headers, and
 * {@code key = value} lines where the value is a boolean, an integer, a float, a
 * quoted string, or a single-line array of quoted strings. Anything it cannot read
 * is skipped with a warning, never a crash - a broken config line falls back to the
 * shipped default for that key (pause, never punish - even for packmakers).
 *
 * <p>Hand-rolled on purpose: the same file, parsed by the same code, on NeoForge and
 * Fabric alike - no config-library dependency on either loader, and no chance of the
 * two loaders' keys drifting apart.
 */
public final class SimpleToml {

    /** Dotted-key map: {@code [tiers.canvas] slots = 54} lands as {@code "tiers.canvas.slots"}. */
    public static Map<String, Object> parse(List<String> lines, List<String> problems) {
        Map<String, Object> out = new LinkedHashMap<>();
        String prefix = "";
        int n = 0;
        for (String raw : lines) {
            n++;
            String line = stripComment(raw).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                prefix = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                problems.add("line " + n + ": not a 'key = value' line: " + raw.trim());
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            Object parsed = parseValue(value);
            if (parsed == null) {
                problems.add("line " + n + ": unreadable value for '" + key + "': " + value);
                continue;
            }
            out.put(prefix.isEmpty() ? key : prefix + "." + key, parsed);
        }
        return out;
    }

    /** A comment starts at the first {@code #} that is not inside a quoted string. */
    private static String stripComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inString = !inString;
            else if (c == '#' && !inString) return line.substring(0, i);
        }
        return line;
    }

    /** Boolean, long, double, quoted string, or a single-line array of quoted strings; null = unreadable. */
    private static Object parseValue(String v) {
        if (v.equals("true")) return Boolean.TRUE;
        if (v.equals("false")) return Boolean.FALSE;
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            return v.substring(1, v.length() - 1);
        }
        if (v.startsWith("[") && v.endsWith("]")) {
            String body = v.substring(1, v.length() - 1).trim();
            List<String> items = new ArrayList<>();
            if (body.isEmpty()) return items;
            for (String part : body.split(",")) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                if (p.startsWith("\"") && p.endsWith("\"") && p.length() >= 2) {
                    items.add(p.substring(1, p.length() - 1));
                } else {
                    return null; // arrays hold quoted strings only in this file
                }
            }
            return items;
        }
        try {
            if (v.contains(".") || v.contains("e") || v.contains("E")) return Double.parseDouble(v);
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- typed readers with clamping; a missing or wrong-typed key keeps the default ----

    public static int readInt(Map<String, Object> map, String key, int def, int min, int max, List<String> problems) {
        Object v = map.get(key);
        if (v == null) return def;
        if (!(v instanceof Long l)) {
            problems.add("'" + key + "' wants a whole number; keeping " + def);
            return def;
        }
        long clamped = Math.max(min, Math.min(max, l));
        if (clamped != l) problems.add("'" + key + "' = " + l + " is outside " + min + ".." + max + "; clamped to " + clamped);
        return (int) clamped;
    }

    public static long readLong(Map<String, Object> map, String key, long def, long min, long max, List<String> problems) {
        Object v = map.get(key);
        if (v == null) return def;
        if (!(v instanceof Long l)) {
            problems.add("'" + key + "' wants a whole number; keeping " + def);
            return def;
        }
        long clamped = Math.max(min, Math.min(max, l));
        if (clamped != l) problems.add("'" + key + "' = " + l + " is outside " + min + ".." + max + "; clamped to " + clamped);
        return clamped;
    }

    public static double readDouble(Map<String, Object> map, String key, double def, double min, double max, List<String> problems) {
        Object v = map.get(key);
        if (v == null) return def;
        double d;
        if (v instanceof Double dd) d = dd;
        else if (v instanceof Long l) d = l;
        else {
            problems.add("'" + key + "' wants a number; keeping " + def);
            return def;
        }
        double clamped = Math.max(min, Math.min(max, d));
        if (clamped != d) problems.add("'" + key + "' = " + d + " is outside " + min + ".." + max + "; clamped to " + clamped);
        return clamped;
    }

    public static boolean readBool(Map<String, Object> map, String key, boolean def, List<String> problems) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        problems.add("'" + key + "' wants true or false; keeping " + def);
        return def;
    }

    public static String readString(Map<String, Object> map, String key, String def, List<String> problems) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof String s) return s;
        problems.add("'" + key + "' wants a quoted string; keeping \"" + def + "\"");
        return def;
    }

    @SuppressWarnings("unchecked")
    public static List<String> readStringList(Map<String, Object> map, String key, List<String> def, List<String> problems) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof List<?> l) return (List<String>) l;
        problems.add("'" + key + "' wants an array of quoted strings; keeping the default");
        return def;
    }

    private SimpleToml() {}
}
