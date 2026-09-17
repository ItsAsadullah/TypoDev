package juloo.keyboard2.translate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Utils;

public class TranslationDialog
{
  private static final String[] LANG_NAMES = {
    "English (ইংরেজি)",
    "Bengali (বাংলা)",
    "Arabic (العربية)",
    "Spanish (Español)",
    "Hindi (हिन्दी)",
    "French (Français)",
    "German (Deutsch)",
    "Japanese (日本語)"
  };

  private static final String[] LANG_CODES = {
    "en",
    "bn",
    "ar",
    "es",
    "hi",
    "fr",
    "de",
    "ja"
  };

  public static void showTranslateMenu(final Context context, final Keyboard2 keyboard)
  {
    final InputConnection ic = keyboard.getCurrentInputConnection();
    if (ic == null)
    {
      Toast.makeText(context, "No active input field", Toast.LENGTH_SHORT).show();
      return;
    }

    CharSequence selected = ic.getSelectedText(0);
    CharSequence before = ic.getTextBeforeCursor(500, 0);
    if ((selected == null || selected.length() == 0) && (before == null || before.length() == 0))
    {
      Toast.makeText(context, "Please type or select text to translate", Toast.LENGTH_SHORT).show();
      return;
    }

    AlertDialog dialog = new AlertDialog.Builder(context)
      .setTitle("🌐 Translate to / কোন ভাষায় অনুবাদ করবেন?")
      .setItems(LANG_NAMES, new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          String targetLang = LANG_CODES[which];
          translateCurrentText(context, keyboard, targetLang);
        }
      })
      .setNegativeButton(android.R.string.cancel, null)
      .create();

    Utils.show_dialog_on_ime(dialog, keyboard);
  }

  private static void translateCurrentText(final Context context, final Keyboard2 keyboard, final String targetLang)
  {
    final InputConnection ic = keyboard.getCurrentInputConnection();
    if (ic == null) return;

    CharSequence selectedText = ic.getSelectedText(0);
    final boolean hasSelection = (selectedText != null && selectedText.length() > 0);
    final String textToTranslate;

    if (hasSelection)
    {
      textToTranslate = selectedText.toString();
    }
    else
    {
      CharSequence before = ic.getTextBeforeCursor(500, 0);
      if (before == null || before.length() == 0) return;
      textToTranslate = before.toString();
    }

    Toast.makeText(context, "🌐 Translating...", Toast.LENGTH_SHORT).show();

    TranslationService.translate(textToTranslate, "auto", targetLang, new TranslationService.TranslateCallback()
    {
      @Override
      public void onSuccess(String translatedText)
      {
        InputConnection conn = keyboard.getCurrentInputConnection();
        if (conn == null) return;

        if (hasSelection)
        {
          conn.commitText(translatedText, 1);
        }
        else
        {
          conn.deleteSurroundingText(textToTranslate.length(), 0);
          conn.commitText(translatedText, 1);
        }
        Toast.makeText(context, "🌐 Translated!", Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onError(String error)
      {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
      }
    });
  }
}
