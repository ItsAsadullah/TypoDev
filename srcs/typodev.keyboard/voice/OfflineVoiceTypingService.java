package typodev.keyboard.voice;

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
import android.view.inputmethod.InputConnection;
import java.util.ArrayList;
import typodev.keyboard.Keyboard2;

/**
 * Continuous high-performance voice typing service like Google Keyboard / Ridmik.
 * Features:
 * 1. Continuous listening that does NOT stop when user takes a breath or pauses.
 * 2. Real-time live composing text directly into the active editor field.
 * 3. Real-time audio RMS spectrum feedback for the microphone button.
 * 4. Inactivity watchdog that automatically finishes after 8 seconds of continuous silence.
 * 5. Manual toggle/stop support.
 */
public class OfflineVoiceTypingService
{
  private static SpeechRecognizer _speechRecognizer = null;
  private static boolean _isListening = false;
  private static String _lastPartialText = "";
  private static final StringBuilder _sessionCommittedText = new StringBuilder();
  private static Keyboard2 _activeKeyboard = null;
  private static Context _appContext = null;
  private static InputConnection _activeInputConnection = null;
  private static long _activeInputSession;
  private static long _recognizerGeneration;
  private static Runnable _restartRunnable;

  private static Handler _handler = null;
  private static final long INACTIVITY_TIMEOUT_MS = 8000; // 8 seconds of continuous silence

  private static final Runnable _inactivityRunnable = new Runnable()
  {
    @Override
    public void run()
    {
      if (_isListening)
      {
        stopListening();
      }
    }
  };

  private static Handler getHandler()
  {
    if (_handler == null)
    {
      try
      {
        Looper mainLooper = Looper.getMainLooper();
        if (mainLooper != null)
        {
          _handler = new Handler(mainLooper);
        }
      }
      catch (Throwable ignored) {}
    }
    return _handler;
  }

  private static boolean isMainLooper()
  {
    try
    {
      Looper mainLooper = Looper.getMainLooper();
      return mainLooper == null || Looper.myLooper() == mainLooper;
    }
    catch (Throwable ignored)
    {
      return true;
    }
  }

  private static void postToMain(Runnable r)
  {
    Handler h = getHandler();
    if (h != null)
    {
      h.post(r);
    }
    else
    {
      r.run();
    }
  }

  private static void postDelayedToMain(Runnable r, long delayMillis)
  {
    Handler h = getHandler();
    if (h != null)
    {
      h.postDelayed(r, delayMillis);
    }
  }

  private static void removeCallbacksFromMain(Runnable r)
  {
    Handler h = getHandler();
    if (h != null)
    {
      h.removeCallbacks(r);
    }
  }

  public static boolean isListening()
  {
    return _isListening;
  }

  private static void resetInactivityTimer()
  {
    removeCallbacksFromMain(_inactivityRunnable);
    if (_isListening)
    {
      postDelayedToMain(_inactivityRunnable, INACTIVITY_TIMEOUT_MS);
    }
  }

