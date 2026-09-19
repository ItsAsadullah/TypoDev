package juloo.keyboard2.snippet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.IBinder;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import juloo.keyboard2.Utils;

/**
 * Dialog for adding or editing a snippet.
 *
 * Can be opened:
 *   - From SnippetManagerActivity with no pre-fill (new snippet)
 *   - From SnippetManagerActivity with a Snippet to edit
 *   - From ClipboardHistoryView with pre-filled expansion text
 */
public final class SnippetEditorDialog
{
  public interface OnSavedCallback
  {
    void onSnippetSaved(Snippet snippet);
  }

  private SnippetEditorDialog() {}

  /**
   * Show dialog to CREATE a new snippet.
   * @param context  Activity or Service context.
   * @param prefilledExpansion  If not null/empty, pre-fills the expansion field (e.g. from clipboard).
   * @param callback  Called when the user successfully saves the snippet.
   */
  public static AlertDialog showCreate(Context context, String prefilledExpansion, OnSavedCallback callback)
  {
    return showInternal(context, null, null, prefilledExpansion, callback, null);
  }

  /**
   * Show dialog from inside an IME window (e.g. clipboard pane).
   * Requires the window token to attach the dialog to the IME layer.
   */
  public static AlertDialog showCreate(Context context, IBinder windowToken,
      String prefilledExpansion, OnSavedCallback callback)
  {
    return showInternal(context, windowToken, null, prefilledExpansion, callback, null);
  }

  /**
   * Show dialog to EDIT an existing snippet.
   */
  public static AlertDialog showEdit(Context context, Snippet existing, OnSavedCallback callback)
  {
    return showInternal(context, null, existing, null, callback, null);
  }

  public static AlertDialog showInternal(final Context context, final IBinder windowToken,
      final Snippet existing, String prefilledExpansion, final OnSavedCallback callback)
  {
    return showInternal(context, windowToken, existing, prefilledExpansion, callback, null);
  }

