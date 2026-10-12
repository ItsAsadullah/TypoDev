package typodev.keyboard.autofill;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import typodev.keyboard.ClipboardHistoryService;
import typodev.keyboard.DialogTheme;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.Logs;
import typodev.keyboard.Utils;

public final class PasswordAutofillHelper
{
  private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
  private static final String DIGITS = "23456789";
  private static final String SYMBOLS = "!@#$%^&*_-+=";
  private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SYMBOLS;

  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordAutofillHelper() {}

  /**
   * Generates a strong, high-entropy password of specified length.
   * Guarantees at least 1 uppercase, 1 lowercase, 1 digit, and 1 symbol.
   */
  public static String generateStrongPassword(int length)
  {
    if (length < 8) length = 14;

    List<Character> chars = new ArrayList<>();
    chars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
    chars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
    chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
    chars.add(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));

    for (int i = 4; i < length; i++)
    {
      chars.add(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
    }

    Collections.shuffle(chars, RANDOM);

    StringBuilder sb = new StringBuilder();
    for (char c : chars)
    {
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Triggers Android system Autofill service (Google Autofill, Bitwarden, Samsung Pass, etc.)
   */
  public static boolean triggerSystemAutofill(Keyboard2 keyboard)
  {
    if (keyboard == null) return false;
    boolean handled = false;
    try
    {
      InputConnection ic = keyboard.getCurrentInputConnection();
      if (ic != null)
      {
        handled = ic.performContextMenuAction(android.R.id.autofill);
      }
    }
    catch (Throwable ignored) {}

    return handled;
  }

  /**
   * Generates a strong password, inserts it into current input, and copies to clipboard.
   */
  public static void generateAndInsertPassword(Keyboard2 keyboard, Context context)
  {
    if (keyboard == null) return;
    String pwd = generateStrongPassword(14);
    InputConnection ic = keyboard.getCurrentInputConnection();
    if (ic != null)
    {
      ic.commitText(pwd, 1);
    }

    // Save to clipboard history so user never loses it
    ClipboardHistoryService hs = ClipboardHistoryService.get_service(context);
    if (hs != null)
    {
      hs.add_clip(pwd);
    }

    Toast.makeText(context, "Password generated & copied to clipboard", Toast.LENGTH_SHORT).show();
  }

  /**
   * Opens Android OS Autofill Settings so user can enable or configure autofill providers.
   */
  public static void openAutofillSettings(Context context)
  {
    if (context == null) return;
    if (Build.VERSION.SDK_INT >= 26)
    {
      try
      {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
        intent.setData(Uri.parse("package:android"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return;
      }
      catch (Throwable ignored) {}
    }

    try
    {
      Intent intent = new Intent(Settings.ACTION_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    }
    catch (Throwable ignored) {}
  }

  /**
   * Opens Google Password Manager directly if available.
   */
  public static void openGooglePasswordManager(Context context)
  {
    if (context == null) return;
    try
    {
      Intent gmsIntent = new Intent("com.google.android.gms.settings.AUTOFILL_SETTINGS");
      gmsIntent.setPackage("com.google.android.gms");
      gmsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(gmsIntent);
      return;
    }
    catch (Throwable ignored) {}

    try
    {
      Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://passwords.google.com"));
      webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(webIntent);
    }
    catch (Throwable ignored) {}
  }

  /**
   * Shows a quick interactive dialog for password and autofill actions.
   */
  public static void showPasswordMenu(final Context context, final Keyboard2 keyboard)
  {
    showPasswordMenu(context, keyboard, null);
  }

  /**
   * Shows a quick interactive dialog for password and autofill actions using a valid window token.
   */
  public static void showPasswordMenu(final Context context, final Keyboard2 keyboard, final IBinder windowToken)
  {
    if (context == null || keyboard == null) return;

    try
    {
      CharSequence[] options = new CharSequence[] {
          "Autofill Saved Credentials",
          "Generate Strong Password (14 chars)",
          "Google Password Manager",
          "Autofill Service Settings"
      };

      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setTitle("Password & Autofill");
      builder.setItems(options, new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          switch (which)
          {
            case 0:
              boolean ok = triggerSystemAutofill(keyboard);
              if (!ok)
              {
                Toast.makeText(context, "No saved credentials found for this field", Toast.LENGTH_SHORT).show();
              }
              break;
            case 1:
              generateAndInsertPassword(keyboard, context);
              break;
            case 2:
              openGooglePasswordManager(context);
              break;
            case 3:
              openAutofillSettings(context);
              break;
          }
        }
      });
      builder.setNegativeButton(android.R.string.cancel, null);

      final AlertDialog dialog = builder.create();

      final DialogTheme.Palette palette = new DialogTheme.Palette(context);
      final float density = context.getResources().getDisplayMetrics().density;

      try
      {
        DialogTheme.applyDialogWindowStyle(dialog, palette, density);
      }
      catch (Throwable ignored) {}

      dialog.setOnShowListener(new DialogInterface.OnShowListener()
      {
        @Override
        public void onShow(DialogInterface d)
        {
          try
          {
            DialogTheme.styleDialogListItems(dialog, palette);
            DialogTheme.styleButtonsNow(dialog, palette);
          }
          catch (Throwable ignored) {}
        }
      });

      IBinder token = windowToken;
      if (token == null && keyboard.getWindow() != null && keyboard.getWindow().getWindow() != null)
      {
        token = keyboard.getWindow().getWindow().getDecorView().getWindowToken();
      }

      if (token != null)
      {
        Utils.show_dialog_on_ime(dialog, token);
      }
      else
      {
        Utils.show_dialog_on_ime(dialog, keyboard);
      }
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
      try
      {
        Toast.makeText(context, "Autofill requested", Toast.LENGTH_SHORT).show();
      }
      catch (Throwable ignored) {}
    }
  }
}
