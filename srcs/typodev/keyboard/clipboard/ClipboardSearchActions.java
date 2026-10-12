package typodev.keyboard.clipboard;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import typodev.keyboard.R;

/** Opens search only after an explicit action on a clipboard item. */
public final class ClipboardSearchActions {
  public static Intent webIntent(String text) {
    Uri url = Uri.parse("https://www.google.com/search").buildUpon().appendQueryParameter("q", text).build();
    return new Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }
  public static Intent imageIntent(Uri image, String mime, String app) {
    Intent intent = new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, image);
    intent.setClipData(new ClipData("Clipboard image", new String[]{mime}, new ClipData.Item(image)));
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
    if (app != null) intent.setPackage(app);
    return intent;
  }
  public static void web(Context context, String text) {
    try { context.startActivity(webIntent(text)); }
    catch (RuntimeException error) { Toast.makeText(context, R.string.clipboard_search_unavailable, Toast.LENGTH_SHORT).show(); }
  }
  public static void lens(Context context, Uri image, String mime) {
    for (String app : new String[]{"com.google.ar.lens", "com.google.android.googlequicksearchbox"}) {
      try { context.startActivity(imageIntent(image, mime, app)); return; }
      catch (android.content.ActivityNotFoundException unavailable) { }
      catch (SecurityException unavailable) { }
    }
    Toast.makeText(context, R.string.clipboard_lens_choose, Toast.LENGTH_LONG).show();
    try { context.startActivity(Intent.createChooser(imageIntent(image, mime, null),
        context.getString(R.string.clipboard_visual_search)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    catch (RuntimeException error) { Toast.makeText(context, R.string.clipboard_search_unavailable, Toast.LENGTH_SHORT).show(); }
  }
}
