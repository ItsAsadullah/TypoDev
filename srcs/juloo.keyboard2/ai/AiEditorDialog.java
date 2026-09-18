package juloo.keyboard2.ai;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Utils;

public class AiEditorDialog
{
  private static final int TAB_TRANSLATE = 0;
  private static final int TAB_STYLE = 1;
  private static final int TAB_FIX = 2;

  public static void show(final Context context, final Keyboard2 keyboard)
  {
    final String apiKey = GeminiAiService.getApiKey(context);
    if (apiKey.isEmpty())
    {
      AiActionDialog.showApiKeyDialog(context, keyboard);
      return;
    }

    final InputConnection ic = keyboard.getCurrentInputConnection();
    if (ic == null)
    {
      Toast.makeText(context, "No active input field", Toast.LENGTH_SHORT).show();
      return;
    }

    CharSequence sel = ic.getSelectedText(0);
    final boolean hasSelection = (sel != null && sel.length() > 0);
    final String initialText;

    if (hasSelection)
    {
      initialText = sel.toString();
    }
    else
    {
      CharSequence before = ic.getTextBeforeCursor(1000, 0);
      if (before == null || before.length() == 0)
      {
        Toast.makeText(context, "Please type or select text first", Toast.LENGTH_SHORT).show();
        return;
      }
      initialText = before.toString();
    }

    new EditorSession(context, keyboard, initialText, hasSelection).open();
  }

  private static class EditorSession
  {
    private final Context context;
    private final Keyboard2 keyboard;
    private final String originalText;
    private final boolean hasSelection;

    private int activeTab = TAB_STYLE;
    private AiStyle activeStyle;
    private boolean emojify = true;
    private String currentResult = "";
    private boolean isGenerating = false;

    private AlertDialog dialog;
    private TextView tvHeaderTitle;
    private LinearLayout tabContainer;
    private HorizontalScrollView stylesScroll;
    private LinearLayout stylesRow;
    private TextView tvStatusLabel;
    private CheckBox cbEmojify;
    private TextView tvPreview;
    private LinearLayout fixDiffContainer;
    private TextView tvFixOriginal;
    private TextView tvFixResult;
    private LinearLayout llFixChanges;
    private ProgressBar progressBar;
    private Button btnApply;
    private Button btnCopy;

    public EditorSession(Context context, Keyboard2 keyboard, String text, boolean hasSelection)
    {
      this.context = context;
      this.keyboard = keyboard;
      this.originalText = text;
      this.hasSelection = hasSelection;
      this.currentResult = text;
      this.emojify = GeminiAiService.isEmojifyEnabled(context);

      List<AiStyle> styles = AiStyle.getAllStyles(context);
      this.activeStyle = styles.isEmpty() ? null : styles.get(0);
    }

