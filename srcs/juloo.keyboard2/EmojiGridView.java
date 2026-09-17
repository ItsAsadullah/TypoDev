package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmojiGridView extends GridView
  implements GridView.OnItemClickListener
{
  public static final int GROUP_LAST_USE = -1;

  private static final String LAST_USE_PREF = "emoji_last_use";

  private List<Emoji> _emojiArray;
  private HashMap<Emoji, Integer> _lastUsed;
  private int _currentGroup = GROUP_LAST_USE;
  private android.view.GestureDetector _gestureDetector;
  private float _downX, _downY;
  private boolean _isSwipingHorizontal = false;

  public interface OnGroupChangeListener
  {
    void onGroupChanged(int group);
  }

  private OnGroupChangeListener _groupChangeListener = null;

  public void setOnGroupChangeListener(OnGroupChangeListener listener)
  {
    _groupChangeListener = listener;
  }

  public int getCurrentGroup()
  {
    return _currentGroup;
  }

  public EmojiGridView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    Emoji.init(context.getResources());
    setOnItemClickListener(this);
    loadLastUsed();
    initGestureDetector(context);
    setEmojiGroup((_lastUsed.size() == 0) ? 0 : GROUP_LAST_USE);
  }

  private void initGestureDetector(Context context)
  {
    final int touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
    final int minFlingVelocity = android.view.ViewConfiguration.get(context).getScaledMinimumFlingVelocity();

    _gestureDetector = new android.view.GestureDetector(context, new android.view.GestureDetector.SimpleOnGestureListener()
    {
      @Override
      public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY)
      {
        if (e1 == null || e2 == null)
          return false;
        float dx = e2.getX() - e1.getX();
        float dy = e2.getY() - e1.getY();

        if (Math.abs(dx) > Math.abs(dy) * 1.2f && Math.abs(dx) > touchSlop * 2 && Math.abs(velocityX) > minFlingVelocity)
        {
          if (dx < 0)
          {
            // Swiped left -> next category
            nextEmojiGroup();
            return true;
          }
          else
          {
            // Swiped right -> previous category
            prevEmojiGroup();
            return true;
          }
        }
        return false;
      }
    });
  }

  @Override
  public boolean onInterceptTouchEvent(android.view.MotionEvent ev)
  {
    if (_gestureDetector != null && _gestureDetector.onTouchEvent(ev))
      return true;

    switch (ev.getActionMasked())
    {
      case android.view.MotionEvent.ACTION_DOWN:
        _downX = ev.getX();
        _downY = ev.getY();
        _isSwipingHorizontal = false;
        break;

      case android.view.MotionEvent.ACTION_MOVE:
        float dx = Math.abs(ev.getX() - _downX);
        float dy = Math.abs(ev.getY() - _downY);
        if (dx > dy * 1.5f && dx > 25)
        {
          _isSwipingHorizontal = true;
          return true; // Intercept horizontal swipe for emoji category paging
        }
        break;
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(android.view.MotionEvent ev)
  {
    if (_gestureDetector != null && _gestureDetector.onTouchEvent(ev))
      return true;

    if (_isSwipingHorizontal && ev.getActionMasked() == android.view.MotionEvent.ACTION_UP)
    {
      float dx = ev.getX() - _downX;
      if (Math.abs(dx) > 60)
      {
        if (dx < 0)
          nextEmojiGroup();
        else
          prevEmojiGroup();
        return true;
      }
    }
    return super.onTouchEvent(ev);
  }

  public void nextEmojiGroup()
  {
    int numGroups = Emoji.getNumGroups();
    if (_currentGroup == GROUP_LAST_USE)
    {
      if (numGroups > 0)
        setEmojiGroup(0);
    }
    else if (_currentGroup < numGroups - 1)
    {
      setEmojiGroup(_currentGroup + 1);
    }
  }

  public void prevEmojiGroup()
  {
    if (_currentGroup > 0)
    {
      setEmojiGroup(_currentGroup - 1);
    }
    else if (_currentGroup == 0)
    {
      setEmojiGroup(GROUP_LAST_USE);
    }
  }

  public void setEmojiGroup(int group)
  {
    _currentGroup = group;
    _emojiArray = (group == GROUP_LAST_USE) ? getLastEmojis() : Emoji.getEmojisByGroup(group);
    setAdapter(new EmojiViewAdpater(getContext(), _emojiArray));
    setSelection(0);
    if (_groupChangeListener != null)
      _groupChangeListener.onGroupChanged(group);
  }

  public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
  {
    Config config = Config.globalConfig();
    Integer used = _lastUsed.get(_emojiArray.get(pos));
    _lastUsed.put(_emojiArray.get(pos), (used == null) ? 1 : used.intValue() + 1);
    config.handler.key_up(_emojiArray.get(pos).kv(), Pointers.Modifiers.EMPTY);
    saveLastUsed(); // TODO: opti
  }

  private List<Emoji> getLastEmojis()
  {
    List<Emoji> list = new ArrayList<>(_lastUsed.keySet());
    Collections.sort(list, new Comparator<Emoji>()
        {
          public int compare(Emoji a, Emoji b)
          {
            return _lastUsed.get(b) - _lastUsed.get(a);
          }
        });
    return list;
  }

  private void saveLastUsed()
  {
    SharedPreferences.Editor edit;
    try { edit = emojiSharedPreferences().edit(); }
    catch (Exception _e) { return; }
    HashSet<String> set = new HashSet<String>();
    for (Emoji emoji : _lastUsed.keySet())
      set.add(String.valueOf(_lastUsed.get(emoji)) + "-" + emoji.kv().getString());
    edit.putStringSet(LAST_USE_PREF, set);
    edit.apply();
  }

  private void loadLastUsed()
  {
    _lastUsed = new HashMap<Emoji, Integer>();
    SharedPreferences prefs;
    // Storage might not be available (eg. the device is locked), avoid
    // crashing.
    try { prefs = emojiSharedPreferences(); }
    catch (Exception _e) { return; }
    Set<String> lastUseSet = prefs.getStringSet(LAST_USE_PREF, null);
    if (lastUseSet != null)
      for (String emojiData : lastUseSet)
      {
        String[] data = emojiData.split("-", 2);
        Emoji emoji;
        if (data.length != 2)
          continue ;
        emoji = Emoji.getEmojiByString(data[1]);
        if (emoji == null)
          continue ;
        _lastUsed.put(emoji, Integer.valueOf(data[0]));
      }
  }

  SharedPreferences emojiSharedPreferences()
  {
    return getContext().getSharedPreferences("emoji_last_use", Context.MODE_PRIVATE);
  }

  static class EmojiView extends TextView
  {
    public EmojiView(Context context)
    {
      super(context);
    }

    public void setEmoji(Emoji emoji)
    {
      setText(emoji.kv().getString());
    }
  }

  static class EmojiViewAdpater extends BaseAdapter
  {
    Context _button_context;

    List<Emoji> _emojiArray;

    public EmojiViewAdpater(Context context, List<Emoji> emojiArray)
    {
      _button_context = new ContextThemeWrapper(context, R.style.emojiGridButton);
      _emojiArray = emojiArray;
    }

    public int getCount()
    {
      if (_emojiArray == null)
        return (0);
      return (_emojiArray.size());
    }

    public Object getItem(int pos)
    {
      return (_emojiArray.get(pos));
    }

    public long getItemId(int pos)
    {
      return (pos);
    }

    public View getView(int pos, View convertView, ViewGroup parent)
    {
      EmojiView view = (EmojiView)convertView;

      if (view == null)
        view = new EmojiView(_button_context);
      view.setEmoji(_emojiArray.get(pos));
      return view;
    }
  }
}