  public static void toggleListening(final Context context, final Keyboard2 keyboard)
  {
    if (!isMainLooper())
    {
      postToMain(new Runnable()
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
    if (!isMainLooper())
    {
      postToMain(new Runnable()
      {
        @Override
        public void run()
        {
          startListening(context, keyboard);
        }
      });
      return;
    }

    if (context == null || keyboard == null) return;
    cancelListening();
    _appContext = context.getApplicationContext();
    _activeKeyboard = keyboard;
    _activeInputConnection = keyboard.getCurrentInputConnection();
    _activeInputSession = keyboard.getInputSessionId();
    _lastPartialText = "";
    _sessionCommittedText.setLength(0);
    _isListening = true;

    // 1. Check microphone permission
    if (context != null && context.checkCallingOrSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
    {
      VoicePermissionActivity.requestVoicePermission(context, keyboard);
      _isListening = false;
      _activeKeyboard = null;
      _activeInputConnection = null;
      return;
    }

    // 2. Check SpeechRecognizer availability
    if (context != null && !SpeechRecognizer.isRecognitionAvailable(context))
    {
      Toast.makeText(context, "স্পিচ রিকগনিশন সার্ভিস পাওয়া যায়নি। Google Speech Services সক্রিয় করুন।", Toast.LENGTH_LONG).show();
      _isListening = false;
      _activeKeyboard = null;
      _activeInputConnection = null;
      if (keyboard != null && keyboard.getCandidatesView() != null)
      {
        keyboard.getCandidatesView().onVoiceListeningStopped();
      }
      return;
    }

    if (keyboard != null && keyboard.getCandidatesView() != null)
    {
      keyboard.getCandidatesView().onVoiceListeningStarted();
    }

    startRecognizerInternal(context);
    resetInactivityTimer();
  }

  private static Intent createSpeechIntent(Keyboard2 keyboard, Context context)
  {
    String langCode = (keyboard != null) ? keyboard.getCurrentLanguageCode() : "bn-BD";
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode);
    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    intent.putExtra("android.speech.extra.PARTIAL_RESULTS", true);
    intent.putExtra("android.speech.extra.DICTATION_MODE", true);
    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
    if (context != null)
    {
      intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
    }
    // Allow continuous speech flow without premature timeout on breathing pauses
    intent.putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 1500L);
    intent.putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 1000L);
    intent.putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 15000L);
    return intent;
  }

  private static String extractTextFromBundle(Bundle bundle)
  {
    if (bundle == null) return null;

    // 1. Standard RESULTS_RECOGNITION ArrayList<String>
    try
    {
      ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
      if (matches != null && !matches.isEmpty())
      {
        String first = matches.get(0);
        if (first != null && !first.trim().isEmpty())
        {
          return first;
        }
      }
    }
    catch (Throwable ignored) {}

    // 2. RESULTS_RECOGNITION CharSequenceArrayList
    try
    {
      ArrayList<CharSequence> cseqList = bundle.getCharSequenceArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
      if (cseqList != null && !cseqList.isEmpty())
      {
        CharSequence first = cseqList.get(0);
        if (first != null && !first.toString().trim().isEmpty())
        {
          return first.toString();
        }
      }
    }
    catch (Throwable ignored) {}

    // 3. RESULTS_RECOGNITION String[]
    try
    {
      String[] strArray = bundle.getStringArray(SpeechRecognizer.RESULTS_RECOGNITION);
      if (strArray != null && strArray.length > 0 && strArray[0] != null && !strArray[0].trim().isEmpty())
      {
        return strArray[0];
      }
    }
    catch (Throwable ignored) {}

    // 4. UNSTABLE_TEXT (real-time live streaming in Google Voice Typing dictation)
    try
    {
      ArrayList<String> unstable = bundle.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");
      if (unstable != null && !unstable.isEmpty())
      {
        String first = unstable.get(0);
        if (first != null && !first.trim().isEmpty())
        {
          return first;
        }
      }
      CharSequence cs = bundle.getCharSequence("android.speech.extra.UNSTABLE_TEXT");
      if (cs != null && !cs.toString().trim().isEmpty())
      {
        return cs.toString();
      }
    }
    catch (Throwable ignored) {}

    return null;
  }

  private static void startRecognizerInternal(final Context context)
  {
    if (!_isListening) return;

    destroyRecognizer();

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
      stopListening();
      return;
    }

    final long generation = _recognizerGeneration;
    _speechRecognizer.setRecognitionListener(new RecognitionListener()
    {
      private boolean isCurrent()
      {
        return generation == _recognizerGeneration && isCurrentInput();
      }

      @Override
      public void onReadyForSpeech(Bundle params)
      {
        // Readiness is not speech: repeated empty sessions must still time out.
      }

      @Override
      public void onBeginningOfSpeech()
      {
        if (!isCurrent()) return;
        resetInactivityTimer();
      }

      @Override
      public void onRmsChanged(float rmsdB)
      {
        if (!isCurrent()) return;
        if (_isListening && _activeKeyboard != null && _activeKeyboard.getCandidatesView() != null)
        {
          _activeKeyboard.getCandidatesView().onVoiceRmsChanged(rmsdB);
        }
        if (rmsdB > 2.0f)
        {
          resetInactivityTimer();
        }
      }

      @Override
      public void onBufferReceived(byte[] buffer) {}

      @Override
      public void onEndOfSpeech() {}

      @Override
      public void onPartialResults(Bundle partialResults)
      {
        if (!isCurrent()) return;
        if (partialResults != null && _isListening)
        {
          String text = extractTextFromBundle(partialResults);
          if (text != null && !text.trim().isEmpty())
          {
            resetInactivityTimer();
            String langCode = (_activeKeyboard != null) ? _activeKeyboard.getCurrentLanguageCode() : "bn-BD";
            String processed = VoicePunctuationHelper.applyVoiceCommands(_appContext, text, langCode);
            _lastPartialText = processed;
            // Real-time live update into the editor field as composing text
            if (_activeKeyboard != null)
            {
              _activeKeyboard.setVoiceComposingText(processed);
              // Real-time live visual candidate bar feedback
              if (_activeKeyboard.getCandidatesView() != null)
              {
                _activeKeyboard.getCandidatesView().onVoicePartialResult(processed);
              }
            }
          }
        }
      }

      @Override
      public void onResults(Bundle results)
      {
        if (!isCurrent()) return;

        String recognizedText = extractTextFromBundle(results);
        if (recognizedText == null || recognizedText.trim().isEmpty())
        {
          recognizedText = _lastPartialText;
        }

        if (recognizedText != null && !recognizedText.trim().isEmpty())
        {
          resetInactivityTimer();
          String langCode = (_activeKeyboard != null) ? _activeKeyboard.getCurrentLanguageCode() : "bn-BD";
          String processed = VoicePunctuationHelper.applyVoiceCommands(_appContext, recognizedText, langCode);
          if (_activeKeyboard != null)
          {
            _activeKeyboard.commitVoiceSegment(processed);
            if (_activeKeyboard.getCandidatesView() != null)
            {
              _activeKeyboard.getCandidatesView().onVoiceResult(processed);
            }
          }
          if (_sessionCommittedText.length() > 0)
          {
            _sessionCommittedText.append(" ");
          }
          _sessionCommittedText.append(processed);
          _lastPartialText = "";
        }

        // Fast ultra-low-latency restart so following speech is never dropped
        fastRestartListening();
      }

      @Override
      public void onError(int error)
      {
        if (!isCurrent()) return;

        switch (error)
        {
          case SpeechRecognizer.ERROR_NO_MATCH:
          case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            // Natural pause in speech: commit any existing partial text and resume listening fast
            if (!_lastPartialText.trim().isEmpty() && _activeKeyboard != null)
            {
              String langCode = _activeKeyboard.getCurrentLanguageCode();
              String processed = VoicePunctuationHelper.applyVoiceCommands(_appContext, _lastPartialText, langCode);
              _activeKeyboard.commitVoiceSegment(processed);
              if (_activeKeyboard.getCandidatesView() != null)
              {
                _activeKeyboard.getCandidatesView().onVoiceResult(processed);
              }
              if (_sessionCommittedText.length() > 0)
              {
                _sessionCommittedText.append(" ");
              }
              _sessionCommittedText.append(processed);
              _lastPartialText = "";
            }
            else if (_activeInputConnection != null)
            {
              try { _activeInputConnection.finishComposingText(); } catch (Throwable ignored) {}
            }
            fastRestartListening();
            break;

          case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            postDelayedToMain(new Runnable()
            {
              @Override
              public void run()
              {
                if (generation == _recognizerGeneration && isCurrentInput() && _isListening)
                {
                  fastRestartListening();
                }
              }
            }, 80);
            break;

          case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
            Toast.makeText(context, "মাইক্রোফোন অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show();
            VoicePermissionActivity.requestVoicePermission(context, _activeKeyboard);
            stopListening();
            break;

          case SpeechRecognizer.ERROR_NETWORK:
          case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            if (!isNetworkConnected(context))
            {
              Toast.makeText(context, "ভয়েস টাইপিংয়ের জন্য ইন্টারনেট সংযোগ প্রয়োজন", Toast.LENGTH_SHORT).show();
              stopListening();
            }
            else
            {
              fastRestartListening();
            }
            break;

          case SpeechRecognizer.ERROR_CLIENT:
            // Client side cancel; ignore if still listening or stopped
            break;

          default:
            fastRestartListening();
            break;
        }
      }

      @Override
      public void onEvent(int eventType, Bundle params) {}
    });

    try
    {
      Intent intent = createSpeechIntent(_activeKeyboard, context);
      _speechRecognizer.startListening(intent);
    }
    catch (Exception e)
    {
      destroyRecognizer();
      fastRestartListening();
    }
  }

  private static void fastRestartListening()
  {
    if (!_isListening) return;
    if (_restartRunnable != null)
    {
      removeCallbacksFromMain(_restartRunnable);
      _restartRunnable = null;
    }

    final long generation = _recognizerGeneration;
    _restartRunnable = new Runnable()
    {
      @Override
      public void run()
      {
        _restartRunnable = null;
        if (generation != _recognizerGeneration || !isCurrentInput() || !_isListening) return;

        if (_speechRecognizer != null && _activeKeyboard != null && _appContext != null)
        {
          try
          {
            _speechRecognizer.cancel();
            Intent intent = createSpeechIntent(_activeKeyboard, _appContext);
            _speechRecognizer.startListening(intent);
            return;
          }
          catch (Throwable t)
          {
            // Binder might be dead or in invalid state, fallback to clean recreation
          }
        }

        if (_appContext != null)
        {
          startRecognizerInternal(_appContext);
        }
      }
    };
    // 35ms breather allows audio buffer recycling without missing user's subsequent words
    postDelayedToMain(_restartRunnable, 35);
  }

  private static boolean isCurrentInput()
  {
    return _isListening && _activeKeyboard != null
        && _activeInputSession == _activeKeyboard.getInputSessionId()
        && _activeInputConnection == _activeKeyboard.getCurrentInputConnection();
  }

  /** End an editor lifecycle without inserting unfinished speech into a new field. */
  public static void cancelListening()
  {
    if (!isMainLooper())
    {
      postToMain(new Runnable() { @Override public void run() { cancelListening(); } });
      return;
    }
    VoicePermissionActivity.cancelPendingRequest();
    _lastPartialText = "";
    _sessionCommittedText.setLength(0);
    stopListening();
  }

  public static void stopListening()
  {
    if (!isMainLooper())
    {
      postToMain(new Runnable()
      {
        @Override
        public void run()
        {
          stopListening();
        }
      });
      return;
    }

    boolean currentInput = isCurrentInput();
    _isListening = false;
    removeCallbacksFromMain(_inactivityRunnable);
    if (_restartRunnable != null)
    {
      removeCallbacksFromMain(_restartRunnable);
      _restartRunnable = null;
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
    Context ctx = _appContext;

    // Commit any uncommitted partial segment
    if (currentInput && !_lastPartialText.trim().isEmpty() && kb != null)
    {
      String langCode = kb.getCurrentLanguageCode();
      String processed = VoicePunctuationHelper.applyVoiceCommands(ctx, _lastPartialText, langCode);
      kb.commitVoiceSegment(processed);
      if (_sessionCommittedText.length() > 0)
      {
        _sessionCommittedText.append(" ");
      }
      _sessionCommittedText.append(processed);
      _lastPartialText = "";
    }

    destroyRecognizer();

    if (currentInput && kb != null)
    {
      kb.finishVoiceTyping();
    }

    final String sessionText = _sessionCommittedText.toString().trim();
    _sessionCommittedText.setLength(0);

    if (currentInput && !sessionText.isEmpty() && kb != null && ctx != null)
    {
      final String langCode = kb.getCurrentLanguageCode();
      if (VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(ctx))
      {
        if (VoicePunctuationHelper.isVoiceAiPunctuationEnabled(ctx))
        {
          // Tier 1: Instant rule-based sentence ending & spacing formatting
          String instant = VoicePunctuationHelper.applyInstantFormatting(sessionText, langCode);
          if (!instant.equals(sessionText))
          {
            kb.replaceVoiceSessionText(sessionText, instant);
          }

          // Tier 2: AI cloud auto-punctuation & polishing
          VoicePunctuationHelper.processAiPunctuation(ctx, kb, instant, langCode);
        }
        else
        {
          String instant = VoicePunctuationHelper.applyInstantFormatting(sessionText, langCode);
          if (!instant.equals(sessionText))
          {
            kb.replaceVoiceSessionText(sessionText, instant);
          }
        }
      }
    }

    _activeKeyboard = null;
    _activeInputConnection = null;
    _appContext = null;
    _lastPartialText = "";
  }

  private static void destroyRecognizer()
  {
    ++_recognizerGeneration;
    SpeechRecognizer recognizer = _speechRecognizer;
    _speechRecognizer = null;
    if (recognizer != null)
    {
      try
      {
        recognizer.cancel();
      }
      catch (Exception ignored) {}
      try { recognizer.destroy(); }
      catch (Exception ignored) {}
    }
  }
}
