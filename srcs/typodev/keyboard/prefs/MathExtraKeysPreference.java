package typodev.keyboard.prefs;

import android.content.Context;
import android.util.AttributeSet;

/** Uses the same checkbox persistence and corner-swipe placement as other extra keys. */
public final class MathExtraKeysPreference extends ExtraKeysPreference
{
  public MathExtraKeysPreference(Context context, AttributeSet attrs) { super(context, attrs); }
  @Override protected String[] selectable_keys() { return math_keys; }
}
