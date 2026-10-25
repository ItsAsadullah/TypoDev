package typodev.keyboard.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.EnumMap;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.R;
import typodev.keyboard.BanglaFont;

/** Scrollable worked solutions owned by the current IME input session, not a window. */
@android.annotation.SuppressLint("ViewConstructor")
public final class MathSolverPaneView extends LinearLayout {
  interface Solver {
    void solve(String problem, MathSolverResponse.Mode mode, GeminiAiService.AiCallback callback);
  }
  private final Keyboard2 keyboard;
  private final Solver solver;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final LinearLayout paper;
  private final ScrollView scroll;
  private final TextView notebook, explained;
  private final View regenerate, copy, insertFull, insertAnswer;
  private final EnumMap<MathSolverResponse.Mode, MathSolverResponse> results = new EnumMap<>(MathSolverResponse.Mode.class);
  private MathSolverResponse.Mode mode = MathSolverResponse.Mode.NOTEBOOK;
  private MathSolverResponse result;
  private String problem = "", before = "", after = "", selection = "";
  private InputConnection connection;
  private long session, request;
  private boolean active, loading, backendInFlight;
  private final int foreground, surface;

  public MathSolverPaneView(Context context, Keyboard2 owner) {
    this(context, owner, (problem, mode, callback) -> {
      String key = GeminiAiService.getApiKey(context);
      if (key == null || key.trim().isEmpty()) callback.onError(context.getString(R.string.math_api_needed));
      else GeminiAiService.processMathPrompt(context, MathSolverResponse.prompt(problem, mode), callback);
    });
  }

  MathSolverPaneView(Context context, Keyboard2 owner, Solver backend) {
    super(context);
    keyboard = owner; solver = backend;
    foreground = owner.getColorLabel(); surface = owner.getColorKey();
    setTag("math_solver_pane");
    setOrientation(VERTICAL);
    setPadding(dp(10), dp(4), dp(10), dp(4) + owner.getBottomSafety());
    setBackgroundColor(owner.getColorKeyboard());

    LinearLayout header = row();
    header.setTag("math_header");
    TextView title = label(context.getString(R.string.math_pane_title), 14);
    title.setTypeface(null, Typeface.BOLD);
    title.setSingleLine(true);
    title.setEllipsize(TextUtils.TruncateAt.END);
    header.addView(title, new LayoutParams(0, dp(48), 1));
    copy = icon(R.drawable.ic_math_copy, R.string.math_copy, "math_copy", this::copy);
    insertFull = icon(R.drawable.ic_ai_paste, R.string.math_insert_full, "math_insert_full", () -> insert(false));
    insertAnswer = icon(R.drawable.ic_math_answer, R.string.math_insert_answer, "math_insert_answer", () -> insert(true));
    regenerate = icon(R.drawable.ic_ai_refresh, R.string.math_regenerate, "math_regenerate", this::generate);
    View back = icon(R.drawable.ic_math_keyboard, R.string.math_back_keyboard, "math_back", () -> keyboard.closeMathSolver());
    for (View action : new View[]{copy, insertFull, insertAnswer, regenerate, back})
      header.addView(action, new LayoutParams(dp(44), dp(48)));
    addView(header, new LayoutParams(-1, dp(48)));

    LinearLayout tabs = row();
    notebook = button(context.getString(R.string.math_mode_notebook_short), "math_notebook", () -> select(MathSolverResponse.Mode.NOTEBOOK));
    explained = button(context.getString(R.string.math_mode_explained_short), "math_explained", () -> select(MathSolverResponse.Mode.EXPLAINED));
    notebook.setContentDescription(context.getString(R.string.math_mode_notebook));
    explained.setContentDescription(context.getString(R.string.math_mode_explained));
    for (TextView tab : new TextView[]{notebook, explained}) {
      tab.setTextSize(12);
      tab.setSingleLine(true);
      tab.setEllipsize(TextUtils.TruncateAt.END);
      LayoutParams params = new LayoutParams(0, dp(36), 1);
      params.setMarginEnd(dp(4));
      tabs.addView(tab, params);
    }
    addView(tabs);

    scroll = new ScrollView(context);
    scroll.setVerticalScrollBarEnabled(false);
    paper = new LinearLayout(context);
    paper.setOrientation(VERTICAL);
    paper.setPadding(dp(14), dp(14), dp(14), dp(14));
    paper.setBackground(background(surface));
    paper.setTag("math_working");
    scroll.addView(paper, new ScrollView.LayoutParams(-1, -2));
    LayoutParams pageParams = new LayoutParams(-1, 0, 1);
    pageParams.topMargin = dp(6); pageParams.bottomMargin = dp(6);
    addView(scroll, pageParams);

    updateActions();
  }

