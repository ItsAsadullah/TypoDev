package juloo.keyboard2.voice;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;
import java.util.ArrayList;
import juloo.keyboard2.Keyboard2;

/**
 * High-performance, lightweight voice typing service with live streaming into the candidate bar.
 */
public class OfflineVoiceTypingService
{
  private static SpeechRecognizer _speechRecognizer = null;
  private static boolean _isListening = false;
  private static boolean _hasCommitted = false;
  private static String _lastPartialText = "";
  private static Keyboard2 _activeKeyboard = null;

  public static boolean isListening()
  {
    return _isListening;
  }

  private static synchronized void commitVoiceResult(Keyboard2 keyboard, String text)
  {
    if (_hasCommitted) return;
    if (text == null || text.trim().isEmpty()) return;
    _hasCommitted = true;
    _lastPartialText = "";
    if (keyboard != null)
    {
      keyboard.commitVoiceText(text);
    }
  }

  public static void toggleListening(final Context context, final Keyboard2 keyboard)
  {
    if (Looper.myLooper() != Looper.getMainLooper())
    {
      new Handler(Looper.getMainLooper()).post(new Runnable()
      {
        @Override
        public void run()
        {
          toggleListening(context, keyboard);
        }
      });
      return;
    }

    if (_isListening)
    {
      stopListening();
    }
    else
    {
      startListening(context, keyboard);
    }
  }

  public static boolean isNetworkConnected(Context context)
  {
    try
    {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if (cm != null)
      {
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
      }
    }
    catch (Throwable ignored) {}
    return false;
  }

