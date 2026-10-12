package typodev.keyboard.theme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorPickerDialog
{
  public interface OnColorSelectedListener
  {
    void onColorSelected(int color);
  }

  // 24 Curated Swatches: Dracula, Cyberpunk, Nord, Material
  private static final int[] PALETTE = {
      0xFF282A36, // Dracula BG
      0xFF343746, // Dracula Key
      0xFF44475A, // Dracula Current Line
      0xFF6272A4, // Dracula Slate
      0xFFBD93F9, // Dracula Purple
      0xFFFF79C6, // Dracula Pink
      0xFF50FA7B, // Dracula Green
      0xFFFF5555, // Dracula Coral Red
      0xFFFFB86C, // Dracula Orange
      0xFFF1FA8C, // Dracula Yellow
      0xFF8BE9FD, // Dracula Cyan
      0xFFF8F8F2, // Dracula White
      0xFF000000, // True Black
      0xFF120E24, // Cyberpunk Midnight
      0xFF00F0FF, // Neon Cyan
      0xFFFF007F, // Neon Magenta
      0xFF2E3440, // Nord Polar
      0xFF88C0D0, // Nord Frost
      0xFFA3BE8C, // Nord Green
      0xFFBF616A, // Nord Red
      0xFF3B82F6, // Material Blue
      0xFF10B981, // Material Emerald
      0xFF8B5CF6, // Material Purple
      0xFFFFFFFF  // Crisp White
  };

  private static final int[] HUE_COLORS = {
      0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
  };

  public static void show(Context context, String title, int initialColor, final OnColorSelectedListener listener)
  {
    final float density = context.getResources().getDisplayMetrics().density;
    final int[] selectedColor = new int[] { initialColor };
    final float[] currentHsv = new float[3];
    Color.colorToHSV(initialColor, currentHsv);

    ScrollView scrollView = new ScrollView(context);
    scrollView.setFillViewport(true);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20, density), dp(16, density), dp(20, density), dp(10, density));
    root.setBackgroundColor(Color.parseColor("#181B26"));
    scrollView.addView(root);

    // Title Header
    TextView tvTitle = new TextView(context);
    tvTitle.setText(title);
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
    tvTitle.setTextColor(Color.WHITE);
    tvTitle.setTypeface(null, Typeface.BOLD);
    root.addView(tvTitle);

    // Color Preview Bar & Hex Input Row
    LinearLayout previewRow = new LinearLayout(context);
    previewRow.setOrientation(LinearLayout.HORIZONTAL);
    previewRow.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams previewRowLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    previewRowLp.setMargins(0, dp(14, density), 0, dp(14, density));
    previewRow.setLayoutParams(previewRowLp);

    final View previewSwatch = new View(context);
    LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(dp(54, density), dp(44, density));
    previewSwatch.setLayoutParams(swatchLp);
    final GradientDrawable swatchBg = new GradientDrawable();
    swatchBg.setShape(GradientDrawable.RECTANGLE);
    swatchBg.setCornerRadius(dp(8, density));
    swatchBg.setColor(initialColor);
    swatchBg.setStroke(dp(1.5f, density), Color.parseColor("#4B5563"));
    previewSwatch.setBackground(swatchBg);
    previewRow.addView(previewSwatch);

    // Column for Hex input & RGB details
    LinearLayout inputCol = new LinearLayout(context);
    inputCol.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams inputColLp = new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    inputColLp.setMargins(dp(12, density), 0, 0, 0);
    inputCol.setLayoutParams(inputColLp);

    final EditText hexInput = new EditText(context);
    hexInput.setText(String.format("#%06X", (0xFFFFFF & initialColor)));
    hexInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    hexInput.setTextColor(Color.WHITE);
    hexInput.setHint("#RRGGBB");
    hexInput.setHintTextColor(Color.parseColor("#64748B"));
    GradientDrawable hexBg = new GradientDrawable();
    hexBg.setShape(GradientDrawable.RECTANGLE);
    hexBg.setCornerRadius(dp(8, density));
    hexBg.setColor(Color.parseColor("#222738"));
    hexBg.setStroke(dp(1, density), Color.parseColor("#374151"));
    hexInput.setBackground(hexBg);
    hexInput.setPadding(dp(12, density), dp(8, density), dp(12, density), dp(8, density));
    inputCol.addView(hexInput);

    final TextView tvRgb = new TextView(context);
    tvRgb.setText(formatRgb(initialColor));
    tvRgb.setTextColor(Color.parseColor("#94A3B8"));
    tvRgb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tvRgb.setPadding(dp(2, density), dp(4, density), 0, 0);
    inputCol.addView(tvRgb);

    previewRow.addView(inputCol);
    root.addView(previewRow);

    // Visual Custom Color Sliders Section Header
    TextView tvSliders = new TextView(context);
    tvSliders.setText("Interactive Custom Color Picker");
    tvSliders.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    tvSliders.setTextColor(Color.parseColor("#BD93F9"));
    tvSliders.setTypeface(null, Typeface.BOLD);
    root.addView(tvSliders);

    // 1. Hue Slider
    final TextView tvHueLabel = new TextView(context);
    tvHueLabel.setText(String.format("Hue: %d°", Math.round(currentHsv[0])));
    tvHueLabel.setTextColor(Color.parseColor("#CBD5E1"));
    tvHueLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tvHueLabel.setPadding(0, dp(8, density), 0, dp(4, density));
    root.addView(tvHueLabel);

    final SeekBar sbHue = new SeekBar(context);
    sbHue.setMax(360);
    sbHue.setProgress(Math.round(currentHsv[0]));
    GradientDrawable hueTrack = new GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT, HUE_COLORS);
    hueTrack.setCornerRadius(dp(6, density));
    sbHue.setProgressDrawable(hueTrack);
    root.addView(sbHue);

    // 2. Saturation Slider
    final TextView tvSatLabel = new TextView(context);
    tvSatLabel.setText(String.format("Saturation: %d%%", Math.round(currentHsv[1] * 100)));
    tvSatLabel.setTextColor(Color.parseColor("#CBD5E1"));
    tvSatLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tvSatLabel.setPadding(0, dp(10, density), 0, dp(4, density));
    root.addView(tvSatLabel);

    final SeekBar sbSat = new SeekBar(context);
    sbSat.setMax(100);
    sbSat.setProgress(Math.round(currentHsv[1] * 100));
    final GradientDrawable satTrack = new GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        new int[] { Color.HSVToColor(new float[]{ currentHsv[0], 0f, currentHsv[2] }),
                    Color.HSVToColor(new float[]{ currentHsv[0], 1f, currentHsv[2] }) });
    satTrack.setCornerRadius(dp(6, density));
    sbSat.setProgressDrawable(satTrack);
    root.addView(sbSat);

    // 3. Brightness / Value Slider
    final TextView tvValLabel = new TextView(context);
    tvValLabel.setText(String.format("Brightness: %d%%", Math.round(currentHsv[2] * 100)));
    tvValLabel.setTextColor(Color.parseColor("#CBD5E1"));
    tvValLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tvValLabel.setPadding(0, dp(10, density), 0, dp(4, density));
    root.addView(tvValLabel);

    final SeekBar sbVal = new SeekBar(context);
    sbVal.setMax(100);
    sbVal.setProgress(Math.round(currentHsv[2] * 100));
    final GradientDrawable valTrack = new GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        new int[] { Color.BLACK, Color.HSVToColor(new float[]{ currentHsv[0], currentHsv[1], 1f }) });
    valTrack.setCornerRadius(dp(6, density));
    sbVal.setProgressDrawable(valTrack);
    root.addView(sbVal);

    // Preset Swatches Header
    TextView tvPresets = new TextView(context);
    tvPresets.setText("Quick Palette Swatches");
    tvPresets.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    tvPresets.setTextColor(Color.parseColor("#BD93F9"));
    tvPresets.setTypeface(null, Typeface.BOLD);
    tvPresets.setPadding(0, dp(16, density), 0, dp(6, density));
    root.addView(tvPresets);

    // Swatches Grid
    GridLayout grid = new GridLayout(context);
    grid.setColumnCount(6);
    grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

    final boolean[] isSelfUpdating = new boolean[] { false };

    final Runnable updateSlidersFromColor = new Runnable()
    {
      @Override
      public void run()
      {
        isSelfUpdating[0] = true;
        Color.colorToHSV(selectedColor[0], currentHsv);
        sbHue.setProgress(Math.round(currentHsv[0]));
        sbSat.setProgress(Math.round(currentHsv[1] * 100));
        sbVal.setProgress(Math.round(currentHsv[2] * 100));
        tvHueLabel.setText(String.format("Hue: %d°", Math.round(currentHsv[0])));
        tvSatLabel.setText(String.format("Saturation: %d%%", Math.round(currentHsv[1] * 100)));
        tvValLabel.setText(String.format("Brightness: %d%%", Math.round(currentHsv[2] * 100)));

        satTrack.setColors(new int[] {
            Color.HSVToColor(new float[]{ currentHsv[0], 0f, currentHsv[2] }),
            Color.HSVToColor(new float[]{ currentHsv[0], 1f, currentHsv[2] })
        });
        valTrack.setColors(new int[] {
            Color.BLACK,
            Color.HSVToColor(new float[]{ currentHsv[0], currentHsv[1], 1f })
        });
        tvRgb.setText(formatRgb(selectedColor[0]));
        isSelfUpdating[0] = false;
      }
    };

    SeekBar.OnSeekBarChangeListener sliderListener = new SeekBar.OnSeekBarChangeListener()
    {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
      {
        if (!fromUser || isSelfUpdating[0]) return;
        currentHsv[0] = sbHue.getProgress();
        currentHsv[1] = sbSat.getProgress() / 100f;
        currentHsv[2] = sbVal.getProgress() / 100f;

        selectedColor[0] = Color.HSVToColor(currentHsv);
        swatchBg.setColor(selectedColor[0]);
        previewSwatch.invalidate();

        isSelfUpdating[0] = true;
        hexInput.setText(String.format("#%06X", (0xFFFFFF & selectedColor[0])));
        hexInput.setSelection(hexInput.getText().length());
        isSelfUpdating[0] = false;

        tvHueLabel.setText(String.format("Hue: %d°", Math.round(currentHsv[0])));
        tvSatLabel.setText(String.format("Saturation: %d%%", Math.round(currentHsv[1] * 100)));
        tvValLabel.setText(String.format("Brightness: %d%%", Math.round(currentHsv[2] * 100)));

        satTrack.setColors(new int[] {
            Color.HSVToColor(new float[]{ currentHsv[0], 0f, currentHsv[2] }),
            Color.HSVToColor(new float[]{ currentHsv[0], 1f, currentHsv[2] })
        });
        valTrack.setColors(new int[] {
            Color.BLACK,
            Color.HSVToColor(new float[]{ currentHsv[0], currentHsv[1], 1f })
        });
        tvRgb.setText(formatRgb(selectedColor[0]));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {}

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    sbHue.setOnSeekBarChangeListener(sliderListener);
    sbSat.setOnSeekBarChangeListener(sliderListener);
    sbVal.setOnSeekBarChangeListener(sliderListener);

    for (final int c : PALETTE)
    {
      final View chip = new View(context);
      GridLayout.LayoutParams chipLp = new GridLayout.LayoutParams();
      chipLp.width = dp(38, density);
      chipLp.height = dp(38, density);
      chipLp.setMargins(dp(4, density), dp(4, density), dp(4, density), dp(4, density));
      chip.setLayoutParams(chipLp);

      GradientDrawable chipBg = new GradientDrawable();
      chipBg.setShape(GradientDrawable.RECTANGLE);
      chipBg.setColor(c);
      chipBg.setCornerRadius(dp(6, density));
      chipBg.setStroke(dp(1, density), Color.parseColor("#374151"));
      chip.setBackground(chipBg);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          selectedColor[0] = c;
          swatchBg.setColor(c);
          previewSwatch.invalidate();
          isSelfUpdating[0] = true;
          hexInput.setText(String.format("#%06X", (0xFFFFFF & c)));
          hexInput.setSelection(hexInput.getText().length());
          isSelfUpdating[0] = false;
          updateSlidersFromColor.run();
        }
      });

      grid.addView(chip);
    }
    root.addView(grid);

    hexInput.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        if (isSelfUpdating[0]) return;
        String raw = s.toString().trim();
        if (!raw.startsWith("#")) raw = "#" + raw;
        try
        {
          if (raw.length() == 7 || raw.length() == 9)
          {
            int parsed = Color.parseColor(raw);
            selectedColor[0] = parsed;
            swatchBg.setColor(parsed);
            previewSwatch.invalidate();
            updateSlidersFromColor.run();
          }
        }
        catch (Exception ignored) {}
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    AlertDialog dialog = new AlertDialog.Builder(context)
        .setView(scrollView)
        .setPositiveButton("Apply", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface d, int which)
          {
            if (listener != null)
            {
              listener.onColorSelected(selectedColor[0]);
            }
          }
        })
        .setNegativeButton("Cancel", null)
        .create();

    dialog.show();
  }

  private static String formatRgb(int color)
  {
    int r = (color >> 16) & 0xFF;
    int g = (color >> 8) & 0xFF;
    int b = color & 0xFF;
    return String.format("RGB: %d, %d, %d", r, g, b);
  }

  private static int dp(float dp, float density)
  {
    return Math.round(dp * density);
  }
}
