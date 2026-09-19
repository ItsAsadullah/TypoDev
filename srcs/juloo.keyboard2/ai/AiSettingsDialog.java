package juloo.keyboard2.ai;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import juloo.keyboard2.DialogTheme;
import juloo.keyboard2.Keyboard2;

public class AiSettingsDialog
{
  public static EditText sActiveDialogEditText = null;
  private static AlertDialog sCurrentDialog = null;
  private static boolean sIsShowing = false;
  private static long sLastShowTime = 0;

  public interface OnSettingsSavedListener
  {
    void onSaved();
  }

  public static boolean pasteFromClipboard(Context context, EditText target, String fieldName)
  {
    try
    {
      ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0)
      {
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        CharSequence cs = (item != null) ? item.getText() : null;
        if (cs != null && cs.length() > 0)
        {
          String text = cs.toString().trim();
          target.setText(text);
          target.setSelection(text.length());
          Toast.makeText(context, "✅ " + fieldName + " পেস্ট করা হয়েছে!", Toast.LENGTH_SHORT).show();
          return true;
        }
      }
    }
    catch (Exception ignored) {}
    Toast.makeText(context, "⚠️ ক্লিপবোর্ড খালি! প্রথমে API Key কপি করুন।", Toast.LENGTH_SHORT).show();
    return false;
  }

  public static String getClipboardText(Context context)
  {
    try
    {
      ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0)
      {
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        CharSequence cs = (item != null) ? item.getText() : null;
        if (cs != null)
          return cs.toString().trim();
      }
    }
    catch (Exception ignored) {}
    return "";
  }

  public static void show(final Context context, final Keyboard2 keyboard, final OnSettingsSavedListener listener)
  {
    long now = System.currentTimeMillis();
    if (now - sLastShowTime < 1000)
    {
      return; // Debounce rapid taps
    }
    if (sIsShowing || sCurrentDialog != null)
    {
      try
      {
        if (sCurrentDialog != null && sCurrentDialog.isShowing())
        {
          return;
        }
      }
      catch (Throwable ignored) {}
      if (sIsShowing) return;
    }
    sLastShowTime = now;
    sIsShowing = true;

    if (keyboard != null)
    {
      keyboard.onAiSettingsOpened();
    }

    final float density = context.getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);
    final int pad16 = (int)(16 * density);
    final int pad12 = (int)(12 * density);
    final int pad8 = (int)(8 * density);
    final int pad6 = (int)(6 * density);

    final String clipText = getClipboardText(context);

    ScrollView scrollView = new ScrollView(context);
    scrollView.setVerticalScrollBarEnabled(false);
    scrollView.setHorizontalScrollBarEnabled(false);
    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    scrollView.setBackgroundColor(Color.TRANSPARENT);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(pad16, pad16, pad16, pad16);
    root.setBackgroundColor(Color.TRANSPARENT);
    scrollView.addView(root);

    // Header
    TextView tvTitle = new TextView(context);
    tvTitle.setText("⚙️ AI Provider & API Settings");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    tvTitle.setTypeface(null, Typeface.BOLD);
    tvTitle.setTextColor(palette.textPrimary);
    tvTitle.setPadding(0, 0, 0, pad12);
    root.addView(tvTitle);

    // Provider Selector
    TextView tvProvLabel = new TextView(context);
    tvProvLabel.setText("Active AI Provider:");
    tvProvLabel.setTypeface(null, Typeface.BOLD);
    tvProvLabel.setTextColor(palette.textSecondary);
    tvProvLabel.setPadding(0, pad8, 0, (int)(4 * density));
    root.addView(tvProvLabel);

    final RadioGroup rgProvider = new RadioGroup(context);
    rgProvider.setOrientation(RadioGroup.HORIZONTAL);

    final RadioButton rbGemini = new RadioButton(context);
    rbGemini.setText("Google Gemini");
    DialogTheme.styleRadioButton(rbGemini, palette);
    rgProvider.addView(rbGemini);

    final RadioButton rbOpenAi = new RadioButton(context);
    rbOpenAi.setText("OpenAI / DeepSeek / Groq");
    DialogTheme.styleRadioButton(rbOpenAi, palette);
    rgProvider.addView(rbOpenAi);

    AiProvider.ProviderType currentProvider = AiProvider.Manager.getActiveProviderType(context);
    if (currentProvider == AiProvider.ProviderType.OPENAI_COMPATIBLE)
      rbOpenAi.setChecked(true);
    else
      rbGemini.setChecked(true);

    root.addView(rgProvider);

    // Focus & Click Listeners for routing soft input directly
    final View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener()
    {
      @Override
      public void onFocusChange(View v, boolean hasFocus)
      {
        if (hasFocus && v instanceof EditText)
        {
          sActiveDialogEditText = (EditText)v;
          if (keyboard != null)
          {
            keyboard.notifyDialogEditTextFocused((EditText)v);
          }
        }
      }
    };
    final View.OnClickListener clickListener = new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (v instanceof EditText)
        {
          sActiveDialogEditText = (EditText)v;
          if (keyboard != null)
          {
            keyboard.notifyDialogEditTextFocused((EditText)v);
          }
        }
      }
    };

    // ==========================================
    // Gemini Container
    // ==========================================
    final LinearLayout llGemini = new LinearLayout(context);
    llGemini.setOrientation(LinearLayout.VERTICAL);
    llGemini.setPadding(0, pad8, 0, pad8);

    TextView tvGeminiKeyLabel = new TextView(context);
    tvGeminiKeyLabel.setText("Gemini API Key:");
    tvGeminiKeyLabel.setTypeface(null, Typeface.BOLD);
    tvGeminiKeyLabel.setTextColor(palette.textSecondary);
    llGemini.addView(tvGeminiKeyLabel);

    // Initial Gemini Key resolution (Check saved, or auto-detect from clipboard)
    String savedGeminiKey = GeminiAiService.getApiKey(context);
    if (savedGeminiKey.isEmpty() && (clipText.startsWith("AIza") || clipText.length() >= 25))
    {
      savedGeminiKey = clipText;
      Toast.makeText(context, "📋 ক্লিপবোর্ড থেকে আপনার Gemini Key স্বয়ংক্রিয়ভাবে বসানো হয়েছে!", Toast.LENGTH_SHORT).show();
    }

    // Input row with EditText, small Paste button, Eye Toggle, and Clear Button
    LinearLayout rowGeminiInput = new LinearLayout(context);
    rowGeminiInput.setOrientation(LinearLayout.HORIZONTAL);
    rowGeminiInput.setGravity(Gravity.CENTER_VERTICAL);

    final EditText etGeminiKey = new EditText(context);
    etGeminiKey.setHint("Paste Gemini AI Studio key here");
    etGeminiKey.setText(savedGeminiKey);
    etGeminiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    etGeminiKey.setTransformationMethod(new PasswordTransformationMethod());
    DialogTheme.styleEditText(etGeminiKey, palette, density);
    LinearLayout.LayoutParams lpGeminiEt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    etGeminiKey.setLayoutParams(lpGeminiEt);
    etGeminiKey.setOnFocusChangeListener(focusListener);
    etGeminiKey.setOnClickListener(clickListener);
    etGeminiKey.setOnLongClickListener(new View.OnLongClickListener()
    {
      @Override
      public boolean onLongClick(View v)
      {
        pasteFromClipboard(context, etGeminiKey, "Gemini API Key");
        return true;
      }
    });
    rowGeminiInput.addView(etGeminiKey);

    // Small Paste icon button (📋) right next to the API key input
    final TextView btnPasteGemini = new TextView(context);
    btnPasteGemini.setText("📋");
    btnPasteGemini.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnPasteGemini.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnPasteGemini, palette, density);
    LinearLayout.LayoutParams lpPasteGemini = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpPasteGemini.setMargins((int)(4 * density), 0, 0, 0);
    btnPasteGemini.setLayoutParams(lpPasteGemini);
    btnPasteGemini.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        pasteFromClipboard(context, etGeminiKey, "Gemini API Key");
      }
    });
    rowGeminiInput.addView(btnPasteGemini);

    // Eye toggle button (👁️ / 🙈)
    final TextView btnEyeGemini = new TextView(context);
    btnEyeGemini.setText("👁️");
    btnEyeGemini.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnEyeGemini.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnEyeGemini, palette, density);
    LinearLayout.LayoutParams lpEye = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpEye.setMargins((int)(4 * density), 0, 0, 0);
    btnEyeGemini.setLayoutParams(lpEye);
    btnEyeGemini.setOnClickListener(new View.OnClickListener()
    {
      boolean isVisible = false;
      @Override
      public void onClick(View v)
      {
        isVisible = !isVisible;
        etGeminiKey.setTransformationMethod(isVisible ? null : new PasswordTransformationMethod());
        btnEyeGemini.setText(isVisible ? "🙈" : "👁️");
        etGeminiKey.setSelection(etGeminiKey.getText().length());
      }
    });
    rowGeminiInput.addView(btnEyeGemini);

    // Clear button (❌)
    final TextView btnClearGemini = new TextView(context);
    btnClearGemini.setText("❌");
    btnClearGemini.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    btnClearGemini.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnClearGemini, palette, density);
    LinearLayout.LayoutParams lpClear = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpClear.setMargins((int)(4 * density), 0, 0, 0);
    btnClearGemini.setLayoutParams(lpClear);
    btnClearGemini.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        etGeminiKey.setText("");
        Toast.makeText(context, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
      }
    });
    rowGeminiInput.addView(btnClearGemini);

    llGemini.addView(rowGeminiInput);

    // "Get Free Key" button
    Button btnGetGemini = new Button(context);
    btnGetGemini.setText("🔑 Get Free Gemini API Key (AI Studio)");
    btnGetGemini.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    DialogTheme.styleSecondaryButton(btnGetGemini, palette, density);
    LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpBtn.setMargins(0, pad8, 0, pad8);
    btnGetGemini.setLayoutParams(lpBtn);
    btnGetGemini.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try
        {
          Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(i);
        }
        catch (Exception e)
        {
          Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show();
        }
      }
    });
    llGemini.addView(btnGetGemini);

    TextView tvGeminiModelLabel = new TextView(context);
    tvGeminiModelLabel.setText("Gemini Model:");
    tvGeminiModelLabel.setTypeface(null, Typeface.BOLD);
    tvGeminiModelLabel.setTextColor(palette.textSecondary);
    tvGeminiModelLabel.setPadding(0, pad6, 0, (int)(4 * density));
    llGemini.addView(tvGeminiModelLabel);

    final String[] geminiModels = {
      "gemini-2.0-flash",
      "gemini-2.0-flash-lite",
      "gemini-1.5-flash",
      "gemini-1.5-flash-8b",
      "gemini-1.5-pro",
      "gemini-2.0-pro-exp-02-05"
    };
    final String[] currentSelectedGeminiModel = new String[]{GeminiAiService.getModel(context)};
    final Spinner spinnerGemini = new Spinner(context);
    ArrayAdapter<String> geminiAdapter = DialogTheme.createThemedAdapter(context, geminiModels, palette, density);
    spinnerGemini.setAdapter(geminiAdapter);
    DialogTheme.styleSpinner(spinnerGemini, palette, density);
    int geminiIndex = 0;
    for (int i = 0; i < geminiModels.length; i++)
    {
      if (geminiModels[i].equalsIgnoreCase(currentSelectedGeminiModel[0]))
      {
        geminiIndex = i;
        break;
      }
    }
    spinnerGemini.setSelection(geminiIndex);
    spinnerGemini.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
    {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
      {
        currentSelectedGeminiModel[0] = geminiModels[position];
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
    });
    llGemini.addView(spinnerGemini);
    root.addView(llGemini);

    // ==========================================
    // OpenAI Compatible Container
    // ==========================================
    final LinearLayout llOpenAi = new LinearLayout(context);
    llOpenAi.setOrientation(LinearLayout.VERTICAL);
    llOpenAi.setPadding(0, pad8, 0, pad8);

    TextView tvOpenAiBaseLabel = new TextView(context);
    tvOpenAiBaseLabel.setText("Base URL (OpenAI / DeepSeek / Groq / Ollama):");
    tvOpenAiBaseLabel.setTypeface(null, Typeface.BOLD);
    tvOpenAiBaseLabel.setTextColor(palette.textSecondary);
    llOpenAi.addView(tvOpenAiBaseLabel);

    LinearLayout rowBaseUrl = new LinearLayout(context);
    rowBaseUrl.setOrientation(LinearLayout.HORIZONTAL);
    rowBaseUrl.setGravity(Gravity.CENTER_VERTICAL);

    final EditText etBaseUrl = new EditText(context);
    etBaseUrl.setHint("https://api.openai.com/v1");
    etBaseUrl.setText(OpenAiCompatibleProvider.getBaseUrl(context));
    DialogTheme.styleEditText(etBaseUrl, palette, density);
    LinearLayout.LayoutParams lpBaseUrl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    etBaseUrl.setLayoutParams(lpBaseUrl);
    etBaseUrl.setOnFocusChangeListener(focusListener);
    etBaseUrl.setOnClickListener(clickListener);
    rowBaseUrl.addView(etBaseUrl);

    final TextView btnPasteUrl = new TextView(context);
    btnPasteUrl.setText("📋");
    btnPasteUrl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnPasteUrl.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnPasteUrl, palette, density);
    LinearLayout.LayoutParams lpPasteUrl = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpPasteUrl.setMargins((int)(4 * density), 0, 0, 0);
    btnPasteUrl.setLayoutParams(lpPasteUrl);
    btnPasteUrl.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        pasteFromClipboard(context, etBaseUrl, "Base URL");
      }
    });
    rowBaseUrl.addView(btnPasteUrl);
    llOpenAi.addView(rowBaseUrl);

    // Preset quick chips
    HorizontalScrollView scrollPresets = new HorizontalScrollView(context);
    scrollPresets.setHorizontalScrollBarEnabled(false);
    scrollPresets.setVerticalScrollBarEnabled(false);
    scrollPresets.setOverScrollMode(View.OVER_SCROLL_NEVER);
    LinearLayout rowPresets = new LinearLayout(context);
    rowPresets.setOrientation(LinearLayout.HORIZONTAL);
    scrollPresets.addView(rowPresets);

    final EditText etOpenAiModel = new EditText(context);
    etOpenAiModel.setHint("Model (e.g. gpt-4o-mini, deepseek-chat)");
    etOpenAiModel.setText(OpenAiCompatibleProvider.getModel(context));
    DialogTheme.styleEditText(etOpenAiModel, palette, density);
    etOpenAiModel.setOnFocusChangeListener(focusListener);
    etOpenAiModel.setOnClickListener(clickListener);

    addPresetChip(context, rowPresets, "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", etBaseUrl, etOpenAiModel, palette);
    addPresetChip(context, rowPresets, "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", etBaseUrl, etOpenAiModel, palette);
    addPresetChip(context, rowPresets, "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", etBaseUrl, etOpenAiModel, palette);
    addPresetChip(context, rowPresets, "OpenRouter", "https://openrouter.ai/api/v1", "deepseek/deepseek-chat", etBaseUrl, etOpenAiModel, palette);
    addPresetChip(context, rowPresets, "Ollama Local", "http://10.0.2.2:11434/v1", "llama3.2", etBaseUrl, etOpenAiModel, palette);
    llOpenAi.addView(scrollPresets);

    TextView tvOpenAiKeyLabel = new TextView(context);
    tvOpenAiKeyLabel.setText("API Key:");
    tvOpenAiKeyLabel.setTypeface(null, Typeface.BOLD);
    tvOpenAiKeyLabel.setTextColor(palette.textSecondary);
    tvOpenAiKeyLabel.setPadding(0, pad8, 0, (int)(4 * density));
    llOpenAi.addView(tvOpenAiKeyLabel);

    String savedOpenAiKey = OpenAiCompatibleProvider.getApiKey(context);
    if (savedOpenAiKey.isEmpty() && clipText.startsWith("sk-"))
    {
      savedOpenAiKey = clipText;
      Toast.makeText(context, "📋 ক্লিপবোর্ড থেকে OpenAI API Key স্বয়ংক্রিয়ভাবে বসানো হয়েছে!", Toast.LENGTH_SHORT).show();
    }

    LinearLayout rowOpenAiInput = new LinearLayout(context);
    rowOpenAiInput.setOrientation(LinearLayout.HORIZONTAL);
    rowOpenAiInput.setGravity(Gravity.CENTER_VERTICAL);

    final EditText etOpenAiKey = new EditText(context);
    etOpenAiKey.setHint("sk-...");
    etOpenAiKey.setText(savedOpenAiKey);
    etOpenAiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    etOpenAiKey.setTransformationMethod(new PasswordTransformationMethod());
    DialogTheme.styleEditText(etOpenAiKey, palette, density);
    LinearLayout.LayoutParams lpOpenAiEt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    etOpenAiKey.setLayoutParams(lpOpenAiEt);
    etOpenAiKey.setOnFocusChangeListener(focusListener);
    etOpenAiKey.setOnClickListener(clickListener);
    etOpenAiKey.setOnLongClickListener(new View.OnLongClickListener()
    {
      @Override
      public boolean onLongClick(View v)
      {
        pasteFromClipboard(context, etOpenAiKey, "API Key");
        return true;
      }
    });
    rowOpenAiInput.addView(etOpenAiKey);

    // Small Paste icon button (📋) right next to OpenAI API Key
    final TextView btnPasteOpenAi = new TextView(context);
    btnPasteOpenAi.setText("📋");
    btnPasteOpenAi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnPasteOpenAi.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnPasteOpenAi, palette, density);
    LinearLayout.LayoutParams lpPasteOpenAi = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpPasteOpenAi.setMargins((int)(4 * density), 0, 0, 0);
    btnPasteOpenAi.setLayoutParams(lpPasteOpenAi);
    btnPasteOpenAi.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        pasteFromClipboard(context, etOpenAiKey, "API Key");
      }
    });
    rowOpenAiInput.addView(btnPasteOpenAi);

    final TextView btnEyeOpenAi = new TextView(context);
    btnEyeOpenAi.setText("👁️");
    btnEyeOpenAi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnEyeOpenAi.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnEyeOpenAi, palette, density);
    LinearLayout.LayoutParams lpEyeOpenAi = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpEyeOpenAi.setMargins((int)(4 * density), 0, 0, 0);
    btnEyeOpenAi.setLayoutParams(lpEyeOpenAi);
    btnEyeOpenAi.setOnClickListener(new View.OnClickListener()
    {
      boolean isVisible = false;
      @Override
      public void onClick(View v)
      {
        isVisible = !isVisible;
        etOpenAiKey.setTransformationMethod(isVisible ? null : new PasswordTransformationMethod());
        btnEyeOpenAi.setText(isVisible ? "🙈" : "👁️");
        etOpenAiKey.setSelection(etOpenAiKey.getText().length());
      }
    });
    rowOpenAiInput.addView(btnEyeOpenAi);

    final TextView btnClearOpenAi = new TextView(context);
    btnClearOpenAi.setText("❌");
    btnClearOpenAi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    btnClearOpenAi.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnClearOpenAi, palette, density);
    LinearLayout.LayoutParams lpClearOpenAi = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpClearOpenAi.setMargins((int)(4 * density), 0, 0, 0);
    btnClearOpenAi.setLayoutParams(lpClearOpenAi);
    btnClearOpenAi.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        etOpenAiKey.setText("");
        Toast.makeText(context, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
      }
    });
    rowOpenAiInput.addView(btnClearOpenAi);

    llOpenAi.addView(rowOpenAiInput);

    TextView tvOpenAiModelLabel = new TextView(context);
    tvOpenAiModelLabel.setText("Model Name:");
    tvOpenAiModelLabel.setTypeface(null, Typeface.BOLD);
    tvOpenAiModelLabel.setTextColor(palette.textSecondary);
    tvOpenAiModelLabel.setPadding(0, pad8, 0, (int)(4 * density));
    llOpenAi.addView(tvOpenAiModelLabel);

    LinearLayout rowModel = new LinearLayout(context);
    rowModel.setOrientation(LinearLayout.HORIZONTAL);
    rowModel.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams lpModelEt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    etOpenAiModel.setLayoutParams(lpModelEt);
    rowModel.addView(etOpenAiModel);

    final TextView btnPasteModel = new TextView(context);
    btnPasteModel.setText("📋");
    btnPasteModel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnPasteModel.setGravity(Gravity.CENTER);
    DialogTheme.styleIconButton(btnPasteModel, palette, density);
    LinearLayout.LayoutParams lpPasteModel = new LinearLayout.LayoutParams((int)(38 * density), (int)(38 * density));
    lpPasteModel.setMargins((int)(4 * density), 0, 0, 0);
    btnPasteModel.setLayoutParams(lpPasteModel);
    btnPasteModel.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        pasteFromClipboard(context, etOpenAiModel, "Model Name");
      }
    });
    rowModel.addView(btnPasteModel);
    llOpenAi.addView(rowModel);

    final String[] popularOpenAiModels = {
      "gpt-4o-mini",
      "gpt-4o",
      "deepseek-chat",
      "deepseek-reasoner",
      "llama-3.3-70b-versatile",
      "claude-3-5-sonnet-20241022",
      "gemini-2.0-flash"
    };
    String[] spinnerOptions = new String[popularOpenAiModels.length + 1];
    spinnerOptions[0] = "-- Choose to autofill popular model --";
    System.arraycopy(popularOpenAiModels, 0, spinnerOptions, 1, popularOpenAiModels.length);

    final Spinner spinnerOpenAi = new Spinner(context);
    ArrayAdapter<String> openAiAdapter = DialogTheme.createThemedAdapter(context, spinnerOptions, palette, density);
    spinnerOpenAi.setAdapter(openAiAdapter);
    DialogTheme.styleSpinner(spinnerOpenAi, palette, density);
    spinnerOpenAi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
    {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
      {
        if (position > 0)
        {
          etOpenAiModel.setText(popularOpenAiModels[position - 1]);
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
    });
    llOpenAi.addView(spinnerOpenAi);

    root.addView(llOpenAi);

    // Initial Active EditText
    final EditText initialEt = (currentProvider == AiProvider.ProviderType.OPENAI_COMPATIBLE) ? etOpenAiKey : etGeminiKey;
    sActiveDialogEditText = initialEt;
    if (keyboard != null)
    {
      keyboard.notifyDialogEditTextFocused(initialEt);
    }

    // Toggle container visibility
    Runnable updateVisibility = new Runnable()
    {
      @Override
      public void run()
      {
        boolean isOpenAi = rbOpenAi.isChecked();
        llOpenAi.setVisibility(isOpenAi ? View.VISIBLE : View.GONE);
        llGemini.setVisibility(isOpenAi ? View.GONE : View.VISIBLE);
      }
    };
    updateVisibility.run();

    rgProvider.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
    {
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId)
      {
        boolean isOpenAi = rbOpenAi.isChecked();
        llOpenAi.setVisibility(isOpenAi ? View.VISIBLE : View.GONE);
        llGemini.setVisibility(isOpenAi ? View.GONE : View.VISIBLE);
      }
    });

    // Emoji Preference Switch / Checkbox
    TextView tvEmojiLabel = new TextView(context);
    tvEmojiLabel.setText("Emoji Generation:");
    tvEmojiLabel.setTypeface(null, Typeface.BOLD);
    tvEmojiLabel.setTextColor(palette.textSecondary);
    tvEmojiLabel.setPadding(0, pad12, 0, (int)(4 * density));
    root.addView(tvEmojiLabel);

    final android.widget.CheckBox cbGlobalEmojify = new android.widget.CheckBox(context);
    cbGlobalEmojify.setText("Generate responses with emojis");
    cbGlobalEmojify.setChecked(GeminiAiService.isEmojifyEnabled(context));
    DialogTheme.styleCheckBox(cbGlobalEmojify, palette);
    root.addView(cbGlobalEmojify);

    // Open In Full System Settings Button
    Button btnOpenFullSettings = new Button(context);
    btnOpenFullSettings.setText("⚙️ Open in Full Settings (সেটিংসে খুলুন)");
    btnOpenFullSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    DialogTheme.styleSecondaryButton(btnOpenFullSettings, palette, density);
    LinearLayout.LayoutParams lpSettings = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpSettings.setMargins(0, pad12, 0, 0);
    btnOpenFullSettings.setLayoutParams(lpSettings);

    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setView(scrollView)
        .setPositiveButton("Save Settings", null)
        .setNegativeButton("Cancel", null)
        .create();

    dialog.setCanceledOnTouchOutside(false);
    sCurrentDialog = dialog;

    btnOpenFullSettings.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        dialog.dismiss();
        Intent intent = new Intent(context, juloo.keyboard2.SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
      }
    });
    root.addView(btnOpenFullSettings);

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);

    dialog.setOnShowListener(new DialogInterface.OnShowListener()
    {
      @Override
      public void onShow(DialogInterface d)
      {
        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btnSave != null)
        {
          btnSave.setTextColor(palette.accentColor);
          btnSave.setTypeface(null, Typeface.BOLD);
          btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
        Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (btnCancel != null)
        {
          btnCancel.setTextColor(palette.textSecondary);
          btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }

        if (btnSave != null)
        {
          btnSave.setOnClickListener(new View.OnClickListener()
          {
            @Override
            public void onClick(View v)
            {
              boolean isOpenAi = rbOpenAi.isChecked();
              AiProvider.Manager.setActiveProviderType(context, isOpenAi ? AiProvider.ProviderType.OPENAI_COMPATIBLE : AiProvider.ProviderType.GEMINI);

              // Save Gemini
              GeminiAiService.setApiKey(context, etGeminiKey.getText().toString().trim());
              GeminiAiService.setModel(context, currentSelectedGeminiModel[0]);

              // Save OpenAI
              OpenAiCompatibleProvider.setBaseUrl(context, etBaseUrl.getText().toString().trim());
              OpenAiCompatibleProvider.setApiKey(context, etOpenAiKey.getText().toString().trim());
              OpenAiCompatibleProvider.setModel(context, etOpenAiModel.getText().toString().trim());

              // Save Emojify preference
              GeminiAiService.setEmojifyEnabled(context, cbGlobalEmojify.isChecked());

              Toast.makeText(context, "✅ AI settings saved successfully", Toast.LENGTH_SHORT).show();
              dialog.dismiss();
              if (listener != null) listener.onSaved();
            }
          });
        }
      }
    });

    dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
    {
      @Override
      public void onDismiss(DialogInterface d)
      {
        sCurrentDialog = null;
        sIsShowing = false;
        if (keyboard != null)
        {
          keyboard.onAiSettingsClosed();
        }
      }
    });

    dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
    {
      @Override
      public void onCancel(DialogInterface d)
      {
        sCurrentDialog = null;
        sIsShowing = false;
      }
    });

    // Configure window attributes: attach to IME and set FLAG_ALT_FOCUSABLE_IM so IME does not restart or flicker/close the dialog
    Window win = dialog.getWindow();
    if (win != null)
    {
      if (keyboard != null && keyboard.getWindow() != null && keyboard.getWindow().getWindow() != null)
      {
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.token = keyboard.getWindow().getWindow().getDecorView().getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        win.setAttributes(lp);
        win.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
      }
    }

    try
    {
      dialog.show();
    }
    catch (Throwable t)
    {
      juloo.keyboard2.Logs.print_exception(t);
      sCurrentDialog = null;
      sIsShowing = false;
    }
  }

  private static void addPresetChip(
      final Context context,
      LinearLayout container,
      final String label,
      final String url,
      final String model,
      final EditText etUrl,
      final EditText etModel,
      final DialogTheme.Palette palette)
  {
    final float density = context.getResources().getDisplayMetrics().density;
    Button chip = new Button(context);
    chip.setText(label);
    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    chip.setTextColor(palette.textPrimary);
    chip.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, (int)(32 * density));
    lp.setMargins((int)(2 * density), (int)(4 * density), (int)(4 * density), (int)(4 * density));
    chip.setLayoutParams(lp);

    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(16 * density);
    bg.setColor(palette.inputBg);
    bg.setStroke((int)(1 * density), palette.inputBorder);
    chip.setBackground(bg);

    chip.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        etUrl.setText(url);
        etModel.setText(model);
        Toast.makeText(context, label + " preset applied", Toast.LENGTH_SHORT).show();
      }
    });

    container.addView(chip);
  }
}
