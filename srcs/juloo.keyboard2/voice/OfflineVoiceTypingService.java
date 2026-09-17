package juloo.keyboard2.voice;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import java.util.ArrayList;
import juloo.keyboard2.Keyboard2;

public class OfflineVoiceTypingService
{
  private static SpeechRecognizer _speechRecognizer = null;
  private static boolean _isListening = false;

  public static boolean isListening()
  {
    return _isListening;
  }

  public static void toggleListening(final Context context, final Keyboard2 keyboard)
  {
    if (_isListening)
    {
      stopListening();
    }
    else
    {
      startListening(context, keyboard);
    }
  }

  public static void startListening(final Context context, final Keyboard2 keyboard)
  {
    if (context.checkCallingOrSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
    {
      Toast.makeText(context, "Microphone permission required for voice typing", Toast.LENGTH_SHORT).show();
      return;
    }

    if (!SpeechRecognizer.isRecognitionAvailable(context))
    {
      Toast.makeText(context, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
      return;
    }

    stopListening();

    try
    {
      if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
      {
        _speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
      }
      else
      {
        _speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
      }
    }
    catch (Exception e)
    {
      _speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
    }

    if (_speechRecognizer == null)
    {
      Toast.makeText(context, "Failed to initialize SpeechRecognizer", Toast.LENGTH_SHORT).show();
      return;
    }

    _speechRecognizer.setRecognitionListener(new RecognitionListener()
    {
      @Override public void onReadyForSpeech(Bundle params)
      {
        _isListening = true;
        Toast.makeText(context, "🎙️ Listening (Offline)...", Toast.LENGTH_SHORT).show();
      }

      @Override public void onBeginningOfSpeech() {}
      @Override public void onRmsChanged(float rmsdB) {}
      @Override public void onBufferReceived(byte[] buffer) {}
      @Override public void onEndOfSpeech()
      {
        _isListening = false;
      }

      @Override public void onError(int error)
      {
        _isListening = false;
        String message;
        switch (error)
        {
          case SpeechRecognizer.ERROR_AUDIO: message = "Audio recording error"; break;
          case SpeechRecognizer.ERROR_NO_MATCH: message = "No speech detected"; break;
          case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "Speech timeout"; break;
          default: message = "Voice input ended (" + error + ")"; break;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
      }

      @Override public void onResults(Bundle results)
      {
        _isListening = false;
        if (results != null)
        {
          ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
          if (matches != null && !matches.isEmpty())
          {
            String recognizedText = matches.get(0);
            InputConnection ic = keyboard.getCurrentInputConnection();
            if (ic != null && !recognizedText.isEmpty())
            {
              ic.commitText(recognizedText + " ", 1);
            }
          }
        }
      }

      @Override public void onPartialResults(Bundle partialResults) {}
      @Override public void onEvent(int eventType, Bundle params) {}
    });

    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
    // Request on-device offline recognition
    intent.putExtra("android.speech.extra.PREFER_OFFLINE", true);
    if (Build.VERSION.SDK_INT >= 23)
    {
      intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
    }

    _speechRecognizer.startListening(intent);
  }

  public static void stopListening()
  {
    if (_speechRecognizer != null)
    {
      try
      {
        _speechRecognizer.stopListening();
        _speechRecognizer.cancel();
        _speechRecognizer.destroy();
      }
      catch (Exception ignored) {}
      _speechRecognizer = null;
    }
    _isListening = false;
  }
}
