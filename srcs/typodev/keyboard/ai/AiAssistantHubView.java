package typodev.keyboard.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.graphics.ColorUtils;

import typodev.keyboard.Keyboard2;
import typodev.keyboard.R;

/** Writing shortcuts hosted directly inside the keyboard. */
public final class AiAssistantHubView
{
  private static final String DEFAULT_TONE = "স্বাভাবিক";

  private AiAssistantHubView() {}

  public static View create(final Context context, final Keyboard2 keyboard2)
  {
    if (context == null || keyboard2 == null) return null;

    final LayoutInflater inflater = LayoutInflater.from(context);
    final View view = inflater.inflate(R.layout.magic_tools_pane, null);
    view.findViewById(R.id.btn_hub_settings).setOnClickListener(v -> {
      keyboard2.closeMagicTools();
      keyboard2.showAiPane();
    });
    View restoreDraft = view.findViewById(R.id.btn_hub_restore_draft);
    restoreDraft.setVisibility(keyboard2.hasWritingDraft() ? View.VISIBLE : View.GONE);
    restoreDraft.setOnClickListener(v -> keyboard2.restoreWritingDraft());
    final View guide = view.findViewById(R.id.hub_guide);
    final TextView guideButton = view.findViewById(R.id.btn_hub_guide);
    android.content.SharedPreferences guidePrefs =
        (android.os.Build.VERSION.SDK_INT >= 24 ? context.createDeviceProtectedStorageContext() : context)
            .getSharedPreferences("ai_writing_onboarding", Context.MODE_PRIVATE);
    boolean firstGuide = !guidePrefs.getBoolean("ai_magic_guide_seen", false);
    guide.setVisibility(firstGuide ? View.VISIBLE : View.GONE);
    guideButton.setVisibility(firstGuide ? View.VISIBLE : View.GONE);
    guideButton.setText(R.string.ai_magic_guide_close);
    if (firstGuide) guidePrefs.edit().putBoolean("ai_magic_guide_seen", true).apply();
    guideButton.setOnClickListener(v -> {
      guide.setVisibility(View.GONE);
      guideButton.setVisibility(View.GONE);
    });
    view.findViewById(R.id.btn_hub_guide_settings).setOnClickListener(v -> {
      keyboard2.closeMagicTools();
      keyboard2.showAiPane();
    });

    View writing = view.findViewById(R.id.card_action_writing);
    if (writing != null) {
      writing.setOnClickListener(v -> {
        keyboard2.closeMagicTools();
        keyboard2.startWritingTools(false);
      });
    }

    View mathSolver = view.findViewById(R.id.card_action_math_solver);
    if (mathSolver != null) {
      mathSolver.setOnClickListener(v -> {
        keyboard2.closeMagicTools();
        android.view.inputmethod.InputConnection ic = keyboard2.getCurrentInputConnection();
        CharSequence selected = (ic != null) ? ic.getSelectedText(0) : null;
        CharSequence before = selected != null && selected.length() > 0 ? selected
            : (ic != null) ? ic.getTextBeforeCursor(160, 0) : null;
        String problem = (before != null) ? before.toString().trim() : "";
        keyboard2.showMathSolver(problem);
      });
    }

    // Setup Close Button
    ImageView btnClose = view.findViewById(R.id.btn_hub_close);
    if (btnClose != null)
    {
      btnClose.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          keyboard2.closeMagicTools();
        }
      });
    }

    final android.view.inputmethod.EditorInfo editor = keyboard2.getCurrentInputEditorInfo();
    final String editorPackage = editor == null ? null : editor.packageName;
    // Vision Callback
    final AiScreenCaptureActivity.AiVisionCallback visionCallback = new AiScreenCaptureActivity.AiVisionCallback()
    {
      @Override
      public void onImageCaptured(byte[] jpegBytes, String tone)
      {
        keyboard2.showCapturedWritingImage(jpegBytes, tone, editorPackage);
      }

      @Override
      public void onError(String errorMessage)
      {
        if (errorMessage != null && !errorMessage.isEmpty())
        {
          Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        }
      }
    };

    // Request consent immediately while the keyboard is still a visible user action.
    View cardScreenScan = view.findViewById(R.id.card_action_screen_scan);
    if (cardScreenScan != null)
    {
      cardScreenScan.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          keyboard2.closeMagicTools();
          if (ensureApiKey(context, keyboard2))
          {
            // Immediately hide keyboard so it is not visible on the screenshot
            if (keyboard2 != null)
            {
              try { keyboard2.requestHideSelf(0); } catch (Throwable ignored) {}
            }
            AiScreenCaptureActivity.startScreenScan(context, DEFAULT_TONE, visionCallback);
          }
        }
      });
    }

    // 2. Recent Screenshot Card
    View cardScreenshot = view.findViewById(R.id.card_action_screenshot);
    if (cardScreenshot != null)
    {
      cardScreenshot.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          keyboard2.closeMagicTools();
          if (ensureApiKey(context, keyboard2))
          {
            AiScreenCaptureActivity.processRecentScreenshot(context, DEFAULT_TONE, visionCallback);
          }
        }
      });
    }

    // 3. Gallery Card
    View cardGallery = view.findViewById(R.id.card_action_gallery);
    if (cardGallery != null)
    {
      cardGallery.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          keyboard2.closeMagicTools();
          if (ensureApiKey(context, keyboard2))
          {
            AiScreenCaptureActivity.pickFromGallery(context, DEFAULT_TONE, visionCallback);
          }
        }
      });
    }

    // 4. Copied Post Reply Card
    View cardCopied = view.findViewById(R.id.card_action_copied_reply);
    if (cardCopied != null)
    {
      cardCopied.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (!ensureApiKey(context, keyboard2)) return;

          String clip = getClipboardText(context);
          if (clip == null || clip.trim().isEmpty())
          {
            Toast.makeText(context, "Copy a post or message first, then try again.", Toast.LENGTH_LONG).show();
            return;
          }

          if (keyboard2.getCandidatesView() != null)
          {
            keyboard2.closeMagicTools();
            keyboard2.getCandidatesView().generateAndShowTextComments(clip.trim(), DEFAULT_TONE);
          }
        }
      });
    }

    // 5. Autocomplete Card
    View cardAutocomplete = view.findViewById(R.id.card_action_autocomplete);
    if (cardAutocomplete != null)
    {
      cardAutocomplete.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          keyboard2.closeMagicTools();
          if (keyboard2.getCandidatesView() != null)
          {
            keyboard2.getCandidatesView().trigger_magic_autocomplete();
          }
        }
      });
    }

    view.setBackgroundColor(keyboard2.getColorKeyboard());
    applyTheme(view, keyboard2);
    float density = context.getResources().getDisplayMetrics().density;
    for (int id : new int[] {R.id.btn_hub_guide, R.id.btn_hub_restore_draft,
        R.id.btn_hub_guide_settings}) {
      TextView button = view.findViewById(id);
      button.setBackgroundTintList(null);
      GradientDrawable surface = new GradientDrawable();
      surface.setColor(keyboard2.getColorKey());
      surface.setCornerRadius(12 * density);
      surface.setStroke(Math.round(density), ColorUtils.setAlphaComponent(keyboard2.getColorLabel(), 65));
      button.setBackground(new RippleDrawable(ColorStateList.valueOf(
          ColorUtils.setAlphaComponent(keyboard2.getColorLabel(), 40)), surface, null));
      button.setTextColor(keyboard2.getColorLabel());
      button.setTextSize(14);
      button.setPadding(Math.round(12 * density), Math.round(8 * density),
          Math.round(12 * density), Math.round(8 * density));
      android.widget.LinearLayout.LayoutParams params =
          (android.widget.LinearLayout.LayoutParams)button.getLayoutParams();
      params.bottomMargin = Math.round(8 * density);
      button.setLayoutParams(params);
    }
    for (int id : new int[] {R.id.card_action_writing, R.id.card_action_copied_reply,
        R.id.card_action_screen_scan, R.id.card_action_screenshot,
        R.id.card_action_gallery, R.id.card_action_autocomplete}) {
      View action = view.findViewById(id);
      GradientDrawable surface = new GradientDrawable();
      surface.setColor(keyboard2.getColorKey());
      surface.setCornerRadius(12 * density);
      action.setBackground(new RippleDrawable(ColorStateList.valueOf(
          ColorUtils.setAlphaComponent(keyboard2.getColorLabel(), 40)), surface, null));
      android.widget.LinearLayout.LayoutParams params =
          (android.widget.LinearLayout.LayoutParams)action.getLayoutParams();
      params.bottomMargin = Math.round(4 * density);
      action.setLayoutParams(params);
    }
    return view;
  }

  private static void applyTheme(View view, Keyboard2 keyboard)
  {
    if (view instanceof TextView)
    {
      TextView text = (TextView)view;
      text.setTextColor(text.getTextSize() / text.getResources().getDisplayMetrics().scaledDensity <= 12
          ? ColorUtils.blendARGB(keyboard.getColorKeyboard(), keyboard.getColorLabel(), .75f)
          : keyboard.getColorLabel());
    }
    else if (view instanceof ImageView)
      ((ImageView)view).setColorFilter(keyboard.getColorLabel());
    if (view instanceof ViewGroup)
    {
      ViewGroup group = (ViewGroup)view;
      for (int i = 0; i < group.getChildCount(); i++) applyTheme(group.getChildAt(i), keyboard);
    }
  }

  private static boolean ensureApiKey(Context context, Keyboard2 keyboard2)
  {
    String apiKey = GeminiAiService.getApiKey(context);
    if (apiKey == null || apiKey.trim().isEmpty())
    {
      Toast.makeText(context, "Add your API key in Writing Settings to use these tools.", Toast.LENGTH_LONG).show();
      if (keyboard2 != null)
      {
        keyboard2.closeMagicTools();
        keyboard2.showAiPane();
      }
      return false;
    }
    return true;
  }

  private static String getClipboardText(Context context)
  {
    try
    {
      ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0)
      {
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        if (item != null && item.getText() != null)
        {
          return item.getText().toString();
        }
      }
    }
    catch (Throwable ignored)
    {
    }
    return null;
  }
}
