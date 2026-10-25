package typodev.keyboard;

import android.content.res.Configuration;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LauncherActivityTest
{
  @Test
  public void testLauncherActivityLaunchesAndRenders()
  {
    ActivityController<LauncherActivity> controller = Robolectric.buildActivity(LauncherActivity.class);
    LauncherActivity activity = controller.create().start().resume().visible().get();
    assertNotNull(activity);

    View tryHereArea = activity.findViewById(R.id.launcher_tryhere_area);
    assertNotNull("launcher_tryhere_area should be inflated and not null", tryHereArea);

    View btnSettings = activity.findViewById(R.id.btn_launcher_settings);
    assertNotNull("btn_launcher_settings should exist", btnSettings);

    View cardTheme = activity.findViewById(R.id.card_launcher_theme);
    assertNotNull("card_launcher_theme should exist", cardTheme);

    View cardAi = activity.findViewById(R.id.card_launcher_ai);
    assertNotNull("card_launcher_ai should exist", cardAi);
  }

  @Test
  @Config(qualifiers = "night")
  public void testLauncherActivityInNightMode()
  {
    ActivityController<LauncherActivity> controller = Robolectric.buildActivity(LauncherActivity.class);
    LauncherActivity activity = controller.create().start().resume().visible().get();
    assertNotNull(activity);

    View tryHereArea = activity.findViewById(R.id.launcher_tryhere_area);
    assertNotNull("launcher_tryhere_area should be inflated in night mode", tryHereArea);
  }
}
