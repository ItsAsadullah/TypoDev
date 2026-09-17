package juloo.keyboard2.ai;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Utils;

public class AiActionDialog
{
  public static void showAiMenu(final Context context, final Keyboard2 keyboard)
  {
    final String currentKey = GeminiAiService.getApiKey(context);
    if (currentKey.isEmpty())
    {
      showApiKeyDialog(context, keyboard);
      return;
    }

    final String currentModel = GeminiAiService.getModel(context);
    final String[] actions = {
      "🚀 Telegram-Style AI Editor (লাইভ প্রিভিউ ও কাস্টম স্টাইল)",
      "📝 Grammar & Spell Fix (ব্যাকরণ ও বানান সংশোধন)",
      "💼 Professional Tone (পেশাদার টোন)",
      "😊 Casual & Friendly (বন্ধুসুলভ টোন)",
      "✍️ Rewrite & Polish (মার্জিতভাবে নতুন করে লেখা)",
      "🎭 Formal Mood (ফরমাল ভাব)",
      "⚡ Enthusiastic Mood (উৎসাহব্যঞ্জক ভাব)",
      "🤖 Model: " + currentModel + " (মডেল পরিবর্তন)",
      "🔑 Change Gemini API Key (এআই কী পরিবর্তন)"
    };

    AlertDialog dialog = new AlertDialog.Builder(context)
      .setTitle("✨ AI Writing Assistant")
      .setItems(actions, new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          if (which == 0)
          {
            AiEditorDialog.show(context, keyboard);
            return;
          }
          if (which == 7)
          {
            showModelSelectDialog(context, keyboard);
            return;
          }
          if (which == 8)
          {
            showApiKeyDialog(context, keyboard);
            return;
          }

          GeminiAiService.Action action = null;
          switch (which)
          {
            case 1: action = GeminiAiService.Action.GRAMMAR_FIX; break;
            case 2: action = GeminiAiService.Action.TONE_PROFESSIONAL; break;
            case 3: action = GeminiAiService.Action.TONE_CASUAL; break;
            case 4: action = GeminiAiService.Action.REWRITE_POLISH; break;
            case 5: action = GeminiAiService.Action.MOOD_FORMAL; break;
            case 6: action = GeminiAiService.Action.MOOD_ENTHUSIASTIC; break;
          }

          if (action != null)
            executeAiAction(context, keyboard, action);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .create();

    Utils.show_dialog_on_ime(dialog, keyboard);
  }

  public static void showModelSelectDialog(final Context context, final Keyboard2 keyboard)
  {
    final String[] models = {
      "Gemini 2.0 Flash (ডিফল্ট / দ্রুততম ও রিকমেন্ডেড)",
      "Gemini 2.0 Flash-Lite (হালকা ও অত্যন্ত দ্রুত)",
      "Gemini 1.5 Flash (উচ্চ ফ্রি-কোটা ও নির্ভরযোগ্য)",
      "Gemini 1.5 Flash-8B (লাইটওয়েট)",
      "Gemini 1.5 Pro (উন্নত রিজনিং ও বড় কনটেক্সট)",
      "Gemini 2.0 Pro Experimental (এক্সপেরিমেন্টাল)"
    };
    final String[] modelValues = {
      "gemini-2.0-flash",
      "gemini-2.0-flash-lite",
      "gemini-1.5-flash",
      "gemini-1.5-flash-8b",
      "gemini-1.5-pro",
      "gemini-2.0-pro-exp-02-05"
    };

    String current = GeminiAiService.getModel(context);
    int selectedIdx = 0;
    for (int i = 0; i < modelValues.length; i++)
    {
      if (modelValues[i].equalsIgnoreCase(current))
      {
        selectedIdx = i;
        break;
      }
    }

    AlertDialog dialog = new AlertDialog.Builder(context)
      .setTitle("🤖 Select Gemini Model")
      .setSingleChoiceItems(models, selectedIdx, new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface d, int which)
        {
          GeminiAiService.setModel(context, modelValues[which]);
          Toast.makeText(context, "✅ Model changed to: " + modelValues[which], Toast.LENGTH_SHORT).show();
          d.dismiss();
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .create();

    Utils.show_dialog_on_ime(dialog, keyboard);
  }

  private static void executeAiAction(final Context context, final Keyboard2 keyboard, final GeminiAiService.Action action)
  {
    final InputConnection ic = keyboard.getCurrentInputConnection();
    if (ic == null)
    {
      Toast.makeText(context, "No active input field", Toast.LENGTH_SHORT).show();
      return;
    }

    CharSequence selectedText = ic.getSelectedText(0);
    final boolean hasSelection = (selectedText != null && selectedText.length() > 0);
    final String textToProcess;

    if (hasSelection)
    {
      textToProcess = selectedText.toString();
    }
    else
    {
      CharSequence before = ic.getTextBeforeCursor(500, 0);
      if (before == null || before.length() == 0)
      {
        Toast.makeText(context, "Please type or select text first", Toast.LENGTH_SHORT).show();
        return;
      }
      textToProcess = before.toString();
    }

    Toast.makeText(context, "✨ AI is thinking...", Toast.LENGTH_SHORT).show();

    GeminiAiService.processText(context, textToProcess, action, new GeminiAiService.AiCallback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        InputConnection conn = keyboard.getCurrentInputConnection();
        if (conn == null) return;

        if (hasSelection)
        {
          conn.commitText(resultText, 1);
        }
        else
        {
          conn.deleteSurroundingText(textToProcess.length(), 0);
          conn.commitText(resultText, 1);
        }
        Toast.makeText(context, "✨ Done!", Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onError(String errorMessage)
      {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
      }
    });
  }

  public static void showApiKeyDialog(final Context context, final Keyboard2 keyboard)
  {
    String clipboardKey = "";
    final ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm != null && cm.hasPrimaryClip())
    {
      ClipData clip = cm.getPrimaryClip();
      if (clip != null && clip.getItemCount() > 0)
      {
        CharSequence cs = clip.getItemAt(0).getText();
        if (cs != null)
          clipboardKey = cs.toString().trim();
      }
    }

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad / 2, pad, pad / 2);

    TextView helpText = new TextView(context);
    helpText.setText("Google AI Studio (aistudio.google.com) থেকে আপনার Gemini API Key কপি করে নিচের বাটনে চাপ দিন:");
    helpText.setPadding(0, 0, 0, pad / 2);
    layout.addView(helpText);

    final EditText input = new EditText(context);
    input.setHint("Paste or type Gemini API key");
    String savedKey = GeminiAiService.getApiKey(context);
    if (!savedKey.isEmpty())
      input.setText(savedKey);
    else if (clipboardKey.startsWith("AIza") || clipboardKey.length() > 20)
      input.setText(clipboardKey);
    layout.addView(input);

    Button btnPaste = new Button(context);
    btnPaste.setText("📋 Paste from Clipboard (ক্লিপবোর্ড থেকে পেস্ট)");
    layout.addView(btnPaste);

    Button btnSettings = new Button(context);
    btnSettings.setText("⚙️ Open in Settings (সেটিংসে খুলুন)");
    layout.addView(btnSettings);

    final AlertDialog dialog = new AlertDialog.Builder(context)
      .setTitle("✨ Google Gemini API Key")
      .setView(layout)
      .setPositiveButton("Save", new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface d, int which)
        {
          String key = input.getText().toString().trim();
          if (!key.isEmpty())
          {
            GeminiAiService.setApiKey(context, key);
            Toast.makeText(context, "✅ API Key Saved!", Toast.LENGTH_SHORT).show();
          }
          else
          {
            Toast.makeText(context, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
          }
        }
      })
      .setNeutralButton("Get Key", new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface d, int which)
        {
          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(intent);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .create();

    btnPaste.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (cm != null && cm.hasPrimaryClip())
        {
          ClipData clip = cm.getPrimaryClip();
          if (clip != null && clip.getItemCount() > 0)
          {
            CharSequence cs = clip.getItemAt(0).getText();
            if (cs != null && cs.length() > 0)
            {
              String pasted = cs.toString().trim();
              input.setText(pasted);
              GeminiAiService.setApiKey(context, pasted);
              Toast.makeText(context, "✅ Key Pasted & Saved Successfully!", Toast.LENGTH_SHORT).show();
              dialog.dismiss();
              return;
            }
          }
        }
        Toast.makeText(context, "Clipboard is empty! Copy your API key first.", Toast.LENGTH_SHORT).show();
      }
    });

    btnSettings.setOnClickListener(new View.OnClickListener()
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

    Window win = dialog.getWindow();
    if (win != null)
    {
      WindowManager.LayoutParams lp = win.getAttributes();
      lp.token = keyboard.getWindow().getWindow().getDecorView().getWindowToken();
      lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
      win.setAttributes(lp);
      win.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
      win.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
    dialog.show();
  }
}
