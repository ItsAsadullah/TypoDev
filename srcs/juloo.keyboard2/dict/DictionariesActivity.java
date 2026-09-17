package juloo.keyboard2.dict;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;

public class DictionariesActivity extends Activity
{
  private static final int REQ_CODE_IMPORT = 1001;

  private TextView tvExternalStats;
  private TextView tvNoDicts;
  private LinearLayout llInstalledDicts;

  private Button btnDlBengali;
  private Button btnDlEnAosp;
  private Button btnDlEnLarge;

  private Button btnImportFile;
  private Button btnCustomUrl;
  private Button btnClearExternal;

  private ExternalDictionaryManager extManager;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    try
    {
      setContentView(R.layout.dictionaries_activity);

      extManager = ExternalDictionaryManager.instance(this);

      tvExternalStats = (TextView) findViewById(R.id.tv_external_stats);
      tvNoDicts = (TextView) findViewById(R.id.tv_no_dicts);
      llInstalledDicts = (LinearLayout) findViewById(R.id.ll_installed_dicts);

      btnDlBengali = (Button) findViewById(R.id.btn_dl_bengali);
      btnDlEnAosp = (Button) findViewById(R.id.btn_dl_en_aosp);
      btnDlEnLarge = (Button) findViewById(R.id.btn_dl_en_large);

      btnImportFile = (Button) findViewById(R.id.btn_import_file);
      btnCustomUrl = (Button) findViewById(R.id.btn_custom_url);
      btnClearExternal = (Button) findViewById(R.id.btn_clear_external);

      setupButtons();
      refreshInstalledDictsUI();
    }
    catch (Exception e)
    {
      Logs.exn("DictionariesActivity", e);
      Toast.makeText(this, "Failed to load dictionaries view: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
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

  private void refreshInstalledDictsUI()
  {
    if (extManager == null) return;

    int totalCount = extManager.getWordCount();
    String formattedTotal = NumberFormat.getInstance().format(totalCount);
    if (tvExternalStats != null)
    {
      tvExternalStats.setText("Total Words Loaded: " + formattedTotal + " words");
    }

    if (llInstalledDicts == null) return;
    llInstalledDicts.removeAllViews();

    List<ExternalDictionaryManager.DictInfo> list = extManager.getInstalledDictionaries();
    if (list.isEmpty())
    {
      if (tvNoDicts != null) tvNoDicts.setVisibility(View.VISIBLE);
      return;
    }

    if (tvNoDicts != null) tvNoDicts.setVisibility(View.GONE);

    LayoutInflater inflater = LayoutInflater.from(this);
    for (final ExternalDictionaryManager.DictInfo info : list)
    {
      View itemView = inflater.inflate(R.layout.item_installed_dict, llInstalledDicts, false);

      TextView tvTitle = (TextView) itemView.findViewById(R.id.tv_dict_title);
      TextView tvSubtitle = (TextView) itemView.findViewById(R.id.tv_dict_subtitle);
      Button btnDelete = (Button) itemView.findViewById(R.id.btn_delete_dict);

      if (tvTitle != null) tvTitle.setText(info.title);
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
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    input.setPadding(pad, pad, pad, pad);

    new AlertDialog.Builder(this)
        .setTitle("Custom GitHub / Raw URL")
        .setMessage("Enter the direct URL to a wordlist or dictionary file (.txt, .dic, .combined, .gz):")
        .setView(input)
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
