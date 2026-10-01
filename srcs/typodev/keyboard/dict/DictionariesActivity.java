package typodev.keyboard.dict;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;
import typodev.keyboard.Logs;
import typodev.keyboard.R;
import typodev.keyboard.SettingsThemeHelper;

public class DictionariesActivity extends Activity
{
  private static final int REQ_CODE_IMPORT = 1001;

  private TextView tvExternalStats;
  private TextView tvNoDicts;
  private LinearLayout llInstalledDicts;

  private View btnDlBengali;
  private View btnDlEnAosp;
  private View btnDlEnLarge;

  private View btnImportFile;
  private View btnCustomUrl;
  private View btnClearExternal;

  private ExternalDictionaryManager extManager;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    try
    {
      if (getActionBar() != null)
      {
        getActionBar().hide();
      }
      SettingsThemeHelper.applyAdaptiveSystemBars(this);

      setContentView(R.layout.dictionaries_activity);

      View btnBack = findViewById(R.id.btn_dict_back);
      if (btnBack != null)
      {
        btnBack.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            finish();
          }
        });
      }

      extManager = ExternalDictionaryManager.instance(this);

      tvExternalStats = (TextView) findViewById(R.id.tv_external_stats);
      tvNoDicts = (TextView) findViewById(R.id.tv_no_dicts);
      llInstalledDicts = (LinearLayout) findViewById(R.id.ll_installed_dicts);

      btnDlBengali = findViewById(R.id.btn_dl_bengali);
      btnDlEnAosp = findViewById(R.id.btn_dl_en_aosp);
      btnDlEnLarge = findViewById(R.id.btn_dl_en_large);

      btnImportFile = findViewById(R.id.btn_import_file);
      btnCustomUrl = findViewById(R.id.btn_custom_url);
      btnClearExternal = findViewById(R.id.btn_clear_external);

      setupButtons();
      View dictListDevice = findViewById(R.id.dict_list_device);
      View dictListDivider = findViewById(R.id.dict_list_divider);
      if (dictListDevice != null && dictListDivider != null && dictListDevice.getVisibility() == View.GONE)
      {
        dictListDivider.setVisibility(View.GONE);
      }
      refreshInstalledDictsUI();
    }
    catch (Exception e)
    {
      Logs.exn("DictionariesActivity", e);
      Toast.makeText(this, "Failed to load dictionaries view: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    SettingsThemeHelper.applyAdaptiveSystemBars(this);
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    refreshInstalledDictsUI();
  }

  private void setupButtons()
  {
    if (btnDlBengali != null)
    {
      btnDlBengali.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          startDownload(
              ExternalDictionaryManager.PRESET_BN_URL,
              "dict_bn",
              "🇧🇩 বাংলা ডিকশনারি (Bengali Wordlist)",
              "Downloading Large Bengali Dictionary (43.9k words)...");
        }
      });
    }

    if (btnDlEnAosp != null)
    {
      btnDlEnAosp.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          startDownload(
              ExternalDictionaryManager.PRESET_EN_AOSP_URL,
              "dict_en_aosp",
              "🇬🇧 English AOSP Wordlist (FrostKeys)",
              "Downloading AOSP English Wordlist from Codeberg...");
        }
      });
    }

    if (btnDlEnLarge != null)
    {
      btnDlEnLarge.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          startDownload(
              ExternalDictionaryManager.PRESET_EN_LARGE_URL,
              "dict_en_large",
              "🇬🇧 English Standard Wordlist",
              "Downloading English Standard Wordlist...");
        }
      });
    }

    if (btnImportFile != null)
    {
      btnImportFile.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          launchFilePicker();
        }
      });
    }

    if (btnCustomUrl != null)
    {
      btnCustomUrl.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          showCustomUrlDialog();
        }
      });
    }

    if (btnClearExternal != null)
    {
      btnClearExternal.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          confirmClearAll();
        }
      });
    }
  }

  void refreshInstalledDictsUI()
  {
    if (extManager == null) return;

    List<ExternalDictionaryManager.DictInfo> list = extManager.getInstalledDictionaries();

    int totalCount = extManager.getWordCount();
    if (totalCount <= 0 && list != null && !list.isEmpty())
    {
      for (ExternalDictionaryManager.DictInfo info : list)
      {
        totalCount += info.wordCount;
      }
    }

    if (tvExternalStats != null)
    {
      if (totalCount > 0)
      {
        String formattedTotal = NumberFormat.getInstance().format(totalCount);
        tvExternalStats.setText("Total Words Loaded: " + formattedTotal + " words");
        tvExternalStats.setTextColor(getResources().getColor(R.color.settings_accent));
      }
      else
      {
        tvExternalStats.setText("No external wordlists active");
        tvExternalStats.setTextColor(getResources().getColor(R.color.settings_text_subtitle));
      }
    }

    if (llInstalledDicts == null) return;
    llInstalledDicts.removeAllViews();

    if (list == null || list.isEmpty())
    {
      if (tvNoDicts != null) tvNoDicts.setVisibility(View.VISIBLE);
      return;
    }

    if (tvNoDicts != null) tvNoDicts.setVisibility(View.GONE);

    LayoutInflater inflater = LayoutInflater.from(this);
    for (int i = 0; i < list.size(); i++)
    {
      final ExternalDictionaryManager.DictInfo info = list.get(i);
      View itemView = inflater.inflate(R.layout.item_installed_dict, llInstalledDicts, false);

      TextView tvIcon = (TextView) itemView.findViewById(R.id.tv_dict_icon);
      TextView tvTitle = (TextView) itemView.findViewById(R.id.tv_dict_title);
      TextView tvSubtitle = (TextView) itemView.findViewById(R.id.tv_dict_subtitle);
      View btnDelete = itemView.findViewById(R.id.btn_delete_dict);

      String icon = "📁";
      String cleanTitle = info.title;
      if (info.id.startsWith("dict_bn") || (info.title != null && (info.title.contains("বাংলা") || info.title.contains("Bengali"))))
      {
        icon = "🇧🇩";
      }
      else if (info.id.startsWith("dict_en") || (info.title != null && (info.title.contains("English") || info.title.contains("AOSP"))))
      {
        icon = "🇬🇧";
      }
      else if (info.title != null && info.title.startsWith("📂"))
      {
        icon = "📥";
      }
      else if (info.title != null && info.title.startsWith("🔗"))
      {
        icon = "🔗";
      }

      if (cleanTitle != null)
      {
        cleanTitle = cleanTitle.replaceFirst("^[\\p{So}\\p{Cs}\\p{Cn}\\s\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "").trim();
      }

      if (tvIcon != null) tvIcon.setText(icon);
      if (tvTitle != null) tvTitle.setText(cleanTitle != null && !cleanTitle.isEmpty() ? cleanTitle : info.title);
      if (tvSubtitle != null)
      {
        String wordsStr = NumberFormat.getInstance().format(info.wordCount) + " words";
        tvSubtitle.setText(wordsStr + " • " + info.fileName);
      }

      if (btnDelete != null)
      {
        btnDelete.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            confirmDeleteDictionary(info);
          }
        });
      }

      llInstalledDicts.addView(itemView);
    }
  }

  private void confirmDeleteDictionary(final ExternalDictionaryManager.DictInfo info)
  {
    if (isFinishing()) return;
    new AlertDialog.Builder(this)
        .setTitle("Delete Dictionary")
        .setMessage("Are you sure you want to remove:\n" + info.title + "?")
        .setPositiveButton("Delete", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            extManager.deleteDictionary(info.id, new Runnable()
            {
              @Override
              public void run()
              {
                refreshInstalledDictsUI();
                Toast.makeText(DictionariesActivity.this, "Removed: " + info.title, Toast.LENGTH_SHORT).show();
              }
            });
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmClearAll()
  {
    if (isFinishing()) return;
    new AlertDialog.Builder(this)
        .setTitle("Clear All Wordlists")
        .setMessage("Are you sure you want to clear ALL imported and downloaded external wordlists?")
        .setPositiveButton("Clear All", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            extManager.clearAllWords(new Runnable()
            {
              @Override
              public void run()
              {
                refreshInstalledDictsUI();
                Toast.makeText(DictionariesActivity.this, "All external wordlists cleared", Toast.LENGTH_SHORT).show();
              }
            });
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void launchFilePicker()
  {
    try
    {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("*/*");
      startActivityForResult(intent, REQ_CODE_IMPORT);
    }
    catch (Exception e)
    {
      try
      {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_CODE_IMPORT);
      }
      catch (Exception ex)
      {
        Toast.makeText(this, "No file manager found: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void showCustomUrlDialog()
  {
    if (isFinishing()) return;
    final EditText input = new EditText(this);
    input.setHint("https://raw.githubusercontent.com/.../words.txt");
    int pad = (int) (14 * getResources().getDisplayMetrics().density);
    input.setPadding(pad, pad, pad, pad);
    input.setTextColor(getResources().getColor(R.color.settings_text_title));
    input.setHintTextColor(getResources().getColor(R.color.settings_text_subtitle));
    input.setBackgroundResource(R.drawable.settings_search_bar_bg);

    LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    int horiz = (int) (20 * getResources().getDisplayMetrics().density);
    int top = (int) (10 * getResources().getDisplayMetrics().density);
    container.setPadding(horiz, top, horiz, 0);
    container.addView(input);

    new AlertDialog.Builder(this)
        .setTitle("Custom GitHub / Raw URL")
        .setMessage("Enter the direct URL to a wordlist or dictionary file (.txt, .dic, .combined, .gz):")
        .setView(container)
        .setPositiveButton("Download", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            String url = input.getText().toString().trim();
            if (!url.isEmpty())
            {
              String customId = "dict_custom_" + System.currentTimeMillis();
              startDownload(url, customId, "🔗 Custom URL Wordlist", "Downloading wordlist from custom URL...");
            }
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void startDownload(String url, String dictId, String dictTitle, String message)
  {
    if (isFinishing()) return;
    final ProgressDialog pd = new ProgressDialog(this);
    pd.setTitle("Downloading Wordlist");
    pd.setMessage(message);
    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    pd.setCancelable(false);
    try
    {
      pd.show();
    }
    catch (Exception ignored) {}

    extManager.downloadFromUrl(url, dictId, dictTitle, new ExternalDictionaryManager.DownloadCallback()
    {
      @Override
      public void onProgress(final int wordsImported)
      {
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (!isFinishing() && pd.isShowing())
            {
              try
              {
                pd.setMessage("Importing words: " + NumberFormat.getInstance().format(wordsImported) + "...");
              }
              catch (Exception ignored) {}
            }
          }
        });
      }

      @Override
      public void onSuccess(final int totalWords)
      {
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (!isFinishing())
            {
              try { pd.dismiss(); } catch (Exception ignored) {}
              refreshInstalledDictsUI();
              Toast.makeText(DictionariesActivity.this, "✅ Successfully imported " + NumberFormat.getInstance().format(totalWords) + " words!", Toast.LENGTH_LONG).show();
            }
          }
        });
      }

      @Override
      public void onError(final String error)
      {
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (!isFinishing())
            {
              try { pd.dismiss(); } catch (Exception ignored) {}
              Toast.makeText(DictionariesActivity.this, "❌ " + error, Toast.LENGTH_LONG).show();
            }
          }
        });
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQ_CODE_IMPORT && resultCode == RESULT_OK && data != null && data.getData() != null)
    {
      Uri uri = data.getData();
      try
      {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is != null)
        {
          final ProgressDialog pd = new ProgressDialog(this);
          pd.setTitle("Importing Dictionary");
          pd.setMessage("Reading and indexing words...");
          pd.setCancelable(false);
          try { pd.show(); } catch (Exception ignored) {}

          String customTitle = "📂 File: " + (uri.getLastPathSegment() != null ? uri.getLastPathSegment() : "Wordlist");

          extManager.importFromStream(is, customTitle, new ExternalDictionaryManager.DownloadCallback()
          {
            @Override
            public void onProgress(final int wordsImported)
            {
              runOnUiThread(new Runnable()
              {
                @Override
                public void run()
                {
                  if (!isFinishing() && pd.isShowing())
                  {
                    try
                    {
                      pd.setMessage("Importing words: " + NumberFormat.getInstance().format(wordsImported) + "...");
                    }
                    catch (Exception ignored) {}
                  }
                }
              });
            }

            @Override
            public void onSuccess(final int totalWords)
            {
              runOnUiThread(new Runnable()
              {
                @Override
                public void run()
                {
                  if (!isFinishing())
                  {
                    try { pd.dismiss(); } catch (Exception ignored) {}
                    refreshInstalledDictsUI();
                    Toast.makeText(DictionariesActivity.this, "✅ Successfully imported " + NumberFormat.getInstance().format(totalWords) + " words!", Toast.LENGTH_LONG).show();
                  }
                }
              });
            }

            @Override
            public void onError(final String error)
            {
              runOnUiThread(new Runnable()
              {
                @Override
                public void run()
                {
                  if (!isFinishing())
                  {
                    try { pd.dismiss(); } catch (Exception ignored) {}
                    Toast.makeText(DictionariesActivity.this, "❌ " + error, Toast.LENGTH_LONG).show();
                  }
                }
              });
            }
          });
        }
      }
      catch (Exception e)
      {
        Toast.makeText(this, "Failed to read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      }
    }
  }
}
