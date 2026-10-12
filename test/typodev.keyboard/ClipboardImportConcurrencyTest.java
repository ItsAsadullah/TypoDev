package typodev.keyboard;

import android.app.Activity;
import android.net.Uri;
import android.os.Looper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import typodev.keyboard.clipboard.ClipboardImageStore;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ClipboardImportConcurrencyTest {
  @Test public void duplicateImportWaitsForRealResultRatherThanReportingEarlySuccess() throws Exception {
    Field singleton = ClipboardImageStore.class.getDeclaredField("instance"); singleton.setAccessible(true); singleton.set(null, null);
    Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
    ClipboardImageStore store = ClipboardImageStore.get(activity);
    Field field = ClipboardImageStore.class.getDeclaredField("worker"); field.setAccessible(true);
    ExecutorService worker = (ExecutorService)field.get(store);
    CountDownLatch release = new CountDownLatch(1);
    worker.execute(() -> { try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
    List<Boolean> results = new ArrayList<>();
    try {
      Uri missing = Uri.parse("content://missing/image");
      store.importImage(missing, results::add); store.importImage(missing, results::add);
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      assertTrue("An in-progress import has not succeeded yet", results.isEmpty());
      release.countDown(); worker.submit(() -> {}).get(5, TimeUnit.SECONDS);
      Shadows.shadowOf(Looper.getMainLooper()).idle();
      assertEquals(Arrays.asList(false, false), results);
    } finally { release.countDown(); worker.shutdownNow(); singleton.set(null, null); }
  }
}
