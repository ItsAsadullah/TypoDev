package typodev.keyboard.clipboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import typodev.keyboard.DialogTheme;
import typodev.keyboard.R;
import typodev.keyboard.Utils;

/**
 * Dialog allowing the user to manually type and pin a custom note, template,
 * address, or number directly into their pinned clipboard.
 */
public class AddClipDialog
{
  public interface OnClipAddedListener
  {
    void onClipAdded(String text);
  }

  public static void show(final Context context, final IBinder windowToken, final OnClipAddedListener listener)
  {
    final float density = context.getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
    rootBg.setColor(palette.dialogBg);
    rootBg.setCornerRadius(16f * density);
    rootBg.setStroke(Math.round(1.5f * density), palette.dialogBorder);
    root.setBackground(rootBg);
    int padH = Math.round(18f * density);
    int padV = Math.round(16f * density);
    root.setPadding(padH, padV, padH, padV);

    // Title row with icon
    LinearLayout titleRow = new LinearLayout(context);
    titleRow.setOrientation(LinearLayout.HORIZONTAL);
    titleRow.setGravity(Gravity.CENTER_VERTICAL);
    titleRow.setPadding(0, 0, 0, Math.round(12f * density));

    ImageView ivIcon = new ImageView(context);
    ivIcon.setImageResource(R.drawable.ic_clip_pin);
    ivIcon.setColorFilter(palette.accentColor);
    int iconSize = Math.round(20f * density);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(iconSize, iconSize);
    lpIcon.rightMargin = Math.round(8f * density);
    titleRow.addView(ivIcon, lpIcon);

    TextView tvTitle = new TextView(context);
    tvTitle.setText("Add to Pinned Clipboard");
    tvTitle.setTextColor(palette.textPrimary);
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    titleRow.addView(tvTitle);
    root.addView(titleRow);

    final EditText etInput = new EditText(context);
    etInput.setHint("Type or paste text to pin (e.g. number, email, address)...");
    etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    etInput.setMinLines(3);
    etInput.setMaxLines(8);
    etInput.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
    DialogTheme.styleEditText(etInput, palette, density);

    root.addView(etInput, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // Buttons row
    LinearLayout btnRow = new LinearLayout(context);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.END);
    btnRow.setPadding(0, Math.round(14f * density), 0, 0);

    // Cancel button
    TextView btnCancel = new TextView(context);
    btnCancel.setText("Cancel");
    btnCancel.setTextColor(palette.textSecondary);
    btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    btnCancel.setPadding(Math.round(12f * density), Math.round(8f * density), Math.round(12f * density), Math.round(8f * density));

    // Pin button with icon
    int pinTextColor = DialogTheme.getContrastingTextColor(palette.accentColor);
    LinearLayout btnPin = new LinearLayout(context);
    btnPin.setOrientation(LinearLayout.HORIZONTAL);
    btnPin.setGravity(Gravity.CENTER_VERTICAL);
    android.graphics.drawable.GradientDrawable pinBg = new android.graphics.drawable.GradientDrawable();
    pinBg.setColor(palette.accentColor);
    pinBg.setCornerRadius(8f * density);
    btnPin.setBackground(pinBg);
    btnPin.setPadding(Math.round(12f * density), Math.round(8f * density), Math.round(14f * density), Math.round(8f * density));

    ImageView ivPin = new ImageView(context);
    ivPin.setImageResource(R.drawable.ic_clip_pin);
    ivPin.setColorFilter(pinTextColor);
    int pinIconSize = Math.round(16f * density);
    LinearLayout.LayoutParams lpPinIcon = new LinearLayout.LayoutParams(pinIconSize, pinIconSize);
    lpPinIcon.rightMargin = Math.round(6f * density);
    btnPin.addView(ivPin, lpPinIcon);

    TextView tvPin = new TextView(context);
    tvPin.setText("Pin");
    tvPin.setTextColor(pinTextColor);
    tvPin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    tvPin.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    btnPin.addView(tvPin);

    LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpCancel.rightMargin = Math.round(8f * density);
    btnRow.addView(btnCancel, lpCancel);
    btnRow.addView(btnPin, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    root.addView(btnRow);

    final AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setView(root)
        .create();

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);

    btnCancel.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        dialog.dismiss();
      }
    });

    btnPin.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String text = etInput.getText() != null ? etInput.getText().toString().trim() : "";
        if (!text.isEmpty())
        {
          if (listener != null)
          {
            listener.onClipAdded(text);
          }
          Toast.makeText(context, "Pinned to clipboard", Toast.LENGTH_SHORT).show();
          dialog.dismiss();
        }
      }
    });

    if (windowToken != null)
    {
      Utils.show_dialog_on_ime(dialog, windowToken);
    }
    else
    {
      dialog.show();
    }
  }
}