    public void open()
    {
      int dp16 = (int) (16 * context.getResources().getDisplayMetrics().density);
      int dp12 = (int) (12 * context.getResources().getDisplayMetrics().density);
      int dp8 = (int) (8 * context.getResources().getDisplayMetrics().density);
      int dp4 = (int) (4 * context.getResources().getDisplayMetrics().density);
      int dp48 = (int) (48 * context.getResources().getDisplayMetrics().density);

      // Root layout (Dark sleek container matching Telegram AI Editor)
      LinearLayout root = new LinearLayout(context);
      root.setOrientation(LinearLayout.VERTICAL);
      root.setBackgroundColor(Color.parseColor("#151A23"));
      root.setPadding(dp16, dp12, dp16, dp16);

      // 1. Header: "AI Editor" + Close (X)
      RelativeLayout header = new RelativeLayout(context);
      header.setPadding(0, 0, 0, dp12);

      tvHeaderTitle = new TextView(context);
      tvHeaderTitle.setText("AI Editor");
      tvHeaderTitle.setTextColor(Color.WHITE);
      tvHeaderTitle.setTextSize(18);
      tvHeaderTitle.setTypeface(null, android.graphics.Typeface.BOLD);
      RelativeLayout.LayoutParams lpHTitle = new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      lpHTitle.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      lpHTitle.addRule(RelativeLayout.CENTER_VERTICAL);
      header.addView(tvHeaderTitle, lpHTitle);

      LinearLayout headerRight = new LinearLayout(context);
      headerRight.setOrientation(LinearLayout.HORIZONTAL);
      headerRight.setGravity(Gravity.CENTER_VERTICAL);
      RelativeLayout.LayoutParams lpHRight = new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      lpHRight.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      lpHRight.addRule(RelativeLayout.CENTER_VERTICAL);

      TextView btnSettings = new TextView(context);
      btnSettings.setText("⚙️");
      btnSettings.setTextSize(17);
      btnSettings.setPadding(dp8, dp4, dp8, dp4);
      btnSettings.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          String currentModel = GeminiAiService.getModel(context);
          String[] options = {
            "🤖 Change Model (" + currentModel + ")",
            "🔑 Change API Key",
            "⚙️ Open Keyboard Settings"
          };
          AlertDialog settingsDialog = new AlertDialog.Builder(context)
              .setTitle("Gemini Settings")
              .setItems(options, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface d, int which)
                {
                  if (which == 0)
                  {
                    AiActionDialog.showModelSelectDialog(context, keyboard);
                  }
                  else if (which == 1)
                  {
                    AiActionDialog.showApiKeyDialog(context, keyboard);
                  }
                  else if (which == 2)
                  {
                    if (dialog != null) dialog.dismiss();
                    Intent intent = new Intent(context, juloo.keyboard2.SettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                  }
                }
              })
              .setNegativeButton(android.R.string.cancel, null)
              .create();
          Utils.show_dialog_on_ime(settingsDialog, keyboard);
        }
      });
      headerRight.addView(btnSettings);

      TextView btnClose = new TextView(context);
      btnClose.setText("✕");
      btnClose.setTextColor(Color.parseColor("#8E99A8"));
      btnClose.setTextSize(19);
      btnClose.setPadding(dp8, dp4, dp8, dp4);
      btnClose.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (dialog != null) dialog.dismiss();
        }
      });
      headerRight.addView(btnClose);
      header.addView(headerRight, lpHRight);
      root.addView(header);

      // 2. Tabs Row: Translate | Style | Fix
      tabContainer = new LinearLayout(context);
      tabContainer.setOrientation(LinearLayout.HORIZONTAL);
      tabContainer.setGravity(Gravity.CENTER);
      tabContainer.setPadding(0, 0, 0, dp12);

      final TextView tabTranslate = createTabButton("🌐 Translate", TAB_TRANSLATE);
      final TextView tabStyle = createTabButton("✨ Style", TAB_STYLE);
      final TextView tabFix = createTabButton("🔍 Fix", TAB_FIX);

      tabContainer.addView(tabTranslate);
      tabContainer.addView(tabStyle);
      tabContainer.addView(tabFix);
      root.addView(tabContainer);

      // 3. Horizontal Styles Scroll (matching Telegram icons)
      stylesScroll = new HorizontalScrollView(context);
      stylesScroll.setHorizontalScrollBarEnabled(false);
      stylesRow = new LinearLayout(context);
      stylesRow.setOrientation(LinearLayout.HORIZONTAL);
      stylesRow.setPadding(0, 0, 0, dp12);
      stylesScroll.addView(stylesRow);
      root.addView(stylesScroll);
      refreshStylesRow();

      // 4. Status Row: "Original" / "Result" + "☑ emojify"
      RelativeLayout statusRow = new RelativeLayout(context);
      statusRow.setPadding(0, 0, 0, dp8);

      tvStatusLabel = new TextView(context);
      tvStatusLabel.setText("Original");
      tvStatusLabel.setTextColor(Color.parseColor("#8E99A8"));
      tvStatusLabel.setTextSize(14);
      tvStatusLabel.setTypeface(null, android.graphics.Typeface.BOLD);
      RelativeLayout.LayoutParams lpStatus = new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      lpStatus.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      lpStatus.addRule(RelativeLayout.CENTER_VERTICAL);
      statusRow.addView(tvStatusLabel, lpStatus);

      cbEmojify = new CheckBox(context);
      cbEmojify.setText("emojify");
      cbEmojify.setTextSize(14);
      int[][] states = new int[][] {
          new int[] { android.R.attr.state_checked },
          new int[] { -android.R.attr.state_checked }
      };
      int[] colors = new int[] {
          Color.parseColor("#2AABEE"),
          Color.parseColor("#8E99A8")
      };
      ColorStateList csl = new ColorStateList(states, colors);
      if (android.os.Build.VERSION.SDK_INT >= 21)
      {
        cbEmojify.setButtonTintList(csl);
      }
      cbEmojify.setChecked(emojify);
      cbEmojify.setTextColor(emojify ? Color.parseColor("#2AABEE") : Color.parseColor("#8E99A8"));
      cbEmojify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          emojify = isChecked;
          cbEmojify.setTextColor(isChecked ? Color.parseColor("#2AABEE") : Color.parseColor("#8E99A8"));
          GeminiAiService.setEmojifyEnabled(context, isChecked);
          triggerActiveTab();
        }
      });
      RelativeLayout.LayoutParams lpEmo = new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      lpEmo.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      lpEmo.addRule(RelativeLayout.CENTER_VERTICAL);
      statusRow.addView(cbEmojify, lpEmo);
      root.addView(statusRow);

      // 5. Preview Card (Dark card with copy button)
      RelativeLayout previewCard = new RelativeLayout(context);
      GradientDrawable cardBg = new GradientDrawable();
      cardBg.setColor(Color.parseColor("#1C2330"));
      cardBg.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
      previewCard.setBackground(cardBg);
      previewCard.setPadding(dp12, dp12, dp12, dp12);

      ScrollView previewScroll = new ScrollView(context);
      previewScroll.setLayoutParams(new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.MATCH_PARENT, (int) (140 * context.getResources().getDisplayMetrics().density)));

      LinearLayout previewContent = new LinearLayout(context);
      previewContent.setOrientation(LinearLayout.VERTICAL);

      tvPreview = new TextView(context);
      tvPreview.setText(originalText);
      tvPreview.setTextColor(Color.WHITE);
      tvPreview.setTextSize(15);
      tvPreview.setLineSpacing(dp4, 1.1f);
      previewContent.addView(tvPreview);

      progressBar = new ProgressBar(context);
      progressBar.setVisibility(View.GONE);
      previewContent.addView(progressBar);

      // Fix Diff Container for showing Original vs Fixed with highlighting
      fixDiffContainer = new LinearLayout(context);
      fixDiffContainer.setOrientation(LinearLayout.VERTICAL);
      fixDiffContainer.setVisibility(View.GONE);

      TextView tvOrigHeader = new TextView(context);
      tvOrigHeader.setText("Original (আগের লেখা):");
      tvOrigHeader.setTextColor(Color.parseColor("#8E99A8"));
      tvOrigHeader.setTextSize(12);
      tvOrigHeader.setTypeface(null, android.graphics.Typeface.BOLD);
      tvOrigHeader.setPadding(0, 0, 0, dp4);
      fixDiffContainer.addView(tvOrigHeader);

      tvFixOriginal = new TextView(context);
      tvFixOriginal.setTextColor(Color.parseColor("#E0E0E0"));
      tvFixOriginal.setTextSize(14);
      tvFixOriginal.setLineSpacing(dp4, 1.1f);
      fixDiffContainer.addView(tvFixOriginal);

      View div = new View(context);
      div.setBackgroundColor(Color.parseColor("#2A3444"));
      LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * context.getResources().getDisplayMetrics().density));
      lpDiv.setMargins(0, dp8, 0, dp8);
      fixDiffContainer.addView(div, lpDiv);

      TextView tvFixedHeader = new TextView(context);
      tvFixedHeader.setText("Fixed Result (সংশোধিত লেখা):");
      tvFixedHeader.setTextColor(Color.parseColor("#2AABEE"));
      tvFixedHeader.setTextSize(12);
      tvFixedHeader.setTypeface(null, android.graphics.Typeface.BOLD);
      tvFixedHeader.setPadding(0, 0, 0, dp4);
      fixDiffContainer.addView(tvFixedHeader);

      tvFixResult = new TextView(context);
      tvFixResult.setTextColor(Color.WHITE);
      tvFixResult.setTextSize(14);
      tvFixResult.setLineSpacing(dp4, 1.1f);
      fixDiffContainer.addView(tvFixResult);

      llFixChanges = new LinearLayout(context);
      llFixChanges.setOrientation(LinearLayout.VERTICAL);
      llFixChanges.setPadding(0, dp8, 0, 0);
      fixDiffContainer.addView(llFixChanges);

      previewContent.addView(fixDiffContainer);

      previewScroll.addView(previewContent);
      previewCard.addView(previewScroll);

      // Copy icon inside card at bottom-right
      btnCopy = new Button(context);
      btnCopy.setText("📋 Copy");
      btnCopy.setTextColor(Color.parseColor("#2AABEE"));
      btnCopy.setTextSize(12);
      btnCopy.setBackgroundColor(Color.TRANSPARENT);
      RelativeLayout.LayoutParams lpCopy = new RelativeLayout.LayoutParams(
          RelativeLayout.LayoutParams.WRAP_CONTENT, (int) (32 * context.getResources().getDisplayMetrics().density));
      lpCopy.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      lpCopy.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
      btnCopy.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
          if (cm != null && !TextUtils.isEmpty(currentResult))
          {
            cm.setPrimaryClip(ClipData.newPlainText("AI Result", currentResult));
            Toast.makeText(context, "📋 Copied to clipboard!", Toast.LENGTH_SHORT).show();
          }
        }
      });
      previewCard.addView(btnCopy, lpCopy);

      root.addView(previewCard);

      // 6. Action Button: Blue [ Apply ] Button
      btnApply = new Button(context);
      btnApply.setText("Apply");
      btnApply.setTextColor(Color.WHITE);
      btnApply.setTextSize(16);
      btnApply.setTypeface(null, android.graphics.Typeface.BOLD);
      GradientDrawable applyBg = new GradientDrawable();
      applyBg.setCornerRadius(24 * context.getResources().getDisplayMetrics().density);
      applyBg.setColor(Color.parseColor("#2AABEE"));
      btnApply.setBackground(applyBg);
      btnApply.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          applyResultToInput();
        }
      });
      LinearLayout.LayoutParams lpApply = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, dp48);
      lpApply.setMargins(0, dp16, 0, 0);
      root.addView(btnApply, lpApply);

      dialog = new AlertDialog.Builder(context)
          .setView(root)
          .create();

      Utils.show_dialog_on_ime(dialog, keyboard);

      // Display original text cleanly on open; only generate when user clicks a style/fix button!
      tvStatusLabel.setText("Original");
    }

    private TextView createTabButton(final String text, final int tabIndex)
    {
      final TextView tv = new TextView(context);
      tv.setText(text);
      tv.setTextSize(14);
      tv.setGravity(Gravity.CENTER);
      tv.setPadding(16, 10, 16, 10);
      updateTabStyle(tv, tabIndex == activeTab);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
      lp.setMargins(6, 0, 6, 0);
      tv.setLayoutParams(lp);

      tv.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          activeTab = tabIndex;
          for (int i = 0; i < tabContainer.getChildCount(); i++)
          {
            View child = tabContainer.getChildAt(i);
            if (child instanceof TextView)
            {
              updateTabStyle((TextView) child, i == activeTab);
            }
          }
          if (activeTab == TAB_STYLE)
          {
            stylesScroll.setVisibility(View.VISIBLE);
          }
          else
          {
            stylesScroll.setVisibility(View.GONE);
          }
          triggerActiveTab();
        }
      });
      return tv;
    }

    private void updateTabStyle(TextView tv, boolean isActive)
    {
      GradientDrawable gd = new GradientDrawable();
      gd.setCornerRadius(18 * context.getResources().getDisplayMetrics().density);
      if (isActive)
      {
        gd.setColor(Color.parseColor("#20344D"));
        gd.setStroke((int) (1.5 * context.getResources().getDisplayMetrics().density), Color.parseColor("#2AABEE"));
        tv.setTextColor(Color.parseColor("#2AABEE"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
      }
      else
      {
        gd.setColor(Color.parseColor("#1C2330"));
        tv.setTextColor(Color.parseColor("#8E99A8"));
        tv.setTypeface(null, android.graphics.Typeface.NORMAL);
      }
      tv.setBackground(gd);
    }

    private void refreshStylesRow()
    {
      stylesRow.removeAllViews();
      int dp8 = (int) (8 * context.getResources().getDisplayMetrics().density);
      int dp4 = (int) (4 * context.getResources().getDisplayMetrics().density);
      int dp44 = (int) (44 * context.getResources().getDisplayMetrics().density);

      // Item 0: (+) Create button
      LinearLayout createItem = new LinearLayout(context);
      createItem.setOrientation(LinearLayout.VERTICAL);
      createItem.setGravity(Gravity.CENTER);
      createItem.setPadding(dp8, dp4, dp8, dp4);

      TextView tvPlus = new TextView(context);
      tvPlus.setText("＋");
      tvPlus.setTextColor(Color.parseColor("#2AABEE"));
      tvPlus.setTextSize(20);
      tvPlus.setGravity(Gravity.CENTER);
      GradientDrawable plusBg = new GradientDrawable();
      plusBg.setShape(GradientDrawable.OVAL);
      plusBg.setColor(Color.parseColor("#20344D"));
      plusBg.setStroke(2, Color.parseColor("#2AABEE"));
      tvPlus.setBackground(plusBg);
      createItem.addView(tvPlus, new LinearLayout.LayoutParams(dp44, dp44));

      TextView tvCreateLabel = new TextView(context);
      tvCreateLabel.setText("Create");
      tvCreateLabel.setTextColor(Color.parseColor("#8E99A8"));
      tvCreateLabel.setTextSize(11);
      tvCreateLabel.setGravity(Gravity.CENTER);
      tvCreateLabel.setPadding(0, dp4, 0, 0);
      createItem.addView(tvCreateLabel);

      createItem.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          // Open NewStyleActivity
          Intent intent = new Intent(context, NewStyleActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(intent);
          Toast.makeText(context, "Create your custom style!", Toast.LENGTH_SHORT).show();
          if (dialog != null) dialog.dismiss();
        }
      });
      stylesRow.addView(createItem);

      // Add all preset and custom styles
      List<AiStyle> styles = AiStyle.getAllStyles(context);
      for (final AiStyle style : styles)
      {
        final boolean isSelected = (activeStyle != null && activeStyle.getId().equals(style.getId()));

        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp8, dp4, dp8, dp4);

        TextView tvIcon = new TextView(context);
        tvIcon.setText(style.getIcon());
        tvIcon.setTextSize(20);
        tvIcon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        if (isSelected)
        {
          iconBg.setColor(Color.parseColor("#2AABEE"));
        }
        else
        {
          iconBg.setColor(Color.parseColor("#212836"));
        }
        tvIcon.setBackground(iconBg);
        item.addView(tvIcon, new LinearLayout.LayoutParams(dp44, dp44));

        TextView tvName = new TextView(context);
        tvName.setText(style.getName());
        tvName.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#8E99A8"));
        tvName.setTextSize(11);
        tvName.setGravity(Gravity.CENTER);
        tvName.setPadding(0, dp4, 0, 0);
        if (isSelected) tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        item.addView(tvName);

        item.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            activeStyle = style;
            refreshStylesRow();
            triggerActiveTab();
          }
        });

        if (style.isCustom())
        {
          item.setOnLongClickListener(new View.OnLongClickListener()
          {
            @Override
            public boolean onLongClick(View v)
            {
              AlertDialog delDialog = new AlertDialog.Builder(context)
                  .setTitle("Delete Style")
                  .setMessage("Do you want to delete custom style \"" + style.getName() + "\"?")
                  .setPositiveButton("Delete", new DialogInterface.OnClickListener()
                  {
                    @Override
                    public void onClick(DialogInterface d, int which)
                    {
                      AiStyle.deleteCustomStyle(context, style.getId());
                      activeStyle = AiStyle.getBuiltInStyles().get(0);
                      refreshStylesRow();
                      triggerActiveTab();
                    }
                  })
                  .setNegativeButton(android.R.string.cancel, null)
                  .create();
              Utils.show_dialog_on_ime(delDialog, keyboard);
              return true;
            }
          });
        }

        stylesRow.addView(item);
      }
    }

    private void triggerActiveTab()
    {
      if (isGenerating) return;
      isGenerating = true;

      tvStatusLabel.setText("✨ AI Generating...");
      tvStatusLabel.setTextColor(Color.parseColor("#2AABEE"));
      progressBar.setVisibility(View.VISIBLE);

      if (activeTab == TAB_FIX)
      {
        tvPreview.setVisibility(View.GONE);
        fixDiffContainer.setVisibility(View.VISIBLE);
        tvFixOriginal.setText(originalText);
        tvFixResult.setText("Analyzing grammar and spelling with Gemini...");
        llFixChanges.removeAllViews();

        GeminiAiService.processGrammarDiff(context, originalText, emojify, new GeminiAiService.GrammarDiffCallback()
        {
          @Override
          public void onSuccess(final GeminiAiService.GrammarDiffResult result)
          {
            if (dialog == null || !dialog.isShowing()) return;
            try
            {
              isGenerating = false;
              progressBar.setVisibility(View.GONE);
              currentResult = result.fixedText;
              tvStatusLabel.setText("Result (Fix Complete)");
              tvStatusLabel.setTextColor(Color.parseColor("#4ECCA3"));

              // 1. Highlight errors in original text with red background & strikethrough
              SpannableStringBuilder origSpan = new SpannableStringBuilder(result.originalText);
              for (GeminiAiService.GrammarChange c : result.changes)
              {
                if (c.wrong != null && !c.wrong.trim().isEmpty())
                {
                  int start = result.originalText.indexOf(c.wrong);
                  while (start >= 0)
                  {
                    int end = start + c.wrong.length();
                    origSpan.setSpan(new BackgroundColorSpan(Color.parseColor("#663333")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    origSpan.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = result.originalText.indexOf(c.wrong, end);
                  }
                }
              }
              tvFixOriginal.setText(origSpan);

              // 2. Highlight corrections in fixed text with green background
              SpannableStringBuilder fixedSpan = new SpannableStringBuilder(result.fixedText);
              for (GeminiAiService.GrammarChange c : result.changes)
              {
                if (c.fixed != null && !c.fixed.trim().isEmpty())
                {
                  int start = result.fixedText.indexOf(c.fixed);
                  while (start >= 0)
                  {
                    int end = start + c.fixed.length();
                    fixedSpan.setSpan(new BackgroundColorSpan(Color.parseColor("#1B4D2E")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = result.fixedText.indexOf(c.fixed, end);
                  }
                }
              }
              tvFixResult.setText(fixedSpan);

              // 3. Populate changes explanation list
              llFixChanges.removeAllViews();
              int dp4 = (int) (4 * context.getResources().getDisplayMetrics().density);
              if (result.changes != null && !result.changes.isEmpty())
              {
                TextView tvChangesTitle = new TextView(context);
                tvChangesTitle.setText("সংশোধিত বিষয়সমূহ:");
                tvChangesTitle.setTextColor(Color.parseColor("#8E99A8"));
                tvChangesTitle.setTextSize(12);
                tvChangesTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvChangesTitle.setPadding(0, dp4, 0, dp4);
                llFixChanges.addView(tvChangesTitle);

                for (GeminiAiService.GrammarChange c : result.changes)
                {
                  TextView row = new TextView(context);
                  String reasonStr = (c.reason != null && !c.reason.trim().isEmpty()) ? " — " + c.reason : "";
                  row.setText("❌ " + c.wrong + " ➔ ✅ " + c.fixed + reasonStr);
                  row.setTextColor(Color.parseColor("#D0D7DE"));
                  row.setTextSize(12);
                  row.setPadding(0, 2, 0, 4);
                  llFixChanges.addView(row);
                }
              }
            }
            catch (Throwable ignored) {}
          }

          @Override
          public void onError(final String errorMessage)
          {
            if (dialog == null || !dialog.isShowing()) return;
            try
            {
              isGenerating = false;
              progressBar.setVisibility(View.GONE);
              tvStatusLabel.setText("Notice");
              tvStatusLabel.setTextColor(Color.parseColor("#FF6B6B"));
              tvPreview.setVisibility(View.VISIBLE);
              fixDiffContainer.setVisibility(View.GONE);
              tvPreview.setText(errorMessage);
            }
            catch (Throwable ignored) {}
          }
        });
      }
      else
      {
        tvPreview.setVisibility(View.VISIBLE);
        fixDiffContainer.setVisibility(View.GONE);
        tvPreview.setText("Analyzing and generating response with Gemini...");

        GeminiAiService.AiCallback callback = new GeminiAiService.AiCallback()
        {
          @Override
          public void onSuccess(final String resultText)
          {
            if (dialog == null || !dialog.isShowing()) return;
            try
            {
              isGenerating = false;
              progressBar.setVisibility(View.GONE);
              currentResult = resultText;
              tvPreview.setText(resultText);
              tvStatusLabel.setText("Result");
              tvStatusLabel.setTextColor(Color.WHITE);
            }
            catch (Throwable ignored) {}
          }

          @Override
          public void onError(final String errorMessage)
          {
            if (dialog == null || !dialog.isShowing()) return;
            try
            {
              isGenerating = false;
              progressBar.setVisibility(View.GONE);
              tvPreview.setText(errorMessage);
              tvStatusLabel.setText("Notice");
              tvStatusLabel.setTextColor(Color.parseColor("#FF6B6B"));
            }
            catch (Throwable ignored) {}
          }
        };

        if (activeTab == TAB_TRANSLATE)
        {
          GeminiAiService.processTranslate(context, originalText, "English / Bengali", emojify, callback);
        }
        else
        {
          String inst = (activeStyle != null) ? activeStyle.getInstruction() : "Rewrite cleanly and elegantly";
          GeminiAiService.processStyle(context, originalText, inst, emojify, callback);
        }
      }
    }

    private void applyResultToInput()
    {
      if (TextUtils.isEmpty(currentResult))
      {
        Toast.makeText(context, "Nothing to apply", Toast.LENGTH_SHORT).show();
        return;
      }

      try
      {
        InputConnection conn = keyboard.getCurrentInputConnection();
        if (conn != null)
        {
          if (hasSelection)
          {
            conn.commitText(currentResult, 1);
          }
          else
          {
            conn.deleteSurroundingText(originalText.length(), 0);
            conn.commitText(currentResult, 1);
          }
          Toast.makeText(context, "✨ Applied successfully!", Toast.LENGTH_SHORT).show();
        }
      }
      catch (Throwable ignored) {}

      try
      {
        if (dialog != null && dialog.isShowing())
        {
          dialog.dismiss();
        }
      }
      catch (Throwable ignored) {}
    }
  }
}
