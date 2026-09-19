package juloo.keyboard2.snippet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Lightweight transparent activity that hosts the SnippetEditorDialog.
 * By running inside an Activity instead of an IME service window, Android's
 * InputMethodManager allows the software keyboard (TypoDev) to appear and
 * type into the dialog's EditText fields seamlessly.
 */
public class SnippetEditActivity extends Activity
{
  private AlertDialog _dialog;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    overridePendingTransition(0, 0);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    View dummy = new View(this);
    dummy.setBackgroundColor(0);
    setContentView(dummy);

    String clipText = getIntent().getStringExtra("clip_text");
    String snippetId = getIntent().getStringExtra("snippet_id");
    Snippet existing = null;
    if (snippetId != null)
    {
      existing = SnippetStore.instance(this).getById(snippetId);
    }

    _dialog = SnippetEditorDialog.showInternal(this, null, existing, clipText,
        new SnippetEditorDialog.OnSavedCallback()
        {
          @Override
          public void onSnippetSaved(Snippet s)
          {
            finish();
          }
        },
        new DialogInterface.OnDismissListener()
        {
          @Override
          public void onDismiss(DialogInterface d)
          {
            if (!isFinishing())
            {
              finish();
            }
          }
        });

    if (_dialog == null && !isFinishing())
    {
      finish();
    }
  }

  @Override
  public void onBackPressed()
  {
    if (_dialog != null && _dialog.isShowing())
    {
      try { _dialog.dismiss(); } catch (Throwable ignored) {}
    }
    super.onBackPressed();
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    if (_dialog != null && _dialog.isShowing())
    {
      try { _dialog.dismiss(); } catch (Throwable ignored) {}
    }
  }

  @Override
  public void finish()
  {
    super.finish();
    overridePendingTransition(0, 0);
  }
}
