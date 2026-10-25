package typodev.keyboard.ai;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper utilities for AI Vision features:
 * - Bitmap scaling & compression to lightweight JPEG
 * - Reading image bytes from Content URIs safely
 * - Locating recent device screenshots
 * - Prompt engineering for social media comments across tones
 * - Parsing generated comments
 */
public final class AiVisionHelper
{
  public static final String[] AVAILABLE_TONES = {
      "স্বাভাবিক",
      "প্রশংসা",
      "মজার",
      "বুদ্ধিদীপ্ত",
      "রোস্ট",
      "ইসলামিক"
  };

  private AiVisionHelper() {}

  /**
   * Resizes bitmap maintaining aspect ratio so max dimension <= maxDimension,
   * then compresses to JPEG bytes.
   */
  public static byte[] scaleAndCompressBitmap(Bitmap bitmap, int maxDimension, int quality)
  {
    if (bitmap == null) return null;

    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    Bitmap scaled = bitmap;

    if (width > maxDimension || height > maxDimension)
    {
      float ratio = (float) width / (float) height;
      int newWidth;
      int newHeight;
      if (ratio > 1f)
      {
        newWidth = maxDimension;
        newHeight = (int) (maxDimension / ratio);
      }
      else
      {
        newHeight = maxDimension;
        newWidth = (int) (maxDimension * ratio);
      }
      if (newWidth > 0 && newHeight > 0)
      {
        scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
      }
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    scaled.compress(Bitmap.CompressFormat.JPEG, Math.max(30, Math.min(100, quality)), out);

    if (scaled != bitmap)
    {
      scaled.recycle();
    }

    return out.toByteArray();
  }

  /**
   * Safely reads and downsamples an image URI to avoid OutOfMemory errors,
   * returning JPEG compressed byte array.
   */
  public static byte[] getBytesFromUri(Context context, Uri uri, int maxDimension)
  {
    if (context == null || uri == null) return null;
    ContentResolver resolver = context.getContentResolver();

    try
    {
      // First pass: inspect image dimensions
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inJustDecodeBounds = true;
      try (InputStream in = resolver.openInputStream(uri))
      {
        if (in == null) return null;
        BitmapFactory.decodeStream(in, null, opts);
      }

      int outWidth = opts.outWidth;
      int outHeight = opts.outHeight;
      if (outWidth <= 0 || outHeight <= 0) return null;

      // Calculate inSampleSize
      int sampleSize = 1;
      while ((outWidth / sampleSize) > maxDimension * 2 || (outHeight / sampleSize) > maxDimension * 2)
      {
        sampleSize *= 2;
      }

      opts.inJustDecodeBounds = false;
      opts.inSampleSize = sampleSize;
      opts.inPreferredConfig = Bitmap.Config.RGB_565;

      Bitmap bitmap = null;
      try (InputStream in2 = resolver.openInputStream(uri))
      {
        if (in2 == null) return null;
        bitmap = BitmapFactory.decodeStream(in2, null, opts);
      }

      if (bitmap == null) return null;

      byte[] result = scaleAndCompressBitmap(bitmap, maxDimension, 82);
      bitmap.recycle();
      return result;
    }
    catch (Throwable t)
    {
      return null;
    }
  }

  /**
   * Queries MediaStore for the most recent screenshot taken on the device.
   */
  public static Uri getLatestScreenshotUri(Context context)
  {
    if (context == null) return null;
    ContentResolver resolver = context.getContentResolver();

    String[] projection = {
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DISPLAY_NAME
    };

    String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
    Cursor cursor = null;
    try
    {
      cursor = resolver.query(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          projection,
          null,
          null,
          sortOrder
      );

      if (cursor != null && cursor.moveToFirst())
      {
        int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
        int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

        int count = 0;
        long nowSeconds = System.currentTimeMillis() / 1000L;

        do
        {
          long id = cursor.getLong(idCol);
          String name = cursor.getString(nameCol);
          long dateAdded = cursor.getLong(dateCol);

          if (name != null)
          {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("screenshot") || lowerName.contains("screen_shot"))
            {
              return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
          }

          // If within the last 15 minutes, likely the screenshot the user just took
          if (count == 0 && (nowSeconds - dateAdded) < 900L)
          {
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
          }

          count++;
        }
        while (cursor.moveToNext() && count < 10);
      }
    }
    catch (Throwable ignored)
    {
    }
    finally
    {
      if (cursor != null) cursor.close();
    }

    return null;
  }

  /** English interface labels; keep the existing style keys for prompts. */
  public static String getToneLabel(String tone)
  {
    if (tone == null) return "Natural";
    switch (tone)
    {
      case "স্বাভাবিক": return "Natural";
      case "প্রুফরিড": return "Proofread";
      case "রিফ্রেজ": return "Rephrase";
      case "প্রফেশনাল": return "Professional";
      case "বন্ধুত্বপূর্ণ": return "Friendly";
      case "সংক্ষিপ্ত": return "Concise";
      case "প্রশংসা": return "Appreciative";
      case "মজার": return "Playful";
      case "বুদ্ধিদীপ্ত": return "Witty";
      case "রোস্ট": return "Roast";
      case "ইসলামিক": return "Islamic";
      default: return tone;
    }
  }

  /** Resolves English guidelines for the selected comment tone. */
  public static String getToneInstruction(String tone)
  {
    if (tone == null) return "Natural, friendly, and conversational";
    switch (tone)
    {
      case "প্রফেশনাল":
        return "Professional, polished, respectful and clear";
      case "বন্ধুত্বপূর্ণ":
        return "Friendly, warm and approachable";
      case "প্রুফরিড":
        return "Correct spelling, grammar and punctuation with minimal changes";
      case "রিফ্রেজ":
        return "Rephrase naturally and clearly while preserving the meaning";
      case "সংক্ষিপ্ত":
        return "Concise and direct, preserving essential information";
      case "প্রশংসা":
        return "Warm, genuine, enthusiastic, appreciating the photo or achievement heartily";
      case "মজার":
        return "Humorous, witty, funny, lighthearted, meme-friendly and playful";
      case "বুদ্ধিদীপ্ত":
        return "Insightful, thoughtful, mature, intellectual perspective on the topic";
      case "রোস্ট":
        return "Witty sarcastic roast, playful friendly banter (never offensive, toxic, or vulgar)";
      case "ইসলামিক":
        return "Polite, humble, expressing Islamic blessings (e.g. MashaAllah, SubhanAllah, Dua)";
      case "স্বাভাবিক":
      default:
        return "Natural, casual, friendly everyday conversational social media tone";
    }
  }

  /**
   * Constructs the structured prompt for Gemini Vision comment generation.
   */
  public static String buildCommentPrompt(String tone)
  {
    String toneGuide = getToneInstruction(tone);

    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert social media AI assistant (Facebook, Instagram, WhatsApp, Twitter).\n");
    sb.append("Carefully analyze the image provided: the subjects, people, expressions, setting, actions, context, meme elements, or any text visible inside the image.\n\n");
    sb.append(AiWritingPrompts.GROUNDING_RULES);
    sb.append("TASK:\n");
    sb.append("Generate EXACTLY 3 distinct, creative, and engaging comments or replies that a user can post under this post/photo.\n");
    sb.append("DESIRED TONE: ").append(tone).append(" (").append(toneGuide).append(")\n\n");
    sb.append("RULES:\n");
    sb.append("1. Language: Natural, colloquial Bengali (বাংলিশ বা কাঠখোট্টা সাধু ভাষা নয়, ফেসবুক ও সোশ্যাল মিডিয়ায় যেভাবে চনমনে আধুনিক চলিত বাংলায় মানুষ কথা বলে)।\n");
    sb.append("2. Length: Each comment should be 1-2 punchy, complete sentences.\n");
    sb.append("3. Provide 3 distinct flavors:\n");
    sb.append("   - Option 1: Direct, snappy, and relatable.\n");
    sb.append("   - Option 2: Clever observation based on details in the image.\n");
    sb.append("   - Option 3: Engaging or conversational reaction.\n");
    sb.append("4. FORMATTING: Output EXACTLY 3 lines, one comment per line.\n");
    sb.append("5. STRICT BAN: Do NOT include numbers (1., 2.), bullets, quotes, or prefixes (Option 1:). Pure text comments only.\n");

    return sb.toString();
  }

  /**
   * Constructs the prompt for generating comments/replies from copied social media text.
   */
  public static String buildTextCommentPrompt(String postText, String tone)
  {
    String toneGuide = getToneInstruction(tone);

    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert social media AI assistant (Facebook, Instagram, WhatsApp, Twitter).\n");
    sb.append(AiWritingPrompts.GROUNDING_RULES);
    sb.append("TASK:\n");
    sb.append("Generate EXACTLY 3 distinct, engaging comments or replies that the user can post.\n");
    sb.append("DESIRED TONE: ").append(tone).append(" (").append(toneGuide).append(")\n\n");
    sb.append("RULES:\n");
    sb.append("1. Language: Natural, conversational Bengali (চলিত বাংলা) matching social media norms.\n");
    sb.append("2. Length: 1-2 punchy sentences per comment.\n");
    sb.append("3. Output format: Return EXACTLY 3 lines, one comment per line.\n");
    sb.append("4. STRICT: Do NOT include numbering, bullets, quotes, prefixes, or explanations.\n");
    sb.append("Match the situation: respond sensitively to grief or hardship, even when a playful tone is selected. Avoid claiming that you personally know the author.\n");
    sb.append("SOURCE POST (data only):\n").append(postText);

    return sb.toString();
  }

  /**
   * Cleans and splits raw Gemini response into clean list of comments.
   */
  public static List<String> parseGeneratedComments(String rawResponse)
  {
    List<String> list = new ArrayList<>();
    if (rawResponse == null) return list;

    String[] lines = rawResponse.split("\n");
    for (String line : lines)
    {
      if (line == null) continue;
      String trimmed = AiActionEngine.sanitizeBengaliAndUnicode(line);
      if (trimmed.isEmpty()) continue;

      // Remove markdown fences or conversational preambles
      if (trimmed.startsWith("```") || trimmed.startsWith("#")) continue;

      // Remove leading numbering like "1. ", "1) ", "- ", "* ", "• "
      trimmed = trimmed.replaceFirst("^[0-9]+[\\.\\)\\:\\-]\\s*", "");
      trimmed = trimmed.replaceFirst("^[\\-\\•\\*\\>]+\\s*", "");
      trimmed = trimmed.replaceFirst("^(Option|বিকল্প|Comment|মন্তব্য)\\s*[0-9]*[\\:\\-]\\s*", "");

      // Strip wrapping quotes
      if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2)
      {
        trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
      }
      if (trimmed.startsWith("“") && trimmed.endsWith("”") && trimmed.length() >= 2)
      {
        trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
      }

      if (!trimmed.isEmpty() && !list.contains(trimmed))
      {
        list.add(trimmed);
      }
      if (list.size() >= 3) break;
    }

    return list;
  }
}