  public void start(String initialProblem) {
    problem = initialProblem == null ? "" : initialProblem.trim();
    connection = keyboard.getCurrentInputConnection();
    session = keyboard.getInputSessionId();
    active = true;
    if (connection != null) {
      connection.finishComposingText();
      before = text(connection.getTextBeforeCursor(4096, 0));
      after = text(connection.getTextAfterCursor(256, 0));
      selection = text(connection.getSelectedText(0));
    }
    generate();
  }

  private void select(MathSolverResponse.Mode selected) {
    if (selected == mode) return;
    mode = selected;
    request++; // Any previous mode's asynchronous answer is now obsolete.
    loading = false;
    result = results.get(mode);
    if (result == null) generate(); else render(null);
  }

  private boolean sameSession() {
    return active && keyboard.getInputSessionId() == session
        && keyboard.getCurrentInputConnection() == connection;
  }

  private void generate() {
    if (loading || !active) return;
    result = null;
    if (problem.isEmpty()) { render(getContext().getString(R.string.math_empty)); return; }
    if (!sameSession()) { render(getContext().getString(R.string.math_editor_changed)); return; }
    final long id = ++request;
    final MathSolverResponse.Mode requestedMode = mode;
    MathSolverResponse local = MathSolverResponse.offline(problem, mode);
    if (local != null) { result = local; results.put(mode, local); render(null); return; }
    loading = true;
    render(getContext().getString(R.string.math_loading));
    // Gemini serializes requests globally. Queue a newly selected mode until the
    // existing request completes instead of immediately receiving a busy error.
    if (backendInFlight) return;
    backendInFlight = true;
    solver.solve(problem, mode, new GeminiAiService.AiCallback() {
      @Override public void onSuccess(String raw) {
        main.post(() -> complete(id, requestedMode, raw, null));
      }
      @Override public void onError(String error) {
        main.post(() -> complete(id, requestedMode, null, error));
      }
    });
  }

  private void complete(long id, MathSolverResponse.Mode requestedMode, String raw, String error) {
    backendInFlight = false;
    if (!sameSession()) return;
    MathSolverResponse parsed = MathSolverResponse.parse(raw);
    if (parsed != null) results.put(requestedMode, parsed);
    loading = false;
    if (id != request) {
      result = results.get(mode);
      if (result == null) generate(); else render(null);
      return;
    }
    result = parsed;
    render(result != null ? null : error != null ? error : getContext().getString(R.string.math_incomplete));
  }

  private void render(String status) {
    paper.removeAllViews();
    heading(getContext().getString(R.string.math_question));
    line(problem.isEmpty() ? getContext().getString(R.string.math_empty) : problem, false);
    if (status != null) line(status, false);
    if (result != null) {
      heading(getContext().getString(mode == MathSolverResponse.Mode.NOTEBOOK
          ? R.string.math_mode_notebook : R.string.math_mode_explained));
      for (String step : result.steps.split("\n")) {
        String value = step.trim();
        if (!value.isEmpty()) line(value, mode == MathSolverResponse.Mode.NOTEBOOK && isEquation(value));
      }
      heading(getContext().getString(R.string.math_answer));
      line(result.finalAnswer, isEquation(result.finalAnswer));
      if (!result.formulas.isEmpty()) {
        heading(getContext().getString(R.string.math_rules));
        line(result.formulas, false);
      }
    }
    updateActions();
    scroll.scrollTo(0, 0);
  }

  private static boolean isEquation(String value) {
    return value.startsWith("=") || (!value.matches(".*[\\u0980-\\u09FF].*")
        && (value.contains("²") || value.contains("=") || value.contains("³")));
  }

  private void heading(String text) {
    TextView view = label(text, 12);
    view.setTypeface(null, Typeface.BOLD);
    view.setPadding(0, dp(6), 0, dp(4));
    paper.addView(view, new LayoutParams(-1, -2));
  }

