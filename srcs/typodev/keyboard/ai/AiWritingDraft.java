package typodev.keyboard.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One in-memory draft. Never retains an editor connection or writes user text to disk. */
public final class AiWritingDraft {
  public final String source, tone, before, after, selection, editorPackage;
  public final int fieldId, index;
  public final AiWritingResultsView.Task task;
  public final SlashCommand.Input command;
  public final List<String> results;
  private final byte[] image;

  public AiWritingDraft(String source, byte[] image, String tone, AiWritingResultsView.Task task,
      List<String> results, int index, String before, String after, String selection,
      String editorPackage, int fieldId) {
    this(source, image, tone, task, results, index, before, after, selection, editorPackage, fieldId, null);
  }

  public AiWritingDraft(String source, byte[] image, String tone, AiWritingResultsView.Task task,
      List<String> results, int index, String before, String after, String selection,
      String editorPackage, int fieldId, SlashCommand.Input command) {
    this.command = command;
    this.source = source;
    this.image = image == null ? null : image.clone();
    this.tone = tone;
    this.task = task;
    this.results = Collections.unmodifiableList(new ArrayList<>(results));
    this.index = Math.max(0, Math.min(index, results.size() - 1));
    this.before = before; this.after = after; this.selection = selection;
    this.editorPackage = editorPackage; this.fieldId = fieldId;
  }

  public byte[] getImage() { return image == null ? null : image.clone(); }

  public boolean matchesEditor(String packageName, int id, String preceding,
      String following, String selected) {
    return editorPackage != null && editorPackage.equals(packageName) && fieldId == id
        && before.equals(preceding) && after.equals(following) && selection.equals(selected);
  }
}