  public static AlertDialog showInternal(final Context context, final IBinder windowToken,
      final Snippet existing, String prefilledExpansion, final OnSavedCallback callback,
      final DialogInterface.OnDismissListener dismissListener)
  {
    // If context is an InputMethodService or not an Activity, redirect to SnippetEditActivity
    // so Android's InputMethodManager can attach the software keyboard to type into the EditTexts!
    if (context instanceof android.inputmethodservice.InputMethodService || !(context instanceof Activity))
    {
      Intent intent = new Intent(context, SnippetEditActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
      if (existing != null) intent.putExtra("snippet_id", existing.id);
      if (prefilledExpansion != null) intent.putExtra("clip_text", prefilledExpansion);
      context.startActivity(intent);
      return null;
    }

    final float density = context.getResources().getDisplayMetrics().density;
    final juloo.keyboard2.DialogTheme.Palette palette = juloo.keyboard2.DialogTheme.getPalette(context);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = (int)(16 * density);
    root.setPadding(pad, pad, pad, 0);

    // ── Header Title ──────────────────────────────────────────
    TextView tvTitle = new TextView(context);
    tvTitle.setText(existing != null ? "✏️ Edit Snippet" : "✨ New Snippet");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    tvTitle.setTypeface(null, Typeface.BOLD);
    tvTitle.setTextColor(palette.textPrimary);
    tvTitle.setPadding(0, 0, 0, (int)(8 * density));
    root.addView(tvTitle);

    // ── Shortcut field ────────────────────────────────────────
    root.addView(makeLabel(context, "Shortcut (trigger word)", density, palette));
    final EditText etShortcut = makeEditText(context, "e.g. email, greet, phone", density, palette);
    etShortcut.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    if (existing != null) etShortcut.setText(existing.shortcut);
    root.addView(etShortcut);

    // ── Expansion field ───────────────────────────────────────
    root.addView(makeLabel(context, "Expansion (full text)", density, palette));
    final EditText etExpansion = makeEditText(context, "e.g. asad@example.com", density, palette);
    etExpansion.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    etExpansion.setMinLines(2);
    etExpansion.setMaxLines(5);
    etExpansion.setGravity(Gravity.TOP | Gravity.START);
    if (existing != null)
      etExpansion.setText(existing.expansion);
    else if (prefilledExpansion != null && !prefilledExpansion.isEmpty())
      etExpansion.setText(prefilledExpansion);
    root.addView(etExpansion);

    // ── Name field ────────────────────────────────────────────
    root.addView(makeLabel(context, "Name (optional label)", density, palette));
    final EditText etName = makeEditText(context, "e.g. My Email Address", density, palette);
    if (existing != null) etName.setText(existing.name);
    root.addView(etName);

    // ── Category spinner ──────────────────────────────────────
    root.addView(makeLabel(context, "Category", density, palette));
    final Spinner spinnerCat = new Spinner(context);
    SnippetCategory[] cats = SnippetCategory.values();
    String[] catLabels = new String[cats.length];
    for (int i = 0; i < cats.length; i++) catLabels[i] = cats[i].displayName();
    ArrayAdapter<String> catAdapter = juloo.keyboard2.DialogTheme.createThemedAdapter(
        context, catLabels, palette, density);
    spinnerCat.setAdapter(catAdapter);
    juloo.keyboard2.DialogTheme.styleSpinner(spinnerCat, palette, density);
    if (existing != null)
    {
      for (int i = 0; i < cats.length; i++)
        if (cats[i] == existing.category) { spinnerCat.setSelection(i); break; }
    }
    LinearLayout.LayoutParams lpSpinner = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpSpinner.setMargins(0, 0, 0, (int)(8 * density));
    spinnerCat.setLayoutParams(lpSpinner);
    root.addView(spinnerCat);

    // ── Dialog ────────────────────────────────────────────────
    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setView(root)
        .setPositiveButton("Save", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface d, int which)
          {
            String shortcut = etShortcut.getText().toString().trim().toLowerCase();
            String expansion = etExpansion.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (shortcut.isEmpty())
            {
              Toast.makeText(context, "Shortcut cannot be empty", Toast.LENGTH_SHORT).show();
              return;
            }
            if (expansion.isEmpty())
            {
              Toast.makeText(context, "Expansion cannot be empty", Toast.LENGTH_SHORT).show();
              return;
            }
            if (name.isEmpty()) name = shortcut;

            SnippetCategory cat = cats[spinnerCat.getSelectedItemPosition()];

            Snippet result;
            if (existing != null)
            {
              // Edit existing
              existing.shortcut = shortcut;
              existing.expansion = expansion;
              existing.name = name;
              existing.category = cat;
              result = existing;
            }
            else
            {
              result = Snippet.create(shortcut, expansion, name, cat);
            }

            SnippetStore.instance(context).save(result);
            Toast.makeText(context, "✅ Snippet saved: \"" + shortcut + "\"", Toast.LENGTH_SHORT).show();

            if (callback != null) callback.onSnippetSaved(result);
          }
        })
        .setNegativeButton("Cancel", null)
        .create();

    if (dismissListener != null)
    {
      dialog.setOnDismissListener(dismissListener);
    }

    juloo.keyboard2.DialogTheme.applyDialogWindowStyle(dialog, palette, density);

    if (dialog.getWindow() != null)
    {
      dialog.getWindow().clearFlags(
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
          | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
      dialog.getWindow().setSoftInputMode(
          WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
          | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    dialog.setOnShowListener(new DialogInterface.OnShowListener()
    {
      @Override
      public void onShow(DialogInterface d)
      {
        juloo.keyboard2.DialogTheme.styleButtonsNow(dialog, palette);
        etShortcut.requestFocus();
        etShortcut.postDelayed(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
              if (imm != null)
              {
                imm.showSoftInput(etShortcut, InputMethodManager.SHOW_IMPLICIT);
              }
            }
            catch (Throwable ignored) {}
          }
        }, 120);
      }
    });

    try
    {
      dialog.show();
    }
    catch (Throwable t)
    {
      juloo.keyboard2.Logs.exn("SnippetEditorDialog", t);
    }

    return dialog;
  }

  private static TextView makeLabel(Context context, String text, float density, juloo.keyboard2.DialogTheme.Palette p)
  {
    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setTextColor(p.textSecondary);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, (int)(8 * density), 0, (int)(3 * density));
    tv.setLayoutParams(lp);
    return tv;
  }

  private static EditText makeEditText(Context context, String hint, float density, juloo.keyboard2.DialogTheme.Palette p)
  {
    EditText et = new EditText(context);
    et.setHint(hint);
    juloo.keyboard2.DialogTheme.styleEditText(et, p, density);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, 0, 0, (int)(4 * density));
    et.setLayoutParams(lp);
    return et;
  }
}
