package juloo.keyboard2.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class AiStyle
{
  private static final String PREF_CUSTOM_STYLES = "pref_custom_ai_styles";

  private final String id;
  private final String name;
  private final String icon;
  private final String instruction;
  private final boolean isCustom;

  public AiStyle(String id, String name, String icon, String instruction, boolean isCustom)
  {
    this.id = id;
    this.name = name;
    this.icon = icon;
    this.instruction = instruction;
    this.isCustom = isCustom;
  }

  public String getId() { return id; }
  public String getName() { return name; }
  public String getIcon() { return icon; }
  public String getInstruction() { return instruction; }
  public boolean isCustom() { return isCustom; }

  public static List<AiStyle> getBuiltInStyles()
  {
    List<AiStyle> list = new ArrayList<>();
    list.add(new AiStyle("short", "Short", "🎯", "Make it very concise, punchy, and short without unnecessary words.", false));
    list.add(new AiStyle("corp", "Corp", "💼", "Rewrite in a professional, confident corporate business tone.", false));
    list.add(new AiStyle("tribal", "Tribal", "🍗", "Rewrite in a tribal, rhythmic, primal, and spirited style.", false));
    list.add(new AiStyle("formal", "Formal", "🤝", "Rewrite with polite, respectful, elegant, and diplomatic formal phrasing.", false));
    list.add(new AiStyle("biblical", "Biblical", "🕯️", "Rewrite in a solemn, majestic, archaic biblical prose style.", false));
    list.add(new AiStyle("viking", "Viking", "🪓", "Write like a fierce, heroic Norse warrior and Viking. Speak of honor, glory, battle, and axes.", false));
    list.add(new AiStyle("zen", "Zen", "🗿", "Rewrite in a calm, mindful, peaceful, and minimalist Zen style.", false));
    list.add(new AiStyle("casual", "Casual", "😊", "Rewrite in a warm, friendly, natural, and conversational casual tone.", false));
    list.add(new AiStyle("enthusiastic", "Enthusiastic", "⚡", "Rewrite with high energy, upbeat vibes, positivity, and enthusiasm.", false));
    return list;
  }

  public static List<AiStyle> getAllStyles(Context context)
  {
    List<AiStyle> all = new ArrayList<>(getBuiltInStyles());
    List<AiStyle> custom = getCustomStyles(context);
    all.addAll(custom);
    return all;
  }

  public static List<AiStyle> getCustomStyles(Context context)
  {
    List<AiStyle> list = new ArrayList<>();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String jsonStr = prefs.getString(PREF_CUSTOM_STYLES, "[]");
    try
    {
      JSONArray arr = new JSONArray(jsonStr);
      for (int i = 0; i < arr.length(); i++)
      {
        JSONObject obj = arr.getJSONObject(i);
        String id = obj.optString("id", UUID.randomUUID().toString());
        String name = obj.optString("name", "Custom");
        String icon = obj.optString("icon", "✨");
        String instruction = obj.optString("instruction", "");
        list.add(new AiStyle(id, name, icon, instruction, true));
      }
    }
    catch (Exception ignored) {}
    return list;
  }

  public static AiStyle addCustomStyle(Context context, String name, String icon, String instruction)
  {
    List<AiStyle> customList = getCustomStyles(context);
    String id = UUID.randomUUID().toString();
    String safeIcon = (icon == null || icon.trim().isEmpty()) ? "✨" : icon.trim();
    AiStyle newStyle = new AiStyle(id, name.trim(), safeIcon, instruction.trim(), true);
    customList.add(newStyle);
    saveCustomStyles(context, customList);
    return newStyle;
  }

  public static void deleteCustomStyle(Context context, String id)
  {
    List<AiStyle> customList = getCustomStyles(context);
    List<AiStyle> updated = new ArrayList<>();
    for (AiStyle s : customList)
    {
      if (!s.getId().equals(id))
      {
        updated.add(s);
      }
    }
    saveCustomStyles(context, updated);
  }

  private static void saveCustomStyles(Context context, List<AiStyle> list)
  {
    try
    {
      JSONArray arr = new JSONArray();
      for (AiStyle s : list)
      {
        JSONObject obj = new JSONObject();
        obj.put("id", s.getId());
        obj.put("name", s.getName());
        obj.put("icon", s.getIcon());
        obj.put("instruction", s.getInstruction());
        arr.put(obj);
      }
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      prefs.edit().putString(PREF_CUSTOM_STYLES, arr.toString()).apply();
    }
    catch (Exception ignored) {}
  }
}
