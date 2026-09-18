package juloo.keyboard2.ai;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AiActionEngine
{
  public enum Category
  {
    GRAMMAR("🔍 Grammar", "Fix spelling, punctuation & grammar"),
    TRANSLATE("🔄 Translate", "Translate naturally into any language"),
    REWRITE("✍️ Rewrite", "Rephrase, polish, or change writing style"),
    ISLAMIC("🕌 Islamic", "Islamic tone, blessings, Salam & authentic Arabic"),
    CONTENT("📰 Content", "Titles, descriptions, hooks, captions & outlines"),
    REPLY("💬 Reply", "Generate contextual chat & comment replies"),
    EMAIL("📧 Email", "Generate professional & formal emails"),
    SOCIAL("📱 Social", "Create posts for Facebook, LinkedIn, X, Insta"),
    SUMMARIZE("📝 Summarize", "Summarize into bullets, key points or short text"),
    EXPLAIN("💡 Explain", "Explain concepts, ideas or programming code"),
    NATURALIZE("✨ Naturalize", "Humanize AI text to sound authentic"),
    TONE("🎭 Tone", "Adjust tone to professional, casual, confident..."),
    EMOTION("❤️ Emotion", "Add heartfelt emotion to your message"),
    HUMOR("😂 Humor", "Add funny, witty, or sarcastic humor"),
    LENGTH("📏 Length", "Shorten or expand your writing"),
    ASK_AI("🎯 Ask AI", "Type any custom instruction for AI");

    private final String title;
    private final String subtitle;

    Category(String title, String subtitle)
    {
      this.title = title;
      this.subtitle = subtitle;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
  }

  public static class ActionOption
  {
    public final String id;
    public final String label;
    public final String description;

    public ActionOption(String id, String label, String description)
    {
      this.id = id;
      this.label = label;
      this.description = description;
    }
  }

  public static List<ActionOption> getOptionsForCategory(Category cat)
  {
    List<ActionOption> list = new ArrayList<>();
    switch (cat)
    {
      case GRAMMAR:
        list.add(new ActionOption("grammar_all", "Fix All", "Spelling, punctuation & grammar"));
        list.add(new ActionOption("grammar_spelling", "Spelling Only", "Fix typos and spelling"));
        list.add(new ActionOption("grammar_structure", "Sentence Flow", "Improve sentence structure"));
        break;

      case TRANSLATE:
        list.add(new ActionOption("tr_banglish", "🔤 Banglish (বাংলিশ)", "Phonetic English letters e.g. Tumi kemon acho?"));
        list.add(new ActionOption("tr_bn", "বাংলা (Bengali)", "Natural idiomatic Bengali"));
        list.add(new ActionOption("tr_en", "English", "Fluent modern English"));
        list.add(new ActionOption("tr_ar", "العربية (Arabic)", "Accurate modern Arabic"));
        list.add(new ActionOption("tr_hi", "हिन्दी (Hindi)", "Natural Hindi"));
        list.add(new ActionOption("tr_ur", "اردو (Urdu)", "Polite literary Urdu"));
        list.add(new ActionOption("tr_es", "Español", "Natural Spanish"));
        list.add(new ActionOption("tr_fr", "Français", "Natural French"));
        list.add(new ActionOption("tr_zh", "中文 (Chinese)", "Standard Chinese"));
        list.add(new ActionOption("tr_ja", "日本語 (Japanese)", "Natural Japanese"));
        list.add(new ActionOption("tr_de", "Deutsch", "Natural German"));
        break;

      case REWRITE:
        list.add(new ActionOption("rephrase", "Rephrase", "Rewrite with better phrasing"));
        list.add(new ActionOption("prof", "Professional", "Polite and confident"));
        list.add(new ActionOption("rewrite_islamic", "Islamic Style", "Salam, InshaAllah & Islamic blessings"));
        list.add(new ActionOption("rewrite_islamic_ar", "Islamic + Arabic", "Salam with authentic Arabic script"));
        list.add(new ActionOption("casual", "Casual", "Warm and easygoing"));
        list.add(new ActionOption("friendly", "Friendly", "Warm and approachable"));
        list.add(new ActionOption("formal", "Formal", "Dignified and elegant"));
        list.add(new ActionOption("clearer", "Make Clearer", "Remove fluff & ambiguity"));
        list.add(new ActionOption("simplify", "Simplify", "Easy to understand"));
        list.add(new ActionOption("vocab", "Rich Vocab", "Sophisticated words"));
        list.add(new ActionOption("expand", "Expand", "Add helpful detail"));
        list.add(new ActionOption("shorter", "Shorter", "Punchy and concise"));
        break;

      case ISLAMIC:
        list.add(new ActionOption("islamic_standard", "Islamic Vibe", "Salam, InshaAllah, SubhanAllah, blessings"));
        list.add(new ActionOption("islamic_arabic", "With Arabic Script", "Salam + Arabic phrases & Duas (السلام عليكم)"));
        list.add(new ActionOption("islamic_post", "Islamic Social Post", "Heart-touching post with hashtags"));
        list.add(new ActionOption("islamic_dua", "Dua & Blessings", "Heartfelt Islamic prayers & wishes"));
        list.add(new ActionOption("islamic_jummah", "Jummah Mubarak", "Jummah Mubarak greeting & reminder"));
        break;

      case CONTENT:
        list.add(new ActionOption("content_title", "Catchy Title (ক্যাচি টাইটেল)", "5 viral, click-worthy titles for articles or videos"));
        list.add(new ActionOption("content_yt_title", "YouTube Title (ইউটিউব টাইটেল)", "5 high-CTR, SEO-optimized YouTube video titles"));
        list.add(new ActionOption("content_desc", "General Description (বিবরণ)", "Comprehensive, engaging description of the topic/media"));
        list.add(new ActionOption("content_yt_desc", "YouTube Description (ইউটিউব ডেসক্রিপশন)", "Full YouTube description with summary, timestamps, CTA & hashtags"));
        list.add(new ActionOption("content_prod_desc", "Product Description (পণ্যের বিবরণ)", "Persuasive e-commerce copy with features, benefits & CTA"));
        list.add(new ActionOption("content_seo_meta", "SEO Meta Description (মেটা বিবরণ)", "150-160 char Google SEO meta description with keywords"));
        list.add(new ActionOption("content_caption", "Social Caption (সোশ্যাল ক্যাপশন)", "Engaging caption for Facebook/Instagram with hashtags"));
        list.add(new ActionOption("content_hook", "Viral Hook (ভাইরাল হুক)", "5 magnetic, scroll-stopping opening lines"));
        list.add(new ActionOption("content_outline", "Article Outline (আউটলাইন)", "Structured H1, H2, H3 headings and talking points"));
        list.add(new ActionOption("content_cta", "Call to Action (CTA)", "5 high-converting closing CTAs to drive action"));
        break;

      case REPLY:
        list.add(new ActionOption("reply_friendly", "Friendly", "Warm & kind reply"));
        list.add(new ActionOption("reply_islamic", "Islamic Reply", "Start with Salam, warm Islamic wishes"));
        list.add(new ActionOption("reply_islamic_dua", "Dua / Gratitude", "Jazakallah Khair, Barakallah Feek"));
        list.add(new ActionOption("reply_casual", "Casual", "Everyday natural reply"));
        list.add(new ActionOption("reply_prof", "Professional", "Polite & business-ready"));
        list.add(new ActionOption("reply_funny", "Funny", "Humorous & lighthearted"));
        list.add(new ActionOption("reply_polite", "Polite", "Courteous & respectful"));
        list.add(new ActionOption("reply_short", "Short", "Direct 1-line answer"));
        list.add(new ActionOption("reply_witty", "Witty", "Clever & sharp"));
        list.add(new ActionOption("reply_flirty", "Romantic", "Charming & romantic"));
        list.add(new ActionOption("reply_apology", "Apologetic", "Sincere apology"));
        list.add(new ActionOption("reply_grateful", "Grateful", "Appreciative & thankful"));
        list.add(new ActionOption("reply_comment", "Comment Mode", "Social comment reply"));
        break;

      case EMAIL:
        list.add(new ActionOption("email_prof", "Professional", "Standard business email"));
        list.add(new ActionOption("email_formal", "Formal", "Official & corporate"));
        list.add(new ActionOption("email_friendly", "Friendly", "Warm team email"));
        list.add(new ActionOption("email_meeting", "Meeting Request", "Schedule or invite to meeting"));
        list.add(new ActionOption("email_leave", "Leave Request", "Holiday / sick leave request"));
        list.add(new ActionOption("email_job", "Job Application", "Cover letter / application"));
        list.add(new ActionOption("email_followup", "Follow-up", "Gentle project/meeting follow-up"));
        list.add(new ActionOption("email_apology", "Apology", "Apology for delay or error"));
        list.add(new ActionOption("email_thanks", "Thank You", "Expression of gratitude"));
        list.add(new ActionOption("email_resignation", "Resignation", "Formal resignation notice"));
        list.add(new ActionOption("email_complaint", "Complaint", "Firm and polite complaint"));
        list.add(new ActionOption("email_support", "Customer Support", "Empathetic support reply"));
        break;

      case SOCIAL:
        list.add(new ActionOption("soc_fb", "Facebook", "Engaging post + hashtags"));
        list.add(new ActionOption("soc_islamic", "Islamic Status", "Inspiring Islamic post + tags"));
        list.add(new ActionOption("soc_li", "LinkedIn", "Professional career post + tags"));
        list.add(new ActionOption("soc_x", "X / Twitter", "Punchy tweet under 280 chars + tags"));
        list.add(new ActionOption("soc_ig", "Instagram", "Aesthetic caption + hashtags"));
        list.add(new ActionOption("soc_yt", "YouTube", "Community tab announcement"));
        list.add(new ActionOption("soc_viral", "Viral Style", "Catchy hook & high engagement + tags"));
        list.add(new ActionOption("soc_story", "Storytelling", "Narrative emotional style + tags"));
        list.add(new ActionOption("soc_promo", "Promotional", "Compelling offer / CTA + tags"));
        break;

      case SUMMARIZE:
        list.add(new ActionOption("sum_one", "1 Sentence", "Ultra-concise single sentence"));
        list.add(new ActionOption("sum_short", "Short", "2-3 sentences overview"));
        list.add(new ActionOption("sum_bullets", "Bullet Points", "Key takeaways in bullets"));
        list.add(new ActionOption("sum_key", "Key Facts", "Core insights and stats"));
        list.add(new ActionOption("sum_detailed", "Detailed", "Comprehensive summary"));
        break;

      case EXPLAIN:
        list.add(new ActionOption("exp_simple", "Simple (ELI5)", "Clear everyday words"));
        list.add(new ActionOption("exp_code", "Explain Code", "Step-by-step code explanation"));
        list.add(new ActionOption("exp_code_fix", "Fix Code Bug", "Find bugs and fix code"));
        list.add(new ActionOption("exp_bn", "In Bangla", "Explain clearly in Bengali"));
        list.add(new ActionOption("exp_beginner", "Beginner Guide", "Gentle intro for novices"));
        list.add(new ActionOption("exp_tech", "Technical", "Deep technical architecture"));
        break;

      case NATURALIZE:
        list.add(new ActionOption("nat_human", "Humanize", "Remove robotic AI clichés"));
        list.add(new ActionOption("nat_casual", "Casual Human", "Easygoing personal phrasing"));
        list.add(new ActionOption("nat_chat", "Conversational", "Sounds like talking face-to-face"));
        list.add(new ActionOption("nat_less_formal", "Less Formal", "Relax stiff phrasing"));
        break;

      case TONE:
        list.add(new ActionOption("tone_prof", "Professional", "Polite and confident"));
        list.add(new ActionOption("tone_islamic", "Islamic Vibe", "Polite, modest with Salam & blessings"));
        list.add(new ActionOption("tone_islamic_ar", "Islamic + Arabic", "Polite with authentic Arabic phrases"));
        list.add(new ActionOption("tone_friendly", "Friendly", "Warm and approachable"));
        list.add(new ActionOption("tone_formal", "Formal", "Respectful & diplomatic"));
        list.add(new ActionOption("tone_casual", "Casual", "Everyday informal"));
        list.add(new ActionOption("tone_confident", "Confident", "Strong and assertive"));
        list.add(new ActionOption("tone_humble", "Humble", "Modest & grateful"));
        list.add(new ActionOption("tone_persuasive", "Persuasive", "Convincing and compelling"));
        list.add(new ActionOption("tone_empathetic", "Empathetic", "Compassionate & caring"));
        list.add(new ActionOption("tone_direct", "Direct", "Straight to the point"));
        list.add(new ActionOption("tone_enthusiastic", "Enthusiastic", "Upbeat and energetic"));
        break;

      case EMOTION:
        list.add(new ActionOption("emo_grateful", "Grateful", "Deep appreciation & thanks"));
        list.add(new ActionOption("emo_happy", "Happy", "Joyful & positive"));
        list.add(new ActionOption("emo_excited", "Excited", "High energy excitement"));
        list.add(new ActionOption("emo_romantic", "Romantic", "Heartfelt & sweet"));
        list.add(new ActionOption("emo_apologetic", "Apologetic", "Sincere remorse"));
        list.add(new ActionOption("emo_encouraging", "Encouraging", "Inspiring & supportive"));
        list.add(new ActionOption("emo_calm", "Calm", "Peaceful & soothing"));
        break;

      case HUMOR:
        list.add(new ActionOption("humor_light", "Light & Fun", "Playful touch of humor"));
        list.add(new ActionOption("humor_funny", "Funny", "Genuinely hilarious"));
        list.add(new ActionOption("humor_sarcastic", "Sarcastic", "Dry wit & mild sarcasm"));
        list.add(new ActionOption("humor_witty", "Witty", "Clever punchline"));
        list.add(new ActionOption("humor_meme", "Meme Style", "Internet humor / meme phrasing"));
        break;

      case LENGTH:
        list.add(new ActionOption("len_tldr", "TL;DR", "Super short essence"));
        list.add(new ActionOption("len_short", "Shorten", "Cut words, keep meaning"));
        list.add(new ActionOption("len_expand", "Expand", "Elaborate with examples"));
        list.add(new ActionOption("len_story", "Story Expansion", "Expand into a story"));
        break;

      case ASK_AI:
        list.add(new ActionOption("ask_custom", "Custom Prompt", "Enter your own instructions"));
        break;
    }
    return list;
  }

  public static class ContextDetectionResult
  {
    public final String detectedType; // "Message / Question", "Request", "Email", "Code", "Article", "General"
    public final List<String> quickSuggestions; // 3 instant quick reply suggestions
    public final Category recommendedCategory;
    public final String recommendedOptionId;

    public ContextDetectionResult(String detectedType, List<String> quickSuggestions, Category cat, String optId)
    {
      this.detectedType = detectedType;
      this.quickSuggestions = quickSuggestions;
      this.recommendedCategory = cat;
      this.recommendedOptionId = optId;
    }
  }

  /**
   * Fast, zero-lag local heuristics to inspect text or clipboard and recommend 1-tap actions.
   */
  public static ContextDetectionResult analyzeContext(String text)
  {
    if (text == null || text.trim().isEmpty())
    {
      return new ContextDetectionResult("Empty", Collections.<String>emptyList(), Category.REWRITE, "rephrase");
    }

    String trimmed = text.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);

    // 1. Detect Code
    if (trimmed.contains("{") && trimmed.contains("}") && (trimmed.contains("function") || trimmed.contains("class") || trimmed.contains("public") || trimmed.contains("const") || trimmed.contains("let") || trimmed.contains("def ") || trimmed.contains("import ")))
    {
      List<String> codeSugg = new ArrayList<>();
      codeSugg.add("Explain how this code works");
      codeSugg.add("Find bugs and optimize");
      codeSugg.add("Add comments and documentation");
      return new ContextDetectionResult("Code Snippet", codeSugg, Category.EXPLAIN, "exp_code");
    }

    // 2. Detect Email / Letter
    if (lower.contains("dear ") || lower.contains("subject:") || lower.contains("regards") || lower.contains("sincerely") || lower.contains("hi team") || lower.contains("জনাব"))
    {
      List<String> emailSugg = new ArrayList<>();
      emailSugg.add("Thank you for your email. I will review it shortly.");
      emailSugg.add("Received with thanks. Let me get back to you soon.");
      emailSugg.add("I have received your request and will follow up today.");
      return new ContextDetectionResult("Email Draft", emailSugg, Category.REPLY, "reply_prof");
    }

    // 3. Detect Request (asking for file/report/time)
    if (lower.contains("can you") || lower.contains("could you") || lower.contains("please send") || lower.contains("report") || lower.contains("দিতে পারবেন") || lower.contains("পাঠাবেন") || lower.contains("একটু সাহায্য"))
    {
      List<String> reqSugg = new ArrayList<>();
      reqSugg.add("Sure, I'll send it over shortly!");
      reqSugg.add("I'm working on it now and will send it soon.");
      reqSugg.add("Sorry, I may need a little more time on this.");
      return new ContextDetectionResult("Action Request", reqSugg, Category.REPLY, "reply_prof");
    }

    // 4. Detect Invitation / Asking if coming
    if (lower.contains("are you coming") || lower.contains("you free") || lower.contains("where are you") || lower.contains("আসছ") || lower.contains("আসবেন") || lower.contains("কোথায় আছ"))
    {
      List<String> invSugg = new ArrayList<>();
      invSugg.add("Yes, I'll be there on time!");
      invSugg.add("Almost there! See you in a few minutes.");
      invSugg.add("Unfortunately I won't be able to make it today.");
      return new ContextDetectionResult("Question / Invitation", invSugg, Category.REPLY, "reply_friendly");
    }

    // 5. Detect Question
    if (trimmed.endsWith("?") || lower.startsWith("what") || lower.startsWith("how") || lower.startsWith("why") || lower.startsWith("when") || lower.contains("কেমন") || lower.contains("কী") || lower.contains("কিভাবে"))
    {
      List<String> qSugg = new ArrayList<>();
      qSugg.add("Yes, absolutely!");
      qSugg.add("Let me check and get back to you in a moment.");
      qSugg.add("I'm not sure, let's discuss later.");
      return new ContextDetectionResult("Question", qSugg, Category.REPLY, "reply_casual");
    }

    // 6. Detect Article / Long Text (more than 250 characters)
    if (trimmed.length() > 250)
    {
      List<String> artSugg = new ArrayList<>();
      artSugg.add("Key takeaways in 3 bullet points");
      artSugg.add("1-sentence concise summary");
      artSugg.add("Rewrite in simple language");
      return new ContextDetectionResult("Long Text / Article", artSugg, Category.SUMMARIZE, "sum_bullets");
    }

    // 7. Detect Content Writing request (Title, Description, Outline, Hook, Caption)
    if (lower.contains("title") || lower.contains("headline") || lower.contains("description")
        || lower.contains("caption") || lower.contains("outline") || lower.contains("hook")
        || lower.contains("টাইটেল") || lower.contains("ডেসক্রিপশন") || lower.contains("বিবরণ")
        || lower.contains("ক্যাপশন") || lower.contains("আউটলাইন") || lower.contains("হুক"))
    {
      List<String> contentSugg = new ArrayList<>();
      contentSugg.add("Generate catchy titles");
      contentSugg.add("Write YouTube description");
      contentSugg.add("Write detailed description");
      return new ContextDetectionResult("Content Writing", contentSugg, Category.CONTENT, "content_title");
    }

    // 8. General Text -> Recommend Rewrite / Naturalize
    List<String> genSugg = new ArrayList<>();
    genSugg.add("Make it professional");
    genSugg.add("Make it natural & human");
    genSugg.add("Fix grammar & spelling");
    return new ContextDetectionResult("Message / Text", genSugg, Category.REWRITE, "rephrase");
  }

  public static final String ISLAMIC_CONTEXT_RULES =
      "CRITICAL ISLAMIC VOCABULARY APPLICATION & ETIQUETTE RULES (ইসলামিক শব্দের সঠিক প্রয়োগবিধি):\n"
      + "Strictly adhere to the following situational and theological rules. NEVER insert Islamic words randomly or blindly:\n"
      + "1. GREETINGS (সালাম): Always begin respectfully with 'আসসালামু আলাইকুম' or 'আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু' (Arabic: السلام عليكم ورحمة الله وبركاته).\n"
      + "2. GOOD NEWS, SUCCESS, COMPLETION & BLESSINGS (খুশির সংবাদ, সফলতা, সুস্থতা, নিয়ামত ও স্বপ্ন পূরণ):\n"
      + "   - Use 'আলহামদুলিল্লাহ' (الحمد لله) for any joyful news, recovery from illness, achievement, graduation, job, dream fulfilled, relief, or gratitude to Allah.\n"
      + "   - Use 'সুবহানাল্লাহ' (سبحان الله) when witnessing miraculous, astonishing, or awe-inspiring blessings or natural wonder.\n"
      + "3. PRAISE, APPRECIATION & WARDING OFF EVIL EYE (বাহবা, প্রশংসা ও নজর থেকে সুরক্ষা):\n"
      + "   - Use 'মাশাআল্লাহ' / 'মা শা আল্লাহ' (ما شاء الله) or 'তাবারাকাল্লাহ' (تبارك الله) when praising someone's skill, beauty, child, good character, or achievement, acknowledging that all excellence is by Allah's will and protecting from evil eye (নজর).\n"
      + "4. FUTURE PLANS, HOPES & COMMITMENTS (ভবিষ্যতের ইচ্ছা, পরিকল্পনা ও প্রতিশ্রুতি):\n"
      + "   - Use 'ইনশাআল্লাহ্' (إن شاء الله) STRICTLY for future actions, promises, or hopes (e.g. 'কাল দেখা হবে ইনশাআল্লাহ', 'আমরা সফল হব ইনশাআল্লাহ').\n"
      + "   - RULE: NEVER use 'ইনশাআল্লাহ্' for events that have already happened or are currently happening in the past/present!\n"
      + "5. BEREAVEMENT, HARDSHIP, SADNESS & CONDOLENCES (শোক, মৃত্যু, বিপদ, ক্ষতি ও সান্ত্বনা):\n"
      + "   - Use 'ইন্না লিল্লাহি ওয়া ইন্না ইলাইহি রাজিউন' (إنا لله وإنا إليه راجعون) or 'ইন্না লিল্লাহ' upon hearing of someone's death, an accident, illness, tragic news, or financial loss.\n"
      + "   - Include comforting prayers for patience: 'আল্লাহ আপনাকে ধৈর্য/সবর দান করুন' and prayers for the deceased: 'আল্লাহ তাকে জান্নাতুল ফিরদাউস নসিব করুন'.\n"
      + "6. GRATITUDE TOWARDS PEOPLE (মানুষের প্রতি কৃতজ্ঞতা ও দোয়া):\n"
      + "   - Use 'জাযাকাল্লাহু খাইরান' (جزاكم الله خيراً - May Allah reward you with goodness) when thanking someone for their help, kindness, or advice.\n"
      + "   - Use 'বারাকাল্লাহু ফিক' (بارك الله فيك - May Allah bless you) to invoke blessings on someone, their family, or their work.\n"
      + "7. SINS, ERRORS & SEEKING PARDON (ভুলত্রুটি, অনুশোচনা ও ক্ষমা প্রার্থনা):\n"
      + "   - Use 'আস্তাগফিরুল্লাহ' (أستغفر الله) when admitting a flaw, feeling regret, or seeking Allah's forgiveness.\n"
      + "8. COURAGE & RELIANCE ON ALLAH (ভরসা ও প্রতিকূলতা):\n"
      + "   - Use 'আল্লাহ ভরসা' or 'হাসবুনাল্লাহু ওয়া নি'মাল ওয়াকিল' (حسبনা الله ونعم الوكيل) when facing challenges or needing strength.\n"
      + "9. FAREWELL (বিদায়):\n"
      + "   - Conclude with 'আল্লাহ হাফেজ' or 'ফি আমানিল্লাহ' (في أمان الله).\n"
      + "10. NATURAL ADAB & MODERATION (পরিমিতিবোধ ও আদব):\n"
      + "   - Do NOT spam Islamic words unnaturally into every single sentence. Integrate the appropriate phrases smoothly and elegantly only where contextually suitable, maintaining dignified, warm, and authentic Bengali or English prose.\n";

  public static String buildSystemPrompt(Category category, String optionId, String globalTone, String customPrompt)
  {
    return buildSystemPrompt(category, optionId, globalTone, customPrompt, true);
  }

  /**
   * Builds the comprehensive, tailored system prompt for the chosen Category, Option, and optional global Tone.
   */
  public static String buildSystemPrompt(Category category, String optionId, String globalTone, String customPrompt, boolean emojify)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert, world-class AI writing assistant integrated into a mobile keyboard.\n");
    sb.append("CRITICAL RULES:\n");
    sb.append("1. Output ONLY the resulting text. Do NOT include any explanations, conversational remarks, preamble ('Here is...'), quotation marks, or meta-commentary.\n");
    sb.append("2. Preserve the target language (if the input is in Bengali, output in natural Bengali; if English, output in English), unless specifically instructed to translate.\n");
    sb.append("3. Make the writing sound authentic, human, and modern. Avoid robotic clichés like 'delighted to inform', 'in conclusion', 'furthermore', or unnecessary jargon.\n");
    sb.append("4. BENGALI SCRIPT & ORTHOGRAPHY RULE: When generating Bengali (বাংলা), strictly follow modern standard Bengali orthography. Form proper conjuncts (ক্ষ, জ্ঞ, ঙ্ক, ঙ্গ, ঞ্চ, ঞ্জ, ষ্ণ, ষ্ঠ, ণ্ড, ণ্ট, ন্ধ, ম্প, ক্ত, ত্র, প্র ইত্যাদি) without broken spaces. Correctly attach all vowel matras (া, ি, ী, ু, ূ, ৃ, ে, ৈ, ো, ৌ). NEVER output disjointed or broken characters (যেমন ভুলভাবে 'ক ্ ষ' বা কার আলাদা করে ভাঙা শব্দ লেখা সম্পূর্ণ নিষেধ)।\n");
    if (emojify)
    {
      sb.append("5. EMOJI DIRECTIVE: Naturally enhance the message with fitting, expressive, and lively emojis.\n\n");
    }
    else
    {
      sb.append("5. STRICT EMOJI BAN: Do NOT include ANY emojis, emoticons, pictograms, or symbols under any circumstances. Output 100% plain text only.\n\n");
    }

    // Specific category instructions
    switch (category)
    {
      case REWRITE:
        if ("prof".equals(optionId))
          sb.append("TASK: Rewrite the input in a polished, confident, professional business tone.\n");
        else if ("rewrite_islamic".equals(optionId))
        {
          sb.append("TASK: Rewrite the text with an authentic, respectful Islamic tone.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("rewrite_islamic_ar".equals(optionId))
        {
          sb.append("TASK: Rewrite the text with an authentic Islamic tone and include authentic Arabic script (السلام عليكم ورحمة الله وبركاته, إن شاء الله, الحمد لله, ما شاء الله) alongside natural Bengali or English.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("casual".equals(optionId))
          sb.append("TASK: Rewrite in a warm, relaxed, friendly everyday casual style.\n");
        else if ("friendly".equals(optionId))
          sb.append("TASK: Rewrite in an approachable, warm, enthusiastic and friendly tone.\n");
        else if ("formal".equals(optionId))
          sb.append("TASK: Rewrite with respectful, formal, diplomatic and elegant wording.\n");
        else if ("clearer".equals(optionId))
          sb.append("TASK: Rewrite to maximize clarity, removing fluff, ambiguity, and wordiness.\n");
        else if ("simplify".equals(optionId))
          sb.append("TASK: Simplify the language so it is effortless to understand for anyone.\n");
        else if ("vocab".equals(optionId))
          sb.append("TASK: Upgrade the vocabulary with sophisticated, evocative, and high-quality words.\n");
        else if ("expand".equals(optionId))
          sb.append("TASK: Expand and elaborate on the input with vivid, helpful details and natural flow.\n");
        else if ("shorter".equals(optionId))
          sb.append("TASK: Condense and shorten into a punchy, concise message without losing key facts.\n");
        else
          sb.append("TASK: Rephrase and polish the text for smooth rhythm and natural phrasing.\n");
        break;

      case ISLAMIC:
        sb.append("TASK: Compose, rewrite, or polish the text with authentic Islamic etiquette, vocabulary, and adab.\n");
        sb.append(ISLAMIC_CONTEXT_RULES);
        if ("islamic_arabic".equals(optionId))
        {
          sb.append("ADDITIONAL RULE: Include authentic Arabic script with accurate diacritics/tashkeel for Salam, Quranic phrasing, or Duas (e.g. السلام عليكم ورحمة الله وبركاته, إن شاء الله, ما شاء الله, سبحان الله, الحمد لله, جزاكم الله خيراً).\n");
        }
        else if ("islamic_post".equals(optionId))
        {
          sb.append("ADDITIONAL RULE: Craft as an inspiring Islamic social media post or status. Always end with 3 to 6 relevant Bengali and English hashtags (#IslamicReminder #Bangla #Deen #Alhamdulillah #IslamicStatus #DreamFulfilled).\n");
        }
        else if ("islamic_dua".equals(optionId))
        {
          sb.append("ADDITIONAL RULE: Structure as a heartfelt Islamic Dua (Supplication) and prayer, wishing profound barakah, peace, and goodness.\n");
        }
        else if ("islamic_jummah".equals(optionId))
        {
          sb.append("ADDITIONAL RULE: Craft as a blessed Jummah Mubarak greeting with meaningful spiritual reflections, Durood reminder, and warm Jummah wishes.\n");
        }
        break;

      case CONTENT:
        if ("content_yt_title".equals(optionId))
        {
          sb.append("TASK: Generate 5 high-CTR, SEO-optimized YouTube video titles based on the input.\nFormat: Numbered list (1 to 5). Include emotional power words and curiosity hooks that maximize clicks.\n");
        }
        else if ("content_desc".equals(optionId))
        {
          sb.append("TASK: Write a clear, comprehensive, and engaging description based on the input. Structure with a strong opening overview, followed by key highlights, and an engaging concluding thought.\n");
        }
        else if ("content_yt_desc".equals(optionId))
        {
          sb.append("TASK: Write a complete, professional YouTube video description based on the input.\nStructure required:\n1. Hook & Video Summary (2-3 engaging lines)\n2. Key Timestamps / Chapters breakdown (e.g. 0:00 Intro, etc.)\n3. Links / Social / Resources placeholder\n4. Call to action (Like, Subscribe, Comment)\n5. 5-8 relevant hashtags (#Tag1 #Tag2...)\n");
        }
        else if ("content_prod_desc".equals(optionId))
        {
          sb.append("TASK: Write an irresistible, high-converting e-commerce product description based on the input.\nStructure required:\n1. Attention-grabbing headline\n2. Engaging product overview highlighting what makes it special\n3. Bulleted Key Features & Benefits (focus on how it solves real problems)\n4. Specifications/Details\n5. Strong Call to Action (Buy Now / Order Today)\n");
        }
        else if ("content_seo_meta".equals(optionId))
        {
          sb.append("TASK: Write an optimized SEO Meta Description (strictly between 150 to 160 characters) based on the input.\nInclude primary search keywords naturally, highlight unique value, and include a clear click-intent CTA.\n");
        }
        else if ("content_caption".equals(optionId))
        {
          sb.append("TASK: Write an engaging social media caption based on the input.\nInclude an attention-grabbing first line (hook), a relatable or informative message body, an interactive question to drive comments, and 4-6 relevant hashtags.\n");
        }
        else if ("content_hook".equals(optionId))
        {
          sb.append("TASK: Generate 5 powerful, magnetic opening hooks (first 3-5 seconds of a video or first line of a post) that stop the scroll and hook viewers instantly.\nFormat: Numbered list (1 to 5). Use curiosity gap, bold statements, or surprising facts.\n");
        }
        else if ("content_outline".equals(optionId))
        {
          sb.append("TASK: Create a detailed, structured content/article outline based on the input.\nInclude H1 Main Title, H2 Section Headings, and bulleted sub-points or talking points for each section.\n");
        }
        else if ("content_cta".equals(optionId))
        {
          sb.append("TASK: Generate 5 compelling, action-oriented Call to Action (CTA) phrases based on the input to drive user action (e.g. subscribe, comment, buy, share, follow).\nFormat: Numbered list (1 to 5).\n");
        }
        else
        {
          sb.append("TASK: Generate 5 catchy, high-converting, click-worthy titles/headlines based on the input.\nFormat: Numbered list (1 to 5). Make them punchy, intriguing, and memorable.\n");
        }
        break;

      case REPLY:
        sb.append("TASK: The user is writing a reply to the provided message or comment.\n");
        if ("reply_islamic".equals(optionId))
        {
          sb.append("Mode: Courteous Islamic reply.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("reply_islamic_dua".equals(optionId))
        {
          sb.append("Mode: Islamic gratitude and heartfelt Dua (Supplication), expressing blessings such as 'জাযাকাল্লাহু খাইরান' (جزاكم الله خيراً) and 'বারাকাল্লাহু ফিক'.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("reply_prof".equals(optionId))
          sb.append("Mode: Professional, polite, and constructive business reply.\n");
        else if ("reply_funny".equals(optionId))
          sb.append("Mode: Witty, lighthearted, and humorous reply with fitting emoji.\n");
        else if ("reply_polite".equals(optionId))
          sb.append("Mode: Courteous, respectful, and appreciative reply.\n");
        else if ("reply_short".equals(optionId))
          sb.append("Mode: Crisp, punchy, 1-line direct answer.\n");
        else if ("reply_witty".equals(optionId))
          sb.append("Mode: Clever, sharp, and intelligent reply.\n");
        else if ("reply_flirty".equals(optionId))
          sb.append("Mode: Charming, sweet, and romantic reply.\n");
        else if ("reply_apology".equals(optionId))
          sb.append("Mode: Sincere, humble, and polite apology.\n");
        else if ("reply_grateful".equals(optionId))
          sb.append("Mode: Heartfelt appreciation and gratitude.\n");
        else if ("reply_comment".equals(optionId))
          sb.append("Mode: Engaging, friendly social media comment reply (YouTube/Facebook/Instagram).\n");
        else
          sb.append("Mode: Warm, natural, friendly conversation reply.\n");
        break;

      case EMAIL:
        sb.append("TASK: Generate a complete, beautifully structured email based on the user's brief points or input.\n");
        sb.append("Format required:\nSubject: [Engaging & Clear Subject Line]\n\n[Salutation],\n\n[Body Paragraphs]\n\n[Sign-off],\n[Name]\n");
        if ("email_formal".equals(optionId))
          sb.append("Tone: Formal, corporate, and dignified.\n");
        else if ("email_friendly".equals(optionId))
          sb.append("Tone: Warm, collaborative, and friendly team email.\n");
        else if ("email_meeting".equals(optionId))
          sb.append("Type: Meeting invitation / scheduling request with clear agenda and proposed times.\n");
        else if ("email_leave".equals(optionId))
          sb.append("Type: Formal leave application with dates, reason, and handover details.\n");
        else if ("email_job".equals(optionId))
          sb.append("Type: Professional job application / cover letter highlighting skills and enthusiasm.\n");
        else if ("email_followup".equals(optionId))
          sb.append("Type: Gentle, polite follow-up checking in on previous discussion or email.\n");
        else if ("email_apology".equals(optionId))
          sb.append("Type: Sincere apology acknowledging an issue and offering a solution.\n");
        else if ("email_thanks".equals(optionId))
          sb.append("Type: Heartfelt thank-you note recognizing someone's help or partnership.\n");
        else if ("email_resignation".equals(optionId))
          sb.append("Type: Professional resignation letter with notice period and gratitude.\n");
        else if ("email_complaint".equals(optionId))
          sb.append("Type: Firm, constructive, and polite formal complaint.\n");
        else if ("email_support".equals(optionId))
          sb.append("Type: Helpful, empathetic, solution-focused customer support response.\n");
        else
          sb.append("Tone: Professional, clear, and confident business email.\n");
        break;

      case SOCIAL:
        sb.append("TASK: Generate an engaging social media post based on the user's input.\n");
        sb.append("HASHTAG REQUIREMENT: Always append 3 to 6 relevant, high-impact hashtags at the bottom of the post (e.g. #Bangla #FacebookPost #Trending or topic-focused tags).\n");
        if ("soc_islamic".equals(optionId))
        {
          sb.append("Style: Inspiring Islamic post.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("soc_li".equals(optionId))
          sb.append("Platform: LinkedIn. Use a strong hook, concise professional insights, bulleted takeaways, and relevant professional hashtags.\n");
        else if ("soc_x".equals(optionId))
          sb.append("Platform: X (Twitter). Keep it under 280 characters with a punchy hook and 1-2 trending hashtags.\n");
        else if ("soc_ig".equals(optionId))
          sb.append("Platform: Instagram. Aesthetic caption with expressive emojis, clean line breaks, and a curated set of 8-12 relevant hashtags at the bottom.\n");
        else if ("soc_yt".equals(optionId))
          sb.append("Platform: YouTube Community Tab. Friendly creator tone with excitement and a question to drive comments.\n");
        else if ("soc_viral".equals(optionId))
          sb.append("Style: Viral style with an irresistible opening hook, curiosity gap, and high shareability.\n");
        else if ("soc_story".equals(optionId))
          sb.append("Style: Storytelling narrative with emotional depth and a meaningful conclusion.\n");
        else if ("soc_promo".equals(optionId))
          sb.append("Style: Promotional post highlighting benefits, urgency, and a clear Call To Action (CTA).\n");
        else
          sb.append("Platform: Facebook. Friendly, engaging post with natural emojis, an invitation to comment, and relevant hashtags.\n");
        break;

      case GRAMMAR:
        sb.append("TASK: Fix all spelling, typos, punctuation, capitalization, and grammatical errors in the text.\n");
        sb.append("Preserve the exact tone, language, and meaning of the original author. Output ONLY the corrected text.\n");
        break;

      case TRANSLATE:
        if ("tr_banglish".equals(optionId))
        {
          sb.append("TASK: Transliterate/translate the text into natural Banglish (Bengali written using Latin/English letters, such as 'Ami tomake bhalobashi', 'Kemon acho?', 'Kalke dekha hobe'). Make it sound completely natural as commonly used in chat and social messaging. Output ONLY the Banglish text.\n");
        }
        else
        {
          String targetLang = "English";
          if ("tr_bn".equals(optionId)) targetLang = "Bengali (বাংলা)";
          else if ("tr_ar".equals(optionId)) targetLang = "Arabic (العربية)";
          else if ("tr_hi".equals(optionId)) targetLang = "Hindi (हिन्दी)";
          else if ("tr_ur".equals(optionId)) targetLang = "Urdu (اردو)";
          else if ("tr_es".equals(optionId)) targetLang = "Spanish (Español)";
          else if ("tr_fr".equals(optionId)) targetLang = "French (Français)";
          else if ("tr_zh".equals(optionId)) targetLang = "Chinese (中文)";
          else if ("tr_ja".equals(optionId)) targetLang = "Japanese (日本語)";
          else if ("tr_de".equals(optionId)) targetLang = "German (Deutsch)";
          sb.append("TASK: Translate the text accurately and idiomatically into ").append(targetLang).append(".\n");
          sb.append("Ensure natural phrasing that native speakers actually use. Output ONLY the translated text.\n");
        }
        break;

      case SUMMARIZE:
        if ("sum_one".equals(optionId))
          sb.append("TASK: Summarize the text into exactly ONE concise, impactful sentence.\n");
        else if ("sum_bullets".equals(optionId))
          sb.append("TASK: Summarize the text into 3-5 clear bullet points highlighting key takeaways.\n");
        else if ("sum_key".equals(optionId))
          sb.append("TASK: Extract the core facts, data, and conclusions.\n");
        else if ("sum_detailed".equals(optionId))
          sb.append("TASK: Provide a comprehensive, structured summary covering all main points.\n");
        else
          sb.append("TASK: Summarize into a concise, readable 2-3 sentence overview.\n");
        break;

      case EXPLAIN:
        if ("exp_code".equals(optionId))
          sb.append("TASK: The input is programming code. Explain clearly how this code works, its logic, and its purpose.\n");
        else if ("exp_code_fix".equals(optionId))
          sb.append("TASK: The input is programming code. Identify any bugs, errors, or inefficiencies, and provide the clean, corrected code with a brief explanation of fixes.\n");
        else if ("exp_bn".equals(optionId))
          sb.append("TASK: Explain the concept in clear, natural Bengali with simple examples.\n");
        else if ("exp_beginner".equals(optionId))
          sb.append("TASK: Explain for an absolute beginner with zero prior knowledge. Use intuitive analogies.\n");
        else if ("exp_tech".equals(optionId))
          sb.append("TASK: Provide a rigorous, technical explanation for engineers or domain experts.\n");
        else
          sb.append("TASK: Explain this topic simply and clearly, as if explaining to a smart 10-year-old (ELI5).\n");
        break;

      case NATURALIZE:
        sb.append("TASK: Humanize and naturalize the text. Strip out robotic AI phrasing (e.g. 'I hope this email finds you well', 'delighted to announce', 'pivotal role', 'in essence').\n");
        sb.append("Make it sound like a real person writing naturally, with authentic rhythm and voice.\n");
        break;

      case TONE:
        if ("tone_islamic".equals(optionId))
        {
          sb.append("TASK: Rewrite the text with an authentic, polite, and modest Islamic tone.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("tone_islamic_ar".equals(optionId))
        {
          sb.append("TASK: Rewrite with an Islamic tone featuring authentic Arabic script phrases (السلام عليكم ورحمة الله وبركاته, إن شاء الله, الحمد لله, ما شاء الله) with great courtesy and modesty.\n").append(ISLAMIC_CONTEXT_RULES);
        }
        else if ("tone_formal".equals(optionId))
          sb.append("TASK: Rewrite the text in a formal, respectful, and diplomatic tone.\n");
        else if ("tone_casual".equals(optionId))
          sb.append("TASK: Rewrite in a relaxed, casual, everyday tone.\n");
        else if ("tone_friendly".equals(optionId))
          sb.append("TASK: Rewrite in a warm, welcoming, and friendly tone.\n");
        else if ("tone_confident".equals(optionId))
          sb.append("TASK: Rewrite with high confidence, authority, and assertiveness.\n");
        else if ("tone_humble".equals(optionId))
          sb.append("TASK: Rewrite in a modest, humble, and polite tone.\n");
        else if ("tone_persuasive".equals(optionId))
          sb.append("TASK: Rewrite to be highly persuasive, compelling, and convincing.\n");
        else if ("tone_empathetic".equals(optionId))
          sb.append("TASK: Rewrite with deep empathy, understanding, and warmth.\n");
        else if ("tone_direct".equals(optionId))
          sb.append("TASK: Rewrite in a direct, no-nonsense, straight-to-the-point manner.\n");
        else if ("tone_enthusiastic".equals(optionId))
          sb.append("TASK: Rewrite with high energy, enthusiasm, and optimism.\n");
        else
          sb.append("TASK: Rewrite in a polished, professional business tone.\n");
        break;

      case EMOTION:
        if ("emo_happy".equals(optionId))
          sb.append("TASK: Infuse the text with genuine happiness, joy, and warmth.\n");
        else if ("emo_excited".equals(optionId))
          sb.append("TASK: Infuse with excitement, anticipation, and high energy.\n");
        else if ("emo_romantic".equals(optionId))
          sb.append("TASK: Express sweet, heartfelt romantic affection.\n");
        else if ("emo_apologetic".equals(optionId))
          sb.append("TASK: Express sincere remorse and humble apology.\n");
        else if ("emo_encouraging".equals(optionId))
          sb.append("TASK: Offer inspiring encouragement and steadfast support.\n");
        else if ("emo_calm".equals(optionId))
          sb.append("TASK: Infuse with calm, peaceful, and reassuring serenity.\n");
        else
          sb.append("TASK: Express deep, genuine gratitude and heartfelt thanks.\n");
        break;

      case HUMOR:
        if ("humor_funny".equals(optionId))
          sb.append("TASK: Add hilarious, entertaining comedy to the text.\n");
        else if ("humor_sarcastic".equals(optionId))
          sb.append("TASK: Add clever, dry sarcasm and playful irony.\n");
        else if ("humor_witty".equals(optionId))
          sb.append("TASK: Add sharp wit and a clever humorous twist.\n");
        else if ("humor_meme".equals(optionId))
          sb.append("TASK: Rewrite using internet meme culture, playful slang, and relatable humor.\n");
        else
          sb.append("TASK: Add a subtle, charming touch of light humor.\n");
        break;

      case LENGTH:
        if ("len_tldr".equals(optionId))
          sb.append("TASK: Condense to a single punchy TL;DR line.\n");
        else if ("len_expand".equals(optionId))
          sb.append("TASK: Expand and elaborate with rich details and clear examples.\n");
        else if ("len_story".equals(optionId))
          sb.append("TASK: Expand into an engaging, illustrative narrative story.\n");
        else
          sb.append("TASK: Shorten and tighten the message to be concise.\n");
        break;

      case ASK_AI:
        if (customPrompt != null && !customPrompt.trim().isEmpty())
        {
          sb.append("USER INSTRUCTION: ").append(customPrompt.trim()).append("\n");
        }
        else
        {
          sb.append("TASK: Improve and polish the input text.\n");
        }
        break;
    }

    // Apply global tone modifier if present
    if (globalTone != null && !globalTone.trim().isEmpty() && !"default".equalsIgnoreCase(globalTone))
    {
      if ("islamic".equalsIgnoreCase(globalTone) || "islamic vibe".equalsIgnoreCase(globalTone))
      {
        sb.append("\nGLOBAL TONE MODIFIER: Maintain an authentic Islamic tone.\n").append(ISLAMIC_CONTEXT_RULES);
      }
      else
      {
        sb.append("\nGLOBAL TONE MODIFIER: Ensure the entire output reflects a '").append(globalTone).append("' tone.\n");
      }
    }

    return sb.toString();
  }

  /**
   * Executes the AI action asynchronously through the active configured provider.
   */
  public static void executeAction(
      Context context,
      String userText,
      Category category,
      String optionId,
      String globalTone,
      String customPrompt,
      final AiProvider.Callback callback)
  {
    boolean emojify = GeminiAiService.isEmojifyEnabled(context);
    executeAction(context, userText, category, optionId, globalTone, customPrompt, emojify, callback);
  }

  public static void executeAction(
      Context context,
      String userText,
      Category category,
      String optionId,
      String globalTone,
      String customPrompt,
      final boolean emojify,
      final AiProvider.Callback callback)
  {
    String input = (userText != null) ? userText.trim() : "";
    String prompt = (customPrompt != null) ? customPrompt.trim() : "";

    if (input.isEmpty() && prompt.isEmpty())
    {
      if (callback != null) callback.onError("No text or prompt provided.");
      return;
    }

    // If no user text was selected but a custom prompt was entered (e.g. in Ask AI),
    // treat the custom prompt as the subject/request
    if (input.isEmpty())
    {
      input = prompt;
    }

    String systemPrompt = buildSystemPrompt(category, optionId, globalTone, customPrompt, emojify);
    AiProvider provider = AiProvider.Manager.getActiveProvider(context);
    provider.generate(context, systemPrompt, input, new AiProvider.Callback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        String clean = sanitizeBengaliAndUnicode(resultText);
        if (!emojify)
        {
          clean = GeminiAiService.stripEmojis(clean);
        }
        if (callback != null) callback.onSuccess(clean);
      }

      @Override
      public void onError(String errorMessage)
      {
        if (callback != null) callback.onError(errorMessage);
      }
    });
  }

  /**
   * Cleans up broken Unicode characters, normalizes Bengali conjuncts,
   * fixes separated hasant / vowel signs, and strips rogue zero-width chars.
   */
  public static String sanitizeBengaliAndUnicode(String text)
  {
    if (text == null || text.isEmpty()) return "";

    // 1. Unicode NFC Canonical Composition
    String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC);

    // 2. Fix broken Bengali conjuncts where spaces were erroneously inserted around hasant (\u09CD)
    // e.g. "ক ্ ষ" -> "ক্ষ"
    normalized = normalized.replaceAll("([\\u0980-\\u09FF])\\s+\\u09CD", "$1\u09CD");
    normalized = normalized.replaceAll("\\u09CD\\s+([\\u0980-\\u09FF])", "\u09CD$1");

    // 3. Fix broken vowel signs separated by spaces e.g. "ক া" -> "কা"
    normalized = normalized.replaceAll("([\\u0980-\\u09FF])\\s+([\\u09BE-\\u09CD\\u09D7])", "$1$2");

    // 4. Remove stray zero-width spaces/formatters that cause glyph separation
    normalized = normalized.replaceAll("[\\u200B\\u200E\\u200F]", "");

    return normalized.trim();
  }
}
