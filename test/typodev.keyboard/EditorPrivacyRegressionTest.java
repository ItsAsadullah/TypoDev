package typodev.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import typodev.keyboard.suggestions.Suggestions;
import typodev.keyboard.suggestions.UserVocabularyStore;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EditorPrivacyRegressionTest {
  @Test public void directBootMirrorOmitsApiKeysAndRemovesOldCopies() {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    SharedPreferences source = PreferenceManager.getDefaultSharedPreferences(activity);
    SharedPreferences target = DirectBootAwarePreferences.get_protected_prefs(activity);
    source.edit().putString("gemini_api_key", "private-gemini").putString("openai_api_key", "private-openai")
        .putBoolean("vibrate_enabled", true).commit();
    target.edit().putString("gemini_api_key", "old-copy").commit();
    DirectBootAwarePreferences.copy_preferences_to_protected_storage(activity, source);
    assertFalse(target.contains("gemini_api_key")); assertFalse(target.contains("openai_api_key"));
    assertTrue(target.getBoolean("vibrate_enabled", false));
    assertEquals("private-gemini", source.getString("gemini_api_key", ""));
    target.edit().putString("openai_api_key", "legacy-copy").commit();
    DirectBootAwarePreferences.get_shared_preferences(activity);
    assertFalse(target.contains("openai_api_key"));
  }
  private void typeSpace(boolean password, boolean personalized) throws Exception {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    Field instance = UserVocabularyStore.class.getDeclaredField("_instance"); instance.setAccessible(true); instance.set(null, null);
    UserVocabularyStore vocabulary = UserVocabularyStore.instance(activity); vocabulary.clearLearnedData();
    StringBuilder text = new StringBuilder("secretword");
    InputConnection editor = (InputConnection)Proxy.newProxyInstance(InputConnection.class.getClassLoader(),
        new Class<?>[]{InputConnection.class}, (proxy, method, args) -> {
          if (method.getName().equals("getTextBeforeCursor")) return text.toString();
          if (method.getName().equals("getTextAfterCursor")) return "";
          if (method.getName().equals("commitText")) text.append(args[0]);
          if (method.getReturnType() == boolean.class) return true;
          if (method.getReturnType() == int.class) return 0;
          return null;
        });
    KeyEventHandler.IReceiver receiver = (KeyEventHandler.IReceiver)Proxy.newProxyInstance(KeyEventHandler.IReceiver.class.getClassLoader(),
        new Class<?>[]{KeyEventHandler.IReceiver.class}, (proxy, method, args) -> {
          if (method.getName().equals("getCurrentInputConnection")) return editor;
          if (method.getName().equals("getTextBeforeCursor")) return text.toString();
          if (method.getName().equals("getHandler")) return new Handler(Looper.getMainLooper());
          if (method.getReturnType() == boolean.class) return false;
          return null;
        });
    typodev.keyboard.Config config = new typodev.keyboard.Config();
    config.editor_config.is_password = password;
    config.editor_config.allow_personalized_learning = personalized;
    config.editor_config.initial_text_before_cursor = "secretword";
    config.editor_config.initial_sel_start = config.editor_config.initial_sel_end = 10;
    Suggestions suggestions = new Suggestions(receiver, config, activity);
    KeyEventHandler handler = new KeyEventHandler(receiver, suggestions); handler.started(config);
    handler.handle_space_bar();
    assertEquals("secretword ", text.toString());
    assertEquals(!password && personalized, vocabulary.containsWord("secretword"));
    handler.finished(); instance.set(null, null);
  }
  @Test public void passwordFieldDoesNotLearnCompletedWords() throws Exception { typeSpace(true, true); }
  @Test public void noPersonalizedLearningFieldDoesNotLearnCompletedWords() throws Exception { typeSpace(false, false); }
  @Test public void ordinaryEditorStillLearnsCompletedWords() throws Exception { typeSpace(false, true); }
  @Test public void editorPrivacyFlagIsRefreshedForEachField() {
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    EditorInfo editor = new EditorInfo(); editor.inputType = InputType.TYPE_CLASS_TEXT;
    editor.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
    EditorConfig config = new EditorConfig(); config.refresh(editor, activity.getResources());
    assertFalse(config.allow_personalized_learning);
    editor.imeOptions = 0; config.refresh(editor, activity.getResources());
    assertTrue(config.allow_personalized_learning);
  }
  @Test public void privatePredictionPathsDoNotLearnSentenceOrFallbackTransitions() throws Exception {
    Field instance = typodev.keyboard.suggestions.NextWordPredictor.class.getDeclaredField("_instance");
    instance.setAccessible(true); Object previous = instance.get(null); instance.set(null, null);
    try {
      typodev.keyboard.Config config = new typodev.keyboard.Config();
      config.editor_config.should_show_candidates_view = true;
      config.editor_config.allow_personalized_learning = false;
      Suggestions.Callback callback = new Suggestions.Callback() {
        @Override public void set_suggestions(Suggestions suggestions) {}
        @Override public CharSequence getTextBeforeCursor(int count, int flags) { return null; }
        @Override public boolean isSelectionActive() { return false; }
      };
      Suggestions suggestions = new Suggestions(callback, config, null); suggestions.started();
      suggestions.predict_sentence_completion("privatephrase secretword ");
      suggestions.set_last_word("privatephrase"); suggestions.predict_next_words("secretword");
      Object predictor = typodev.keyboard.suggestions.NextWordPredictor.instance(null);
      Field learned = predictor.getClass().getDeclaredField("_learnedBigrams"); learned.setAccessible(true);
      assertTrue(((java.util.Map<?, ?>)learned.get(predictor)).isEmpty());
    } finally { instance.set(null, previous); }
  }
}
