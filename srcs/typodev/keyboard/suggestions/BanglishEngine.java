package typodev.keyboard.suggestions;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BanglishEngine {
    private static BanglishEngine sInstance;

    /**
     * LRU cache: stores last N unique (lowercase) words and their candidate lists.
     * Eliminates redundant re-computation when user deletes/re-types same characters.
     * Capacity = 12: covers typical burst re-typing and autocorrect undo patterns.
     */
    private static final int CACHE_CAPACITY = 12;
    private final Map<String, List<Candidate>> mResultCache =
        new LinkedHashMap<String, List<Candidate>>(CACHE_CAPACITY, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<Candidate>> eldest) {
                return size() > CACHE_CAPACITY;
            }
        };

    private BanglishEngine() {
    }

    public static synchronized BanglishEngine instance() {
        if (sInstance == null) {
            sInstance = new BanglishEngine();
        }
        return sInstance;
    }

    public static boolean isLatinOnly(String word) {
        if (word == null || word.isEmpty()) return false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBengaliScript(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0980 && c <= 0x09FF) return true;
        }
        return false;
    }

    private static boolean isConsonant(char ch) {
        char c = Character.toLowerCase(ch);
        return (c >= 'a' && c <= 'z') && !(c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
    }

    private void addCandidateIfNotPresent(List<Candidate> list, String word, Candidate.Source source, int score) {
        if (word == null || word.isEmpty()) return;
        for (Candidate c : list) {
            if (c.word.equals(word)) return;
        }
        list.add(new Candidate(word, source, score, 0, 1.0f));
    }

    private static final java.util.Map<Character, char[]> QWERTY_NEIGHBORS = new java.util.HashMap<>();
    static {
        QWERTY_NEIGHBORS.put('q', new char[]{'w', 'a', 's'});
        QWERTY_NEIGHBORS.put('w', new char[]{'q', 'e', 'a', 's', 'd'});
        QWERTY_NEIGHBORS.put('e', new char[]{'w', 'r', 's', 'd', 'f'});
        QWERTY_NEIGHBORS.put('r', new char[]{'e', 't', 'd', 'f', 'g'});
        QWERTY_NEIGHBORS.put('t', new char[]{'r', 'y', 'f', 'g', 'h'});
        QWERTY_NEIGHBORS.put('y', new char[]{'t', 'u', 'g', 'h', 'j'});
        QWERTY_NEIGHBORS.put('u', new char[]{'y', 'i', 'h', 'j', 'k'});
        QWERTY_NEIGHBORS.put('i', new char[]{'u', 'o', 'j', 'k', 'l'});
        QWERTY_NEIGHBORS.put('o', new char[]{'i', 'p', 'k', 'l'});
        QWERTY_NEIGHBORS.put('p', new char[]{'o', 'l'});
        QWERTY_NEIGHBORS.put('a', new char[]{'q', 'w', 's', 'z'});
        QWERTY_NEIGHBORS.put('s', new char[]{'a', 'w', 'e', 'd', 'x', 'z'});
        QWERTY_NEIGHBORS.put('d', new char[]{'s', 'e', 'r', 'f', 'c', 'x'});
        QWERTY_NEIGHBORS.put('f', new char[]{'d', 'r', 't', 'g', 'v', 'c'});
        QWERTY_NEIGHBORS.put('g', new char[]{'f', 't', 'y', 'h', 'b', 'v'});
        QWERTY_NEIGHBORS.put('h', new char[]{'g', 'y', 'u', 'j', 'n', 'b'});
        QWERTY_NEIGHBORS.put('j', new char[]{'h', 'u', 'i', 'k', 'm', 'n'});
        QWERTY_NEIGHBORS.put('k', new char[]{'j', 'i', 'o', 'l', 'm'});
        QWERTY_NEIGHBORS.put('l', new char[]{'k', 'o', 'p'});
        QWERTY_NEIGHBORS.put('z', new char[]{'a', 's', 'x'});
        QWERTY_NEIGHBORS.put('x', new char[]{'z', 's', 'd', 'c'});
        QWERTY_NEIGHBORS.put('c', new char[]{'x', 'd', 'f', 'v'});
        QWERTY_NEIGHBORS.put('v', new char[]{'c', 'f', 'g', 'b'});
        QWERTY_NEIGHBORS.put('b', new char[]{'v', 'g', 'h', 'n'});
        QWERTY_NEIGHBORS.put('n', new char[]{'b', 'h', 'j', 'm'});
        QWERTY_NEIGHBORS.put('m', new char[]{'n', 'j', 'k'});
    }

    private List<String> getHeuristicVariants(String word) {
        List<String> variants = new ArrayList<>();
        String lower = word.toLowerCase(Locale.ROOT);

        // 1. Initial 'o' for 'অ' vs 'ও'
        if (lower.startsWith("o") && lower.length() > 1) {
            variants.add("a" + lower.substring(1));
        }

        // 2. 'sth' <-> 'st' <-> 't' (e.g. osthisto -> ostitwo / astitwo)
        if (lower.contains("sth")) {
            variants.add(lower.replace("sth", "st"));
            variants.add(lower.replace("sth", "t"));
        }
        if (lower.endsWith("sto") || lower.endsWith("to")) {
            variants.add(lower.replaceAll("(s?to)$", "two"));
            variants.add(lower.replaceAll("(s?to)$", "tto"));
        }

        // 3. 'badhi' <-> 'byadhi'
        if (lower.contains("badhi")) {
            variants.add(lower.replace("badhi", "byadhi"));
        }

        // 4. Missing / extra 'h' after aspirated consonants
        if (lower.contains("k") && !lower.contains("kh")) variants.add(lower.replace("k", "kh"));
        if (lower.contains("kh")) variants.add(lower.replace("kh", "k"));
        if (lower.contains("c") && !lower.contains("ch")) variants.add(lower.replace("c", "ch"));
        if (lower.contains("ch")) variants.add(lower.replace("ch", "c"));
        if (lower.contains("s") && !lower.contains("sh")) variants.add(lower.replace("s", "sh"));
        if (lower.contains("sh")) variants.add(lower.replace("sh", "s"));
        if (lower.contains("t") && !lower.contains("th")) variants.add(lower.replace("t", "th"));
        if (lower.contains("th")) variants.add(lower.replace("th", "t"));
        if (lower.contains("d") && !lower.contains("dh")) variants.add(lower.replace("d", "dh"));
        if (lower.contains("dh")) variants.add(lower.replace("dh", "d"));
        if (lower.contains("b") && !lower.contains("bh")) variants.add(lower.replace("b", "bh"));
        if (lower.contains("bh")) variants.add(lower.replace("bh", "b"));
        if (lower.contains("z")) variants.add(lower.replace("z", "j"));
        if (lower.contains("v")) variants.add(lower.replace("v", "bh"));
        if (lower.contains("bh")) variants.add(lower.replace("bh", "v"));
        if (lower.contains("f")) variants.add(lower.replace("f", "ph"));
        if (lower.contains("ph")) variants.add(lower.replace("ph", "f"));
        if (lower.contains("ey")) variants.add(lower.replace("ey", "ei"));
        if (lower.contains("ei")) variants.add(lower.replace("ei", "ey"));

        return variants;
    }

    public List<Candidate> generateCandidates(String latinWord, int limit) {
        return generateCandidates(latinWord, limit, null);
    }

    public List<Candidate> generateCandidates(String latinWord, int limit, Context context) {
        if (latinWord == null || latinWord.isEmpty() || !isLatinOnly(latinWord)) {
            return Collections.emptyList();
        }

        String lower = latinWord.toLowerCase(Locale.ROOT);

        // Cache hit: return previously computed results immediately
        List<Candidate> cached = mResultCache.get(latinWord);
        if (cached != null) {
            return cached;
        }

        List<Candidate> results = new ArrayList<>();

        // 0. Single letter priority overrides - always AUTOCORRECT so they cannot be displaced
        if (latinWord.length() == 1) {
            if (latinWord.equals("R")) addCandidateIfNotPresent(results, "ড়", Candidate.Source.AUTOCORRECT, 255);
            if (latinWord.equals("r")) {
                addCandidateIfNotPresent(results, "র", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "ড়", Candidate.Source.AUTOCORRECT, 240);
            }
            if (lower.equals("z")) addCandidateIfNotPresent(results, "য", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("j")) {
                addCandidateIfNotPresent(results, "জ", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "য", Candidate.Source.AUTOCORRECT, 240);
            }
            if (lower.equals("a")) addCandidateIfNotPresent(results, "আ", Candidate.Source.AUTOCORRECT, 255);
            // Vowel single-letter overrides — must always win over lexicon
            if (lower.equals("e")) {
                addCandidateIfNotPresent(results, "এ", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "ই", Candidate.Source.AUTOCORRECT, 240);
            }
            if (lower.equals("i")) {
                addCandidateIfNotPresent(results, "ই", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "ঈ", Candidate.Source.AUTOCORRECT, 240);
            }
            if (lower.equals("u")) {
                addCandidateIfNotPresent(results, "উ", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "ঊ", Candidate.Source.AUTOCORRECT, 240);
            }
            if (lower.equals("o")) {
                addCandidateIfNotPresent(results, "ও", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "অ", Candidate.Source.AUTOCORRECT, 240);
            }
            if (latinWord.equals("D")) addCandidateIfNotPresent(results, "ড", Candidate.Source.AUTOCORRECT, 255);
            if (latinWord.equals("d")) {
                addCandidateIfNotPresent(results, "দ", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "ড", Candidate.Source.AUTOCORRECT, 240);
            }
            if (latinWord.equals("T")) addCandidateIfNotPresent(results, "ট", Candidate.Source.AUTOCORRECT, 255);
            if (latinWord.equals("t")) {
                addCandidateIfNotPresent(results, "ত", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "ট", Candidate.Source.AUTOCORRECT, 240);
            }
            if (lower.equals("y")) {
                addCandidateIfNotPresent(results, "য়", Candidate.Source.AUTOCORRECT, 255);
                addCandidateIfNotPresent(results, "য", Candidate.Source.AUTOCORRECT, 240);
            }
            if (latinWord.equals("H")) addCandidateIfNotPresent(results, "ঃ", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("h")) addCandidateIfNotPresent(results, "হ", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("k")) addCandidateIfNotPresent(results, "ক", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("g")) addCandidateIfNotPresent(results, "গ", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("c")) addCandidateIfNotPresent(results, "স", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("n")) addCandidateIfNotPresent(results, "ন", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("p")) addCandidateIfNotPresent(results, "প", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("b")) addCandidateIfNotPresent(results, "ব", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("m")) addCandidateIfNotPresent(results, "ম", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("l")) addCandidateIfNotPresent(results, "ল", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("s")) addCandidateIfNotPresent(results, "স", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("f")) addCandidateIfNotPresent(results, "ফ", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("v")) addCandidateIfNotPresent(results, "ভ", Candidate.Source.AUTOCORRECT, 255);
            if (lower.equals("w")) addCandidateIfNotPresent(results, "ও", Candidate.Source.AUTOCORRECT, 255);
            // Single-letter results are always correct, cache them too
            mResultCache.put(latinWord, results);
            return results;
        }

        // 0.0. User's Personal Banglish Learned Mappings (highest user affinity)
        if (context != null && latinWord.length() >= 2) {
            List<Candidate> userLearned = BanglishUserDictionary.instance(context).getCandidates(lower);
            if (userLearned != null && !userLearned.isEmpty()) {
                for (Candidate uc : userLearned) {
                    addCandidateIfNotPresent(results, uc.word, Candidate.Source.AUTOCORRECT, 255);
                }
            }
        }

        // 0.1. Hardcoded Fuzzy Banglish overrides for top complaints
        if (lower.equals("rin")) addCandidateIfNotPresent(results, "ঋণ", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("ekti")) addCandidateIfNotPresent(results, "একটি", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("cirodin")) addCandidateIfNotPresent(results, "চিরদিন", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("kosto")) addCandidateIfNotPresent(results, "কষ্ট", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("valobasa")) addCandidateIfNotPresent(results, "ভালোবাসা", Candidate.Source.AUTOCORRECT, 255);
        // Phonetic correction overrides — lexicon entries that conflict with phonetic spelling
        if (lower.equals("gud")) addCandidateIfNotPresent(results, "গুড", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("parbe")) addCandidateIfNotPresent(results, "পারবে", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("parbo")) {
            addCandidateIfNotPresent(results, "পারব", Candidate.Source.AUTOCORRECT, 255);
            addCandidateIfNotPresent(results, "পারবো", Candidate.Source.AUTOCORRECT, 250);
        }
        if (lower.equals("thakbo")) {
            addCandidateIfNotPresent(results, "থাকব", Candidate.Source.AUTOCORRECT, 255);
            addCandidateIfNotPresent(results, "থাকবো", Candidate.Source.AUTOCORRECT, 250);
        }
        if (lower.equals("parbi")) addCandidateIfNotPresent(results, "পারবি", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("parben")) addCandidateIfNotPresent(results, "পারবেন", Candidate.Source.AUTOCORRECT, 255);
        if (lower.equals("bood") || lower.equals("bhud") || lower.equals("bhod")) addCandidateIfNotPresent(results, "ভুড", Candidate.Source.AUTOCORRECT, 230);
        // Common phonetic words where lexicon incorrectly maps to unrelated meanings
        if (lower.equals("shikte")) addCandidateIfNotPresent(results, "শিখতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("dekhte")) addCandidateIfNotPresent(results, "দেখতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("khete")) addCandidateIfNotPresent(results, "খেতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("jete")) addCandidateIfNotPresent(results, "যেতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("nite")) addCandidateIfNotPresent(results, "নিতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("dite")) addCandidateIfNotPresent(results, "দিতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("korte")) addCandidateIfNotPresent(results, "করতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("bolte")) addCandidateIfNotPresent(results, "বলতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("porte")) addCandidateIfNotPresent(results, "পড়তে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("likhte")) addCandidateIfNotPresent(results, "লিখতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("sunle")) addCandidateIfNotPresent(results, "শুনলে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("ashte")) addCandidateIfNotPresent(results, "আসতে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("jacche") || lower.equals("jache")) addCandidateIfNotPresent(results, "যাচ্ছে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("asche") || lower.equals("ashche")) addCandidateIfNotPresent(results, "আসছে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("thakbe")) addCandidateIfNotPresent(results, "থাকবে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("khaabe") || lower.equals("khabe")) addCandidateIfNotPresent(results, "খাবে", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("nebo") || lower.equals("nibo")) addCandidateIfNotPresent(results, "নেবো", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("debo") || lower.equals("dibo")) addCandidateIfNotPresent(results, "দেবো", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("nebo")) addCandidateIfNotPresent(results, "নেবো", Candidate.Source.AUTOCORRECT, 480);
        if (lower.equals("xre")) addCandidateIfNotPresent(results, "ধরে", Candidate.Source.AUTOCORRECT, 480);

        // 0.5. Master 50,000+ Banglish Lexicon lookup
        BanglishLexiconManager lex = BanglishLexiconManager.instance(context);
        String[] lexHits = lex.getCandidates(lower);
        if (lexHits != null && lexHits.length > 0) {
            int candScore = 255;
            for (int i = 0; i < lexHits.length; i++) {
                String hit = lexHits[i];
                if (hit != null && !hit.isEmpty()) {
                    Candidate.Source src = (i == 0) ? Candidate.Source.AUTOCORRECT : Candidate.Source.BANGLISH;
                    addCandidateIfNotPresent(results, hit, src, candScore);
                    candScore = Math.max(80, candScore - 20); // 255, 235, 215...
                }
            }
        }

        // 0.7. QWERTY Spatial Typo Search — only for words >= 4 chars
        // (3-char words: results already solid via lexicon; 1-2 chars: single-letter handled above)
        // Running on every keystroke for short words causes false positives and wastes ~35 lookups/word.
        if (lower.length() >= 4) {
            for (int i = 0; i < lower.length(); i++) {
                char ch = lower.charAt(i);
                char[] neighbors = QWERTY_NEIGHBORS.get(ch);
                if (neighbors != null) {
                    for (char n : neighbors) {
                        String typoVar = lower.substring(0, i) + n + lower.substring(i + 1);
                        String[] typoHits = lex.getCandidates(typoVar);
                        if (typoHits != null && typoHits.length > 0) {
                            for (String th : typoHits) {
                                addCandidateIfNotPresent(results, th, Candidate.Source.AUTOCORRECT, 240);
                            }
                        }
                    }
                }
            }
        }

        // 0.8. Dynamic Compound Word Splitting (e.g. moronbadhi -> moron + badhi -> মরণ + ব্যাধি -> মরণব্যাধি)
        if (lower.length() >= 6) {
            for (int split = 3; split <= lower.length() - 3; split++) {
                String p1 = lower.substring(0, split);
                String p2 = lower.substring(split);
                String p1Bn = getExactBangla(p1);
                String p2Bn = getExactBangla(p2);
                if (p1Bn != null && !p1Bn.isEmpty() && p2Bn != null && !p2Bn.isEmpty()) {
                    addCandidateIfNotPresent(results, p1Bn + p2Bn, Candidate.Source.AUTOCORRECT, 250);
                }
            }
        }

        // 1. Full parser hit (Dictionary + Phonetic rules)
        String fullParse = AvroPhoneticParser.parse(latinWord, true, true, true);
        if (fullParse != null && !fullParse.isEmpty()) {
            addCandidateIfNotPresent(results, fullParse, Candidate.Source.BANGLISH, 200);
        }

        // 2. Pure Phonetic fallback without dictionary
        String purePhonetic = AvroPhoneticParser.parse(latinWord, true, true, false);
        if (purePhonetic != null && !purePhonetic.isEmpty() && !purePhonetic.equals(fullParse)) {
            addCandidateIfNotPresent(results, purePhonetic, Candidate.Source.BANGLISH, 150);
        }

        // 2.5. Initial 'o' followed by consonant should also offer 'অ' alternative
        // so words starting with 'o' (like osthisto -> ওস্থিস্ত) are also offered as অস্থিস্ত
        if (lower.startsWith("o") && lower.length() > 1 && isConsonant(lower.charAt(1))) {
            if (purePhonetic != null && purePhonetic.startsWith("ও")) {
                String oAlt = "অ" + purePhonetic.substring(1);
                addCandidateIfNotPresent(results, oAlt, Candidate.Source.BANGLISH, 195);
            }
            if (fullParse != null && fullParse.startsWith("ও")) {
                String oAltFull = "অ" + fullParse.substring(1);
                addCandidateIfNotPresent(results, oAltFull, Candidate.Source.BANGLISH, 205);
            }
        }

        // 3. Heuristic fuzzy variations
        List<String> variants = getHeuristicVariants(latinWord);
        for (String var : variants) {
            String[] vLexHits = lex.getCandidates(var);
            if (vLexHits != null && vLexHits.length > 0) {
                for (String vHit : vLexHits) {
                    addCandidateIfNotPresent(results, vHit, Candidate.Source.BANGLISH, 185);
                }
            }
            String vParse = AvroPhoneticParser.parse(var, true, true, true);
            if (vParse != null && !vParse.isEmpty()) {
                addCandidateIfNotPresent(results, vParse, Candidate.Source.BANGLISH, 140);
            }
        }

        // Store in cache before returning
        mResultCache.put(latinWord, results);
        return results;
    }

    public String getExactBangla(String latinWord) {
        if (latinWord == null) return null;
        String lower = latinWord.trim().toLowerCase(Locale.ROOT);
        // User's personal learned mapping takes top priority
        String userHit = BanglishUserDictionary.instance(null).getTopBengali(lower);
        if (userHit != null) {
            return userHit;
        }
        String smartHit = SmartDictionary.DICT.get(lower);
        if (smartHit != null) {
            return smartHit;
        }
        String[] hits = BanglishLexiconManager.instance().getCandidates(lower);
        if (hits != null && hits.length > 0) {
            return hits[0];
        }
        String avroHit = AvroData.DICTIONARY.get(lower);
        if (avroHit != null) {
            return avroHit.replace("\u09a1\u09bc", "\u09dc")
                          .replace("\u09a2\u09bc", "\u09dd")
                          .replace("\u09af\u09bc", "\u09df");
        }
        return null;
    }

    public static String transliteratePhonetic(String text) {
        if (text == null || text.isEmpty()) return "";
        String exact = instance().getExactBangla(text);
        if (exact != null && !exact.isEmpty()) {
            return exact;
        }
        return AvroPhoneticParser.parse(text, true, true, true);
    }
}
