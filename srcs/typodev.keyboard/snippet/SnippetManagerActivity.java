package typodev.keyboard.snippet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import typodev.keyboard.R;
import typodev.keyboard.SettingsThemeHelper;

/**
 * Full-screen activity for managing personal snippets & shortcuts.
 * Styled with TypoDev's modern clean design system with horizontal swipe tab switching.
 */
public class SnippetManagerActivity extends Activity
{
  private SnippetStore _store;
  private List<Snippet> _allSnippets = new ArrayList<>();
  private List<Snippet> _filteredSnippets = new ArrayList<>();
  private SnippetAdapter _adapter;

  private EditText _searchField;
  private ImageView _btnClearSearch;
  private ListView _listView;
  private View _emptyContainer;
  private TextView _emptyText;
  private TextView _countBadge;
  private HorizontalScrollView _categoryTabsScroll;
  private LinearLayout _categoryTabs;
  private TextView _tvSectionTitle;

  private final List<String> _tabKeys = new ArrayList<>();
  private String _activeCategory = null; // null = All
  private String _searchQuery = "";

  private float _density;
  private GestureDetector _gestureDetector;
  private int _touchSlop;
  private float _touchDownX = 0f;
  private float _touchDownY = 0f;
  private boolean _touchStartedInTabs = false;
  private boolean _isVerticalScrollLocked = false;
  private boolean _isHorizontalSwipeLocked = false;
  private boolean _swipedThisGesture = false;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    _density = getResources().getDisplayMetrics().density;
    _touchSlop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
    _store = SnippetStore.instance(this);
    _store.seedDefaults();

