package typodev.keyboard.suggestions;

import java.util.HashMap;
import java.util.Map;

public class AvroData {

    public static class MatchCondition {
        public boolean isPrefix;
        public String scope; // 'vowel', 'consonant', 'punctuation', 'exact'
        public String value;
        public boolean negative;

        public MatchCondition(boolean isPrefix, String scope, String value, boolean negative) {
            this.isPrefix = isPrefix;
            this.scope = scope;
            this.value = value;
            this.negative = negative;
        }
    }

    public static class PatternRule {
        public MatchCondition[] matches;
        public String replace;

        public PatternRule(MatchCondition[] matches, String replace) {
            this.matches = matches;
            this.replace = replace;
        }
    }

    public static class PatternEntry {
        public String find;
        public String replace;
        public PatternRule[] rules;

        public PatternEntry(String find, String replace, PatternRule[] rules) {
            this.find = find;
            this.replace = replace;
            this.rules = rules;
        }
    }

    public static final Map<String, String> DICTIONARY = new HashMap<>();
    public static final PatternEntry[] PATTERNS;

    static {
        DICTIONARY.put("ami", "আমি");
        DICTIONARY.put("amar", "আমার");
        DICTIONARY.put("amake", "আমাকে");
        DICTIONARY.put("amra", "আমরা");
        DICTIONARY.put("amader", "আমাদের");
        DICTIONARY.put("tumi", "তুমি");
        DICTIONARY.put("tomar", "তোমার");
        DICTIONARY.put("tomake", "তোমাকে");
        DICTIONARY.put("tomra", "তোমরা");
        DICTIONARY.put("tomader", "তোমাদের");
        DICTIONARY.put("tui", "তুই");
        DICTIONARY.put("tor", "তোর");
        DICTIONARY.put("toke", "তোকে");
        DICTIONARY.put("tora", "তোরা");
        DICTIONARY.put("toder", "তোদের");
        DICTIONARY.put("apni", "আপনি");
        DICTIONARY.put("apnar", "আপনার");
        DICTIONARY.put("apnara", "আপনারা");
        DICTIONARY.put("apnader", "আপনাদের");
        DICTIONARY.put("se", "সে");
        DICTIONARY.put("tar", "তার");
        DICTIONARY.put("take", "তাকে");
        DICTIONARY.put("tara", "তারা");
        DICTIONARY.put("tader", "তাদের");
        DICTIONARY.put("tini", "তিনি");
        DICTIONARY.put("tahar", "তাহার");
        DICTIONARY.put("tahara", "তাহারা");
        DICTIONARY.put("o", "ও");
        DICTIONARY.put("ora", "ওরা");
        DICTIONARY.put("oder", "ওদের");
        DICTIONARY.put("ei", "এই");
        DICTIONARY.put("eta", "এটা");
        DICTIONARY.put("eti", "এটি");
        DICTIONARY.put("era", "এরা");
        DICTIONARY.put("eder", "এদের");
        DICTIONARY.put("oi", "ঐ");
        DICTIONARY.put("ota", "ওটা");
        DICTIONARY.put("oti", "ওটি");
        DICTIONARY.put("ekhane", "এখানে");
        DICTIONARY.put("okhane", "ওখানে");
        DICTIONARY.put("sekhane", "সেখানে");
        DICTIONARY.put("ethay", "এথায়");
        DICTIONARY.put("hoy", "হয়");
        DICTIONARY.put("hobe", "হবে");
        DICTIONARY.put("holo", "হলো");
        DICTIONARY.put("hoyeche", "হয়েছে");
        DICTIONARY.put("hoyechilo", "হয়েছিল");
        DICTIONARY.put("hocche", "হচ্ছে");
        DICTIONARY.put("hochilo", "হচ্ছিল");
        DICTIONARY.put("hoye", "হয়ে");
        DICTIONARY.put("ache", "আছে");
        DICTIONARY.put("achi", "আছি");
        DICTIONARY.put("acho", "আছো");
        DICTIONARY.put("achen", "আছেন");
        DICTIONARY.put("achis", "আছিস");
        DICTIONARY.put("chilo", "ছিল");
        DICTIONARY.put("chilam", "ছিলাম");
        DICTIONARY.put("chile", "ছিলে");
        DICTIONARY.put("chilen", "ছিলেন");
        DICTIONARY.put("chilis", "ছিলিস");
        DICTIONARY.put("thaki", "থাকি");
        DICTIONARY.put("thako", "থাকো");
        DICTIONARY.put("thake", "থাকে");
        DICTIONARY.put("thaken", "থাকেন");
        DICTIONARY.put("thakbo", "থাকবো");
        DICTIONARY.put("thakbe", "থাকবে");
        DICTIONARY.put("theke", "থেকে");
        DICTIONARY.put("jay", "যায়");
        DICTIONARY.put("jai", "যাই");
        DICTIONARY.put("jao", "যাও");
        DICTIONARY.put("jan", "যান");
        DICTIONARY.put("jas", "যাস");
        DICTIONARY.put("jabo", "যাবো");
        DICTIONARY.put("jabe", "যাবে");
        DICTIONARY.put("jaben", "যাবেন");
        DICTIONARY.put("jabi", "যাবি");
        DICTIONARY.put("jachchi", "যাচ্ছি");
        DICTIONARY.put("jachche", "যাচ্ছে");
        DICTIONARY.put("gelo", "গেল");
        DICTIONARY.put("gechi", "গেছি");
        DICTIONARY.put("geche", "গেছে");
        DICTIONARY.put("gechen", "গেছেন");
        DICTIONARY.put("giye", "গিয়ে");
        DICTIONARY.put("giyechi", "গিয়েছি");
        DICTIONARY.put("giyeche", "গিয়েছে");
        DICTIONARY.put("kori", "করি");
        DICTIONARY.put("koro", "করো");
        DICTIONARY.put("kore", "করে");
        DICTIONARY.put("koren", "করেন");
        DICTIONARY.put("koris", "করিস");
        DICTIONARY.put("korbo", "করবো");
        DICTIONARY.put("korbe", "করবে");
        DICTIONARY.put("korben", "করবেন");
        DICTIONARY.put("korbi", "করবি");
        DICTIONARY.put("korchi", "করছি");
        DICTIONARY.put("korche", "করছে");
        DICTIONARY.put("korchen", "করছেন");
        DICTIONARY.put("korlam", "করলাম");
        DICTIONARY.put("korlo", "করল");
        DICTIONARY.put("korle", "করলে");
        DICTIONARY.put("koreche", "করেছে");
        DICTIONARY.put("korechi", "করেছি");
        DICTIONARY.put("korechilo", "করেছিল");
        DICTIONARY.put("korte", "করতে");
        DICTIONARY.put("boli", "বলি");
        DICTIONARY.put("bolo", "বলো");
        DICTIONARY.put("bole", "বলে");
        DICTIONARY.put("bolen", "বলেন");
        DICTIONARY.put("bolbo", "বলবো");
        DICTIONARY.put("bolbe", "বলবে");
        DICTIONARY.put("bolben", "বলবেন");
        DICTIONARY.put("bolchi", "বলছি");
        DICTIONARY.put("bolche", "বলছে");
        DICTIONARY.put("bolechi", "বলেছি");
        DICTIONARY.put("boleche", "বলেছে");
        DICTIONARY.put("bolte", "বলতে");
        DICTIONARY.put("bollam", "বললাম");
        DICTIONARY.put("bollo", "বলল");
        DICTIONARY.put("dekhi", "দেখি");
        DICTIONARY.put("dekho", "দেখো");
        DICTIONARY.put("dekhe", "দেখে");
        DICTIONARY.put("dekhen", "দেখেন");
        DICTIONARY.put("dekhbo", "দেখবো");
        DICTIONARY.put("dekhbe", "দেখবে");
        DICTIONARY.put("dekhchi", "দেখছি");
        DICTIONARY.put("dekhche", "দেখছে");
        DICTIONARY.put("dekhechi", "দেখেছি");
        DICTIONARY.put("dekheche", "দেখেছে");
        DICTIONARY.put("dekhte", "দেখতে");
        DICTIONARY.put("dekha", "দেখা");
        DICTIONARY.put("khai", "খাই");
        DICTIONARY.put("khao", "খাও");
        DICTIONARY.put("khay", "খায়");
        DICTIONARY.put("khan", "খান");
        DICTIONARY.put("khabo", "খাবো");
        DICTIONARY.put("khabe", "খাবে");
        DICTIONARY.put("khachchi", "খাচ্ছি");
        DICTIONARY.put("khachche", "খাচ্ছে");
        DICTIONARY.put("khelam", "খেলাম");
        DICTIONARY.put("khelo", "খেল");
        DICTIONARY.put("kheyechi", "খেয়েছি");
        DICTIONARY.put("kheyeche", "খেয়েছে");
        DICTIONARY.put("khete", "খেতে");
        DICTIONARY.put("ashi", "আসি");
        DICTIONARY.put("asho", "আসো");
        DICTIONARY.put("ase", "আসে");
        DICTIONARY.put("asen", "আসেন");
        DICTIONARY.put("ashbo", "আসবো");
        DICTIONARY.put("ashbe", "আসবে");
        DICTIONARY.put("asben", "আসবেন");
        DICTIONARY.put("aschi", "আসছি");
        DICTIONARY.put("asche", "আসছে");
        DICTIONARY.put("eshe", "এসে");
        DICTIONARY.put("eshechi", "এসেছি");
        DICTIONARY.put("esheche", "এসেছে");
        DICTIONARY.put("ashte", "আসতে");
        DICTIONARY.put("dei", "দেই");
        DICTIONARY.put("dao", "দাও");
        DICTIONARY.put("dey", "দেয়");
        DICTIONARY.put("den", "দেন");
        DICTIONARY.put("debo", "দেবো");
        DICTIONARY.put("debe", "দেবে");
        DICTIONARY.put("diye", "দিয়ে");
        DICTIONARY.put("diyechi", "দিয়েছি");
        DICTIONARY.put("diyeche", "দিয়েছে");
        DICTIONARY.put("dilam", "দিলাম");
        DICTIONARY.put("dite", "দিতে");
        DICTIONARY.put("nei", "নেই");
        DICTIONARY.put("nao", "নাও");
        DICTIONARY.put("nen", "নেন");
        DICTIONARY.put("nebo", "নেবো");
        DICTIONARY.put("niye", "নিয়ে");
        DICTIONARY.put("niyechi", "নিয়েছি");
        DICTIONARY.put("niyeche", "নিয়েছে");
        DICTIONARY.put("nilam", "নিলাম");
        DICTIONARY.put("nite", "নিতে");
        DICTIONARY.put("pari", "পারি");
        DICTIONARY.put("paro", "পারো");
        DICTIONARY.put("pare", "পারে");
        DICTIONARY.put("paren", "পারেন");
        DICTIONARY.put("parbo", "পারবো");
        DICTIONARY.put("parbe", "পারবে");
        DICTIONARY.put("parlam", "পারলাম");
        DICTIONARY.put("parchi", "পারছি");
        DICTIONARY.put("parche", "পারছে");
        DICTIONARY.put("chai", "চাই");
        DICTIONARY.put("chao", "চাও");
        DICTIONARY.put("chan", "চান");
        DICTIONARY.put("cheyechi", "চেয়েছি");
        DICTIONARY.put("cheyeche", "চেয়েছে");
        DICTIONARY.put("pori", "পড়ি");
        DICTIONARY.put("poro", "পড়ো");
        DICTIONARY.put("poren", "পড়েন");
        DICTIONARY.put("porbo", "পড়বো");
        DICTIONARY.put("porbe", "পড়বে");
        DICTIONARY.put("porchi", "পড়ছি");
        DICTIONARY.put("porche", "পড়ছে");
        DICTIONARY.put("porechi", "পড়েছি");
        DICTIONARY.put("poreche", "পড়েছে");
        DICTIONARY.put("porte", "পড়তে");
        DICTIONARY.put("shuni", "শুনি");
        DICTIONARY.put("shono", "শোনো");
        DICTIONARY.put("shone", "শোনে");
        DICTIONARY.put("shonen", "শোনেন");
        DICTIONARY.put("shunbo", "শুনবো");
        DICTIONARY.put("shunbe", "শুনবে");
        DICTIONARY.put("shunchi", "শুনছি");
        DICTIONARY.put("shunche", "শুনছে");
        DICTIONARY.put("shunechi", "শুনেছি");
        DICTIONARY.put("shuneche", "শুনেছে");
        DICTIONARY.put("shune", "শুনে");
        DICTIONARY.put("bujhi", "বুঝি");
        DICTIONARY.put("bojho", "বোঝো");
        DICTIONARY.put("bojhe", "বোঝে");
        DICTIONARY.put("bujhechi", "বুঝেছি");
        DICTIONARY.put("bujheche", "বুঝেছে");
        DICTIONARY.put("bujhle", "বুঝলে");
        DICTIONARY.put("ke", "কে");
        DICTIONARY.put("ki", "কি");
        DICTIONARY.put("kee", "কী");
        DICTIONARY.put("kar", "কার");
        DICTIONARY.put("kake", "কাকে");
        DICTIONARY.put("kara", "কারা");
        DICTIONARY.put("kader", "কাদের");
        DICTIONARY.put("kothay", "কোথায়");
        DICTIONARY.put("kothao", "কোথাও");
        DICTIONARY.put("kemon", "কেমন");
        DICTIONARY.put("keno", "কেন");
        DICTIONARY.put("kobe", "কবে");
        DICTIONARY.put("koto", "কত");
        DICTIONARY.put("kotota", "কতটা");
        DICTIONARY.put("konta", "কোনটা");
        DICTIONARY.put("konti", "কোনটি");
        DICTIONARY.put("kon", "কোন");
        DICTIONARY.put("ekhon", "এখন");
        DICTIONARY.put("jokhon", "যখন");
        DICTIONARY.put("tokhon", "তখন");
        DICTIONARY.put("aaj", "আজ");
        DICTIONARY.put("ajke", "আজকে");
        DICTIONARY.put("kal", "কাল");
        DICTIONARY.put("kalke", "কালকে");
        DICTIONARY.put("poroshu", "পরশু");
        DICTIONARY.put("shokal", "সকাল");
        DICTIONARY.put("dupur", "দুপুর");
        DICTIONARY.put("bikel", "বিকেল");
        DICTIONARY.put("shondha", "সন্ধ্যা");
        DICTIONARY.put("raat", "রাত");
        DICTIONARY.put("raate", "রাতে");
        DICTIONARY.put("shokale", "সকালে");
        DICTIONARY.put("ekhuni", "এখুনি");
        DICTIONARY.put("age", "আগে");
        DICTIONARY.put("pore", "পরে");
        DICTIONARY.put("somoy", "সময়");
        DICTIONARY.put("ebong", "এবং");
        DICTIONARY.put("kintu", "কিন্তু");
        DICTIONARY.put("ar", "আর");
        DICTIONARY.put("ba", "বা");
        DICTIONARY.put("othoba", "অথবা");
        DICTIONARY.put("na", "না");
        DICTIONARY.put("hyan", "হ্যাঁ");
        DICTIONARY.put("ha", "হ্যাঁ");
        DICTIONARY.put("jodi", "যদি");
        DICTIONARY.put("tahole", "তাহলে");
        DICTIONARY.put("tobe", "তবে");
        DICTIONARY.put("karon", "কারণ");
        DICTIONARY.put("jeno", "যেন");
        DICTIONARY.put("jodio", "যদিও");
        DICTIONARY.put("to", "তো");
        DICTIONARY.put("hoyto", "হয়তো");
        DICTIONARY.put("noyto", "নয়তো");
        DICTIONARY.put("oho", "ওহো");
        DICTIONARY.put("aha", "আহা");
        DICTIONARY.put("mone", "মনে");
        DICTIONARY.put("jonno", "জন্য");
        DICTIONARY.put("jonye", "জন্যে");
        DICTIONARY.put("shathe", "সাথে");
        DICTIONARY.put("songe", "সঙ্গে");
        DICTIONARY.put("upor", "উপর");
        DICTIONARY.put("niche", "নিচে");
        DICTIONARY.put("bhitor", "ভিতর");
        DICTIONARY.put("baire", "বাইরে");
        DICTIONARY.put("majhe", "মাঝে");
        DICTIONARY.put("modhye", "মধ্যে");
        DICTIONARY.put("ma", "মা");
        DICTIONARY.put("baba", "বাবা");
        DICTIONARY.put("bhai", "ভাই");
        DICTIONARY.put("bon", "বোন");
        DICTIONARY.put("dada", "দাদা");
        DICTIONARY.put("didi", "দিদি");
        DICTIONARY.put("chele", "ছেলে");
        DICTIONARY.put("meye", "মেয়ে");
        DICTIONARY.put("baccha", "বাচ্চা");
        DICTIONARY.put("nana", "নানা");
        DICTIONARY.put("nani", "নানি");
        DICTIONARY.put("dadi", "দাদি");
        DICTIONARY.put("mama", "মামা");
        DICTIONARY.put("mami", "মামি");
        DICTIONARY.put("chacha", "চাচা");
        DICTIONARY.put("chachi", "চাচি");
        DICTIONARY.put("sonar", "সোনার");
        DICTIONARY.put("sona", "সোনা");
        DICTIONARY.put("lok", "লোক");
        DICTIONARY.put("lokjon", "লোকজন");
        DICTIONARY.put("chor", "চোর");
        DICTIONARY.put("bonus", "বোনাস");
        DICTIONARY.put("goyenda", "গোয়েন্দা");
        DICTIONARY.put("fon", "ফোন");
        DICTIONARY.put("bot", "বট");
        DICTIONARY.put("mon", "মন");
        DICTIONARY.put("ghor", "ঘর");
        DICTIONARY.put("bari", "বাড়ি");
        DICTIONARY.put("desh", "দেশ");
        DICTIONARY.put("bhasha", "ভাষা");
        DICTIONARY.put("shahor", "শহর");
        DICTIONARY.put("gram", "গ্রাম");
        DICTIONARY.put("rasta", "রাস্তা");
        DICTIONARY.put("gari", "গাড়ি");
        DICTIONARY.put("boi", "বই");
        DICTIONARY.put("kolom", "কলম");
        DICTIONARY.put("khata", "খাতা");
        DICTIONARY.put("kagoj", "কাগজ");
        DICTIONARY.put("jol", "জল");
        DICTIONARY.put("pani", "পানি");
        DICTIONARY.put("bhat", "ভাত");
        DICTIONARY.put("ruti", "রুটি");
        DICTIONARY.put("dal", "ডাল");
        DICTIONARY.put("machh", "মাছ");
        DICTIONARY.put("mangsho", "মাংস");
        DICTIONARY.put("doodh", "দুধ");
        DICTIONARY.put("cha", "চা");
        DICTIONARY.put("cini", "চিনি");
        DICTIONARY.put("lobon", "লবণ");
        DICTIONARY.put("tel", "তেল");
        DICTIONARY.put("gach", "গাছ");
        DICTIONARY.put("ful", "ফুল");
        DICTIONARY.put("pata", "পাতা");
        DICTIONARY.put("nodi", "নদী");
        DICTIONARY.put("sagor", "সাগর");
        DICTIONARY.put("pahar", "পাহাড়");
        DICTIONARY.put("akash", "আকাশ");
        DICTIONARY.put("surjo", "সূর্য");
        DICTIONARY.put("chand", "চাঁদ");
        DICTIONARY.put("nokkhotro", "নক্ষত্র");
        DICTIONARY.put("megh", "মেঘ");
        DICTIONARY.put("brishti", "বৃষ্টি");
        DICTIONARY.put("haowa", "হাওয়া");
        DICTIONARY.put("batas", "বাতাস");
        DICTIONARY.put("agun", "আগুন");
        DICTIONARY.put("mati", "মাটি");
        DICTIONARY.put("poth", "পথ");
        DICTIONARY.put("por", "পর");
        DICTIONARY.put("jor", "জ্বর");
        DICTIONARY.put("bol", "বল");
        DICTIONARY.put("phol", "ফল");
        DICTIONARY.put("jhol", "ঝোল");
        DICTIONARY.put("chokh", "চোখ");
        DICTIONARY.put("mukh", "মুখ");
        DICTIONARY.put("kan", "কান");
        DICTIONARY.put("nak", "নাক");
        DICTIONARY.put("hath", "হাত");
        DICTIONARY.put("pa", "পা");
        DICTIONARY.put("math", "মাঠ");
        DICTIONARY.put("matha", "মাথা");
        DICTIONARY.put("chul", "চুল");
        DICTIONARY.put("pet", "পেট");
        DICTIONARY.put("rokto", "রক্ত");
        DICTIONARY.put("pran", "প্রাণ");
        DICTIONARY.put("hridoy", "হৃদয়");
        DICTIONARY.put("bhalo", "ভালো");
        DICTIONARY.put("kharap", "খারাপ");
        DICTIONARY.put("shundor", "সুন্দর");
        DICTIONARY.put("boro", "বড়");
        DICTIONARY.put("choto", "ছোট");
        DICTIONARY.put("lomba", "লম্বা");
        DICTIONARY.put("khato", "খাটো");
        DICTIONARY.put("thanda", "ঠান্ডা");
        DICTIONARY.put("gorom", "গরম");
        DICTIONARY.put("notun", "নতুন");
        DICTIONARY.put("puran", "পুরান");
        DICTIONARY.put("purono", "পুরনো");
        DICTIONARY.put("mishti", "মিষ্টি");
        DICTIONARY.put("tito", "তিতো");
        DICTIONARY.put("shoja", "সোজা");
        DICTIONARY.put("shokto", "শক্ত");
        DICTIONARY.put("norom", "নরম");
        DICTIONARY.put("ucca", "উচ্চ");
        DICTIONARY.put("nichu", "নিচু");
        DICTIONARY.put("shada", "সাদা");
        DICTIONARY.put("kalo", "কালো");
        DICTIONARY.put("lal", "লাল");
        DICTIONARY.put("nil", "নীল");
        DICTIONARY.put("sobuj", "সবুজ");
        DICTIONARY.put("holud", "হলুদ");
        DICTIONARY.put("ek", "এক");
        DICTIONARY.put("dui", "দুই");
        DICTIONARY.put("tin", "তিন");
        DICTIONARY.put("char", "চার");
        DICTIONARY.put("panch", "পাঁচ");
        DICTIONARY.put("choy", "ছয়");
        DICTIONARY.put("saat", "সাত");
        DICTIONARY.put("aat", "আট");
        DICTIONARY.put("noy", "নয়");
        DICTIONARY.put("dosh", "দশ");
        DICTIONARY.put("egaro", "এগারো");
        DICTIONARY.put("baro", "বারো");
        DICTIONARY.put("tero", "তেরো");
        DICTIONARY.put("choddo", "চৌদ্দ");
        DICTIONARY.put("ponero", "পনেরো");
        DICTIONARY.put("solo", "ষোলো");
        DICTIONARY.put("sotero", "সতেরো");
        DICTIONARY.put("ataro", "আঠারো");
        DICTIONARY.put("unish", "উনিশ");
        DICTIONARY.put("bish", "বিশ");
        DICTIONARY.put("prem", "প্রেম");
        DICTIONARY.put("bhalobasha", "ভালোবাসা");
        DICTIONARY.put("ghrina", "ঘৃণা");
        DICTIONARY.put("rag", "রাগ");
        DICTIONARY.put("dukkho", "দুঃখ");
        DICTIONARY.put("sukh", "সুখ");
        DICTIONARY.put("anondo", "আনন্দ");
        DICTIONARY.put("hashi", "হাসি");
        DICTIONARY.put("kanna", "কান্না");
        DICTIONARY.put("bhoy", "ভয়");
        DICTIONARY.put("asha", "আশা");
        DICTIONARY.put("shopno", "স্বপ্ন");
        DICTIONARY.put("shanti", "শান্তি");
        DICTIONARY.put("dhonnobad", "ধন্যবাদ");
        DICTIONARY.put("shagotom", "স্বাগতম");
        DICTIONARY.put("namaskar", "নমস্কার");
        DICTIONARY.put("assalamualaikum", "আসসালামু আলাইকুম");
        DICTIONARY.put("bangla", "বাংলা");
        DICTIONARY.put("bondhu", "বন্ধু");
        DICTIONARY.put("manush", "মানুষ");
        DICTIONARY.put("bharat", "ভারত");
        DICTIONARY.put("bangladesh", "বাংলাদেশ");
        DICTIONARY.put("bhor", "ভোর");
        DICTIONARY.put("bhore", "ভোরে");
        DICTIONARY.put("bhorer", "ভোরের");
        DICTIONARY.put("shokalbela", "সকালবেলা");
        DICTIONARY.put("alo", "আলো");
        DICTIONARY.put("alor", "আলোর");
        DICTIONARY.put("aloy", "আলোয়");
        DICTIONARY.put("adhar", "আঁধার");
        DICTIONARY.put("prithibi", "পৃথিবী");
        DICTIONARY.put("prithibir", "পৃথিবীর");
        DICTIONARY.put("prokriti", "প্রকৃতি");
        DICTIONARY.put("shishir", "শিশির");
        DICTIONARY.put("bindu", "বিন্দু");
        DICTIONARY.put("pakhi", "পাখি");
        DICTIONARY.put("pakhir", "পাখির");
        DICTIONARY.put("shobuj", "সবুজ");
        DICTIONARY.put("dak", "ডাক");
        DICTIONARY.put("daak", "ডাক");
        DICTIONARY.put("othe", "ওঠে");
        DICTIONARY.put("uthe", "উঠে");
        DICTIONARY.put("uthi", "উঠি");
        DICTIONARY.put("jege", "জেগে");
        DICTIONARY.put("chheye", "ছেয়ে");
        DICTIONARY.put("kotha", "কথা");
        DICTIONARY.put("shuru", "শুরু");
        DICTIONARY.put("shesh", "শেষ");
        DICTIONARY.put("jibon", "জীবন");
        DICTIONARY.put("shomoy", "সময়");
        DICTIONARY.put("onnorokom", "অন্যরকম");
        DICTIONARY.put("shomvob", "সম্ভব");
        DICTIONARY.put("sombhob", "সম্ভব");
        DICTIONARY.put("oshomvob", "অসম্ভব");
        DICTIONARY.put("jobe", "যবে");
        DICTIONARY.put("ekdin", "একদিন");
        DICTIONARY.put("ekbar", "একবার");
        DICTIONARY.put("ekjon", "একজন");
        DICTIONARY.put("ekta", "একটা");
        DICTIONARY.put("ekti", "একটি");
        DICTIONARY.put("ektu", "একটু");
        DICTIONARY.put("ekdom", "একদম");
        DICTIONARY.put("eksathe", "একসাথে");
        DICTIONARY.put("eksonge", "একসঙ্গে");
        DICTIONARY.put("ekebare", "একেবারে");
        DICTIONARY.put("protidin", "প্রতিদিন");
        DICTIONARY.put("protibar", "প্রতিবার");
        DICTIONARY.put("duijon", "দুইজন");
        DICTIONARY.put("tinjon", "তিনজন");
        DICTIONARY.put("onek", "অনেক");
        DICTIONARY.put("onekta", "অনেকটা");
        DICTIONARY.put("onno", "অন্য");
        DICTIONARY.put("ortho", "অর্থ");
        DICTIONARY.put("olpo", "অল্প");
        DICTIONARY.put("ongsho", "অংশ");
        DICTIONARY.put("otit", "অতীত");
        DICTIONARY.put("odhik", "অধিক");
        DICTIONARY.put("odhikar", "অধিকার");
        DICTIONARY.put("obostha", "অবস্থা");
        DICTIONARY.put("oporadh", "অপরাধ");
        DICTIONARY.put("onurodh", "অনুরোধ");
        DICTIONARY.put("onumoti", "অনুমতি");
        DICTIONARY.put("onuvuti", "অনুভূতি");
        DICTIONARY.put("ovinoy", "অভিনয়");
        DICTIONARY.put("ovab", "অভাব");
        DICTIONARY.put("oshukh", "অসুখ");
        DICTIONARY.put("thik", "ঠিক");
        DICTIONARY.put("ekhono", "এখনো");
        DICTIONARY.put("kokhono", "কখনো");
        DICTIONARY.put("kichu", "কিছু");
        DICTIONARY.put("kichui", "কিছুই");
        DICTIONARY.put("shob", "সব");
        DICTIONARY.put("sob", "সব");
        DICTIONARY.put("shobai", "সবাই");
        DICTIONARY.put("shobkichu", "সবকিছু");
        DICTIONARY.put("sotti", "সত্যি");
        DICTIONARY.put("shotti", "সত্যি");
        DICTIONARY.put("mittha", "মিথ্যা");
        DICTIONARY.put("ichcha", "ইচ্ছা");
        DICTIONARY.put("chesta", "চেষ্টা");
        DICTIONARY.put("jinish", "জিনিস");
        DICTIONARY.put("bishoy", "বিষয়");
        DICTIONARY.put("shomossa", "সমস্যা");
        DICTIONARY.put("somossa", "সমস্যা");
        DICTIONARY.put("shorkar", "সরকার");
        DICTIONARY.put("sorkar", "সরকার");
        DICTIONARY.put("shadharon", "সাধারণ");
        DICTIONARY.put("shahajjo", "সাহায্য");
        DICTIONARY.put("sahajjo", "সাহায্য");
        DICTIONARY.put("shomporko", "সম্পর্ক");
        DICTIONARY.put("jonogon", "জনগণ");
        DICTIONARY.put("tablet", "ট্যাবলেট");
        DICTIONARY.put("tab", "ট্যাব");
        DICTIONARY.put("capsule", "ক্যাপসুল");
        DICTIONARY.put("cap", "ক্যাপ");
        DICTIONARY.put("syrup", "সিরাপ");
        DICTIONARY.put("injection", "ইনজেকশন");
        DICTIONARY.put("inhaler", "ইনহেলার");
        DICTIONARY.put("drop", "ড্রপ");
        DICTIONARY.put("fota", "ফোঁটা");
        DICTIONARY.put("chamoch", "চামচ");
        DICTIONARY.put("matra", "মাত্রা");
        DICTIONARY.put("dose", "ডোজ");
        DICTIONARY.put("miligram", "মিলিগ্রাম");
        DICTIONARY.put("mili", "মিলি");
        DICTIONARY.put("unit", "ইউনিট");
        DICTIONARY.put("adha", "আধা");
        DICTIONARY.put("puro", "পুরো");
        DICTIONARY.put("gota", "গোটা");
        DICTIONARY.put("duto", "দুটো");
        DICTIONARY.put("bar", "বার");
        DICTIONARY.put("din", "দিন");
        DICTIONARY.put("shoptaho", "সপ্তাহ");
        DICTIONARY.put("shoptahe", "সপ্তাহে");
        DICTIONARY.put("mash", "মাস");
        DICTIONARY.put("mashe", "মাসে");
        DICTIONARY.put("bochor", "বছর");
        DICTIONARY.put("ghonta", "ঘণ্টা");
        DICTIONARY.put("ghontay", "ঘণ্টায়");
        DICTIONARY.put("proti", "প্রতি");
        DICTIONARY.put("porpor", "পরপর");
        DICTIONARY.put("ektana", "একটানা");
        DICTIONARY.put("niyomito", "নিয়মিত");
        DICTIONARY.put("khabar", "খাবার");
        DICTIONARY.put("khabarer", "খাবারের");
        DICTIONARY.put("khaowar", "খাওয়ার");
        DICTIONARY.put("khali", "খালি");
        DICTIONARY.put("khalipete", "খালিপেটে");
        DICTIONARY.put("pete", "পেটে");
        DICTIONARY.put("bhora", "ভরা");
        DICTIONARY.put("khaben", "খাবেন");
        DICTIONARY.put("sheban", "সেবন");
        DICTIONARY.put("gile", "গিলে");
        DICTIONARY.put("chibiye", "চিবিয়ে");
        DICTIONARY.put("lagaben", "লাগাবেন");
        DICTIONARY.put("lagano", "লাগানো");
        DICTIONARY.put("byabohar", "ব্যবহার");
        DICTIONARY.put("ghum", "ঘুম");
        DICTIONARY.put("ghumanor", "ঘুমানোর");
        DICTIONARY.put("dupure", "দুপুরে");
        DICTIONARY.put("bikele", "বিকেলে");
        DICTIONARY.put("rate", "রাতে");
        DICTIONARY.put("bishram", "বিশ্রাম");
        DICTIONARY.put("followup", "ফলোআপ");
        DICTIONARY.put("porborti", "পরবর্তী");
        DICTIONARY.put("porbortite", "পরবর্তীতে");
        DICTIONARY.put("proyojon", "প্রয়োজন");
        DICTIONARY.put("proyojone", "প্রয়োজনে");
        DICTIONARY.put("dorkar", "দরকার");
        DICTIONARY.put("bondho", "বন্ধ");
        DICTIONARY.put("chaliye", "চালিয়ে");
        DICTIONARY.put("cholbe", "চলবে");
        DICTIONARY.put("report", "রিপোর্ট");
        DICTIONARY.put("test", "টেস্ট");
        DICTIONARY.put("porikkha", "পরীক্ষা");
        DICTIONARY.put("doctor", "ডাক্তার");
        DICTIONARY.put("daktar", "ডাক্তার");
        DICTIONARY.put("rogi", "রোগী");
        DICTIONARY.put("rog", "রোগ");
        DICTIONARY.put("oshudh", "ওষুধ");
        DICTIONARY.put("oushadh", "ঔষধ");
        DICTIONARY.put("hashpatal", "হাসপাতাল");
        DICTIONARY.put("chikitsa", "চিকিৎসা");
        DICTIONARY.put("shustho", "সুস্থ");
        DICTIONARY.put("jwor", "জ্বর");
        DICTIONARY.put("byatha", "ব্যথা");
        DICTIONARY.put("kashi", "কাশি");
        DICTIONARY.put("shordi", "সর্দি");
        DICTIONARY.put("bomi", "বমি");
        DICTIONARY.put("mathabyatha", "মাথাব্যথা");
        DICTIONARY.put("durbol", "দুর্বল");
        DICTIONARY.put("durbolota", "দুর্বলতা");
        DICTIONARY.put("gas", "গ্যাস");
        DICTIONARY.put("allergy", "অ্যালার্জি");
        DICTIONARY.put("accha", "আচ্ছা");
        DICTIONARY.put("achcha", "আচ্ছা");
        DICTIONARY.put("are", "আরে");
        DICTIONARY.put("bah", "বাহ");
        DICTIONARY.put("ji", "জি");
        DICTIONARY.put("shuvo", "শুভ");
        DICTIONARY.put("shubho", "শুভ");
        DICTIONARY.put("obhinondon", "অভিনন্দন");
        DICTIONARY.put("dukkhito", "দুঃখিত");
        DICTIONARY.put("maf", "মাফ");
        DICTIONARY.put("doya", "দয়া");
        DICTIONARY.put("noboborsho", "নববর্ষ");
        DICTIONARY.put("nobborsho", "নববর্ষ");
        DICTIONARY.put("borsho", "বর্ষ");
        DICTIONARY.put("borso", "বর্ষ");
        DICTIONARY.put("borsha", "বর্ষা");
        DICTIONARY.put("lekha", "লেখা");
        DICTIONARY.put("likhi", "লিখি");
        DICTIONARY.put("likhe", "লিখে");
        DICTIONARY.put("likhbo", "লিখবো");
        DICTIONARY.put("kena", "কেনা");
        DICTIONARY.put("kini", "কিনি");
        DICTIONARY.put("kine", "কিনে");
        DICTIONARY.put("kinbo", "কিনবো");
        DICTIONARY.put("bosha", "বসা");
        DICTIONARY.put("boshi", "বসি");
        DICTIONARY.put("bose", "বসে");
        DICTIONARY.put("boshbo", "বসবো");
        DICTIONARY.put("rakha", "রাখা");
        DICTIONARY.put("rakhi", "রাখি");
        DICTIONARY.put("rakhe", "রাখে");
        DICTIONARY.put("rakho", "রাখো");
        DICTIONARY.put("chola", "চলা");
        DICTIONARY.put("choli", "চলি");
        DICTIONARY.put("chole", "চলে");
        DICTIONARY.put("khola", "খোলা");
        DICTIONARY.put("ana", "আনা");
        DICTIONARY.put("ane", "আনে");
        DICTIONARY.put("taka", "টাকা");
        DICTIONARY.put("poysa", "পয়সা");
        DICTIONARY.put("dokan", "দোকান");
        DICTIONARY.put("bajar", "বাজার");
        DICTIONARY.put("school", "স্কুল");
        DICTIONARY.put("iskul", "স্কুল");
        DICTIONARY.put("office", "অফিস");
        DICTIONARY.put("college", "কলেজ");
        DICTIONARY.put("mobile", "মোবাইল");
        DICTIONARY.put("computer", "কম্পিউটার");
        DICTIONARY.put("internet", "ইন্টারনেট");
        DICTIONARY.put("shorir", "শরীর");
        DICTIONARY.put("kaj", "কাজ");
        DICTIONARY.put("khela", "খেলা");
        DICTIONARY.put("gan", "গান");
        DICTIONARY.put("golpo", "গল্প");
        DICTIONARY.put("khobor", "খবর");
        DICTIONARY.put("chithi", "চিঠি");
        DICTIONARY.put("chhuti", "ছুটি");
        DICTIONARY.put("jonmodin", "জন্মদিন");
        DICTIONARY.put("moja", "মজা");
        DICTIONARY.put("khub", "খুব");
        DICTIONARY.put("beshi", "বেশি");
        DICTIONARY.put("kom", "কম");
        DICTIONARY.put("shudhu", "শুধু");
        DICTIONARY.put("matro", "মাত্র");
        DICTIONARY.put("abar", "আবার");
        DICTIONARY.put("aro", "আরও");
        DICTIONARY.put("prai", "প্রায়");
        DICTIONARY.put("pray", "প্রায়");
        DICTIONARY.put("joldi", "জলদি");
        DICTIONARY.put("taratari", "তাড়াতাড়ি");
        DICTIONARY.put("aste", "আস্তে");
        DICTIONARY.put("obosshoi", "অবশ্যই");
        DICTIONARY.put("shonar", "সোনার");

        PATTERNS = new PatternEntry[] {
            new PatternEntry("bortomane", "বর্তমানে", null),
            new PatternEntry("bortoman", "বর্তমান", null),
            new PatternEntry("kkhoma", "ক্ষমা", null),
            new PatternEntry("shikkha", "শিক্ষা", null),
            new PatternEntry("ggan", "জ্ঞান", null),
            new PatternEntry("cch", "চ্ছ", null),
            new PatternEntry("sw", "স্ব", null),
            new PatternEntry("kkha", "ক্খা", null),
            new PatternEntry("kSha", "ক্ষা", null),
            new PatternEntry("kShi", "ক্ষি", null),
            new PatternEntry("kShu", "ক্ষু", null),
            new PatternEntry("kShe", "ক্ষে", null),
            new PatternEntry("kSho", "ক্ষো", null),
            new PatternEntry("rrai", "ঋ", null),
            new PatternEntry("rrhi", "ঋ", null),
            new PatternEntry("krri", "কৃ", null),
            new PatternEntry("grri", "গৃ", null),
            new PatternEntry("trri", "তৃ", null),
            new PatternEntry("drri", "দৃ", null),
            new PatternEntry("nrri", "নৃ", null),
            new PatternEntry("prri", "পৃ", null),
            new PatternEntry("brri", "বৃ", null),
            new PatternEntry("mrri", "মৃ", null),
            new PatternEntry("hrri", "হৃ", null),
            new PatternEntry("lrri", "লৃ", null),
            new PatternEntry("zrri", "যৃ", null),
            new PatternEntry("srri", "সৃ", null),
            new PatternEntry("rri", "ঋ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ৃ"),
            }),
            new PatternEntry("oou", "ঊ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ূ"),
            }),
            new PatternEntry("kSh", "ক্ষ", null),
            new PatternEntry("ksh", "ক্ষ", null),
            new PatternEntry("chh", "ছ", null),
            new PatternEntry("GNG", "জ্ঞ", null),
            new PatternEntry("jNG", "জ্ঞ", null),
            new PatternEntry("bhl", "ভ্ল", null),
            new PatternEntry("phl", "ফ্ল", null),
            new PatternEntry("shr", "শ্র", null),
            new PatternEntry("skr", "স্ক্র", null),
            new PatternEntry("spr", "স্প্র", null),
            new PatternEntry("str", "স্ত্র", null),
            new PatternEntry("sth", "স্থ", null),
            new PatternEntry("skl", "স্ক্ল", null),
            new PatternEntry("spl", "স্প্ল", null),
            new PatternEntry("Shr", "শ্র", null),
            new PatternEntry("Ngr", "ঙ্র", null),
            new PatternEntry("ndr", "ন্দ্র", null),
            new PatternEntry("ntr", "ন্ত্র", null),
            new PatternEntry("mpr", "ম্প্র", null),
            new PatternEntry("thr", "থ্র", null),
            new PatternEntry("dhr", "ধ্র", null),
            new PatternEntry("khr", "খ্র", null),
            new PatternEntry("ghr", "ঘ্র", null),
            new PatternEntry("bhr", "ভ্র", null),
            new PatternEntry("phr", "ফ্র", null),
            new PatternEntry("mhr", "ম্র", null),
            new PatternEntry("lhr", "ল্র", null),
            new PatternEntry("Thr", "ঠ্র", null),
            new PatternEntry("Dhr", "ঢ্র", null),
            new PatternEntry("NGr", "ঞ্র", null),
            new PatternEntry("ngh", "ন্ঘ", null),
            new PatternEntry("nkh", "ন্খ", null),
            new PatternEntry("nth", "ন্থ", null),
            new PatternEntry("ndh", "ন্ধ", null),
            new PatternEntry("nch", "ন্চ", null),
            new PatternEntry("njh", "ন্ঝ", null),
            new PatternEntry("nsh", "ন্শ", null),
            new PatternEntry("mth", "ম্থ", null),
            new PatternEntry("mtr", "ম্ত্র", null),
            new PatternEntry("mbh", "ম্ভ", null),
            new PatternEntry("mph", "ম্ফ", null),
            new PatternEntry("aa", "আ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "া"),
            }),
            new PatternEntry("ii", "ঈ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ী"),
            }),
            new PatternEntry("ee", "ঈ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ী"),
            }),
            new PatternEntry("uu", "ঊ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ূ"),
            }),
            new PatternEntry("oo", "ঊ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ূ"),
            }),
            new PatternEntry("oi", "ঐ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ৈ"),
            }),
            new PatternEntry("ou", "ঔ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ৌ"),
            }),
            new PatternEntry("OI", "ঐ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ৈ"),
            }),
            new PatternEntry("OU", "ঔ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ৌ"),
            }),
            new PatternEntry("kh", "খ", null),
            new PatternEntry("gh", "ঘ", null),
            new PatternEntry("Ng", "ঙ", null),
            new PatternEntry("ch", "চ", null),
            new PatternEntry("Ch", "ছ", null),
            new PatternEntry("jh", "ঝ", null),
            new PatternEntry("NG", "ঞ", null),
            new PatternEntry("Th", "ঠ", null),
            new PatternEntry("Dh", "ঢ", null),
            new PatternEntry("th", "থ", null),
            new PatternEntry("dh", "ধ", null),
            new PatternEntry("ph", "ফ", null),
            new PatternEntry("bh", "ভ", null),
            new PatternEntry("sh", "শ", null),
            new PatternEntry("Sh", "ষ", null),
            new PatternEntry("Rh", "ঢ়", null),
            new PatternEntry("kt", "ক্ত", null),
            new PatternEntry("kk", "ক্ক", null),
            new PatternEntry("kn", "ক্ন", null),
            new PatternEntry("km", "ক্ম", null),
            new PatternEntry("kl", "ক্ল", null),
            new PatternEntry("kr", "ক্র", null),
            new PatternEntry("ks", "ক্স", null),
            new PatternEntry("gn", "গ্ন", null),
            new PatternEntry("gm", "গ্ম", null),
            new PatternEntry("gl", "গ্ল", null),
            new PatternEntry("gr", "গ্র", null),
            new PatternEntry("gg", "গ্গ", null),
            new PatternEntry("gd", "গ্দ", null),
            new PatternEntry("gt", "গ্ত", null),
            new PatternEntry("gj", "গ্জ", null),
            new PatternEntry("jj", "জ্জ", null),
            new PatternEntry("jn", "জ্ন", null),
            new PatternEntry("jm", "জ্ম", null),
            new PatternEntry("jl", "জ্ল", null),
            new PatternEntry("jr", "জ্র", null),
            new PatternEntry("jb", "জ্ব", null),
            new PatternEntry("nk", "ন্ক", null),
            new PatternEntry("ng", "ং", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(false, "vowel", null, false),
                }, "ঙ্গ"),
            }),
            new PatternEntry("nn", "ন্ন", null),
            new PatternEntry("nm", "ন্ম", null),
            new PatternEntry("nl", "ন্ল", null),
            new PatternEntry("nr", "ন্র", null),
            new PatternEntry("nd", "ন্দ", null),
            new PatternEntry("nt", "ন্ত", null),
            new PatternEntry("np", "ন্প", null),
            new PatternEntry("nb", "ন্ব", null),
            new PatternEntry("nc", "ন্চ", null),
            new PatternEntry("nj", "ন্জ", null),
            new PatternEntry("ns", "ন্স", null),
            new PatternEntry("pt", "প্ত", null),
            new PatternEntry("pk", "প্ক", null),
            new PatternEntry("pp", "প্প", null),
            new PatternEntry("pn", "প্ন", null),
            new PatternEntry("pm", "প্ম", null),
            new PatternEntry("pl", "প্ল", null),
            new PatternEntry("pr", "প্র", null),
            new PatternEntry("ps", "প্স", null),
            new PatternEntry("bt", "ব্ত", null),
            new PatternEntry("bk", "ব্ক", null),
            new PatternEntry("bb", "ব্ব", null),
            new PatternEntry("bn", "ব্ন", null),
            new PatternEntry("bm", "ব্ম", null),
            new PatternEntry("bl", "ব্ল", null),
            new PatternEntry("br", "ব্র", null),
            new PatternEntry("bd", "ব্দ", null),
            new PatternEntry("bj", "ব্জ", null),
            new PatternEntry("mk", "ম্ক", null),
            new PatternEntry("mg", "ম্গ", null),
            new PatternEntry("mm", "ম্ম", null),
            new PatternEntry("mn", "ম্ন", null),
            new PatternEntry("ml", "ম্ল", null),
            new PatternEntry("mr", "ম্র", null),
            new PatternEntry("mb", "ম্ব", null),
            new PatternEntry("ms", "ম্স", null),
            new PatternEntry("mp", "ম্প", null),
            new PatternEntry("mt", "ম্ত", null),
            new PatternEntry("md", "ম্দ", null),
            new PatternEntry("lk", "ল্ক", null),
            new PatternEntry("lg", "ল্গ", null),
            new PatternEntry("ll", "ল্ল", null),
            new PatternEntry("ln", "ল্ন", null),
            new PatternEntry("lm", "ল্ম", null),
            new PatternEntry("lp", "ল্প", null),
            new PatternEntry("lb", "ল্ব", null),
            new PatternEntry("ld", "ল্দ", null),
            new PatternEntry("lt", "ল্ত", null),
            new PatternEntry("ls", "ল্স", null),
            new PatternEntry("lr", "ল্র", null),
            new PatternEntry("rk", "র্ক", null),
            new PatternEntry("rg", "র্গ", null),
            new PatternEntry("rn", "র্ন", null),
            new PatternEntry("rm", "র্ম", null),
            new PatternEntry("rl", "র্ল", null),
            new PatternEntry("rr", "ড়", null),
            new PatternEntry("rb", "র্ব", null),
            new PatternEntry("rd", "র্দ", null),
            new PatternEntry("rt", "র্ত", null),
            new PatternEntry("rs", "র্স", null),
            new PatternEntry("rp", "র্প", null),
            new PatternEntry("sk", "স্ক", null),
            new PatternEntry("sg", "স্গ", null),
            new PatternEntry("sn", "স্ন", null),
            new PatternEntry("sm", "স্ম", null),
            new PatternEntry("sl", "স্ল", null),
            new PatternEntry("sb", "স্ব", null),
            new PatternEntry("sd", "স্দ", null),
            new PatternEntry("st", "স্ত", null),
            new PatternEntry("sp", "স্প", null),
            new PatternEntry("ss", "স্স", null),
            new PatternEntry("sr", "স্র", null),
            new PatternEntry("tk", "ত্ক", null),
            new PatternEntry("tg", "ত্গ", null),
            new PatternEntry("tn", "ত্ন", null),
            new PatternEntry("tm", "ত্ম", null),
            new PatternEntry("tl", "ত্ল", null),
            new PatternEntry("tb", "ত্ব", null),
            new PatternEntry("td", "ত্দ", null),
            new PatternEntry("tt", "ত্ত", null),
            new PatternEntry("tp", "ত্প", null),
            new PatternEntry("tr", "ত্র", null),
            new PatternEntry("ts", "ত্স", null),
            new PatternEntry("dk", "দ্ক", null),
            new PatternEntry("dg", "দ্গ", null),
            new PatternEntry("dn", "দ্ন", null),
            new PatternEntry("dm", "দ্ম", null),
            new PatternEntry("dl", "দ্ল", null),
            new PatternEntry("db", "দ্ব", null),
            new PatternEntry("dd", "দ্দ", null),
            new PatternEntry("dp", "দ্প", null),
            new PatternEntry("dr", "দ্র", null),
            new PatternEntry("ds", "দ্স", null),
            new PatternEntry("dt", "দ্ত", null),
            new PatternEntry("^^", "্", null),
            new PatternEntry(",,", "ঁ", null),
            new PatternEntry("a", "আ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "া"),
            }),
            new PatternEntry("i", "ই", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ি"),
            }),
            new PatternEntry("u", "উ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ু"),
            }),
            new PatternEntry("e", "এ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ে"),
            }),
            new PatternEntry("o", "ও", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "‌"),
            }),
            new PatternEntry("A", "আ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "া"),
            }),
            new PatternEntry("I", "ঈ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ী"),
            }),
            new PatternEntry("U", "ঊ", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "consonant", null, false),
                }, "ূ"),
            }),
            new PatternEntry("E", "এ", null),
            new PatternEntry("O", "ও", null),
            new PatternEntry("k", "ক", null),
            new PatternEntry("g", "গ", null),
            new PatternEntry("j", "জ", null),
            new PatternEntry("T", "ট", null),
            new PatternEntry("D", "ড", null),
            new PatternEntry("N", "ণ", null),
            new PatternEntry("t", "ত", null),
            new PatternEntry("d", "দ", null),
            new PatternEntry("n", "ন", null),
            new PatternEntry("p", "প", null),
            new PatternEntry("b", "ব", null),
            new PatternEntry("m", "ম", null),
            new PatternEntry("z", "য", null),
            new PatternEntry("r", "র", null),
            new PatternEntry("l", "ল", null),
            new PatternEntry("s", "স", null),
            new PatternEntry("h", "হ", null),
            new PatternEntry("R", "ড়", null),
            new PatternEntry("y", "য়", null),
            new PatternEntry("S", "শ", null),
            new PatternEntry("f", "ফ", null),
            new PatternEntry("v", "ভ", null),
            new PatternEntry("q", "ক", null),
            new PatternEntry("w", "ও", null),
            new PatternEntry("x", "ক্স", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(true, "punctuation", null, false),
                }, "এক্স"),
            }),
            new PatternEntry("c", "স", null),
            new PatternEntry("^", "ঁ", null),
            new PatternEntry(":", "ঃ", null),
            new PatternEntry("0", "০", null),
            new PatternEntry("1", "১", null),
            new PatternEntry("2", "২", null),
            new PatternEntry("3", "৩", null),
            new PatternEntry("4", "৪", null),
            new PatternEntry("5", "৫", null),
            new PatternEntry("6", "৬", null),
            new PatternEntry("7", "৭", null),
            new PatternEntry("8", "৮", null),
            new PatternEntry("9", "৯", null),
            new PatternEntry(".", "।", new PatternRule[] {
                new PatternRule(new MatchCondition[] {
                    new MatchCondition(false, "exact", ".", false),
                }, "."),
            }),
        };
    }
}
