package typodev.keyboard.translate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.R;
import typodev.keyboard.Utils;

public class TranslateBarView extends LinearLayout
{
  private TextView _btnClose;
  private Button _btnSourceLang;
  private TextView _btnSwapLang;
  private Button _btnTargetLang;
  private TextView _btnOfflineStatus;
  private EditText _etInput;
  private ProgressBar _progressBar;
  private TextView _btnClear;

  private Keyboard2 _keyboard2 = null;
  private final StringBuilder _inputBuffer = new StringBuilder();
  private String _composingText = "";
  private boolean _hasActiveTargetComposing = false;
  private final Handler _handler = new Handler(Looper.getMainLooper());
  private Runnable _debounceTranslateRunnable = null;
  private String _lastCommittedTranslatedText = "";
  private boolean _isTranslating = false;
  private boolean _isDownloadingModel = false;
  private long _translationRevision = 0;
  private boolean _isOpen = false;

  public TranslateBarView(Context context)
  {
    super(context);
  }

  public TranslateBarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public TranslateBarView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
  }

  public void setKeyboard(Keyboard2 keyboard)
  {
    _keyboard2 = keyboard;
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();

    _btnClose = (TextView) findViewById(R.id.btn_translate_close);
    _btnSourceLang = (Button) findViewById(R.id.btn_source_lang);
    _btnSwapLang = (TextView) findViewById(R.id.btn_swap_lang);
    _btnTargetLang = (Button) findViewById(R.id.btn_target_lang);
    _btnOfflineStatus = (TextView) findViewById(R.id.btn_offline_status);
    _etInput = (EditText) findViewById(R.id.tv_translate_input);
    if (_etInput != null)
    {
      if (Build.VERSION.SDK_INT >= 21)
      {
        _etInput.setShowSoftInputOnFocus(false);
      }
      _etInput.setCursorVisible(true);
      _etInput.setFocusable(true);
      _etInput.setFocusableInTouchMode(true);
    }
    _progressBar = (ProgressBar) findViewById(R.id.translate_progress);
    _btnClear = (TextView) findViewById(R.id.btn_translate_clear);

    if (_btnClose != null)
    {
      _btnClose.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          closeTranslateMode();
        }
      });
    }

    if (_btnSourceLang != null)
    {
      _btnSourceLang.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          showLanguagePicker(true);
        }
      });
    }

    if (_btnTargetLang != null)
    {
      _btnTargetLang.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          showLanguagePicker(false);
        }
      });
    }

    if (_btnSwapLang != null)
    {
      _btnSwapLang.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          TranslationEngine.swapLanguages();
          if (_lastCommittedTranslatedText != null && !_lastCommittedTranslatedText.trim().isEmpty())
          {
            _inputBuffer.setLength(0);
            _inputBuffer.append(_lastCommittedTranslatedText);
            _composingText = "";
            updateInputDisplay();
          }
          updateLanguageButtons();
          triggerTranslationImmediate();
        }
      });
    }

    if (_btnOfflineStatus != null)
    {
      _btnOfflineStatus.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (TranslationEngine.isOfflinePackDownloaded(getContext()))
          {
            showOfflineActiveDialog();
          }
          else
          {
            promptDownloadOfflinePack();
          }
        }
      });
    }

    if (_btnClear != null)
    {
      _btnClear.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          clearBuffer();
        }
      });
    }

    updateLanguageButtons();
    refreshOfflineStatus();
  }

  public void onTranslateOpened()
  {
    _isOpen = true;
    setVisibility(View.VISIBLE);
    clearBuffer();
    updateLanguageButtons();
    refreshOfflineStatus();
  }

  public void closeTranslateMode()
  {
    onTranslateClosed();
    if (_keyboard2 != null)
    {
      _keyboard2.hideTranslateBar();
    }
    else
    {
      setVisibility(View.GONE);
    }
  }

  /** Invalidate delayed work before the keyboard hides or changes editors. */
  public void onTranslateClosed()
  {
    _isOpen = false;
    invalidateTranslation();
    _composingText = "";
    if (_hasActiveTargetComposing && _keyboard2 != null)
    {
      InputConnection ic = _keyboard2.getTargetAppInputConnection();
      if (ic != null) ic.finishComposingText();
    }
    _hasActiveTargetComposing = false;
  }

  @Override
  protected void onDetachedFromWindow()
  {
    _isOpen = false;
    invalidateTranslation();
    super.onDetachedFromWindow();
  }

  private void invalidateTranslation()
  {
    ++_translationRevision;
    if (_debounceTranslateRunnable != null)
    {
      _handler.removeCallbacks(_debounceTranslateRunnable);
      _debounceTranslateRunnable = null;
    }
    _isTranslating = false;
    if (_progressBar != null && !_isDownloadingModel) _progressBar.setVisibility(View.GONE);
  }

  private void updateLanguageButtons()
  {
    TranslationEngine.Language src = TranslationEngine.getLanguageByCode(TranslationEngine.getSourceLanguage());
    TranslationEngine.Language tgt = TranslationEngine.getLanguageByCode(TranslationEngine.getTargetLanguage());

    if (_btnSourceLang != null)
    {
      _btnSourceLang.setText(src.displayName);
    }
    if (_btnTargetLang != null)
    {
      _btnTargetLang.setText(tgt.nativeName);
    }
    refreshOfflineStatus();
  }

  public void refreshOfflineStatus()
  {
    if (_btnOfflineStatus == null || _isDownloadingModel) return;

    final boolean isOnline = TranslationEngine.isNetworkAvailable(getContext());
    final boolean isOfflineReady = TranslationEngine.isOfflinePackDownloaded(getContext());

    if (isOfflineReady)
    {
      _btnOfflineStatus.setText(isOnline ? "✓ Online" : "✓ Offline");
      _btnOfflineStatus.setTextColor(0xFF34A853); // Google Green
    }
    else
    {
      _btnOfflineStatus.setText("📥 Offline Pack");
      _btnOfflineStatus.setTextColor(Color.WHITE);
    }
  }

  private void showOfflineActiveDialog()
  {
    final String srcCode = TranslationEngine.getSourceLanguage();
    final String tgtCode = TranslationEngine.getTargetLanguage();

    AlertDialog dialog = new AlertDialog.Builder(getContext())
        .setTitle("✓ অফলাইন অনুবাদ সক্রিয়")
        .setMessage("অফলাইন অনুবাদ প্যাক ইতিমধ্যে সংরক্ষিত ও প্রস্তুত রয়েছে। ইন্টারনেট না থাকলেও আপনার কিবোর্ডে স্বয়ংক্রিয়ভাবে অফলাইন অনুবাদ কাজ করবে।")
        .setPositiveButton("ঠিক আছে", null)
        .setNeutralButton("পুনরায় ডাউনলোড", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            startModelDownload(srcCode, tgtCode);
          }
        })
        .create();

    if (_keyboard2 != null)
    {
      Utils.show_dialog_on_ime(dialog, _keyboard2);
    }
    else
    {
      try { dialog.show(); } catch (Throwable ignored) {}
    }
  }

  private void promptDownloadOfflinePack()
  {
    final String srcCode = TranslationEngine.getSourceLanguage();
    final String tgtCode = TranslationEngine.getTargetLanguage();
    TranslationEngine.Language src = TranslationEngine.getLanguageByCode(srcCode);
    TranslationEngine.Language tgt = TranslationEngine.getLanguageByCode(tgtCode);

    AlertDialog dialog = new AlertDialog.Builder(getContext())
        .setTitle("📥 অফলাইন ট্রান্সলেশন প্যাক")
        .setMessage(src.displayName + " এবং " + tgt.nativeName + " এর অফলাইন অনুবাদ মডেল ডাউনলোড করতে চান?\n\nডাউনলোড শেষে ইন্টারনেট ছাড়াও যেকোনো সময় দ্রুত অনুবাদ করতে পারবেন।")
        .setPositiveButton("ডাউনলোড করুন", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            startModelDownload(srcCode, tgtCode);
          }
        })
        .setNegativeButton("পরে করব", null)
        .create();

    if (_keyboard2 != null)
    {
      Utils.show_dialog_on_ime(dialog, _keyboard2);
    }
    else
    {
      try { dialog.show(); } catch (Throwable ignored) {}
    }
  }

  private void startModelDownload(String src, String tgt)
  {
    _isDownloadingModel = true;
    if (_progressBar != null) _progressBar.setVisibility(View.VISIBLE);
    if (_btnOfflineStatus != null)
    {
      _btnOfflineStatus.setText("⏳ Downloading...");
      _btnOfflineStatus.setTextColor(0xFF4285F4);
    }

    Toast.makeText(getContext(), "অফলাইন মডেল ডাউনলোড শুরু হয়েছে...", Toast.LENGTH_SHORT).show();

    TranslationEngine.downloadOfflinePair(getContext(), src, tgt, new TranslationEngine.ModelDownloadCallback()
    {
      @Override
      public void onDownloadStarted()
      {
      }

      @Override
      public void onDownloadCompleted()
      {
        _isDownloadingModel = false;
        if (_progressBar != null && !_isTranslating) _progressBar.setVisibility(View.GONE);
        refreshOfflineStatus();
        if (_isOpen || isAttachedToWindow())
        {
          Toast.makeText(getContext(), "অফলাইন মডেল সফলভাবে ডাউনলোড হয়েছে!", Toast.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onDownloadFailed(String error)
      {
        _isDownloadingModel = false;
        if (_progressBar != null && !_isTranslating) _progressBar.setVisibility(View.GONE);
        refreshOfflineStatus();
        if (_isOpen || isAttachedToWindow())
        {
          Toast.makeText(getContext(), "ডাউনলোড ব্যর্থ হয়েছে: " + error, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  private void showLanguagePicker(final boolean isSource)
  {
    final TranslationEngine.Language[] langs = TranslationEngine.SUPPORTED_LANGUAGES;
    String[] items = new String[langs.length];
    for (int i = 0; i < langs.length; i++)
    {
      items[i] = langs[i].toString();
    }

    AlertDialog dialog = new AlertDialog.Builder(getContext())
        .setTitle(isSource ? "সোর্স ভাষা নির্বাচন করুন" : "টার্গেট ভাষা নির্বাচন করুন")
        .setItems(items, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            TranslationEngine.Language chosen = langs[which];
            String prevSrc = TranslationEngine.getSourceLanguage();
            String prevTgt = TranslationEngine.getTargetLanguage();
            if (isSource)
            {
              TranslationEngine.setSourceLanguage(chosen.code);
              if (chosen.code.equalsIgnoreCase(prevTgt))
              {
                TranslationEngine.setTargetLanguage(prevSrc);
              }
            }
            else
            {
              TranslationEngine.setTargetLanguage(chosen.code);
              if (chosen.code.equalsIgnoreCase(prevSrc))
              {
                TranslationEngine.setSourceLanguage(prevTgt);
              }
            }
            updateLanguageButtons();
            triggerTranslationImmediate();
          }
        })
        .setNegativeButton("বাতিল", null)
        .create();

    if (_keyboard2 != null)
    {
      Utils.show_dialog_on_ime(dialog, _keyboard2);
    }
    else
    {
      try { dialog.show(); } catch (Throwable ignored) {}
    }
  }

  private String getFullInputText()
  {
    if (_composingText == null || _composingText.isEmpty())
    {
      return _inputBuffer.toString();
    }
    return _inputBuffer.toString() + _composingText;
  }

  private void onBufferMutated()
  {
    String full = getFullInputText();
    updateInputDisplay();
    if (full.isEmpty())
    {
      clearBuffer();
    }
    else if (full.trim().isEmpty())
    {
      invalidateTranslation();
      _lastCommittedTranslatedText = "";
      if (_hasActiveTargetComposing && _keyboard2 != null)
      {
        InputConnection ic = _keyboard2.getTargetAppInputConnection();
        if (ic != null)
        {
          ic.setComposingText("", 1);
          ic.finishComposingText();
        }
        _hasActiveTargetComposing = false;
      }
    }
    else
    {
      scheduleTranslationDebounced();
    }
  }

  public void clearBuffer()
  {
    invalidateTranslation();
    _inputBuffer.setLength(0);
    _composingText = "";
    _lastCommittedTranslatedText = "";
    updateInputDisplay();
    if (_hasActiveTargetComposing && _keyboard2 != null)
    {
      InputConnection ic = _keyboard2.getTargetAppInputConnection();
      if (ic != null)
      {
        ic.setComposingText("", 1);
        ic.finishComposingText();
      }
    }
    _hasActiveTargetComposing = false;
  }

  public InputConnection createInputConnection(final InputConnection targetAppIc)
  {
    return new android.view.inputmethod.InputConnectionWrapper(targetAppIc != null ? targetAppIc : new android.view.inputmethod.BaseInputConnection(this, false), true)
    {
      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        if (text != null)
        {
          _composingText = "";
          if (text.length() > 0)
          {
            _inputBuffer.append(text);
          }
          onBufferMutated();
          return true;
        }
        return false;
      }

      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition)
      {
        if (text != null)
        {
          _composingText = text.toString();
          onBufferMutated();
          return true;
        }
        return false;
      }

      @Override
      public boolean finishComposingText()
      {
        if (_composingText != null && !_composingText.isEmpty())
        {
          _inputBuffer.append(_composingText);
          _composingText = "";
          updateInputDisplay();
        }
        return true;
      }

      @Override
      public boolean deleteSurroundingText(int beforeLength, int afterLength)
      {
        if (_composingText != null && !_composingText.isEmpty() && _inputBuffer.length() == 0 && beforeLength > 0)
        {
          int cLen = _composingText.length();
          int cStart = Math.max(0, cLen - beforeLength);
          if (cStart > 0 && cStart < cLen
              && Character.isLowSurrogate(_composingText.charAt(cStart))
              && Character.isHighSurrogate(_composingText.charAt(cStart - 1)))
          {
            cStart--;
          }
          _composingText = _composingText.substring(0, cStart);
          onBufferMutated();
          return true;
        }

        int len = _inputBuffer.length();
        int start = Math.max(0, len - Math.max(0, beforeLength));
        if (start > 0 && start < len
            && Character.isLowSurrogate(_inputBuffer.charAt(start))
            && Character.isHighSurrogate(_inputBuffer.charAt(start - 1)))
        {
          start--;
        }
        if (start < len)
        {
          _inputBuffer.delete(start, len);
          onBufferMutated();
        }
        else if (len == 0 && (_composingText == null || _composingText.isEmpty()) && targetAppIc != null)
        {
          targetAppIc.deleteSurroundingText(beforeLength, afterLength);
        }
        return true;
      }

      @Override
      public boolean performEditorAction(int actionCode)
      {
        if (!isBufferEmpty())
        {
          if (_composingText != null && !_composingText.isEmpty())
          {
            _inputBuffer.append(_composingText);
            _composingText = "";
          }
          if (_hasActiveTargetComposing && targetAppIc != null)
          {
            targetAppIc.finishComposingText();
            _hasActiveTargetComposing = false;
          }
          clearBuffer();
          return true;
        }
        return targetAppIc != null && targetAppIc.performEditorAction(actionCode);
      }

      @Override
      public boolean sendKeyEvent(android.view.KeyEvent event)
      {
        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN)
        {
          if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_DEL)
          {
            onBackspace();
            return true;
          }
          else if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)
          {
            if (targetAppIc != null)
            {
              if (_hasActiveTargetComposing)
              {
                targetAppIc.finishComposingText();
                _hasActiveTargetComposing = false;
              }
              targetAppIc.sendKeyEvent(event);
            }
            clearBuffer();
            return true;
          }
        }
        return super.sendKeyEvent(event);
      }

      @Override
      public CharSequence getTextBeforeCursor(int n, int flags)
      {
        String full = getFullInputText();
        int len = full.length();
        if (len <= 0 || n <= 0) return "";
        int from = Math.max(0, len - n);
        return full.subSequence(from, len);
      }

      @Override
      public CharSequence getTextAfterCursor(int n, int flags)
      {
        return "";
      }

      @Override
      public SurroundingText getSurroundingText(int beforeLength, int afterLength, int flags)
      {
        if (Build.VERSION.SDK_INT >= 31)
        {
          String full = getFullInputText();
          int len = full.length();
          int start = Math.max(0, len - Math.max(0, beforeLength));
          CharSequence text = full.subSequence(start, len);
          return new SurroundingText(text, text.length(), text.length(), 0);
        }
        return null;
      }

      @Override
      public CharSequence getSelectedText(int flags)
      {
        return "";
      }
    };
  }

  public void onKeyEntered(String text)
  {
    if (text == null || text.isEmpty()) return;
    _composingText = "";
    _inputBuffer.append(text);
    onBufferMutated();
  }

  public void onBackspace()
  {
    if (_composingText != null && !_composingText.isEmpty())
    {
      int end = _composingText.length();
      int start = Character.offsetByCodePoints(_composingText, end, -1);
      _composingText = _composingText.substring(0, start);
      onBufferMutated();
    }
    else if (_inputBuffer.length() > 0)
    {
      int end = _inputBuffer.length();
      _inputBuffer.delete(_inputBuffer.offsetByCodePoints(end, -1), end);
      onBufferMutated();
    }
    else
    {
      if (_keyboard2 != null)
      {
        InputConnection ic = _keyboard2.getTargetAppInputConnection();
        if (ic != null)
        {
          CharSequence selected = ic.getSelectedText(0);
          if (selected != null && selected.length() > 0)
          {
            ic.commitText("", 1);
          }
          else
          {
            CharSequence before = ic.getTextBeforeCursor(2, 0);
            int deleteLen = 1;
            if (before != null && before.length() == 2
                && Character.isHighSurrogate(before.charAt(0))
                && Character.isLowSurrogate(before.charAt(1)))
            {
              deleteLen = 2;
            }
            ic.deleteSurroundingText(deleteLen, 0);
          }
        }
      }
    }
  }

  private void updateInputDisplay()
  {
    if (_etInput != null)
    {
      String text = getFullInputText();
      _etInput.setText(text);
      if (text.length() > 0)
      {
        _etInput.setSelection(text.length());
        if (_btnClear != null) _btnClear.setVisibility(View.VISIBLE);
      }
      else
      {
        if (_btnClear != null) _btnClear.setVisibility(View.GONE);
      }
    }
  }

  private void scheduleTranslationDebounced()
  {
    invalidateTranslation();
    if (!_isOpen) return;
    _debounceTranslateRunnable = new Runnable()
    {
      @Override
      public void run()
      {
        triggerTranslationImmediate();
      }
    };
    _handler.postDelayed(_debounceTranslateRunnable, 200);
  }

  private void triggerTranslationImmediate()
  {
    invalidateTranslation();
    if (!_isOpen || _keyboard2 == null) return;
    final String textToTranslate = getFullInputText().trim();
    if (textToTranslate.isEmpty())
    {
      if (_progressBar != null && !_isDownloadingModel) _progressBar.setVisibility(View.GONE);
      return;
    }

    if (_progressBar != null) _progressBar.setVisibility(View.VISIBLE);
    _isTranslating = true;
    final long revision = _translationRevision;
    final long sessionId = _keyboard2.getInputSessionId();
    final InputConnection target = _keyboard2.getTargetAppInputConnection();
    if (target == null) return;

    TranslationEngine.translate(
        getContext(),
        textToTranslate,
        TranslationEngine.getSourceLanguage(),
        TranslationEngine.getTargetLanguage(),
        new TranslationEngine.TranslationCallback()
        {
          @Override
          public void onSuccess(final String translatedText, final boolean isOffline)
          {
            if (!_isOpen || revision != _translationRevision || _keyboard2 == null
                || sessionId != _keyboard2.getInputSessionId()
                || target != _keyboard2.getTargetAppInputConnection()) return;
            _isTranslating = false;
            if (_progressBar != null && !_isDownloadingModel) _progressBar.setVisibility(View.GONE);

            // Stream live composing text directly into active editor!
            if (_keyboard2 != null && translatedText != null)
            {
              InputConnection ic = _keyboard2.getTargetAppInputConnection();
              if (ic != null)
              {
                ic.setComposingText(translatedText, 1);
                _hasActiveTargetComposing = !translatedText.isEmpty();
                _lastCommittedTranslatedText = translatedText;
              }
            }
          }

          @Override
          public void onError(final String error)
          {
            if (!_isOpen || revision != _translationRevision) return;
            _isTranslating = false;
            if (_progressBar != null && !_isDownloadingModel) _progressBar.setVisibility(View.GONE);
          }
        });
  }

  public boolean isBufferEmpty()
  {
    return _inputBuffer.length() == 0 && (_composingText == null || _composingText.isEmpty());
  }
}
