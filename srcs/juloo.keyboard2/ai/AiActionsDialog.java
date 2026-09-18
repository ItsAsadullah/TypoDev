package juloo.keyboard2.ai;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Utils;

public class AiActionsDialog
{
  public static void show(final Context context, final Keyboard2 keyboard)
  {
    if (!AiProvider.Manager.hasConfiguredApiKey(context))
    {
      AlertDialog setupDialog = new AlertDialog.Builder(context)
          .setTitle("✨ AI Assistant Setup")
          .setMessage("No AI API key is configured yet. Would you like to configure your Gemini or OpenAI API key now?")
          .setPositiveButton("Configure Key", new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface d, int which)
            {
              AiSettingsDialog.show(context, keyboard, new AiSettingsDialog.OnSettingsSavedListener()
              {
                @Override
                public void onSaved()
                {
                  show(context, keyboard);
                }
              });
            }
          })
          .setNegativeButton("Cancel", null)
          .create();
      Utils.show_dialog_on_ime(setupDialog, keyboard);
      return;
    }

    final InputConnection ic = keyboard.getCurrentInputConnection();
    String textFromInput = "";
    boolean hasSelection = false;
    if (ic != null)
    {
      CharSequence sel = ic.getSelectedText(0);
      if (sel != null && sel.length() > 0)
      {
        textFromInput = sel.toString();
        hasSelection = true;
      }
      else
      {
        CharSequence before = ic.getTextBeforeCursor(1500, 0);
        if (before != null && before.length() > 0)
        {
          textFromInput = before.toString();
        }
      }
    }

    String clipboardText = "";
    try
    {
      ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.hasPrimaryClip())
      {
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        if (item != null && item.getText() != null)
        {
          clipboardText = item.getText().toString().trim();
        }
      }
    }
    catch (Exception ignored) {}

    new Session(context, keyboard, textFromInput, clipboardText, hasSelection).open();
  }

  private static class Session
  {
    private final Context context;
    private final Keyboard2 keyboard;
    private final String inputFieldText;
    private final String clipboardText;
    private final boolean hasSelection;
    private final float density;

    private String currentWorkingText;
    private boolean usingClipboard = false;

    private AiActionEngine.Category activeCategory = AiActionEngine.Category.REWRITE;
    private String activeOptionId = "rephrase";
    private String activeTone = "default";
    private String lastGeneratedResult = "";
    private boolean isGenerating = false;

    private AlertDialog dialog;
    private TextView tvSourceBadge;
    private TextView tvInputSnippet;
    private LinearLayout rowQuickSuggestions;
    private HorizontalScrollView scrollQuick;
    private LinearLayout rowCategories;
    private LinearLayout rowSubOptions;
    private HorizontalScrollView scrollSubOptions;
    private LinearLayout rowToneChips;
    private EditText etCustomAsk;
    private Button btnCustomSend;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private EditText etResultPreview;
    private LinearLayout rowActionButtons;

    public Session(Context context, Keyboard2 keyboard, String inputFieldText, String clipboardText, boolean hasSelection)
    {
      this.context = context;
      this.keyboard = keyboard;
      this.inputFieldText = inputFieldText != null ? inputFieldText.trim() : "";
      this.clipboardText = clipboardText != null ? clipboardText.trim() : "";
      this.hasSelection = hasSelection;
      this.density = context.getResources().getDisplayMetrics().density;

      // Determine initial context source:
      if (this.inputFieldText.isEmpty() && !this.clipboardText.isEmpty())
      {
        this.currentWorkingText = this.clipboardText;
        this.usingClipboard = true;
      }
      else
      {
        this.currentWorkingText = this.inputFieldText;
        this.usingClipboard = false;
      }
    }

    public void open()
    {
      ScrollView rootScrollView = new ScrollView(context);
      rootScrollView.setFillViewport(true);

      LinearLayout root = new LinearLayout(context);
      root.setOrientation(LinearLayout.VERTICAL);
      int p12 = (int)(12 * density);
      int p8 = (int)(8 * density);
      root.setPadding(p12, p12, p12, p12);
      rootScrollView.addView(root);

      // 1. Header Bar: Title, Provider Badge, Settings, Close
      LinearLayout headerBar = new LinearLayout(context);
      headerBar.setOrientation(LinearLayout.HORIZONTAL);
      headerBar.setGravity(Gravity.CENTER_VERTICAL);

      TextView tvTitle = new TextView(context);
      tvTitle.setText("✨ AI Actions");
      tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
      tvTitle.setTypeface(null, Typeface.BOLD);
      headerBar.addView(tvTitle);

      AiProvider provider = AiProvider.Manager.getActiveProvider(context);
      TextView tvProviderBadge = new TextView(context);
      tvProviderBadge.setText(" " + provider.getName() + " (" + provider.getActiveModel(context) + ") ");
      tvProviderBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      tvProviderBadge.setPadding(p8, (int)(2 * density), p8, (int)(2 * density));
      GradientDrawable badgeBg = new GradientDrawable();
      badgeBg.setCornerRadius(10 * density);
      badgeBg.setColor(Color.parseColor("#332196F3"));
      tvProviderBadge.setBackground(badgeBg);
      LinearLayout.LayoutParams lpBadge = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpBadge.setMargins(p8, 0, 0, 0);
      tvProviderBadge.setLayoutParams(lpBadge);
      headerBar.addView(tvProviderBadge);

      View spacer = new View(context);
      LinearLayout.LayoutParams lpSpacer = new LinearLayout.LayoutParams(0, 1, 1.0f);
      headerBar.addView(spacer, lpSpacer);

      Button btnSettings = new Button(context);
      btnSettings.setText("⚙️");
      btnSettings.setPadding(0, 0, 0, 0);
      btnSettings.setBackground(null);
      btnSettings.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          AiSettingsDialog.show(context, keyboard, new AiSettingsDialog.OnSettingsSavedListener()
          {
            @Override
            public void onSaved()
            {
              if (dialog != null) dialog.dismiss();
              AiActionsDialog.show(context, keyboard);
            }
          });
        }
      });
      headerBar.addView(btnSettings, new LinearLayout.LayoutParams((int)(36 * density), (int)(36 * density)));

      Button btnClose = new Button(context);
      btnClose.setText("✕");
      btnClose.setPadding(0, 0, 0, 0);
      btnClose.setBackground(null);
      btnClose.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (dialog != null) dialog.dismiss();
        }
      });
      headerBar.addView(btnClose, new LinearLayout.LayoutParams((int)(36 * density), (int)(36 * density)));
      root.addView(headerBar);

      // 2. Context Source Card & Switcher
      LinearLayout cardContext = new LinearLayout(context);
      cardContext.setOrientation(LinearLayout.VERTICAL);
      cardContext.setPadding(p8, p8, p8, p8);
      GradientDrawable cardBg = new GradientDrawable();
      cardBg.setCornerRadius(8 * density);
      cardBg.setColor(Color.parseColor("#15888888"));
      cardContext.setBackground(cardBg);
      LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpCard.setMargins(0, p8, 0, p8);
      cardContext.setLayoutParams(lpCard);

      LinearLayout rowContextTop = new LinearLayout(context);
      rowContextTop.setOrientation(LinearLayout.HORIZONTAL);
      rowContextTop.setGravity(Gravity.CENTER_VERTICAL);

      tvSourceBadge = new TextView(context);
      tvSourceBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
      tvSourceBadge.setTypeface(null, Typeface.BOLD);
      rowContextTop.addView(tvSourceBadge);

      View spacerCard = new View(context);
      rowContextTop.addView(spacerCard, new LinearLayout.LayoutParams(0, 1, 1.0f));

      final Button btnToggleSource = new Button(context);
      btnToggleSource.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      btnToggleSource.setPadding(p8, (int)(2 * density), p8, (int)(2 * density));
      GradientDrawable toggleBg = new GradientDrawable();
      toggleBg.setCornerRadius(12 * density);
      toggleBg.setColor(Color.parseColor("#30888888"));
      btnToggleSource.setBackground(toggleBg);
      btnToggleSource.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          usingClipboard = !usingClipboard;
          currentWorkingText = usingClipboard ? clipboardText : inputFieldText;
          updateContextDisplay(btnToggleSource);
          rebuildQuickSuggestions();
        }
      });
      rowContextTop.addView(btnToggleSource);
      cardContext.addView(rowContextTop);

      tvInputSnippet = new TextView(context);
      tvInputSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      tvInputSnippet.setMaxLines(3);
      tvInputSnippet.setPadding(0, (int)(4 * density), 0, 0);
      cardContext.addView(tvInputSnippet);
      root.addView(cardContext);

      updateContextDisplay(btnToggleSource);

      // 3. Smart 1-Tap Quick Action Suggestions
      scrollQuick = new HorizontalScrollView(context);
      scrollQuick.setHorizontalScrollBarEnabled(false);
      rowQuickSuggestions = new LinearLayout(context);
      rowQuickSuggestions.setOrientation(LinearLayout.HORIZONTAL);
      scrollQuick.addView(rowQuickSuggestions);
      root.addView(scrollQuick);
      rebuildQuickSuggestions();

      // 4. Main Category Tabs (Horizontal Scroll)
      HorizontalScrollView scrollCat = new HorizontalScrollView(context);
      scrollCat.setHorizontalScrollBarEnabled(false);
      rowCategories = new LinearLayout(context);
      rowCategories.setOrientation(LinearLayout.HORIZONTAL);
      scrollCat.addView(rowCategories);
      LinearLayout.LayoutParams lpScrollCat = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpScrollCat.setMargins(0, p8, 0, (int)(4 * density));
      scrollCat.setLayoutParams(lpScrollCat);
      root.addView(scrollCat);
      buildCategoryChips();

      // 5. Dynamic Sub-options Strip
      scrollSubOptions = new HorizontalScrollView(context);
      scrollSubOptions.setHorizontalScrollBarEnabled(false);
      rowSubOptions = new LinearLayout(context);
      rowSubOptions.setOrientation(LinearLayout.HORIZONTAL);
      scrollSubOptions.addView(rowSubOptions);
      root.addView(scrollSubOptions);

      // 5.1 Custom Ask AI Input Box (Hidden by default, shown for ASK_AI)
      LinearLayout llCustomPrompt = new LinearLayout(context);
      llCustomPrompt.setOrientation(LinearLayout.HORIZONTAL);
      etCustomAsk = new EditText(context);
      etCustomAsk.setHint("Type custom prompt (e.g. Make this sound like a CEO)...");
      etCustomAsk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      llCustomPrompt.addView(etCustomAsk, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

      btnCustomSend = new Button(context);
      btnCustomSend.setText("Generate 🚀");
      btnCustomSend.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
      btnCustomSend.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          triggerExecution();
        }
      });
      llCustomPrompt.addView(btnCustomSend, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      llCustomPrompt.setVisibility(View.GONE);
      root.addView(llCustomPrompt);

      // 6. Universal Tone Modifier Strip
      HorizontalScrollView scrollTone = new HorizontalScrollView(context);
      scrollTone.setHorizontalScrollBarEnabled(false);
      rowToneChips = new LinearLayout(context);
      rowToneChips.setOrientation(LinearLayout.HORIZONTAL);
      scrollTone.addView(rowToneChips);
      LinearLayout.LayoutParams lpTone = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpTone.setMargins(0, (int)(4 * density), 0, p8);
      scrollTone.setLayoutParams(lpTone);
      root.addView(scrollTone);
      buildToneChips();

      buildSubOptionChips(llCustomPrompt);

      // 7. Status & Progress Indicator
      LinearLayout rowStatus = new LinearLayout(context);
      rowStatus.setOrientation(LinearLayout.HORIZONTAL);
      rowStatus.setGravity(Gravity.CENTER_VERTICAL);
      progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
      progressBar.setVisibility(View.GONE);
      rowStatus.addView(progressBar);

      tvStatus = new TextView(context);
      tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      tvStatus.setPadding(p8, 0, 0, 0);
      tvStatus.setText("Tap an action above to generate");
      rowStatus.addView(tvStatus);
      root.addView(rowStatus);

      // 8. Result Preview Box
      etResultPreview = new EditText(context);
      etResultPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
      etResultPreview.setHint("Generated result will appear here. You can also edit it directly before inserting.");
      etResultPreview.setMinLines(3);
      etResultPreview.setMaxLines(8);
      etResultPreview.setGravity(Gravity.TOP);
      GradientDrawable resultBg = new GradientDrawable();
      resultBg.setCornerRadius(8 * density);
      resultBg.setStroke(1, Color.parseColor("#44888888"));
      resultBg.setColor(Color.parseColor("#10888888"));
      etResultPreview.setBackground(resultBg);
      etResultPreview.setPadding(p12, p12, p12, p12);
      LinearLayout.LayoutParams lpResult = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpResult.setMargins(0, p8, 0, p12);
      etResultPreview.setLayoutParams(lpResult);
      root.addView(etResultPreview);

      // 9. Bottom Action Buttons: Replace | Insert | Copy | Regenerate
      rowActionButtons = new LinearLayout(context);
      rowActionButtons.setOrientation(LinearLayout.HORIZONTAL);

      Button btnReplace = createActionButton("✅ Replace", Color.parseColor("#2E7D32"), new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          applyResultToField(true);
        }
      });
      rowActionButtons.addView(btnReplace, new LinearLayout.LayoutParams(0, (int)(40 * density), 1.2f));

      Button btnInsert = createActionButton("➕ Insert", Color.parseColor("#1565C0"), new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          applyResultToField(false);
        }
      });
      rowActionButtons.addView(btnInsert, new LinearLayout.LayoutParams(0, (int)(40 * density), 1.0f));

      Button btnCopy = createActionButton("📋 Copy", Color.parseColor("#424242"), new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          String res = etResultPreview.getText().toString();
          if (res.trim().isEmpty()) return;
          ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
          if (cm != null)
          {
            cm.setPrimaryClip(ClipData.newPlainText("AI Generated", res));
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
          }
        }
      });
      rowActionButtons.addView(btnCopy, new LinearLayout.LayoutParams(0, (int)(40 * density), 0.9f));

      Button btnRegen = createActionButton("🔄", Color.parseColor("#555555"), new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          triggerExecution();
        }
      });
      rowActionButtons.addView(btnRegen, new LinearLayout.LayoutParams((int)(40 * density), (int)(40 * density)));

      root.addView(rowActionButtons);

      dialog = new AlertDialog.Builder(context)
          .setView(rootScrollView)
          .create();

      Utils.show_dialog_on_ime(dialog, keyboard);
    }

    private Button createActionButton(String label, int bgColor, View.OnClickListener listener)
    {
      Button b = new Button(context);
      b.setText(label);
      b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      b.setTextColor(Color.WHITE);
      b.setPadding(0, 0, 0, 0);
      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(6 * density);
      bg.setColor(bgColor);
      b.setBackground(bg);
      b.setOnClickListener(listener);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, (int)(40 * density));
      lp.setMargins((int)(2 * density), 0, (int)(2 * density), 0);
      b.setLayoutParams(lp);
      return b;
    }

    private void updateContextDisplay(Button btnToggle)
    {
      if (usingClipboard)
      {
        tvSourceBadge.setText("📋 Using Clipboard Content");
        btnToggle.setText("Switch to Typed Text");
      }
      else if (hasSelection)
      {
        tvSourceBadge.setText("📝 Using Selected Text");
        btnToggle.setText("Switch to Clipboard");
      }
      else
      {
        tvSourceBadge.setText("✍️ Using Input Field Text");
        btnToggle.setText("Switch to Clipboard");
      }

      if (currentWorkingText == null || currentWorkingText.isEmpty())
      {
        tvInputSnippet.setText("(No text available. Type in an app or copy text to clipboard)");
        tvInputSnippet.setTextColor(Color.GRAY);
      }
      else
      {
        tvInputSnippet.setText("\"" + currentWorkingText + "\"");
        tvInputSnippet.setTextColor(Color.parseColor("#CCCCCC"));
      }
    }

    private void rebuildQuickSuggestions()
    {
      rowQuickSuggestions.removeAllViews();
      AiActionEngine.ContextDetectionResult analysis = AiActionEngine.analyzeContext(currentWorkingText);
      if (analysis.quickSuggestions.isEmpty())
      {
        scrollQuick.setVisibility(View.GONE);
        return;
      }

      scrollQuick.setVisibility(View.VISIBLE);
      TextView label = new TextView(context);
      label.setText("💡 Quick: ");
      label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
      label.setGravity(Gravity.CENTER_VERTICAL);
      rowQuickSuggestions.addView(label);

      for (final String sugg : analysis.quickSuggestions)
      {
        Button chip = new Button(context);
        chip.setText(sugg);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        chip.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14 * density);
        bg.setColor(Color.parseColor("#252196F3"));
        bg.setStroke(1, Color.parseColor("#402196F3"));
        chip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (int)(30 * density));
        lp.setMargins((int)(2 * density), 0, (int)(4 * density), 0);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            activeCategory = AiActionEngine.Category.ASK_AI;
            buildCategoryChips();
            etCustomAsk.setText(sugg);
            triggerExecution();
          }
        });

        rowQuickSuggestions.addView(chip);
      }
    }

    private void buildCategoryChips()
    {
      rowCategories.removeAllViews();
      for (final AiActionEngine.Category cat : AiActionEngine.Category.values())
      {
        final boolean isSelected = (cat == activeCategory);
        Button chip = new Button(context);
        chip.setText(cat.getTitle());
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setPadding((int)(10 * density), 0, (int)(10 * density), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14 * density);
        if (isSelected)
        {
          bg.setColor(Color.parseColor("#FF2196F3"));
          chip.setTextColor(Color.WHITE);
        }
        else
        {
          bg.setColor(Color.parseColor("#20888888"));
          chip.setTextColor(Color.parseColor("#CCCCCC"));
        }
        chip.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (int)(32 * density));
        lp.setMargins((int)(2 * density), 0, (int)(4 * density), 0);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            activeCategory = cat;
            List<AiActionEngine.ActionOption> opts = AiActionEngine.getOptionsForCategory(cat);
            if (!opts.isEmpty())
            {
              activeOptionId = opts.get(0).id;
            }
            buildCategoryChips();
            buildSubOptionChips(etCustomAsk.getParent() instanceof View ? (View)etCustomAsk.getParent() : null);
            if (activeCategory != AiActionEngine.Category.ASK_AI)
            {
              triggerExecution();
            }
          }
        });

        rowCategories.addView(chip);
      }
    }

    private void buildSubOptionChips(View customPromptContainer)
    {
      rowSubOptions.removeAllViews();
      if (activeCategory == AiActionEngine.Category.ASK_AI)
      {
        scrollSubOptions.setVisibility(View.GONE);
        if (customPromptContainer != null) customPromptContainer.setVisibility(View.VISIBLE);
        return;
      }

      scrollSubOptions.setVisibility(View.VISIBLE);
      if (customPromptContainer != null) customPromptContainer.setVisibility(View.GONE);

      List<AiActionEngine.ActionOption> options = AiActionEngine.getOptionsForCategory(activeCategory);
      for (final AiActionEngine.ActionOption opt : options)
      {
        final boolean isSelected = opt.id.equals(activeOptionId);
        Button chip = new Button(context);
        chip.setText(opt.label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        chip.setPadding((int)(8 * density), 0, (int)(8 * density), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * density);
        if (isSelected)
        {
          bg.setColor(Color.parseColor("#442196F3"));
          bg.setStroke(1, Color.parseColor("#FF2196F3"));
          chip.setTextColor(Color.parseColor("#90CAF9"));
        }
        else
        {
          bg.setColor(Color.parseColor("#15888888"));
          chip.setTextColor(Color.parseColor("#AAAAAA"));
        }
        chip.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (int)(28 * density));
        lp.setMargins((int)(2 * density), 0, (int)(4 * density), 0);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            activeOptionId = opt.id;
            buildSubOptionChips(null);
            triggerExecution();
          }
        });

        rowSubOptions.addView(chip);
      }
    }

    private void buildToneChips()
    {
      rowToneChips.removeAllViews();
      TextView lbl = new TextView(context);
      lbl.setText("Tone: ");
      lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      lbl.setGravity(Gravity.CENTER_VERTICAL);
      rowToneChips.addView(lbl);

      String[] tones = {"Default", "Professional", "Friendly", "Casual", "Formal", "Confident", "Funny", "Empathetic"};
      for (final String tone : tones)
      {
        final boolean isSelected = tone.equalsIgnoreCase(activeTone);
        Button chip = new Button(context);
        chip.setText(tone);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        chip.setPadding((int)(6 * density), 0, (int)(6 * density), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(10 * density);
        if (isSelected)
        {
          bg.setColor(Color.parseColor("#334CAF50"));
          bg.setStroke(1, Color.parseColor("#4CAF50"));
          chip.setTextColor(Color.parseColor("#81C784"));
        }
        else
        {
          bg.setColor(Color.parseColor("#15888888"));
          chip.setTextColor(Color.parseColor("#999999"));
        }
        chip.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (int)(24 * density));
        lp.setMargins((int)(2 * density), 0, (int)(3 * density), 0);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            activeTone = tone;
            buildToneChips();
            triggerExecution();
          }
        });

        rowToneChips.addView(chip);
      }
    }

    private void triggerExecution()
    {
      if (isGenerating) return;
      if (currentWorkingText == null || currentWorkingText.trim().isEmpty())
      {
        Toast.makeText(context, "No text to process. Please type or copy text first.", Toast.LENGTH_SHORT).show();
        return;
      }

      isGenerating = true;
      progressBar.setVisibility(View.VISIBLE);
      tvStatus.setText("Generating with " + AiProvider.Manager.getActiveProvider(context).getName() + "...");

      String custom = (activeCategory == AiActionEngine.Category.ASK_AI && etCustomAsk != null)
          ? etCustomAsk.getText().toString().trim() : "";

      AiActionEngine.executeAction(
          context,
          currentWorkingText,
          activeCategory,
          activeOptionId,
          activeTone,
          custom,
          new AiProvider.Callback()
          {
            @Override
            public void onSuccess(final String resultText)
            {
              isGenerating = false;
              progressBar.setVisibility(View.GONE);
              lastGeneratedResult = resultText;
              tvStatus.setText("✅ Generated successfully");
              etResultPreview.setText(resultText);
              etResultPreview.setSelection(resultText.length());
            }

            @Override
            public void onError(final String errorMessage)
            {
              isGenerating = false;
              progressBar.setVisibility(View.GONE);
              tvStatus.setText("❌ Error: " + errorMessage);
              Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            }
          });
    }

    private void applyResultToField(boolean replaceOriginal)
    {
      String output = etResultPreview.getText().toString();
      if (output == null || output.trim().isEmpty())
      {
        Toast.makeText(context, "No output to apply", Toast.LENGTH_SHORT).show();
        return;
      }

      InputConnection ic = keyboard.getCurrentInputConnection();
      if (ic == null)
      {
        // Copy to clipboard fallback if no active field
        ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null)
        {
          cm.setPrimaryClip(ClipData.newPlainText("AI Text", output));
          Toast.makeText(context, "Copied to clipboard (no active input field)", Toast.LENGTH_SHORT).show();
        }
        if (dialog != null) dialog.dismiss();
        return;
      }

      if (replaceOriginal)
      {
        if (hasSelection)
        {
          ic.commitText(output, 1);
        }
        else if (!usingClipboard && inputFieldText != null && !inputFieldText.isEmpty())
        {
          ic.deleteSurroundingText(inputFieldText.length(), 0);
          ic.commitText(output, 1);
        }
        else
        {
          ic.commitText(output, 1);
        }
      }
      else
      {
        // Insert at cursor
        ic.commitText(output, 1);
      }

      Toast.makeText(context, "✅ Text applied", Toast.LENGTH_SHORT).show();
      if (dialog != null) dialog.dismiss();
    }
  }
}