  public static void startListening(final Context context, final Keyboard2 keyboard)
  {
    if (Looper.myLooper() != Looper.getMainLooper())
    {
      new Handler(Looper.getMainLooper()).post(new Runnable()
      {
        @Override
        public void run()
        {
          startListening(context, keyboard);
        }
      });
      return;
    }

    _activeKeyboard = keyboard;
    _lastPartialText = "";
    _hasCommitted = false;
    _isListening = true;

    if (keyboard != null && keyboard.getCandidatesView() != null)
    {
      keyboard.getCandidatesView().onVoiceListeningStarted();
    }

    // 1. Check microphone permission
    if (context.checkCallingOrSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
    {
      VoicePermissionActivity.requestVoicePermission(context, keyboard);
      return;
    }

    // 2. Check SpeechRecognizer availability
    if (!SpeechRecognizer.isRecognitionAvailable(context))
    {
      Toast.makeText(context, "স্পিচ রিকগনিশন সার্ভিস পাওয়া যায়নি। Google Speech Services সক্রিয় করুন।", Toast.LENGTH_LONG).show();
      if (keyboard != null && keyboard.getCandidatesView() != null)
      {
        keyboard.getCandidatesView().onVoiceListeningStopped();
      }
      return;
    }

    stopListening();
    _isListening = true;

    try
    {
      _speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
    }
    catch (Exception e)
    {
      _speechRecognizer = null;
    }

    if (_speechRecognizer == null)
    {
      Toast.makeText(context, "SpeechRecognizer চালু করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show();
      if (keyboard != null && keyboard.getCandidatesView() != null)
      {
        keyboard.getCandidatesView().onVoiceListeningStopped();
      }
      return;
    }

    _speechRecognizer.setRecognitionListener(new RecognitionListener()
    {
      @Override
      public void onReadyForSpeech(Bundle params)
      {
        _isListening = true;
        if (_activeKeyboard != null && _activeKeyboard.getCandidatesView() != null)
        {
          _activeKeyboard.getCandidatesView().onVoiceListeningStarted();
        }
      }

      @Override public void onBeginningOfSpeech() {}
      @Override public void onRmsChanged(float rmsdB) {}
      @Override public void onBufferReceived(byte[] buffer) {}
      @Override public void onEndOfSpeech() {}

      @Override
      public void onError(int error)
      {
        _isListening = false;
        String message;
        switch (error)
        {
          case SpeechRecognizer.ERROR_AUDIO:
            message = "অডিও রেকর্ডিং সমস্যা";
            break;
          case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
            message = "মাইক্রোফোন অনুমতি প্রয়োজন";
            VoicePermissionActivity.requestVoicePermission(context, keyboard);
            break;
          case SpeechRecognizer.ERROR_NETWORK:
          case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            message = "বাংলা ভয়েস টাইপিংয়ের জন্য ইন্টারনেট সংযোগ প্রয়োজন";
            break;
          case SpeechRecognizer.ERROR_NO_MATCH:
            message = "কোনো কথা বোঝা যায়নি";
            break;
          case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            message = "কথা বলার সময় শেষ";
            break;
          case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            message = "ভয়েস সার্ভিস ব্যস্ত";
            break;
          case SpeechRecognizer.ERROR_SERVER:
          case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
            message = "সার্ভার এরর, পুনরায় চেষ্টা করুন";
            break;
          case SpeechRecognizer.ERROR_CLIENT:
            message = null; // Client cancelled
            break;
          default:
            message = "ভয়েস ইনপুট শেষ (" + error + ")";
            break;
        }

        if (message != null)
        {
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }

        // If not already committed and partial text was received before error/timeout, commit once
        if (!_hasCommitted && !_lastPartialText.trim().isEmpty())
        {
          commitVoiceResult(keyboard, _lastPartialText);
        }

        if (keyboard != null && keyboard.getCandidatesView() != null)
        {
          keyboard.getCandidatesView().onVoiceListeningStopped();
        }
        destroyRecognizer();
      }

      @Override
      public void onResults(Bundle results)
      {
        _isListening = false;
        String recognizedText = null;
        if (results != null)
        {
          ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
          if (matches != null && !matches.isEmpty())
          {
            recognizedText = matches.get(0);
          }
        }
        if (recognizedText == null || recognizedText.trim().isEmpty())
        {
          recognizedText = _lastPartialText;
        }

        if (recognizedText != null && !recognizedText.trim().isEmpty())
        {
          commitVoiceResult(keyboard, recognizedText);
        }

        if (keyboard != null && keyboard.getCandidatesView() != null)
        {
          keyboard.getCandidatesView().onVoiceListeningStopped();
        }
        destroyRecognizer();
      }

      @Override
      public void onPartialResults(Bundle partialResults)
      {
        if (partialResults != null)
        {
          ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
          if (matches != null && !matches.isEmpty())
          {
            String text = matches.get(0);
            if (text != null && !text.trim().isEmpty())
            {
              _lastPartialText = text;
              if (keyboard != null && keyboard.getCandidatesView() != null)
              {
                keyboard.getCandidatesView().onVoicePartialResult(text);
              }
            }
          }
        }
      }

      @Override public void onEvent(int eventType, Bundle params) {}
    });

    String langCode = (keyboard != null) ? keyboard.getCurrentLanguageCode() : "bn-BD";
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode);
    if ("bn-BD".equals(langCode))
    {
      intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"bn-BD", "bn-IN", "en-US"});
    }
    else
    {
      intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"en-US", "bn-BD"});
    }
    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

    try
    {
      _speechRecognizer.startListening(intent);
    }
    catch (Exception e)
    {
      Toast.makeText(context, "ভয়েস সার্ভিস শুরু করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      destroyRecognizer();
      _isListening = false;
      if (keyboard != null && keyboard.getCandidatesView() != null)
      {
        keyboard.getCandidatesView().onVoiceListeningStopped();
      }
    }
  }

  public static void stopListening()
  {
    if (Looper.myLooper() != Looper.getMainLooper())
    {
      new Handler(Looper.getMainLooper()).post(new Runnable()
      {
        @Override
        public void run()
        {
          stopListening();
        }
      });
      return;
    }

    if (_speechRecognizer != null)
    {
      try
      {
        _speechRecognizer.stopListening();
      }
      catch (Exception ignored) {}
    }

    Keyboard2 kb = _activeKeyboard;
    _isListening = false;

    // If partial text exists when user manually clicks stop, commit it once if not yet committed
    if (!_hasCommitted && !_lastPartialText.trim().isEmpty() && kb != null)
    {
      commitVoiceResult(kb, _lastPartialText);
    }

    destroyRecognizer();

    if (kb != null && kb.getCandidatesView() != null)
    {
      kb.getCandidatesView().onVoiceListeningStopped();
    }
    _activeKeyboard = null;
  }

  private static void destroyRecognizer()
  {
    if (_speechRecognizer != null)
    {
      try
      {
        _speechRecognizer.cancel();
        _speechRecognizer.destroy();
      }
      catch (Exception ignored) {}
      _speechRecognizer = null;
    }
  }
}