    _gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
    {
      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
      {
        if (_isVerticalScrollLocked || _touchStartedInTabs || _swipedThisGesture) return false;
        if (e1 == null || e2 == null) return false;
        float dx = e2.getX() - e1.getX();
        float dy = e2.getY() - e1.getY();
        if (Math.abs(dx) > dp(70)
            && Math.abs(dx) > Math.abs(dy) * 2.0f
            && Math.abs(velocityX) > Math.abs(velocityY) * 2.0f
            && Math.abs(velocityX) > dp(250))
        {
          _swipedThisGesture = true;
          if (dx < 0)
          {
            selectNextTab();
          }
          else
          {
            selectPreviousTab();
          }
          return true;
        }
        return false;
      }
    });

    // Hide native ActionBar
    if (getActionBar() != null)
    {
      getActionBar().hide();
    }

    // Apply adaptive system bars for Day/Night theme
    SettingsThemeHelper.applyAdaptiveSystemBars(this);

    setContentView(R.layout.activity_snippet_manager);

    View fab = findViewById(R.id.btn_snippet_fab);
    if (fab != null)
    {
      fab.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          SnippetEditorDialog.showCreate(SnippetManagerActivity.this, null,
              new SnippetEditorDialog.OnSavedCallback()
              {
                @Override
                public void onSnippetSaved(Snippet snippet)
                {
                  reload();
                }
              });
        }
      });
    }

    // Wire views
    _searchField = findViewById(R.id.snippet_search);
    _btnClearSearch = findViewById(R.id.btn_clear_search);
    _listView = findViewById(R.id.snippet_list);
    _emptyContainer = findViewById(R.id.snippet_empty_container);
    _emptyText = findViewById(R.id.snippet_empty_text);
    _countBadge = findViewById(R.id.snippet_count_badge);
    _categoryTabsScroll = findViewById(R.id.snippet_category_tabs_scroll);
    _categoryTabs = findViewById(R.id.snippet_category_tabs);
    _tvSectionTitle = findViewById(R.id.tv_snippets_section);

    // Back button
    View btnBack = findViewById(R.id.btn_snippets_back);
    if (btnBack != null)
    {
      btnBack.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v) { finish(); }
      });
    }

    // Clear search button
    if (_btnClearSearch != null)
    {
      _btnClearSearch.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_searchField != null)
          {
            _searchField.setText("");
          }
        }
      });
    }

    // Build category filter tabs
    buildCategoryTabs();

    // Setup adapter
    _adapter = new SnippetAdapter();
    _listView.setAdapter(_adapter);
    _listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
        if (position >= 0 && position < _filteredSnippets.size())
        {
          final Snippet s = _filteredSnippets.get(position);
          SnippetEditorDialog.showEdit(SnippetManagerActivity.this, s,
              new SnippetEditorDialog.OnSavedCallback()
              {
                @Override
                public void onSnippetSaved(Snippet snippet)
                {
                  reload();
                }
              });
        }
      }
    });

    // Search listener
    if (_searchField != null)
    {
      _searchField.addTextChangedListener(new TextWatcher()
      {
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
        @Override
        public void afterTextChanged(Editable s)
        {
          _searchQuery = s.toString().trim().toLowerCase();
          if (_btnClearSearch != null)
          {
            _btnClearSearch.setVisibility(_searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
          }
          applyFilter();
        }
      });
    }

    reload();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    reload();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    SettingsThemeHelper.applyAdaptiveSystemBars(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home) { finish(); return true; }
    return super.onOptionsItemSelected(item);
  }

  private void reload()
  {
    _allSnippets = _store.getAll();
    if (_countBadge != null)
    {
      _countBadge.setText(_allSnippets.size() + " snippets");
    }
    applyFilter();
  }

  private void applyFilter()
  {
    _filteredSnippets.clear();
    for (Snippet s : _allSnippets)
    {
      // Category filter
      if (_activeCategory != null && !s.category.name().equals(_activeCategory))
        continue;
      // Search filter
      if (!_searchQuery.isEmpty())
      {
        boolean matchesShortcut = s.shortcut.toLowerCase().contains(_searchQuery);
        boolean matchesName = s.name.toLowerCase().contains(_searchQuery);
        boolean matchesExpansion = s.expansion.toLowerCase().contains(_searchQuery);
        if (!matchesShortcut && !matchesName && !matchesExpansion) continue;
      }
      _filteredSnippets.add(s);
    }
    _adapter.notifyDataSetChanged();

    if (_tvSectionTitle != null)
    {
      if (_activeCategory == null)
      {
        _tvSectionTitle.setText("ALL SNIPPETS (" + _filteredSnippets.size() + ")");
      }
      else
      {
        SnippetCategory cat = SnippetCategory.fromKey(_activeCategory);
        _tvSectionTitle.setText(cat.displayName().toUpperCase() + " (" + _filteredSnippets.size() + ")");
      }
    }

    boolean isEmpty = _filteredSnippets.isEmpty();
    if (_emptyContainer != null)
    {
      _emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
    if (_listView != null)
    {
      _listView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
  }

  private void buildCategoryTabs()
  {
    if (_categoryTabs == null) return;
    _categoryTabs.removeAllViews();
    _tabKeys.clear();

    addCategoryTab("All", null);
    for (SnippetCategory cat : SnippetCategory.values())
    {
      addCategoryTab(cat.displayName(), cat.name());
    }

    if (_activeCategory == null && _categoryTabsScroll != null)
    {
      _categoryTabsScroll.post(new Runnable()
      {
        @Override
        public void run()
        {
          _categoryTabsScroll.scrollTo(0, 0);
        }
      });
    }
  }

  private void addCategoryTab(final String label, final String catKey)
  {
    _tabKeys.add(catKey);
    final int tabIndex = _tabKeys.size() - 1;

    Button btn = new Button(this);
    btn.setText(label);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    btn.setPadding(dp(14), 0, dp(14), 0);
    boolean active = (catKey == null && _activeCategory == null)
        || (catKey != null && catKey.equals(_activeCategory));

    int accentColor = getResources().getColor(R.color.settings_accent);
    int chipBgColor = getResources().getColor(R.color.settings_chip_bg);
    int chipBorderColor = getResources().getColor(R.color.settings_chip_border);
    int textSubColor = getResources().getColor(R.color.settings_text_subtitle);

    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(16));
    if (active)
    {
      bg.setColor(accentColor);
      btn.setTextColor(Color.WHITE);
      btn.setTypeface(null, Typeface.BOLD);
    }
    else
    {
      bg.setColor(chipBgColor);
      bg.setStroke(dp(1), chipBorderColor);
      btn.setTextColor(textSubColor);
      btn.setTypeface(null, Typeface.NORMAL);
    }
    btn.setBackground(bg);

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
    lp.setMargins(0, 0, dp(6), 0);
    btn.setLayoutParams(lp);

    btn.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        switchToTab(tabIndex);
      }
    });
    _categoryTabs.addView(btn);

    if (active)
    {
      final View activeBtn = btn;
      if (_categoryTabsScroll != null)
      {
        _categoryTabsScroll.post(new Runnable()
        {
          @Override
          public void run()
          {
            if (tabIndex == 0)
            {
              _categoryTabsScroll.smoothScrollTo(0, 0);
            }
            else
            {
              int scrollX = activeBtn.getLeft() - dp(12);
              _categoryTabsScroll.smoothScrollTo(Math.max(0, scrollX), 0);
            }
          }
        });
      }
    }
  }

  private void selectNextTab()
  {
    int currentIndex = _tabKeys.indexOf(_activeCategory);
    if (currentIndex < 0) currentIndex = 0;
    if (currentIndex < _tabKeys.size() - 1)
    {
      switchToTab(currentIndex + 1);
    }
  }

  private void selectPreviousTab()
  {
    int currentIndex = _tabKeys.indexOf(_activeCategory);
    if (currentIndex < 0) currentIndex = 0;
    if (currentIndex > 0)
    {
      switchToTab(currentIndex - 1);
    }
  }

  private void switchToTab(int index)
  {
    if (index < 0 || index >= _tabKeys.size()) return;
    _activeCategory = _tabKeys.get(index);
    buildCategoryTabs();
    applyFilter();

    if (_categoryTabsScroll != null && index == 0)
    {
      _categoryTabsScroll.post(new Runnable()
      {
        @Override
        public void run()
        {
          _categoryTabsScroll.smoothScrollTo(0, 0);
        }
      });
    }

    if (_listView != null && _listView.getVisibility() == View.VISIBLE)
    {
      _listView.setAlpha(0.6f);
      _listView.animate().alpha(1.0f).setDuration(120).start();
    }
  }

  private boolean isTouchInsideView(View view, MotionEvent ev)
  {
    if (view == null || view.getVisibility() != View.VISIBLE) return false;
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    float x = ev.getRawX();
    float y = ev.getRawY();
    return x >= location[0] && x <= location[0] + view.getWidth()
        && y >= location[1] && y <= location[1] + view.getHeight();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev)
  {
    // If touch begins on the category tabs or search field or FAB, let them handle it 100% natively
    if (ev.getActionMasked() == MotionEvent.ACTION_DOWN)
    {
      _touchStartedInTabs = isTouchInsideView(_categoryTabsScroll, ev);
      _touchDownX = ev.getRawX();
      _touchDownY = ev.getRawY();
      _isVerticalScrollLocked = false;
      _isHorizontalSwipeLocked = false;
      _swipedThisGesture = false;
    }

    if (_touchStartedInTabs
        || isTouchInsideView(_searchField, ev)
        || isTouchInsideView(findViewById(R.id.btn_snippet_fab), ev))
    {
      if (ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_CANCEL)
      {
        _touchStartedInTabs = false;
      }
      return super.dispatchTouchEvent(ev);
    }

    switch (ev.getActionMasked())
    {
      case MotionEvent.ACTION_MOVE:
        if (!_isVerticalScrollLocked && !_isHorizontalSwipeLocked)
        {
          float dx = ev.getRawX() - _touchDownX;
          float dy = ev.getRawY() - _touchDownY;
          float absDx = Math.abs(dx);
          float absDy = Math.abs(dy);

          if (absDy > _touchSlop && absDy >= absDx)
          {
            // Scrolling vertically: lock vertical list scrolling. Tab change is completely disabled!
            _isVerticalScrollLocked = true;
          }
          else if (absDx > _touchSlop * 1.5f && absDx > absDy * 2.0f)
          {
            // Intentional horizontal page swipe: lock to horizontal page swipe!
            _isHorizontalSwipeLocked = true;
          }
        }
        break;

      case MotionEvent.ACTION_UP:
        if (!_swipedThisGesture && _isHorizontalSwipeLocked && !_isVerticalScrollLocked)
        {
          float dx = ev.getRawX() - _touchDownX;
          float dy = ev.getRawY() - _touchDownY;
          if (Math.abs(dx) > dp(60) && Math.abs(dx) > Math.abs(dy) * 1.8f)
          {
            _swipedThisGesture = true;
            if (dx < 0)
            {
              selectNextTab();
            }
            else
            {
              selectPreviousTab();
            }

            // Cancel any child item click
            MotionEvent cancel = MotionEvent.obtain(ev);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();

            _touchStartedInTabs = false;
            _isVerticalScrollLocked = false;
            _isHorizontalSwipeLocked = false;
            return true;
          }
        }
        _touchStartedInTabs = false;
        _isVerticalScrollLocked = false;
        _isHorizontalSwipeLocked = false;
        break;

      case MotionEvent.ACTION_CANCEL:
        _touchStartedInTabs = false;
        _isVerticalScrollLocked = false;
        _isHorizontalSwipeLocked = false;
        _swipedThisGesture = false;
        break;
    }

    if (!_isVerticalScrollLocked && !_touchStartedInTabs && _gestureDetector != null)
    {
      if (_gestureDetector.onTouchEvent(ev))
      {
        return true;
      }
    }

    return super.dispatchTouchEvent(ev);
  }

  private int dp(float v) { return (int)(v * _density + 0.5f); }

  // ─── List Adapter ───────────────────────────────────────────────────────────

  class SnippetAdapter extends BaseAdapter
  {
    @Override public int getCount() { return _filteredSnippets.size(); }
    @Override public Object getItem(int pos) { return _filteredSnippets.get(pos); }
    @Override public long getItemId(int pos) { return _filteredSnippets.get(pos).id; }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
      if (convertView == null)
        convertView = getLayoutInflater().inflate(R.layout.item_snippet, parent, false);

      final Snippet s = _filteredSnippets.get(pos);

      TextView tvShortcut = convertView.findViewById(R.id.snippet_shortcut);
      TextView tvName = convertView.findViewById(R.id.snippet_name);
      TextView tvExpansion = convertView.findViewById(R.id.snippet_expansion);
      TextView tvCategory = convertView.findViewById(R.id.snippet_category);
      View btnEdit = convertView.findViewById(R.id.snippet_btn_edit);
      View btnDelete = convertView.findViewById(R.id.snippet_btn_delete);
      View cardRoot = convertView.findViewById(R.id.snippet_card_root);
      if (cardRoot == null) cardRoot = convertView;

      if (tvShortcut != null) tvShortcut.setText(s.shortcut);
      if (tvName != null) tvName.setText(s.name);
      if (tvExpansion != null) tvExpansion.setText(s.expansionPreview(120));
      if (tvCategory != null) tvCategory.setText(s.category.displayName());

      View.OnClickListener editListener = new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          SnippetEditorDialog.showEdit(SnippetManagerActivity.this, s,
              new SnippetEditorDialog.OnSavedCallback()
              {
                @Override
                public void onSnippetSaved(Snippet snippet)
                {
                  reload();
                }
              });
        }
      };

      if (btnEdit != null) btnEdit.setOnClickListener(editListener);
      cardRoot.setOnClickListener(editListener);
      convertView.setOnClickListener(editListener);

      if (btnDelete != null)
      {
        btnDelete.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            new AlertDialog.Builder(SnippetManagerActivity.this)
                .setTitle("Delete Snippet")
                .setMessage("Are you sure you want to delete:\n\"" + s.shortcut + "\" (" + s.name + ")?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(DialogInterface d, int w)
                  {
                    _store.delete(s.id);
                    reload();
                    Toast.makeText(SnippetManagerActivity.this,
                        "Snippet deleted", Toast.LENGTH_SHORT).show();
                  }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
          }
        });
      }

      return convertView;
    }
  }
}
