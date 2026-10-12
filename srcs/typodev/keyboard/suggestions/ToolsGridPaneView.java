package typodev.keyboard.suggestions;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import typodev.keyboard.DialogTheme;
import typodev.keyboard.Keyboard2;

/**
 * An inline bottom panel displayed in place of Keyboard2View when the user
 * taps the four-square More Tools button at the left of the toolbar.
 * Supports:
 * 1. Normal mode: Tapping any tool executes/opens that tool immediately!
 * 2. Edit mode: A separate "✎ Edit" / "✓ Done" button toggles customize mode,
 *    allowing tools to be added to / removed from the top bar or reordered.
 */
public class ToolsGridPaneView extends LinearLayout implements ToolbarToolsManager.OnToolsChangedListener
{
  private Keyboard2 _keyboard;
  private ToolbarToolsManager _toolsManager;
  private final List<ToolbarToolsManager.ToolItem> _gridTools = new ArrayList<>();

  // State
  private boolean _isEditMode = false;

  // UI elements
  private LinearLayout _headerBar;
  private TextView _tvTitle;
  private TextView _tvSubtitle;
  private TextView _btnEditToggle;
  private ScrollView _scrollView;
  private GridLayout _grid;

  // Theme colors
  private int _colorKeyboard = 0xFF151A23;
  private int _colorKey = 0xFF212836;
  private int _colorLabel = 0xFFFFFFFF;
  private int _colorKeyActivated = 0xFF2AABEE;
  private int _colorSubLabel = 0xFF8E99A8;

  private float _density = 1f;
  private int _targetHeight = 0;
  private int _bottomSafety = 0;

  // Drag state
  private View _dragView = null;
  private int _dragFromIndex = -1;

  public ToolsGridPaneView(Context context)
  {
    super(context);
    initView(context);
  }

