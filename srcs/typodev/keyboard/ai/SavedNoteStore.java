package typodev.keyboard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Only explicit /remember actions write here. Never sent to an AI provider. */
public final class SavedNoteStore {
  public static final int MAX_NOTES = 50;
  private final SharedPreferences prefs;
  public SavedNoteStore(Context context) { this(context.getSharedPreferences("slash_saved_notes", Context.MODE_PRIVATE)); }
  public SavedNoteStore(SharedPreferences prefs) { this.prefs = prefs; }
  public static final class Note {
    public final String id, text;
    public final long savedAt;
    Note(String id, String text, long savedAt) { this.id = id; this.text = text; this.savedAt = savedAt; }
  }
  public List<Note> list() {
    try {
      JSONArray data = new JSONArray(prefs.getString("notes", "[]"));
      List<Note> notes = new ArrayList<>();
      for (int i = 0; i < data.length(); i++) {
        JSONObject item = data.getJSONObject(i);
        notes.add(new Note(item.getString("id"), item.getString("text"), item.getLong("savedAt")));
      }
      return notes;
    } catch (JSONException invalid) { throw new IllegalStateException("Saved notes could not be read.", invalid); }
  }
  public void save(String text) {
    String clean = text == null ? "" : text.trim();
    if (clean.isEmpty() || clean.length() > SlashCommand.MAX_CONTEXT) throw new IllegalArgumentException("Invalid note.");
    List<Note> notes = list();
    String id = UUID.randomUUID().toString();
    for (int i = notes.size() - 1; i >= 0; i--) {
      if (notes.get(i).text.equals(clean)) { id = notes.remove(i).id; }
    }
    if (notes.size() >= MAX_NOTES) throw new IllegalStateException("Note limit reached. Delete a note first.");
    notes.add(0, new Note(id, clean, System.currentTimeMillis()));
    write(notes);
  }
  public void delete(String id) {
    List<Note> notes = list();
    for (int i = notes.size() - 1; i >= 0; i--) if (notes.get(i).id.equals(id)) notes.remove(i);
    write(notes);
  }
  public void clear() { write(new ArrayList<>()); }
  private void write(List<Note> notes) {
    try {
      JSONArray data = new JSONArray();
      for (Note note : notes) data.put(new JSONObject().put("id", note.id).put("text", note.text).put("savedAt", note.savedAt));
      if (!prefs.edit().putString("notes", data.toString()).commit()) throw new IllegalStateException("Unable to save notes.");
    } catch (JSONException invalid) { throw new IllegalStateException("Unable to save notes.", invalid); }
  }
}