  private void line(String text, boolean equation) {
    TextView view = label(text, equation ? 20 : 16);
    view.setGravity(equation ? Gravity.CENTER : Gravity.START);
    if (equation) view.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    view.setLineSpacing(dp(4), 1);
    view.setPadding(0, dp(4), 0, dp(10));
    paper.addView(view, new LayoutParams(-1, -2));
  }

  private void updateActions() {
    if (notebook != null) {
      notebook.setSelected(mode == MathSolverResponse.Mode.NOTEBOOK);
      explained.setSelected(mode == MathSolverResponse.Mode.EXPLAINED);
      for (TextView tab : new TextView[]{notebook, explained}) {
        GradientDrawable bg = background(surface);
        if (tab.isSelected()) bg.setStroke(dp(2), foreground);
        tab.setBackground(bg);
      }
    }
    regenerate.setEnabled(!loading && !problem.isEmpty() && active);
    copy.setEnabled(result != null && !loading);
    insertFull.setEnabled(result != null && !loading && connection != null && sameSession());
    insertAnswer.setEnabled(insertFull.isEnabled());
    for (View action : new View[]{copy, insertFull, insertAnswer, regenerate})
      action.setAlpha(action.isEnabled() ? 1 : .4f);
  }

  private void copy() {
    if (result == null || loading) return;
    ClipboardManager clipboard = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      clipboard.setPrimaryClip(ClipData.newPlainText("Math solution", result.fullText));
      Toast.makeText(getContext(), R.string.math_copied, Toast.LENGTH_SHORT).show();
    }
  }

  private void insert(boolean answerOnly) {
    if (result == null || loading || connection == null) return;
    if (!sameSession() || !before.equals(text(connection.getTextBeforeCursor(4096, 0)))
        || !after.equals(text(connection.getTextAfterCursor(256, 0)))
        || !selection.equals(text(connection.getSelectedText(0)))) {
      Toast.makeText(getContext(), R.string.math_editor_changed, Toast.LENGTH_LONG).show();
      return;
    }
    final InputConnection editor = connection;
    String value = answerOnly ? result.finalAnswer : result.fullText;
    boolean committed = false;
    editor.beginBatchEdit();
    try {
      if (selection.isEmpty() && before.endsWith(problem)) {
        if (!editor.deleteSurroundingText(problem.length(), 0)) return;
      }
      committed = editor.commitText(value, 1);
    } finally { editor.endBatchEdit(); }
    if (committed && sameSession()) keyboard.closeMathSolver();
  }

  public void cancel() {
    active = false; request++; loading = false; connection = null; result = null; results.clear();
    main.removeCallbacksAndMessages(null);
  }

  private static String text(CharSequence value) { return value == null ? "" : value.toString(); }
  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  private LinearLayout row() {
    LinearLayout view = new LinearLayout(getContext()); view.setGravity(Gravity.CENTER_VERTICAL); return view;
  }
  private TextView label(String value, int size) {
    TextView view = new TextView(getContext()); view.setText(value); view.setTextSize(size);
    BanglaFont.apply(view);
    view.setTextColor(foreground); view.setGravity(Gravity.CENTER_VERTICAL); return view;
  }
  private TextView button(String value, String tag, Runnable action) {
    TextView view = label(value, 13); view.setTag(tag); view.setGravity(Gravity.CENTER);
    view.setPadding(dp(5), 0, dp(5), 0); view.setFocusable(true);
    view.setOnClickListener(v -> action.run()); return view;
  }
  private View icon(int drawable, int description, String tag, Runnable action) {
    ImageButton view = new ImageButton(getContext());
    view.setTag(tag);
    view.setImageResource(drawable);
    view.setImageTintList(ColorStateList.valueOf(foreground));
    view.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
    view.setPadding(dp(11), dp(12), dp(11), dp(12));
    view.setBackground(new android.graphics.drawable.RippleDrawable(
        ColorStateList.valueOf((foreground & 0x00ffffff) | 0x33000000), null, background(0xffffffff)));
    String hint = getContext().getString(description);
    view.setContentDescription(hint);
    if (Build.VERSION.SDK_INT >= 26) view.setTooltipText(hint);
    else view.setOnLongClickListener(v -> { Toast.makeText(getContext(), hint, Toast.LENGTH_SHORT).show(); return true; });
    view.setOnClickListener(v -> action.run());
    return view;
  }
  private GradientDrawable background(int color) {
    GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(10)); return bg;
  }
}
