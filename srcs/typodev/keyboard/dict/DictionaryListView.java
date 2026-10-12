package typodev.keyboard.dict;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import juloo.cdict.Cdict;
import typodev.keyboard.DeviceLocales;
import typodev.keyboard.Logs;
import typodev.keyboard.R;
import typodev.keyboard.Utils;

public class DictionaryListView extends LinearLayout
{
  List<DictView> _dict_views;
  Dictionaries _dictionaries;
  Set<String> _pending = new HashSet<String>();

  public DictionaryListView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    setOrientation(LinearLayout.VERTICAL);
    _dictionaries = Dictionaries.instance(ctx);
    _dict_views = new ArrayList<DictView>();
    boolean device_locales =
      attrs.getAttributeBooleanValue(null, "device_locales", true);
    if (device_locales)
      inflate_views_device_locales(ctx);
    else
      inflate_views_all(ctx);

    if (_dict_views.isEmpty())
    {
      setVisibility(View.GONE);
    }
    refresh();
  }

  void inflate_views_device_locales(Context ctx)
  {
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    DeviceLocales locales = DeviceLocales.load(ctx);
    for (DeviceLocales.Loc loc : locales.installed)
    {
      if (loc.dictionary != null)
      {
        int idx = ds.find(loc.dictionary);
        if (idx >= 0)
          inflate_item(ctx, ds, idx);
      }
    }
  }

  void inflate_views_all(Context ctx)
  {
    SupportedDictionaries ds = SupportedDictionaries.get(ctx.getResources());
    for (int i = 0; i < ds.length(); i++)
      inflate_item(ctx, ds, i);
  }

  void inflate_item(Context ctx, SupportedDictionaries ds, int i)
  {
    View v = LayoutInflater.from(ctx)
      .inflate(R.layout.dictionary_download_item, this, false);
    _dict_views.add(this.new DictView(v, ds, i));
    addView(v);
  }

  /** Update the "installed" status of item views. Meaning whether the
      "download" or "delete" button is shown. */
  void refresh()
  {
    Set<String> installed = _dictionaries.get_installed();
    for (DictView d : _dict_views)
      d.refresh(installed);

    Context ctx = getContext();
    if (ctx instanceof DictionariesActivity)
    {
      ((DictionariesActivity) ctx).refreshInstalledDictsUI();
    }
  }

  void toggle_installed(String dict_name)
  {
    run_dictionary_action(dict_name, new Runnable()
        {
          public void run()
          {
            if (_dictionaries.get_installed().contains(dict_name))
              _dictionaries.uninstall(dict_name);
            else if (install_dictionary_from_internet(dict_name))
              post_toast(R.string.dictionaries_download_success);
            else
              post_toast(R.string.dictionaries_download_failed);
          }
        });
  }

  void confirm_uninstall(final String dict_name, String displayName)
  {
    new AlertDialog.Builder(getContext())
        .setTitle("Delete Dictionary")
        .setMessage("Are you sure you want to remove:\n" + displayName + "?")
        .setPositiveButton("Delete", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            run_dictionary_action(dict_name, new Runnable()
            {
              public void run()
              {
                _dictionaries.uninstall(dict_name);
              }
            });
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Run action [r] for dictionary [name] if no action is already running for
      that dictionary. Calls [refresh] after the action completed. */
  void run_dictionary_action(String name, Runnable r)
  {
    if (_pending.contains(name))
      return;
    _pending.add(name);
    (new Thread()
     {
       public void run()
       {
         r.run();
         post(new Runnable()
             {
               public void run()
               {
                 _pending.remove(name);
                 refresh();
               }
             });
       }
     }).start();
    refresh();
  }

  public static String getLocaleEmoji(String localeCode)
  {
    if (localeCode == null || localeCode.isEmpty()) return "🌐";
    String lower = localeCode.toLowerCase();
    switch (lower)
    {
      case "en_gb": return "🇬🇧";
      case "en_us": return "🇺🇸";
      case "en_au": return "🇦🇺";
      case "en_ca": return "🇨🇦";
      case "bn": return "🇧🇩";
      case "ar": return "🇸🇦";
      case "af": return "🇿🇦";
      case "az": return "🇦🇿";
      case "be": return "🇧🇾";
      case "bg": return "🇧🇬";
      case "bs": return "🇧🇦";
      case "cs": return "🇨🇿";
      case "da": return "🇩🇰";
      case "de": return "🇩🇪";
      case "de_at": return "🇦🇹";
      case "de_ch": return "🇨🇭";
      case "el": return "🇬🇷";
      case "es": return "🇪🇸";
      case "et": return "🇪🇪";
      case "eu": case "gl": case "ca": return "🇪🇸";
      case "fa_ir": return "🇮🇷";
      case "fi": return "🇫🇮";
      case "fr": return "🇫🇷";
      case "ga": return "🇮🇪";
      case "he": case "iw": return "🇮🇱";
      case "hi": case "hi_zz": case "as": case "gu": case "kn":
      case "ml": case "mr": case "or": case "pa": case "sa":
      case "sat": case "ta": case "te": case "mai": case "gom":
      case "ks": return "🇮🇳";
      case "hr": return "🇭🇷";
      case "hu": return "🇭🇺";
      case "hy": case "hy_east": return "🇦🇲";
      case "id": return "🇮🇩";
      case "is": return "🇮🇸";
      case "it": case "sc": return "🇮🇹";
      case "ka": return "🇬🇪";
      case "kab": return "🇩🇿";
      case "kk": return "🇰🇿";
      case "km": return "🇰🇭";
      case "la": return "🏛️";
      case "lb": return "🇱🇺";
      case "lt": return "🇱🇹";
      case "lv": return "🇱🇻";
      case "mg": return "🇲🇬";
      case "mk": return "🇲🇰";
      case "nb": case "nn": return "🇳🇴";
      case "ne": return "🇳🇵";
      case "nl": return "🇳🇱";
      case "pl": return "🇵🇱";
      case "pt_br": return "🇧🇷";
      case "pt_pt": return "🇵🇹";
      case "ro": return "🇷🇴";
      case "ru": return "🇷🇺";
      case "sk": return "🇸🇰";
      case "sl": return "🇸🇮";
      case "sr": case "sr_zz": return "🇷🇸";
      case "sv": return "🇸🇪";
      case "th": return "🇹🇭";
      case "tl": case "ceb": return "🇵🇭";
      case "tr": return "🇹🇷";
      case "uk": return "🇺🇦";
      case "ur": case "sd": return "🇵🇰";
      case "vi": return "🇻🇳";
      case "zgh": case "zgh_zz": return "🇲🇦";
      default:
        int underscore = localeCode.indexOf('_');
        if (underscore > 0 && underscore + 3 <= localeCode.length())
        {
          String country = localeCode.substring(underscore + 1, underscore + 3).toUpperCase();
          if (country.length() == 2 && Character.isLetter(country.charAt(0)) && Character.isLetter(country.charAt(1)))
          {
            int first = 0x1F1E6 + country.charAt(0) - 'A';
            int second = 0x1F1E6 + country.charAt(1) - 'A';
            return new String(Character.toChars(first)) + new String(Character.toChars(second));
          }
        }
        return "🌐";
    }
  }

  final class DictView implements View.OnClickListener
  {
    public final String dict_name;
    public final String display_name;
    public final String formatted_size;
    public final TextView tv_icon;
    public final TextView tv_locale;
    public final TextView tv_size;
    public final ProgressBar download_progress;
    public final ImageView download_button;

    public DictView(View view, SupportedDictionaries ds, int dict_index)
    {
      dict_name = ds.dict_name(dict_index);
      display_name = ds.display_name(dict_index);
      float size_mb = ds.size(dict_index) / 1048576.f;
      formatted_size = String.format(java.util.Locale.US, "%.2f MB", size_mb);

      tv_icon = (TextView) view.findViewById(R.id.dictionary_download_icon);
      tv_locale = (TextView) view.findViewById(R.id.dictionary_download_locale);
      tv_size = (TextView) view.findViewById(R.id.dictionary_download_size);
      download_progress = (ProgressBar) view.findViewById(R.id.dictionary_download_progress);
      download_button = (ImageView) view.findViewById(R.id.dictionary_download_button);

      if (tv_icon != null)
      {
        tv_icon.setText(getLocaleEmoji(dict_name));
      }
      if (tv_locale != null)
      {
        tv_locale.setText(display_name);
      }
      if (tv_size != null)
      {
        tv_size.setText(formatted_size + " • Offline dictionary");
      }

      if (download_button != null)
      {
        download_button.setOnClickListener(this);
      }
      view.setOnClickListener(this);
    }

    public void refresh(Set<String> installed)
    {
      boolean isPending = _pending.contains(dict_name);
      boolean isInstalled = installed.contains(dict_name);

      if (download_progress != null)
      {
        download_progress.setVisibility(isPending ? View.VISIBLE : View.GONE);
      }
      if (download_button != null)
      {
        download_button.setVisibility(isPending ? View.GONE : View.VISIBLE);
      }

      if (isInstalled)
      {
        if (download_button != null)
        {
          download_button.setImageResource(R.drawable.ic_delete);
          download_button.setColorFilter(0xFFFF4D4D);
          download_button.setContentDescription("Delete dictionary");
        }
        if (tv_size != null)
        {
          tv_size.setText(formatted_size + " • Installed");
          tv_size.setTextColor(0xFF4CAF50);
        }
      }
      else
      {
        int accent = getResources().getColor(R.color.settings_accent);
        int subtitleColor = getResources().getColor(R.color.settings_text_subtitle);
        if (download_button != null)
        {
          download_button.setImageResource(R.drawable.ic_download);
          download_button.setColorFilter(accent);
          download_button.setContentDescription("Download dictionary");
        }
        if (tv_size != null)
        {
          tv_size.setText(formatted_size + " • Offline dictionary");
          tv_size.setTextColor(subtitleColor);
        }
      }
    }

    @Override
    public void onClick(View v)
    {
      if (_pending.contains(dict_name)) return;
      if (_dictionaries.get_installed().contains(dict_name))
      {
        confirm_uninstall(dict_name, display_name);
      }
      else
      {
        toggle_installed(dict_name);
      }
    }
  }

  static final String DICT_REPO_URL =
    "https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main";

  static URL url_of_dictionary(String dict_name)
      throws MalformedURLException
  {
    int format_version = Cdict.format_version();
    return new URL(DICT_REPO_URL + "/v" + format_version + "/" + dict_name
        + ".dict");
  }

  /** Returns [true] on success. */
  boolean install_dictionary_from_internet(String dict_name)
  {
    try
    {
      // Remote files are compressed with gzip at rest. Do not use server side
      // compression and force decompression.
      URLConnection con = url_of_dictionary(dict_name).openConnection();
      con.setRequestProperty("Accept-Encoding", "identity");
      byte[] data = Utils.read_all_bytes(new GZIPInputStream(con.getInputStream()));
      Cdict.of_bytes(data); // Check that the dictionary can load.
      _dictionaries.install(dict_name, data);
      return true;
    }
    catch (Exception e)
    {
      Logs.exn("", e);
      return false;
    }
  }

  void post_toast(int msg_id)
  {
    post(new Runnable()
        {
          public void run()
          {
            Toast.makeText(getContext(), msg_id, Toast.LENGTH_SHORT).show();
          }
        });
  }
}
