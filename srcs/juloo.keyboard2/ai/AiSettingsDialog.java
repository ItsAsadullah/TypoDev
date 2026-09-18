package juloo.keyboard2.ai;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Utils;

public class AiSettingsDialog
{
  public interface OnSettingsSavedListener
  {
    void onSaved();
  }

  public static void show(final Context context, final Keyboard2 keyboard, final OnSettingsSavedListener listener)
  {
    final float density = context.getResources().getDisplayMetrics().density;
    final int pad16 = (int)(16 * density);
    final int pad12 = (int)(12 * density);
    final int pad8 = (int)(8 * density);

    ScrollView scrollView = new ScrollView(context);
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(pad16, pad16, pad16, pad16);
    scrollView.addView(root);

    // Header
    TextView tvTitle = new TextView(context);
    tvTitle.setText("⚙️ AI Provider & API Settings");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
    tvTitle.setPadding(0, 0, 0, pad12);
    root.addView(tvTitle);

    // Provider Selector
    TextView tvProvLabel = new TextView(context);
    tvProvLabel.setText("Active AI Provider:");
    tvProvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
    tvProvLabel.setPadding(0, pad8, 0, (int)(4 * density));
    root.addView(tvProvLabel);

    final RadioGroup rgProvider = new RadioGroup(context);
    rgProvider.setOrientation(RadioGroup.HORIZONTAL);

    final RadioButton rbGemini = new RadioButton(context);
    rbGemini.setText("Google Gemini");
    rgProvider.addView(rbGemini);

    final RadioButton rbOpenAi = new RadioButton(context);
    rbOpenAi.setText("OpenAI / DeepSeek / Groq");
    rgProvider.addView(rbOpenAi);

    AiProvider.ProviderType currentProvider = AiProvider.Manager.getActiveProviderType(context);
    if (currentProvider == AiProvider.ProviderType.OPENAI_COMPATIBLE)
      rbOpenAi.setChecked(true);
    else
      rbGemini.setChecked(true);

    root.addView(rgProvider);

    // Gemini Container
    final LinearLayout llGemini = new LinearLayout(context);
    llGemini.setOrientation(LinearLayout.VERTICAL);
    llGemini.setPadding(0, pad8, 0, pad8);

    TextView tvGeminiKeyLabel = new TextView(context);
    tvGeminiKeyLabel.setText("Gemini API Key:");
    llGemini.addView(tvGeminiKeyLabel);

    final EditText etGeminiKey = new EditText(context);
    etGeminiKey.setHint("Paste Gemini AI Studio key here");
    etGeminiKey.setText(GeminiAiService.getApiKey(context));
    etGeminiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    llGemini.addView(etGeminiKey);

    Button btnGetGemini = new Button(context);
    btnGetGemini.setText("🔑 Get Free Gemini API Key (AI Studio)");
    btnGetGemini.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
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
    tvGeminiModelLabel.setPadding(0, pad8, 0, (int)(2 * density));
    llGemini.addView(tvGeminiModelLabel);

    final String[] geminiModels = {
      "gemini-2.0-flash",
      "gemini-2.0-flash-lite",
      "gemini-1.5-flash",
      "gemini-1.5-flash-8b",
      "gemini-1.5-pro",
      "gemini-2.0-pro-exp-02-05"
    };
    final Button btnGeminiModel = new Button(context);
    btnGeminiModel.setText("Model: " + GeminiAiService.getModel(context));
    final String[] currentSelectedGeminiModel = new String[]{GeminiAiService.getModel(context)};
    btnGeminiModel.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        int selectedIndex = 0;
        for (int i = 0; i < geminiModels.length; i++)
        {
          if (geminiModels[i].equalsIgnoreCase(currentSelectedGeminiModel[0]))
          {
            selectedIndex = i;
            break;
          }
        }
        AlertDialog modelDialog = new AlertDialog.Builder(context)
            .setTitle("Select Gemini Model")
            .setSingleChoiceItems(geminiModels, selectedIndex, new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface d, int which)
              {
                currentSelectedGeminiModel[0] = geminiModels[which];
                btnGeminiModel.setText("Model: " + currentSelectedGeminiModel[0]);
                d.dismiss();
              }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        if (keyboard != null)
          Utils.show_dialog_on_ime(modelDialog, keyboard);
        else
          modelDialog.show();
      }
    });
    llGemini.addView(btnGeminiModel);
    root.addView(llGemini);

    // OpenAI Container
    final LinearLayout llOpenAi = new LinearLayout(context);
    llOpenAi.setOrientation(LinearLayout.VERTICAL);
    llOpenAi.setPadding(0, pad8, 0, pad8);

    TextView tvOpenAiBaseLabel = new TextView(context);
    tvOpenAiBaseLabel.setText("Base URL (OpenAI / DeepSeek / Groq / Ollama):");
    llOpenAi.addView(tvOpenAiBaseLabel);

    final EditText etBaseUrl = new EditText(context);
    etBaseUrl.setHint("https://api.openai.com/v1");
    etBaseUrl.setText(OpenAiCompatibleProvider.getBaseUrl(context));
    llOpenAi.addView(etBaseUrl);

    // Preset quick chips
    HorizontalScrollView scrollPresets = new HorizontalScrollView(context);
    scrollPresets.setHorizontalScrollBarEnabled(false);
    LinearLayout rowPresets = new LinearLayout(context);
    rowPresets.setOrientation(LinearLayout.HORIZONTAL);
    scrollPresets.addView(rowPresets);

    final EditText etOpenAiModel = new EditText(context);
    etOpenAiModel.setHint("Model (e.g. gpt-4o-mini, deepseek-chat)");
    etOpenAiModel.setText(OpenAiCompatibleProvider.getModel(context));

    addPresetChip(context, rowPresets, "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", etBaseUrl, etOpenAiModel);
    addPresetChip(context, rowPresets, "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", etBaseUrl, etOpenAiModel);
    addPresetChip(context, rowPresets, "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat", etBaseUrl, etOpenAiModel);
    addPresetChip(context, rowPresets, "OpenRouter", "https://openrouter.ai/api/v1", "deepseek/deepseek-chat", etBaseUrl, etOpenAiModel);
    addPresetChip(context, rowPresets, "Ollama Local", "http://10.0.2.2:11434/v1", "llama3.2", etBaseUrl, etOpenAiModel);
    llOpenAi.addView(scrollPresets);

    TextView tvOpenAiKeyLabel = new TextView(context);
    tvOpenAiKeyLabel.setText("API Key:");
    tvOpenAiKeyLabel.setPadding(0, pad8, 0, 0);
    llOpenAi.addView(tvOpenAiKeyLabel);

    final EditText etOpenAiKey = new EditText(context);
    etOpenAiKey.setHint("sk-...");
    etOpenAiKey.setText(OpenAiCompatibleProvider.getApiKey(context));
    etOpenAiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    llOpenAi.addView(etOpenAiKey);

    TextView tvOpenAiModelLabel = new TextView(context);
    tvOpenAiModelLabel.setText("Model Name:");
    tvOpenAiModelLabel.setPadding(0, pad8, 0, 0);
    llOpenAi.addView(tvOpenAiModelLabel);
    llOpenAi.addView(etOpenAiModel);

    final String[] popularOpenAiModels = {
      "gpt-4o-mini",
      "gpt-4o",
      "deepseek-chat",
      "deepseek-reasoner",
      "llama-3.3-70b-versatile",
      "claude-3-5-sonnet-20241022",
      "gemini-2.0-flash"
    };
    Button btnPickOpenAiModel = new Button(context);
    btnPickOpenAiModel.setText("📋 Pick Model from List");
    btnPickOpenAiModel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    btnPickOpenAiModel.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        AlertDialog modelDialog = new AlertDialog.Builder(context)
            .setTitle("Select Model")
            .setItems(popularOpenAiModels, new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface d, int which)
              {
                etOpenAiModel.setText(popularOpenAiModels[which]);
              }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        if (keyboard != null)
          Utils.show_dialog_on_ime(modelDialog, keyboard);
        else
          modelDialog.show();
      }
    });
    llOpenAi.addView(btnPickOpenAiModel);

    root.addView(llOpenAi);

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
    tvEmojiLabel.setTypeface(null, android.graphics.Typeface.BOLD);
    tvEmojiLabel.setPadding(0, pad12, 0, (int)(4 * density));
    root.addView(tvEmojiLabel);

    final android.widget.CheckBox cbGlobalEmojify = new android.widget.CheckBox(context);
    cbGlobalEmojify.setText("Generate responses with emojis (ইমুজি ব্যবহার)");
    cbGlobalEmojify.setChecked(GeminiAiService.isEmojifyEnabled(context));
    root.addView(cbGlobalEmojify);

    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setView(scrollView)
        .setPositiveButton("Save Settings", null)
        .setNegativeButton("Cancel", null)
        .create();

    dialog.setOnShowListener(new DialogInterface.OnShowListener()
    {
      @Override
      public void onShow(DialogInterface d)
      {
        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
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
    });

    Utils.show_dialog_on_ime(dialog, keyboard);
  }

  private static void addPresetChip(
      final Context context,
      LinearLayout container,
      final String label,
      final String url,
      final String model,
      final EditText etUrl,
      final EditText etModel)
  {
    final float density = context.getResources().getDisplayMetrics().density;
    Button chip = new Button(context);
    chip.setText(label);
    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    chip.setPadding((int)(8 * density), 0, (int)(8 * density), 0);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, (int)(32 * density));
    lp.setMargins((int)(2 * density), (int)(4 * density), (int)(4 * density), (int)(4 * density));
    chip.setLayoutParams(lp);

    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(16 * density);
    bg.setColor(Color.parseColor("#22888888"));
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