  public ToolsGridPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    initView(context);
  }

  public ToolsGridPaneView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    initView(context);
  }

  public void init(Keyboard2 keyboard)
  {
    _keyboard = keyboard;
  }

  public boolean isEditMode()
  {
    return _isEditMode;
  }

  public void setEditMode(boolean editMode)
  {
    if (_isEditMode == editMode) return;
    _isEditMode = editMode;
    updateHeaderUI();
    rebuildGrid();
    if (_toolsManager != null)
    {
      _toolsManager.notifyListeners();
    }
  }

  private void initView(Context context)
  {
    _density = context.getResources().getDisplayMetrics().density;
    _toolsManager = ToolbarToolsManager.getInstance(context);
    _toolsManager.addListener(this);

    setOrientation(VERTICAL);
    resolveThemeColors(context);
    setBackgroundColor(_colorKeyboard);

    // 1. Header Bar: Title + Subtitle on Left, "✎ Edit" / "✓ Done" button on Right
    _headerBar = new LinearLayout(context);
    _headerBar.setOrientation(HORIZONTAL);
    _headerBar.setGravity(Gravity.CENTER_VERTICAL);
    _headerBar.setPadding(dp(14), dp(8), dp(14), dp(6));

    LinearLayout titleContainer = new LinearLayout(context);
    titleContainer.setOrientation(VERTICAL);
    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    titleContainer.setLayoutParams(titleLp);

    _tvTitle = new TextView(context);
    _tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _tvTitle.setTypeface(null, Typeface.BOLD);
    titleContainer.addView(_tvTitle);

    _tvSubtitle = new TextView(context);
    _tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    titleContainer.addView(_tvSubtitle);

    _headerBar.addView(titleContainer);

    // "Edit" / "Done" action chip
    _btnEditToggle = new TextView(context);
    _btnEditToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _btnEditToggle.setTypeface(null, Typeface.BOLD);
    _btnEditToggle.setGravity(Gravity.CENTER);
    _btnEditToggle.setPadding(dp(12), dp(5), dp(12), dp(5));
    _btnEditToggle.setClickable(true);
    _btnEditToggle.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        setEditMode(!_isEditMode);
      }
    });
    _headerBar.addView(_btnEditToggle);

    addView(_headerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // 2. Middle: Scrollable Grid Container
    _scrollView = new ScrollView(context);
    _scrollView.setFillViewport(true);
    _scrollView.setVerticalScrollBarEnabled(false);
    _scrollView.setHorizontalScrollBarEnabled(false);
    _scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

    _grid = new GridLayout(context);
    _grid.setColumnCount(4);
    _grid.setPadding(dp(8), dp(2), dp(8), dp(8));
    _scrollView.addView(_grid, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
    addView(_scrollView, scrollLp);

    updateHeaderUI();
    loadTools();
  }

  private void updateHeaderUI()
  {
    if (_isEditMode)
    {
      _tvTitle.setText("Customize Tools");
      _tvTitle.setTextColor(_colorKeyActivated);
      _tvSubtitle.setText("Tap or drag to add/remove from top bar");
      _tvSubtitle.setTextColor(adjustAlpha(_colorLabel, 0.65f));

      _btnEditToggle.setText("✓ Done");
      _btnEditToggle.setTextColor(Color.WHITE);
      GradientDrawable doneBg = new GradientDrawable();
      doneBg.setColor(_colorKeyActivated);
      doneBg.setCornerRadius(dp(12));
      _btnEditToggle.setBackground(doneBg);
    }
    else
    {
      _tvTitle.setText("All Tools");
      _tvTitle.setTextColor(_colorLabel);
      _tvSubtitle.setText("Tap any tool to open");
      _tvSubtitle.setTextColor(adjustAlpha(_colorLabel, 0.50f));

      _btnEditToggle.setText("✎ Edit");
      _btnEditToggle.setTextColor(adjustAlpha(_colorLabel, 0.85f));
      GradientDrawable editBg = new GradientDrawable();
      editBg.setColor(blendSurface(0.14f));
      editBg.setCornerRadius(dp(12));
      editBg.setStroke(dp(1), adjustAlpha(_colorLabel, 0.15f));
      _btnEditToggle.setBackground(editBg);
    }
  }

  @Override
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    if (_toolsManager != null) _toolsManager.addListener(this);
  }

  @Override
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    if (_toolsManager != null)
    {
      _toolsManager.removeListener(this);
    }
  }

  @Override
  public void onToolsChanged()
  {
    post(new Runnable()
    {
      @Override
      public void run()
      {
        loadTools();
        rebuildGrid();
      }
    });
  }

  public void open(int targetHeight, int bottomSafety)
  {
    _targetHeight = targetHeight;
    _bottomSafety = bottomSafety;

    // Reset to normal mode whenever opened
    _isEditMode = false;

    resolveThemeColors(getContext());
    setBackgroundColor(_colorKeyboard);

    if (targetHeight > 0)
    {
      ViewGroup.LayoutParams lp = getLayoutParams();
      if (lp != null && lp.height != targetHeight)
      {
        lp.height = targetHeight;
        setLayoutParams(lp);
      }
    }

    // Safe padding for device navigation bar (no extra buttons)
    int safeBottom = Math.max(bottomSafety, dp(8));
    setPadding(0, 0, 0, safeBottom);

    updateHeaderUI();
    loadTools();
    post(new Runnable()
    {
      @Override
      public void run()
      {
        rebuildGrid();
      }
    });
  }

  public void applyTheme(int colorKeyboard, int colorKey, int colorLabel, int colorKeyActivated, int colorSubLabel)
  {
    _colorKeyboard = colorKeyboard;
    _colorKey = colorKey;
    _colorLabel = colorLabel;
    _colorKeyActivated = colorKeyActivated;
    _colorSubLabel = colorSubLabel;

    setBackgroundColor(_colorKeyboard);
    updateHeaderUI();
    rebuildGrid();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh)
  {
    super.onSizeChanged(w, h, oldw, oldh);
    if (w > 0 && w != oldw)
    {
      rebuildGrid();
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    if (_targetHeight > 0 && (heightMode == MeasureSpec.UNSPECIFIED || (heightMode == MeasureSpec.AT_MOST && heightSize <= 0)))
    {
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(_targetHeight, MeasureSpec.EXACTLY);
    }
    else if (heightMode == MeasureSpec.UNSPECIFIED || (heightMode == MeasureSpec.AT_MOST && heightSize <= 0))
    {
      int fallbackHeight = (int)(250 * _density + 0.5f);
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(fallbackHeight, MeasureSpec.EXACTLY);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  // ── Grid Building ────────────────────────────────────────────────────────────

  private void loadTools()
  {
    _gridTools.clear();
    if (_toolsManager != null)
    {
      _gridTools.addAll(_toolsManager.getGridTools());
    }
  }

  private void rebuildGrid()
  {
    if (_grid == null) return;
    _grid.removeAllViews();

    int totalW = getWidth();
    if (totalW <= 0)
    {
      DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
      totalW = dm.widthPixels;
    }

    int gridPadH = dp(10);
    int cardMargin = dp(4); // 4dp margin on each side = 8dp gap between cards!
    int cols = 4;
    int availW = totalW - (gridPadH * 2);
    int colW = Math.max(dp(60), (availW - (cols * cardMargin * 2)) / cols);
    int cardH = dp(62);

    _grid.setPadding(gridPadH, dp(2), gridPadH, dp(8));

    for (int i = 0; i < _gridTools.size(); i++)
    {
      final ToolbarToolsManager.ToolItem tool = _gridTools.get(i);
      final boolean isPinned = (_toolsManager != null && _toolsManager.isPinned(tool.id));
      View cell = buildCell(tool, isPinned, colW, cardH);

      GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
      lp.width = colW;
      lp.height = cardH;
      lp.setMargins(cardMargin, cardMargin, cardMargin, cardMargin);
      lp.rowSpec = GridLayout.spec(i / cols);
      lp.columnSpec = GridLayout.spec(i % cols);
      _grid.addView(cell, lp);
    }
  }

  private View buildCell(final ToolbarToolsManager.ToolItem tool, final boolean isPinned, int width, int height)
  {
    FrameLayout cell = new FrameLayout(getContext());
    cell.setClickable(true);
    cell.setFocusable(false);

    boolean active = isToolActive(tool.id);
    // Card background
    GradientDrawable cardBg = createCardDrawable(active);
    cell.setBackground(cardBg);

    // Content container: Icon + Label
    LinearLayout content = new LinearLayout(getContext());
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER);
    content.setPadding(dp(4), dp(8), dp(4), dp(6));

    int contentColor = active ? _colorKeyActivated : _colorLabel;

    // Icon
    ImageView icon = new ImageView(getContext());
    LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(24), dp(24));
    iconLp.bottomMargin = dp(4);
    icon.setLayoutParams(iconLp);
    try { icon.setImageResource(tool.iconResId); } catch (Throwable ignored) {}
    icon.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
    icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    content.addView(icon);

    // Label
    TextView label = new TextView(getContext());
    label.setText(tool.label);
    label.setTextColor(contentColor);
    label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    label.setGravity(Gravity.CENTER);
    label.setMaxLines(1);
    content.addView(label);

    FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    content.setLayoutParams(contentLp);
    cell.addView(content);

    // In Edit mode: show status badge (+ to add, ✓ if already in top bar)
    if (_isEditMode)
    {
      TextView tvBadge = new TextView(getContext());
      tvBadge.setText(isPinned ? "✓" : "＋");
      tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, isPinned ? 10 : 12);
      tvBadge.setTypeface(null, Typeface.BOLD);
      tvBadge.setGravity(Gravity.CENTER);

      GradientDrawable badgeBg = new GradientDrawable();
      badgeBg.setShape(GradientDrawable.OVAL);
      if (isPinned)
      {
        badgeBg.setColor(Color.parseColor("#10B981")); // Emerald green for pinned
        tvBadge.setTextColor(Color.WHITE);
      }
      else
      {
        badgeBg.setColor(adjustAlpha(_colorKeyActivated, 0.90f)); // Accent color for add
        tvBadge.setTextColor(Color.WHITE);
      }
      tvBadge.setBackground(badgeBg);

      int badgeSize = dp(16);
      FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
      badgeLp.gravity = Gravity.TOP | Gravity.END;
      badgeLp.topMargin = dp(4);
      badgeLp.rightMargin = dp(4);
      tvBadge.setLayoutParams(badgeLp);
      cell.addView(tvBadge);
    }

    cell.setTag(tool.id);

    // Click behavior
    cell.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        if (_isEditMode)
        {
          // Edit mode: toggle pin / unpin in top toolbar
          if (_toolsManager != null)
          {
            if (isPinned)
            {
              _toolsManager.unpinTool(tool.id);
              Toast.makeText(getContext(), tool.label + " removed from top toolbar", Toast.LENGTH_SHORT).show();
            }
            else
            {
              _toolsManager.pinTool(tool.id);
              Toast.makeText(getContext(), tool.label + " added to top toolbar", Toast.LENGTH_SHORT).show();
            }
          }
        }
        else
        {
          // Normal mode: ACTIVATE THE TOOL!
          if (_keyboard != null && _toolsManager != null)
          {
            _keyboard.closeToolsPane();
            _toolsManager.activateTool(getContext(), _keyboard, tool.id);
          }
        }
      }
    });

    // Long press = Start drag to reorder within grid or drag up to pin
    cell.setOnLongClickListener(new OnLongClickListener()
    {
      @Override
      public boolean onLongClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) {}
        if (!_isEditMode)
        {
          setEditMode(true);
        }
        startDrag(v);
        return true;
      }
    });

    return cell;
  }

  private GradientDrawable createCardDrawable(boolean highlighted)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(dp(12));
    if (highlighted)
    {
      gd.setColor(adjustAlpha(_colorKeyActivated, 0.35f));
      gd.setStroke(dp(1.5f), _colorKeyActivated);
    }
    else
    {
      gd.setColor(blendSurface(0.12f));
      gd.setStroke(dp(1), adjustAlpha(_colorLabel, 0.12f));
    }
    return gd;
  }

  // ── Drag & Drop Reorder ──────────────────────────────────────────────────────

  private void startDrag(View draggedCell)
  {
    String id = (String) draggedCell.getTag();
    _dragFromIndex = indexOfId(id);
    if (_dragFromIndex < 0) return;
    _dragView = draggedCell;

    if (_scrollView != null)
    {
      _scrollView.requestDisallowInterceptTouchEvent(true);
    }

    draggedCell.animate().scaleX(1.12f).scaleY(1.12f).alpha(0.65f).setDuration(120).start();

    _grid.setOnTouchListener(new OnTouchListener()
    {
      @Override
      public boolean onTouch(View v, MotionEvent event)
      {
        return handleDragTouch(event);
      }
    });
  }

  private boolean handleDragTouch(MotionEvent event)
  {
    if (_dragView == null) return false;

    int action = event.getAction();
    float x = event.getX();
    float y = event.getY();

    if (action == MotionEvent.ACTION_MOVE)
    {
      // If user drags up past top of grid (into top toolbar zone), pin it to top!
      if (y < dp(-10) && _dragFromIndex >= 0 && _dragFromIndex < _gridTools.size())
      {
        ToolbarToolsManager.ToolItem movedTool = _gridTools.get(_dragFromIndex);
        if (_toolsManager != null && !_toolsManager.isPinned(movedTool.id))
        {
          _toolsManager.pinTool(movedTool.id);
          Toast.makeText(getContext(), movedTool.label + " added to top toolbar", Toast.LENGTH_SHORT).show();
        }
      }

      int targetIdx = cellIndexAt(x, y);
      if (targetIdx >= 0 && targetIdx != _dragFromIndex && targetIdx < _gridTools.size())
      {
        ToolbarToolsManager.ToolItem moved = _gridTools.remove(_dragFromIndex);
        _gridTools.add(targetIdx, moved);
        _dragFromIndex = targetIdx;
        refreshGridContent(_dragFromIndex);
      }
    }
    else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
    {
      endDrag(y);
    }
    return true;
  }

  private void endDrag(float endY)
  {
    if (_dragView != null)
    {
      _dragView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
      _dragView = null;
    }

    // If dragged UP towards top toolbar, pin tool
    if (endY < dp(-10) && _dragFromIndex >= 0 && _dragFromIndex < _gridTools.size())
    {
      ToolbarToolsManager.ToolItem movedTool = _gridTools.get(_dragFromIndex);
      if (_toolsManager != null && !_toolsManager.isPinned(movedTool.id))
      {
        _toolsManager.pinTool(movedTool.id);
      }
    }

    _dragFromIndex = -1;
    _grid.setOnTouchListener(null);
    if (_scrollView != null)
    {
      _scrollView.requestDisallowInterceptTouchEvent(false);
    }
    if (_toolsManager != null)
    {
      _toolsManager.saveGridOrder(_gridTools);
    }
    rebuildGrid();
  }

  private void refreshGridContent(int activeDragIdx)
  {
    for (int i = 0; i < _grid.getChildCount(); i++)
    {
      View child = _grid.getChildAt(i);
      child.setBackground(createCardDrawable(i == activeDragIdx));
    }
  }

  private int cellIndexAt(float x, float y)
  {
    for (int i = 0; i < _grid.getChildCount(); i++)
    {
      View child = _grid.getChildAt(i);
      if (x >= child.getLeft() && x <= child.getRight() && y >= child.getTop() && y <= child.getBottom())
      {
        return i;
      }
    }
    return -1;
  }

  private int indexOfId(String id)
  {
    for (int i = 0; i < _gridTools.size(); i++)
    {
      if (_gridTools.get(i).id.equals(id)) return i;
    }
    return -1;
  }

  // ── Theming Helpers ──────────────────────────────────────────────────────────

  private void resolveThemeColors(Context context)
  {
    try
    {
      DialogTheme.Palette p = new DialogTheme.Palette(context);
      _colorKeyboard = p.dialogBg;
      _colorLabel = p.textPrimary;
      _colorKeyActivated = p.accentColor;
      _colorKey = blendSurface(0.12f);
    }
    catch (Throwable ignored) {}
  }

  private int blendSurface(float factor)
  {
    float inv = 1f - factor;
    int r = Math.round(Color.red(_colorKeyboard) * inv + Color.red(_colorLabel) * factor);
    int g = Math.round(Color.green(_colorKeyboard) * inv + Color.green(_colorLabel) * factor);
    int b = Math.round(Color.blue(_colorKeyboard) * inv + Color.blue(_colorLabel) * factor);
    return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
  }

  private static int adjustAlpha(int color, float factor)
  {
    int alpha = Math.round(255 * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }

  private int dp(float dp)
  {
    return (int)(dp * _density + 0.5f);
  }

  private boolean isToolActive(String toolId)
  {
    typodev.keyboard.Config cfg = typodev.keyboard.Config.globalConfig();
    if (cfg == null) return false;
    if ("translate".equals(toolId)) return (_keyboard != null && _keyboard.isTranslateBarOpen());
    if ("dev".equals(toolId)) return cfg.developer_mode;
    if ("one_hand".equals(toolId)) return (_keyboard != null && _keyboard.isOneHandModeActive());
    if ("haptic".equals(toolId)) return cfg.vibrate_enabled;
    if ("sound".equals(toolId)) return cfg.sound_on_keypress;
    if ("popup".equals(toolId)) return cfg.popup_on_keypress;
    return false;
  }
}
