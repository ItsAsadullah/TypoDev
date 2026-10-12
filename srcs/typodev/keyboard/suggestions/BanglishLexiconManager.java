package typodev.keyboard.suggestions;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import typodev.keyboard.R;

/**
 * High-performance Banglish (Romanized Bangla) Lexicon Manager.
 * Loads 50,000+ words compiled from parallel corpora, English loanwords,
 * and high-frequency colloquial dictionaries.
 */
public class BanglishLexiconManager {
    private static volatile BanglishLexiconManager sInstance;
    private final Map<String, String[]> mDictionary = new HashMap<>(55000);
    private volatile boolean mLoaded = false;
    private volatile boolean mLoading = false;

    private BanglishLexiconManager(Context context) {
        if (context != null) {
            loadAsync(context.getApplicationContext());
        } else {
            loadJvmFallback();
        }
    }

    public static BanglishLexiconManager instance(Context context) {
        if (sInstance == null) {
            synchronized (BanglishLexiconManager.class) {
                if (sInstance == null) {
                    sInstance = new BanglishLexiconManager(context != null ? context.getApplicationContext() : null);
                }
            }
        }
        if (context != null && !sInstance.mLoaded && !sInstance.mLoading) {
            sInstance.loadAsync(context.getApplicationContext());
        }
        return sInstance;
    }

    public static BanglishLexiconManager instance() {
        return instance(null);
    }

    public boolean isLoaded() {
        return mLoaded;
    }

    public void init(Context context) {
        if (!mLoaded && !mLoading && context != null) {
            loadAsync(context.getApplicationContext());
        }
    }

    private void loadAsync(final Context context) {
        if (mLoaded || mLoading) return;
        mLoading = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                loadSync(context);
            }
        }, "BanglishLexiconLoader");
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }

    public synchronized void loadSync(Context context) {
        if (mLoaded) return;
        try {
            InputStream is = null;
            if (context != null) {
                try {
                    is = context.getResources().openRawResource(R.raw.banglish_master_lexicon);
                } catch (Throwable ignored) {}
            }
            if (is == null) {
                // Try file system for JVM testing
                File f = new File("res/raw/banglish_master_lexicon.txt");
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }
            if (is != null) {
                loadFromStream(is);
                mLoaded = true;
            }
        } catch (Throwable e) {
            typodev.keyboard.Logs.exn("BanglishLexiconManager", e);
        } finally {
            mLoading = false;
        }
    }

    private void loadJvmFallback() {
        try {
            File f = new File("res/raw/banglish_master_lexicon.txt");
            if (f.exists()) {
                loadFromStream(new FileInputStream(f));
                mLoaded = true;
            }
        } catch (Throwable ignored) {}
    }

    private void loadFromStream(InputStream is) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8), 32768)) {
            String line;
            while ((line = br.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) {
                    String key = line.substring(0, tab).trim().toLowerCase(Locale.ROOT);
                    String vals = line.substring(tab + 1).trim();
                    if (!key.isEmpty() && !vals.isEmpty()) {
                        vals = vals.replace("\u09a1\u09bc", "\u09dc")
                                   .replace("\u09a2\u09bc", "\u09dd")
                                   .replace("\u09af\u09bc", "\u09df");
                        String[] candidates = vals.split(",");
                        mDictionary.put(key, candidates);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public String[] getCandidates(String latinWord) {
        if (latinWord == null || latinWord.isEmpty()) return null;
        String lower = latinWord.toLowerCase(Locale.ROOT);

        // 1. Direct hit in 50,000+ words dictionary
        String[] hit = mDictionary.get(lower);
        if (hit != null && hit.length > 0) {
            return hit;
        }

        // 2. Fallback to SmartDictionary static map if master not yet loaded
        String staticHit = SmartDictionary.DICT.get(lower);
        if (staticHit != null) {
            return new String[]{staticHit};
        }

        // 3. Normalized fuzzy lookup (e.g. collapse duplicate letters: 'onnek' -> 'onek', 'kkhoma' -> 'khoma')
        String normalized = normalize(lower);
        if (!normalized.equals(lower)) {
            String[] normHit = mDictionary.get(normalized);
            if (normHit != null && normHit.length > 0) {
                return normHit;
            }
        }

        return null;
    }

    private String normalize(String s) {
        if (s == null || s.length() <= 1) return s;
        StringBuilder sb = new StringBuilder(s.length());
        char prev = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // Allow double 'e' or double 'o' (like 'feel', 'good')
            if (c == prev && c != 'e' && c != 'o') {
                continue;
            }
            sb.append(c);
            prev = c;
        }
        return sb.toString();
    }
}
