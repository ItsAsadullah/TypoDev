package typodev.keyboard.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputConnection;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.R;

/** A bounded IME panel: preview first, explicitly insert only into the original editor. */
@android.annotation.SuppressLint("ViewConstructor") // Created with its IME owner, never inflated from XML.
public final class AiWritingResultsView extends LinearLayout {
  public enum Task { REPLY, COMPLETE, REWRITE }
  private final Keyboard2 keyboard;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final TextView body, page, previous, next;
  private final ImageView regenerate;
  private final ProgressBar progress;
  private final LinearLayout tones;
  private final ScrollView scroll;
  private final List<String> results = new ArrayList<>();
  private InputConnection connection;
  private long session, request;
  private int index;
  private String source, tone, before, after, selection;
  private String editorPackage;
  private String resultTone;
  private int fieldId;
  private byte[] image;
  private Task task;
  private boolean loading;
  private final int foreground, surface, accent;

  public AiWritingResultsView(Context context, Keyboard2 owner) {
    super(context);
    keyboard = owner;
    foreground = owner.getColorLabel();
    surface = owner.getColorKey();
    accent = owner.getColorKeyActivated();
    setOrientation(VERTICAL);
    setPadding(dp(12), dp(8), dp(12), dp(8) + owner.getBottomSafety());
    setBackgroundColor(owner.getColorKeyboard());
    LinearLayout header = row();
    ImageView back = iconButton(R.drawable.ic_ai_back,
        context.getString(R.string.ai_writing_back), () -> keyboard.closeWritingResults());
    back.setContentDescription(context.getString(R.string.ai_writing_back));
    TextView title = label(context.getString(R.string.ai_writing_title), 17);
    title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    title.setTypeface(null, Typeface.BOLD);
    header.addView(title, new LayoutParams(0, dp(48), 1));
    regenerate = iconButton(R.drawable.ic_ai_refresh,
        context.getString(R.string.ai_writing_regenerate), this::generate);
    header.addView(regenerate, new LayoutParams(dp(48), dp(48)));
    header.addView(back, new LayoutParams(dp(48), dp(48)));
    addView(header);

    LinearLayout card = new LinearLayout(context);
    card.setOrientation(VERTICAL);
    card.setPadding(dp(14), dp(10), dp(14), dp(8));
    card.setBackground(background(surface));
    LayoutParams cardParams = new LayoutParams(-1, 0, 1);
    cardParams.topMargin = dp(6);
    addView(card, cardParams);
    progress = new ProgressBar(context);
    card.addView(progress, new LayoutParams(dp(26), dp(26)));
    scroll = new ScrollView(context) {
      private float x, y;
      private boolean horizontal, vertical;
      private final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
      @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
          x = event.getX(); y = event.getY(); horizontal = false; vertical = false;
        }
        float dx = event.getX() - x, dy = event.getY() - y;
        if (!horizontal && Math.abs(dy) > touchSlop && Math.abs(dy) >= Math.abs(dx)) vertical = true;
        if (!horizontal && !vertical && event.getActionMasked() == MotionEvent.ACTION_MOVE
            && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.5f) {
          horizontal = true;
          // Cancel the text gesture immediately so swiping cannot insert or copy.
          MotionEvent cancel = MotionEvent.obtain(event);
          cancel.setAction(MotionEvent.ACTION_CANCEL);
          super.dispatchTouchEvent(cancel);
          cancel.recycle();
        }
        if (horizontal) {
          if (event.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(dx) > dp(24))
            move(dx < 0 ? 1 : -1);
          return true;
        }
        return super.dispatchTouchEvent(event);
      }
    };
    body = label("", 18);
    body.setGravity(Gravity.TOP);
    body.setLineSpacing(dp(3), 1);
    body.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    body.setFocusable(true);
    body.setOnClickListener(v -> insert());
    body.setOnLongClickListener(v -> {
      if (loading || results.isEmpty()) return false;
      copyResult();
      return true;
    });
    body.setAccessibilityDelegate(new View.AccessibilityDelegate() {
      @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        if (body.isClickable()) info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
            AccessibilityNodeInfo.ACTION_CLICK, context.getString(R.string.ai_writing_use)));
        if (body.isLongClickable()) info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
            AccessibilityNodeInfo.ACTION_LONG_CLICK, context.getString(R.string.ai_writing_copy)));
      }
    });
    // Only the text is actionable; the empty area remains safe to tap while reading.
    scroll.setFillViewport(false);
    scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
    card.addView(scroll, new LayoutParams(-1, 0, 1));

    LinearLayout navigation = row();
    previous = button("‹", () -> move(-1));
    previous.setContentDescription(context.getString(R.string.ai_writing_previous));
    next = button("›", () -> move(1));
    next.setContentDescription(context.getString(R.string.ai_writing_next));
    page = label("", 12);
    previous.setMinHeight(dp(36));
    next.setMinHeight(dp(36));
    navigation.addView(previous, new LayoutParams(dp(48), dp(36)));
    navigation.addView(page, new LayoutParams(0, dp(36), 1));
    navigation.addView(next, new LayoutParams(dp(48), dp(36)));
    addView(navigation);
    HorizontalScrollView toneScroll = new HorizontalScrollView(context);
    toneScroll.setHorizontalScrollBarEnabled(false);
    tones = row();
    toneScroll.addView(tones);
    addView(toneScroll, new LayoutParams(-1, dp(36)));
  }

  public void start(String text, byte[] bytes, String selectedTone, Task selectedTask) {
    source = text; image = bytes; task = selectedTask;
    tone = selectedTone == null ? "স্বাভাবিক" : selectedTone;
    connection = keyboard.getCurrentInputConnection();
    if (connection != null) connection.finishComposingText();
    session = keyboard.getInputSessionId();
    before = text(connection == null ? null : connection.getTextBeforeCursor(4096, 0));
    after = text(connection == null ? null : connection.getTextAfterCursor(256, 0));
    selection = text(connection == null ? null : connection.getSelectedText(0));
    android.view.inputmethod.EditorInfo editor = keyboard.getCurrentInputEditorInfo();
    editorPackage = editor == null ? null : editor.packageName;
    fieldId = editor == null ? 0 : editor.fieldId;
    populateTones();
    generate();
  }

  public AiWritingDraft getDraft() {
    if (results.isEmpty()) return null;
    return new AiWritingDraft(source, image, resultTone, task, results, index,
        before, after, selection, editorPackage, fieldId);
  }

  /** Restore the preview without another API call; only bind a matching editor. */
  public void restore(AiWritingDraft draft) {
    source = draft.source; image = draft.getImage(); tone = draft.tone; task = draft.task;
    resultTone = draft.tone;
    before = draft.before; after = draft.after; selection = draft.selection;
    editorPackage = draft.editorPackage; fieldId = draft.fieldId;
    results.clear(); results.addAll(draft.results); index = draft.index;
    session = keyboard.getInputSessionId();
    InputConnection current = keyboard.getCurrentInputConnection();
    android.view.inputmethod.EditorInfo editor = keyboard.getCurrentInputEditorInfo();
    connection = current != null && editor != null && draft.matchesEditor(
        editor.packageName, editor.fieldId, text(current.getTextBeforeCursor(4096, 0)),
        text(current.getTextAfterCursor(256, 0)), text(current.getSelectedText(0))) ? current : null;
    populateTones(); render();
    if (connection == null) page.setText(R.string.ai_writing_restored_copy_only);
  }

  private void populateTones() {
    tones.removeAllViews();
    String[] options = task == Task.REWRITE
        ? new String[] {"প্রুফরিড", "রিফ্রেজ", "প্রফেশনাল", "বন্ধুত্বপূর্ণ", "সংক্ষিপ্ত"}
        : new String[] {"স্বাভাবিক", "প্রফেশনাল", "বন্ধুত্বপূর্ণ", "প্রশংসা", "মজার", "বুদ্ধিদীপ্ত", "রোস্ট", "ইসলামিক"};
    for (String option : options) {
      TextView chip = button(AiVisionHelper.getToneLabel(option), () -> { tone = option; populateTones(); generate(); });
      boolean selected = tone.equals(option);
      chip.setSelected(selected);
      chip.setBackground(background(selected ? accent : surface));
      if (selected) ((GradientDrawable)chip.getBackground()).setStroke(dp(1), foreground);
      chip.setTextColor(selected ? contrast(accent) : foreground);
      chip.setEnabled(!loading);
      chip.setMinHeight(dp(36));
      chip.setPadding(dp(10), 0, dp(10), 0);
      LayoutParams lp = new LayoutParams(-2, dp(36));
      lp.setMarginEnd(dp(4));
      tones.addView(chip, lp);
    }
  }

  private boolean sameSession() {
    return connection != null && keyboard.getInputSessionId() == session
        && keyboard.getCurrentInputConnection() == connection;
  }

  private void generate() {
    if (loading || !sameSession()) return;
    final long id = ++request;
    setLoading(true);
    String prompt;
    if (task == Task.REPLY) {
      prompt = image == null ? AiVisionHelper.buildTextCommentPrompt(source, tone)
          : AiVisionHelper.buildCommentPrompt(tone);
    } else {
      prompt = AiWritingPrompts.buildWritingPrompt(source, tone, task == Task.COMPLETE);
    }
    GeminiAiService.AiCallback callback = new GeminiAiService.AiCallback() {
      @Override public void onSuccess(String result) {
        main.post(() -> {
          if (id != request || !sameSession()) return;
          List<String> parsed = AiVisionHelper.parseGeneratedComments(result);
          setLoading(false);
          boolean proofread = task == Task.REWRITE && "প্রুফরিড".equals(tone);
          if (parsed.isEmpty() || (!proofread && parsed.size() != 3)) {
            showError(getContext().getString(R.string.ai_writing_incomplete)); return;
          }
          results.clear(); results.addAll(parsed); resultTone = tone; index = 0; render();
        });
      }
      @Override public void onError(String error) {
        main.post(() -> {
          if (id != request || !sameSession()) return;
          setLoading(false); showError(error);
        });
      }
    };
    if (image == null) GeminiAiService.processPrompt(getContext(), prompt, callback);
    else GeminiAiService.processPromptWithImage(getContext(), prompt, image, "image/jpeg", callback);
  }

  private void setLoading(boolean value) {
    loading = value;
    progress.setVisibility(value ? VISIBLE : GONE);
    regenerate.setEnabled(!value && sameSession());
    body.setClickable(!value && !results.isEmpty() && connection != null);
    body.setLongClickable(!value && !results.isEmpty());
    previous.setEnabled(!value && index > 0);
    next.setEnabled(!value && index < results.size() - 1);
    for (View action : new View[] {regenerate, previous, next})
      action.setAlpha(action.isEnabled() ? 1f : .45f);
    for (int i = 0; i < tones.getChildCount(); i++) tones.getChildAt(i).setEnabled(!value && sameSession());
    if (value) {
      body.setText(R.string.ai_writing_loading);
      page.setText(R.string.ai_writing_wait);
    }
  }

  private void showError(String error) {
    if (!results.isEmpty()) render();
    else { body.setText(error); page.setText(R.string.ai_writing_retry); }
    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
  }

  private void move(int delta) {
    if (loading || results.isEmpty()) return;
    index = Math.max(0, Math.min(results.size() - 1, index + delta));
    render();
  }

  private void render() {
    body.setText(results.get(index));
    scroll.scrollTo(0, 0);
    page.setText(getContext().getString(R.string.ai_writing_page, index + 1, results.size()));
    if (connection == null) page.setText(R.string.ai_writing_restored_copy_only);
    setLoading(false);
  }

  private void insert() {
    if (loading || results.isEmpty() || connection == null) return;
    if (!sameSession() || !before.equals(text(connection.getTextBeforeCursor(4096, 0)))
        || !after.equals(text(connection.getTextAfterCursor(256, 0)))
        || !selection.equals(text(connection.getSelectedText(0)))) {
      Toast.makeText(getContext(), R.string.ai_writing_editor_changed, Toast.LENGTH_LONG).show();
      return;
    }
    String result = results.get(index);
    // Keep the original editor alive through cleanup, even if a callback closes the panel.
    final InputConnection editor = connection;
    final Task insertTask = task;
    final String insertSource = source;
    final String insertSelection = selection;
    final String insertBefore = before;
    boolean committed = false;
    editor.beginBatchEdit();
    try {
      editor.finishComposingText();
      if (insertTask == Task.REWRITE && insertSelection.isEmpty()) {
        if (!editor.deleteSurroundingText(insertSource.length(), 0)) return;
      }
      if (insertTask == Task.COMPLETE) {
        String prefix = insertSelection.isEmpty() ? insertBefore : insertSelection;
        if (!prefix.isEmpty() && !Character.isWhitespace(prefix.charAt(prefix.length() - 1)))
          result = " " + result;
        if (!insertSelection.isEmpty()) result = insertSelection + result;
      }
      committed = editor.commitText(result, 1);
    } finally { editor.endBatchEdit(); }
    if (committed && sameSession()) keyboard.closeWritingResults();
  }

  private void copyResult() {
    if (loading || results.isEmpty()) return;
    ClipboardManager clipboard = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      clipboard.setPrimaryClip(ClipData.newPlainText("Typodev Writing", results.get(index)));
      Toast.makeText(getContext(), R.string.ai_writing_copied, Toast.LENGTH_SHORT).show();
    }
  }

  public void cancel() {
    request++; loading = false; image = null; source = null; connection = null; results.clear();
  }
  private String text(CharSequence value) { return value == null ? "" : value.toString(); }
  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  private LinearLayout row() {
    LinearLayout view = new LinearLayout(getContext());
    view.setOrientation(HORIZONTAL); view.setGravity(Gravity.CENTER_VERTICAL); return view;
  }
  private TextView label(String text, int size) {
    TextView view = new TextView(getContext()); view.setText(text); view.setTextSize(size);
    view.setTextColor(foreground); view.setGravity(Gravity.CENTER); return view;
  }
  private TextView button(String text, Runnable action) {
    TextView view = label(text, 14); view.setPadding(dp(12), 0, dp(12), 0);
    view.setMinHeight(dp(48)); view.setGravity(Gravity.CENTER);
    view.setFocusable(true); view.setOnClickListener(v -> action.run()); return view;
  }
  private ImageView iconButton(int icon, String description, Runnable action) {
    ImageView view = new ImageView(getContext());
    view.setImageResource(icon);
    view.setColorFilter(foreground);
    view.setPadding(dp(14), dp(14), dp(14), dp(14));
    view.setContentDescription(description);
    view.setFocusable(true);
    android.util.TypedValue ripple = new android.util.TypedValue();
    if (getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true)
        && ripple.resourceId != 0) view.setBackgroundResource(ripple.resourceId);
    view.setOnClickListener(v -> action.run());
    return view;
  }
  private int contrast(int color) {
    return Color.red(color) * .299 + Color.green(color) * .587 + Color.blue(color) * .114 > 150
        ? Color.BLACK : Color.WHITE;
  }
  private GradientDrawable background(int color) {
    GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(22)); return shape;
  }
}
