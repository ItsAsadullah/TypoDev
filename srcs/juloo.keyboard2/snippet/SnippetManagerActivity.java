package juloo.keyboard2.snippet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.R;

/**
 * Full-screen activity for managing personal snippets & shortcuts.
 * Dark-themed to match the keyboard aesthetics.
 *
 * Features:
 *  - List all snippets with shortcut, name, expansion preview
 *  - Search / filter bar
 *  - Category filter tabs
 *  - Add (FAB), Edit, Delete per item
 *  - Seeded with 3 demo snippets on first open
 */
public class SnippetManagerActivity extends Activity
{
  private SnippetStore _store;
  private List<Snippet> _allSnippets = new ArrayList<>();
  private List<Snippet> _filteredSnippets = new ArrayList<>();
  private SnippetAdapter _adapter;

  private EditText _searchField;
  private ListView _listView;
  private TextView _emptyText;
  private TextView _countBadge;
  private LinearLayout _categoryTabs;

  private String _activeCategory = null; // null = All
  private String _searchQuery = "";

  private float _density;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    _density = getResources().getDisplayMetrics().density;
    _store = SnippetStore.instance(this);
    _store.seedDefaults();

    // Root FrameLayout to allow FAB overlay
    FrameLayout frame = new FrameLayout(this);
    frame.setBackgroundColor(Color.parseColor("#111827"));

    // Inflate the main content via layout XML
    View content = getLayoutInflater().inflate(R.layout.activity_snippet_manager, frame, false);
    frame.addView(content, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    // FAB (Floating Action Button) – + Add
    Button fab = new Button(this);
    fab.setText("＋");
    fab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
    fab.setTextColor(Color.WHITE);
    fab.setTypeface(null, Typeface.BOLD);
    GradientDrawable fabBg = new GradientDrawable();
    fabBg.setShape(GradientDrawable.OVAL);
    fabBg.setColor(Color.parseColor("#2AABEE"));
    fabBg.setSize(dp(56), dp(56));
    fab.setBackground(fabBg);
    fab.setPadding(0, 0, 0, 0);
    FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(dp(56), dp(56));
    fabLp.gravity = Gravity.BOTTOM | Gravity.END;
    fabLp.setMargins(0, 0, dp(20), dp(20));
    fab.setLayoutParams(fabLp);
    fab.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        SnippetEditorDialog.showCreate(SnippetManagerActivity.this, null,
            new SnippetEditorDialog.OnSavedCallback()
            {
              @Override
              public void onSnippetSaved(Snippet snippet) { reload(); }
            });
      }
    });
    frame.addView(fab);

    setContentView(frame);

    // Wire views
    _searchField = content.findViewById(R.id.snippet_search);
    _listView = content.findViewById(R.id.snippet_list);
    _emptyText = content.findViewById(R.id.snippet_empty_text);
    _countBadge = content.findViewById(R.id.snippet_count_badge);
    _categoryTabs = content.findViewById(R.id.snippet_category_tabs);

    // Back button
    content.findViewById(R.id.btn_snippets_back).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v) { finish(); }
    });

    // Setup ActionBar
    if (getActionBar() != null)
    {
      getActionBar().setDisplayHomeAsUpEnabled(true);
      getActionBar().setTitle("📝 My Snippets");
    }

    // Build category filter tabs
    buildCategoryTabs();

    // Setup adapter
    _adapter = new SnippetAdapter();
    _listView.setAdapter(_adapter);

    // Search listener
    _searchField.addTextChangedListener(new TextWatcher()
    {
      @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
      @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
      @Override
      public void afterTextChanged(Editable s)
      {
        _searchQuery = s.toString().trim().toLowerCase();
        applyFilter();
      }
    });

    reload();
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
    _countBadge.setText(_allSnippets.size() + " snippets");
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
        boolean matchesShortcut = s.shortcut.contains(_searchQuery);
        boolean matchesName = s.name.toLowerCase().contains(_searchQuery);
        boolean matchesExpansion = s.expansion.toLowerCase().contains(_searchQuery);
        if (!matchesShortcut && !matchesName && !matchesExpansion) continue;
      }
      _filteredSnippets.add(s);
    }
    _adapter.notifyDataSetChanged();
    _emptyText.setVisibility(_filteredSnippets.isEmpty() ? View.VISIBLE : View.GONE);
    _listView.setVisibility(_filteredSnippets.isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void buildCategoryTabs()
  {
    _categoryTabs.removeAllViews();
    addCategoryTab("All", null);
    for (SnippetCategory cat : SnippetCategory.values())
      addCategoryTab(cat.displayName(), cat.name());
  }

  private void addCategoryTab(final String label, final String catKey)
  {
    Button btn = new Button(this);
    btn.setText(label);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    btn.setPadding(dp(10), 0, dp(10), 0);
    boolean active = (catKey == null && _activeCategory == null)
        || (catKey != null && catKey.equals(_activeCategory));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(12));
    bg.setColor(active ? Color.parseColor("#2AABEE") : 0x22FFFFFF);
    btn.setBackground(bg);
    btn.setTextColor(active ? Color.WHITE : 0x88FFFFFF);
    if (active) btn.setTypeface(null, Typeface.BOLD);

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
    lp.setMargins(0, 0, dp(6), 0);
    btn.setLayoutParams(lp);

    btn.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _activeCategory = catKey;
        buildCategoryTabs();
        applyFilter();
      }
    });
    _categoryTabs.addView(btn);
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
      TextView btnEdit = convertView.findViewById(R.id.snippet_btn_edit);
      TextView btnDelete = convertView.findViewById(R.id.snippet_btn_delete);

      tvShortcut.setText(s.shortcut);
      tvName.setText(s.name);
      tvExpansion.setText(s.expansionPreview(120));
      tvCategory.setText(s.category.displayName());

      btnEdit.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          SnippetEditorDialog.showEdit(SnippetManagerActivity.this, s,
              new SnippetEditorDialog.OnSavedCallback()
              {
                @Override public void onSnippetSaved(Snippet snippet) { reload(); }
              });
        }
      });

      btnDelete.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          new AlertDialog.Builder(SnippetManagerActivity.this)
              .setTitle("Delete Snippet")
              .setMessage("Delete \"" + s.shortcut + "\" → " + s.expansionPreview(40) + "?")
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
              .setNegativeButton("Cancel", null)
              .show();
        }
      });

      return convertView;
    }
  }
}
