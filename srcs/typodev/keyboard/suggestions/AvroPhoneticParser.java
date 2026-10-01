package typodev.keyboard.suggestions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvroPhoneticParser {
    private static final String BENGALI_NUKTA = "\u09BC"; // ়
    private static final String BENGALI_HASANTA = "\u09CD"; // ্
    private static final String IMPLICIT_A_MARKER = "\u200C"; // ‌ (ZWNJ)

    private static boolean isVowel(char ch) {
        return ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u' ||
               ch == 'A' || ch == 'E' || ch == 'I' || ch == 'O' || ch == 'U';
    }

    private static boolean isConsonant(char ch) {
        boolean isAlpha = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
        return isAlpha && !isVowel(ch);
    }

    private static boolean isPunctuation(String ch) {
        if (ch.isEmpty()) return true;
        char c = ch.charAt(0);
        return !isVowel(c) && !isConsonant(c);
    }

    private static boolean isBanglaConsonant(char cp) {
        return (cp >= 0x0995 && cp <= 0x09B9) ||
               cp == 0x09CE || // ৎ
               cp == 0x09DC || // ড়
               cp == 0x09DD || // ঢ়
               cp == 0x09DF;   // য়
    }

    private static boolean endsInBanglaConsonant(String str) {
        if (str == null || str.isEmpty()) return false;
        char last = str.charAt(str.length() - 1);
        if (isBanglaConsonant(last)) return true;

        if (String.valueOf(last).equals(BENGALI_NUKTA) && str.length() >= 2) {
            return isBanglaConsonant(str.charAt(str.length() - 2));
        }
        return false;
    }

    private static boolean startsWithBanglaConsonant(String str) {
        if (str == null || str.isEmpty()) return false;
        return isBanglaConsonant(str.charAt(0));
    }

    private static String charBefore(String input, int pos) {
        return pos > 0 ? String.valueOf(input.charAt(pos - 1)) : "";
    }

    private static String charAfter(String input, int pos, int findLength) {
        int idx = pos + findLength;
        return idx < input.length() ? String.valueOf(input.charAt(idx)) : "";
    }

    private static boolean testCondition(AvroData.MatchCondition cond, String input, int pos, int findLength) {
        String ch = cond.isPrefix ? charBefore(input, pos) : charAfter(input, pos, findLength);
        boolean result = false;
        
        switch (cond.scope) {
            case "vowel":
                result = !ch.isEmpty() && isVowel(ch.charAt(0));
                break;
            case "consonant":
                result = !ch.isEmpty() && isConsonant(ch.charAt(0));
                break;
            case "punctuation":
                result = isPunctuation(ch);
                break;
            case "exact":
                String val = cond.value != null ? cond.value : "";
                result = ch.equals(val);
                break;
        }

        return cond.negative ? !result : result;
    }

    private static String tryPattern(AvroData.PatternEntry entry, String input, int pos) {
        if (!input.startsWith(entry.find, pos)) return null;

        if (entry.rules != null && entry.rules.length > 0) {
            for (AvroData.PatternRule rule : entry.rules) {
                boolean allMatch = true;
                for (AvroData.MatchCondition cond : rule.matches) {
                    if (!testCondition(cond, input, pos, entry.find.length())) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) return rule.replace;
            }
        }
        return entry.replace;
    }

    private static class Match {
        String find;
        String replace;
        Match(String find, String replace) { this.find = find; this.replace = replace; }
    }

    /**
     * Precomputed index: first char -> sorted list of PatternEntry (longest find first).
     * Reduces findMatch from O(n_patterns) linear scan to O(bucket_size) where bucket ~1-5.
     */
    private static final Map<Character, List<AvroData.PatternEntry>> PATTERN_INDEX;
    static {
        PATTERN_INDEX = new HashMap<>(64);
        for (AvroData.PatternEntry entry : AvroData.PATTERNS) {
            char first = Character.toLowerCase(entry.find.charAt(0));
            List<AvroData.PatternEntry> bucket = PATTERN_INDEX.get(first);
            if (bucket == null) {
                bucket = new ArrayList<>(8);
                PATTERN_INDEX.put(first, bucket);
            }
            bucket.add(entry);
        }
        // Sort each bucket: longest find first (ensures longest-match-first)
        for (List<AvroData.PatternEntry> bucket : PATTERN_INDEX.values()) {
            Collections.sort(bucket, (a, b) -> b.find.length() - a.find.length());
        }
    }

    private static Match findMatch(String segment, int pos) {
        char ch = segment.charAt(pos);
        // Try lowercase bucket first
        char lower = Character.toLowerCase(ch);
        List<AvroData.PatternEntry> bucket = PATTERN_INDEX.get(lower);
        if (bucket != null) {
            for (AvroData.PatternEntry entry : bucket) {
                String replace = tryPattern(entry, segment, pos);
                if (replace != null) return new Match(entry.find, replace);
            }
        }
        // If uppercase, try uppercase bucket too (case-sensitive patterns like 'T', 'D', 'N')
        if (ch != lower) {
            List<AvroData.PatternEntry> upperBucket = PATTERN_INDEX.get(ch);
            if (upperBucket != null) {
                for (AvroData.PatternEntry entry : upperBucket) {
                    String replace = tryPattern(entry, segment, pos);
                    if (replace != null) return new Match(entry.find, replace);
                }
            }
        }
        return null;
    }

    private static String phoneticParse(String segment, boolean banglaDigits) {
        StringBuilder bangla = new StringBuilder();
        int pos = 0;

        while (pos < segment.length()) {
            Match match = findMatch(segment, pos);

            if (match == null) {
                char ch = segment.charAt(pos);
                if (ch >= 'A' && ch <= 'Z') {
                    String lowered = segment.substring(0, pos) + Character.toLowerCase(ch) + segment.substring(pos + 1);
                    match = findMatch(lowered, pos);
                }
            }

            if (match == null) {
                bangla.append(segment.charAt(pos));
                pos++;
                continue;
            }

            String effective = match.replace;
            if (!banglaDigits && effective.matches(".*[০-৯].*")) {
                effective = String.valueOf(segment.charAt(pos));
            }

            if (endsInBanglaConsonant(bangla.toString()) && startsWithBanglaConsonant(effective)) {
                bangla.append(BENGALI_HASANTA);
            }

            bangla.append(effective);
            pos += match.find.length();
        }

        return bangla.toString();
    }

    private static final Pattern TOKEN_REGEX = Pattern.compile("([A-Za-z]+)|([^A-Za-z]+)");

    public static String parse(String input, boolean banglaDigits, boolean banglaFullStop, boolean useDictionary) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder bangla = new StringBuilder();

        if (!useDictionary) {
            bangla.append(phoneticParse(input, banglaDigits));
        } else {
            Matcher matcher = TOKEN_REGEX.matcher(input);
            while (matcher.find()) {
                String word = matcher.group(1);
                String other = matcher.group(2);

                if (word != null) {
                    String hit = AvroData.DICTIONARY.get(word.toLowerCase(Locale.ROOT));
                    if (hit != null) {
                        bangla.append(hit);
                    } else {
                        bangla.append(phoneticParse(word, banglaDigits));
                    }
                } else if (other != null) {
                    bangla.append(phoneticParse(other, banglaDigits));
                }
            }
        }

        String result = bangla.toString();
        if (!banglaFullStop) {
            result = result.replace("।", ".");
        }
        if (result.contains(IMPLICIT_A_MARKER)) {
            result = result.replace(IMPLICIT_A_MARKER, "");
        }

        result = java.text.Normalizer.normalize(result, java.text.Normalizer.Form.NFC);
        result = result.replace("\u09a1\u09bc", "\u09dc")
                       .replace("\u09a2\u09bc", "\u09dd")
                       .replace("\u09af\u09bc", "\u09df");
        return result;
    }
}
