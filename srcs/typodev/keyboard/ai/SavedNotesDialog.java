package typodev.keyboard.ai;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.R;
import typodev.keyboard.Utils;

public final class SavedNotesDialog {
  private SavedNotesDialog() {}
  public static void show(Context context, Keyboard2 keyboard) {
    SavedNoteStore store = new SavedNoteStore(context);
    try {
      List<SavedNoteStore.Note> notes = store.list();
      AlertDialog.Builder dialog = new AlertDialog.Builder(context).setTitle(R.string.slash_saved_notes)
          .setNegativeButton(android.R.string.cancel, null);
      if (notes.isEmpty()) dialog.setMessage(R.string.slash_no_notes);
      else {
        String[] labels = new String[notes.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = notes.get(i).text;
        dialog.setItems(labels, (ignored, index) -> showNote(context, keyboard, store, notes.get(index)))
            .setNeutralButton(R.string.slash_delete_all, (ignored, which) -> {
              try { store.clear(); show(context, keyboard); }
              catch (RuntimeException error) { fail(context); }
            });
      }
      Utils.show_dialog_on_ime(dialog.create(), keyboard);
    } catch (RuntimeException error) { fail(context); }
  }
  private static void showNote(Context context, Keyboard2 keyboard, SavedNoteStore store, SavedNoteStore.Note note) {
    AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.slash_saved_notes)
        .setMessage(note.text + "\n\n" + DateFormat.getDateTimeInstance().format(new Date(note.savedAt)))
        .setPositiveButton(R.string.slash_copy_note, (ignored, which) -> {
          ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
          if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Saved note", note.text));
        })
        .setNeutralButton(R.string.slash_delete_note, (ignored, which) -> {
          try { store.delete(note.id); show(context, keyboard); }
          catch (RuntimeException error) { fail(context); }
        }).setNegativeButton(android.R.string.cancel, null).create();
    Utils.show_dialog_on_ime(dialog, keyboard);
  }
  private static void fail(Context context) { Toast.makeText(context, R.string.slash_note_error, Toast.LENGTH_LONG).show(); }
}
