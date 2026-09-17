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
    REWRITE("✍️ Rewrite", "Rephrase, polish, or change writing style"),
    REPLY("💬 Reply", "Generate contextual chat & comment replies"),
    EMAIL("📧 Email", "Generate professional & formal emails"),
    SOCIAL("📱 Social", "Create posts for Facebook, LinkedIn, X, Insta"),
    GRAMMAR("🔍 Grammar", "Fix spelling, punctuation & grammar"),
    TRANSLATE("🔄 Translate", "Translate naturally into any language"),
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
      case REWRITE:
        list.add(new ActionOption("rephrase", "Rephrase", "Rewrite with better phrasing"));
        list.add(new ActionOption("prof", "Professional", "Polite and confident"));
        list.add(new ActionOption("casual", "Casual", "Warm and easygoing"));
        list.add(new ActionOption("friendly", "Friendly", "Warm and approachable"));
        list.add(new ActionOption("formal", "Formal", "Dignified and elegant"));
        list.add(new ActionOption("clearer", "Make Clearer", "Remove fluff & ambiguity"));
        list.add(new ActionOption("simplify", "Simplify", "Easy to understand"));
        list.add(new ActionOption("vocab", "Rich Vocab", "Sophisticated words"));
        list.add(new ActionOption("expand", "Expand", "Add helpful detail"));
        list.add(new ActionOption("shorter", "Shorter", "Punchy and concise"));
        break;

      case REPLY:
        list.add(new ActionOption("reply_friendly", "Friendly", "Warm & kind reply"));
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
        list.add(new ActionOption("soc_fb", "Facebook", "Engaging post with questions"));
        list.add(new ActionOption("soc_li", "LinkedIn", "Professional career post"));
        list.add(new ActionOption("soc_x", "X / Twitter", "Punchy tweet under 280 chars"));
        list.add(new ActionOption("soc_ig", "Instagram", "Aesthetic caption + hashtags"));
        list.add(new ActionOption("soc_yt", "YouTube", "Community tab announcement"));
        list.add(new ActionOption("soc_viral", "Viral Style", "Catchy hook & high engagement"));
        list.add(new ActionOption("soc_story", "Storytelling", "Narrative emotional style"));
        list.add(new ActionOption("soc_promo", "Promotional", "Compelling offer / CTA"));
        break;

      case GRAMMAR:
        list.add(new ActionOption("grammar_all", "Fix All", "Spelling, punctuation & grammar"));
        list.add(new ActionOption("grammar_spelling", "Spelling Only", "Fix typos and spelling"));
        list.add(new ActionOption("grammar_structure", "Sentence Flow", "Improve sentence structure"));
        break;

      case TRANSLATE:
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

    // 7. General Text -> Recommend Rewrite / Naturalize
    List<String> genSugg = new ArrayList<>();
    genSugg.add("Make it professional");
    genSugg.add("Make it natural & human");
    genSugg.add("Fix grammar & spelling");
    return new ContextDetectionResult("Message / Text", genSugg, Category.REWRITE, "rephrase");
  }

  /**
   * Builds the comprehensive, tailored system prompt for the chosen Category, Option, and optional global Tone.
   */
  public static String buildSystemPrompt(Category category, String optionId, String globalTone, String customPrompt)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert, world-class AI writing assistant integrated into a mobile keyboard.\n");
    sb.append("CRITICAL RULES:\n");
    sb.append("1. Output ONLY the resulting text. Do NOT include any explanations, conversational remarks, preamble ('Here is...'), quotation marks, or meta-commentary.\n");
    sb.append("2. Preserve the target language (if the input is in Bengali, output in natural Bengali; if English, output in English), unless specifically instructed to translate.\n");
    sb.append("3. Make the writing sound authentic, human, and modern. Avoid robotic clichés like 'delighted to inform', 'in conclusion', 'furthermore', or unnecessary jargon.\n\n");

    // Specific category instructions
    switch (category)
    {
      case REWRITE:
        if ("prof".equals(optionId))
          sb.append("TASK: Rewrite the input in a polished, confident, professional business tone.\n");
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

      case REPLY:
        sb.append("TASK: The user is writing a reply to the provided message or comment.\n");
        if ("reply_prof".equals(optionId))
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
        if ("soc_li".equals(optionId))
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
          sb.append("Platform: Facebook. Friendly, engaging post with natural emojis and an invitation to comment.\n");
        break;

      case GRAMMAR:
        sb.append("TASK: Fix all spelling, typos, punctuation, capitalization, and grammatical errors in the text.\n");
        sb.append("Preserve the exact tone, language, and meaning of the original author. Output ONLY the corrected text.\n");
        break;

      case TRANSLATE:
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
        if ("tone_formal".equals(optionId))
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
      sb.append("\nGLOBAL TONE MODIFIER: Ensure the entire output reflects a '").append(globalTone).append("' tone.\n");
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
    if (userText == null || userText.trim().isEmpty())
    {
      if (callback != null) callback.onError("No text provided.");
      return;
    }

    String systemPrompt = buildSystemPrompt(category, optionId, globalTone, customPrompt);
    AiProvider provider = AiProvider.Manager.getActiveProvider(context);
    provider.generate(context, systemPrompt, userText.trim(), callback);
  }
}
