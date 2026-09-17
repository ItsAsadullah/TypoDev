package juloo.keyboard2.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class NewStyleActivity extends Activity
{
  public static final String EXTRA_CREATED_STYLE_ID = "created_style_id";

  private String selectedEmoji = "✨";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
    int dp12 = (int) (12 * getResources().getDisplayMetrics().density);
    int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
    int dp48 = (int) (48 * getResources().getDisplayMetrics().density);
    int dp72 = (int) (72 * getResources().getDisplayMetrics().density);

    // Dark background container
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.parseColor("#181D26"));
    root.setPadding(dp16, dp16, dp16, dp16);

    // Header: Title + Close (X)
    RelativeLayout header = new RelativeLayout(this);
    header.setPadding(0, 0, 0, dp16);

    TextView tvTitle = new TextView(this);
    tvTitle.setText("New Style");
    tvTitle.setTextColor(Color.WHITE);
    tvTitle.setTextSize(19);
    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
    RelativeLayout.LayoutParams lpTitle = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    lpTitle.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    lpTitle.addRule(RelativeLayout.CENTER_VERTICAL);
    header.addView(tvTitle, lpTitle);

    TextView btnClose = new TextView(this);
    btnClose.setText("✕");
    btnClose.setTextColor(Color.parseColor("#8E99A8"));
    btnClose.setTextSize(20);
    btnClose.setPadding(dp8, dp8, dp8, dp8);
    RelativeLayout.LayoutParams lpClose = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    lpClose.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    lpClose.addRule(RelativeLayout.CENTER_VERTICAL);
    btnClose.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        finish();
      }
    });
    header.addView(btnClose, lpClose);
    root.addView(header);

    ScrollView scroll = new ScrollView(this);
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER_HORIZONTAL);

    // Big Icon circle (Screenshot 1)
    final TextView tvIcon = new TextView(this);
    tvIcon.setText(selectedEmoji);
    tvIcon.setTextSize(34);
    tvIcon.setGravity(Gravity.CENTER);
    GradientDrawable iconBg = new GradientDrawable();
    iconBg.setShape(GradientDrawable.OVAL);
    iconBg.setColor(Color.parseColor("#222C3C"));
    tvIcon.setBackground(iconBg);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp72, dp72);
    lpIcon.setMargins(0, dp8, 0, dp16);
    content.addView(tvIcon, lpIcon);

    // Emoji choices row
    LinearLayout emojiRow = new LinearLayout(this);
    emojiRow.setOrientation(LinearLayout.HORIZONTAL);
    emojiRow.setGravity(Gravity.CENTER);
    String[] emojiList = {"✨", "🏴‍☠️", "⚔️", "👑", "🚀", "🎭", "🔥", "🌸", "🧙‍♂️", "🤖"};
    for (final String em : emojiList)
    {
      TextView emBtn = new TextView(this);
      emBtn.setText(em);
      emBtn.setTextSize(18);
      emBtn.setPadding(dp8, dp48 / 8, dp8, dp48 / 8);
      emBtn.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          selectedEmoji = em;
          tvIcon.setText(em);
        }
      });
      emojiRow.addView(emBtn);
    }
    content.addView(emojiRow);

    // Style Name Input
    final EditText etName = new EditText(this);
    etName.setHint("Style Name (for example: \"Pirate\")");
    etName.setHintTextColor(Color.parseColor("#657285"));
    etName.setTextColor(Color.WHITE);
    etName.setTextSize(15);
    GradientDrawable inputBg1 = new GradientDrawable();
    inputBg1.setCornerRadius(12 * getResources().getDisplayMetrics().density);
    inputBg1.setColor(Color.parseColor("#212835"));
    etName.setBackground(inputBg1);
    etName.setPadding(dp16, dp12, dp16, dp12);
    LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lpName.setMargins(0, dp16, 0, dp12);
    content.addView(etName, lpName);

    // Instructions Input
    final EditText etInstruction = new EditText(this);
    etInstruction.setHint("Instructions (for example: \"Write like a swashbuckling pirate. Use arr, ye, matey, and talk about treasure and the sea\")");
    etInstruction.setHintTextColor(Color.parseColor("#657285"));
    etInstruction.setTextColor(Color.WHITE);
    etInstruction.setTextSize(15);
    etInstruction.setMinLines(4);
    etInstruction.setMaxLines(8);
    etInstruction.setGravity(Gravity.TOP | Gravity.START);
    GradientDrawable inputBg2 = new GradientDrawable();
    inputBg2.setCornerRadius(12 * getResources().getDisplayMetrics().density);
    inputBg2.setColor(Color.parseColor("#212835"));
    etInstruction.setBackground(inputBg2);
    etInstruction.setPadding(dp16, dp12, dp16, dp12);
    LinearLayout.LayoutParams lpInst = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lpInst.setMargins(0, 0, 0, dp16);
    content.addView(etInstruction, lpInst);

    // Create Button
    Button btnCreate = new Button(this);
    btnCreate.setText("Create");
    btnCreate.setTextColor(Color.WHITE);
    btnCreate.setTextSize(16);
    btnCreate.setTypeface(null, android.graphics.Typeface.BOLD);
    GradientDrawable btnBg = new GradientDrawable();
    btnBg.setCornerRadius(24 * getResources().getDisplayMetrics().density);
    btnBg.setColor(Color.parseColor("#2AABEE"));
    btnCreate.setBackground(btnBg);
    btnCreate.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String name = etName.getText().toString().trim();
        String instruction = etInstruction.getText().toString().trim();
        if (name.isEmpty())
        {
          Toast.makeText(NewStyleActivity.this, "Please enter a style name", Toast.LENGTH_SHORT).show();
          return;
        }
        if (instruction.isEmpty())
        {
          Toast.makeText(NewStyleActivity.this, "Please enter style instructions", Toast.LENGTH_SHORT).show();
          return;
        }

        AiStyle created = AiStyle.addCustomStyle(NewStyleActivity.this, name, selectedEmoji, instruction);
        Toast.makeText(NewStyleActivity.this, "✅ Style \"" + name + "\" created!", Toast.LENGTH_SHORT).show();

        Intent result = new Intent();
        result.putExtra(EXTRA_CREATED_STYLE_ID, created.getId());
        setResult(RESULT_OK, result);
        finish();
      }
    });
    LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp48);
    lpBtn.setMargins(0, dp8, 0, dp16);
    content.addView(btnCreate, lpBtn);

    scroll.addView(content);
    root.addView(scroll);

    setContentView(root);

    // Window configuration for sleek dialog appearance
    Window win = getWindow();
    if (win != null)
    {
      win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
      win.setGravity(Gravity.CENTER);
    }
  }
}
