package typodev.keyboard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SoundFeedbackManager
{
  private static volatile SoundFeedbackManager _instance;

  private final Context _context;
  private final AudioManager _audioManager;
  private SoundPool _soundPool;
  private final Set<Integer> _readySoundIds = Collections.synchronizedSet(new HashSet<Integer>());

  public static class SoundPack
  {
    public final int standard;
    public final int spacebar;
    public final int delete;
    public final int ret;

    public SoundPack(int standard, int spacebar, int delete, int ret)
    {
      this.standard = standard;
      this.spacebar = spacebar;
      this.delete = delete;
      this.ret = ret;
    }
  }

  private final Map<String, SoundPack> _loadedPacks = new HashMap<>();

  public static SoundFeedbackManager getInstance(Context context)
  {
    if (_instance == null)
    {
      synchronized (SoundFeedbackManager.class)
      {
        if (_instance == null)
        {
          _instance = new SoundFeedbackManager(context.getApplicationContext());
        }
      }
    }
    return _instance;
  }

  private SoundFeedbackManager(Context context)
  {
    _context = context;
    _audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    // Preload default sound pack in the background so it is ready immediately
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          getOrLoadPack("default");
        }
        catch (Throwable ignored) {}
      }
    }).start();
  }

  private synchronized void ensureSoundPool()
  {
    if (_soundPool != null)
      return;

    try
    {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build();

        _soundPool = new SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(audioAttributes)
            .build();
      }
      else
      {
        _soundPool = new SoundPool(16, AudioManager.STREAM_SYSTEM, 0);
      }

      _soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener()
      {
        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
        {
          if (status == 0)
          {
            _readySoundIds.add(sampleId);
          }
        }
      });
    }
    catch (Throwable ignored)
    {
      _soundPool = null;
    }
  }

  private synchronized SoundPack getOrLoadPack(String packName)
  {
    ensureSoundPool();
    if (_soundPool == null) return null;

    String key = (packName != null) ? packName.toLowerCase().trim() : "default";
    if (key.isEmpty() || "system".equals(key))
    {
      key = "default";
    }

    if (_loadedPacks.containsKey(key))
    {
      return _loadedPacks.get(key);
    }

    int standard = loadAssetSound("sounds/" + key + "/KeypressStandard.ogg");
    int spacebar = loadAssetSound("sounds/" + key + "/KeypressSpacebar.ogg");
    int del = loadAssetSound("sounds/" + key + "/KeypressDelete.ogg");
    int ret = loadAssetSound("sounds/" + key + "/KeypressReturn.ogg");

    // If loading failed (e.g. unknown pack), fallback to default if not already default
    if (standard == 0 && !"default".equals(key))
    {
      return getOrLoadPack("default");
    }

    SoundPack pack = new SoundPack(standard, spacebar, del, ret);
    _loadedPacks.put(key, pack);
    return pack;
  }

  private int loadAssetSound(String path)
  {
    if (_soundPool == null) return 0;

    // 1. Try extracting asset to a cache file so SoundPool reads from a direct file path without compression or FD closure issues
    try
    {
      File cacheDir = new File(_context.getCacheDir(), "sounds");
      if (!cacheDir.exists()) cacheDir.mkdirs();
      File soundFile = new File(cacheDir, path.replace('/', '_'));
      if (!soundFile.exists() || soundFile.length() == 0)
      {
        InputStream in = _context.getAssets().open(path);
        FileOutputStream out = new FileOutputStream(soundFile);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
          out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
      }
      if (soundFile.exists() && soundFile.length() > 0)
      {
        return _soundPool.load(soundFile.getAbsolutePath(), 1);
      }
    }
    catch (Throwable ignored) {}

    // 2. Direct openFd fallback
    try
    {
      AssetFileDescriptor afd = _context.getAssets().openFd(path);
      return _soundPool.load(afd, 1);
    }
    catch (Throwable ignored) {}

    return 0;
  }

  public enum KeySoundType
  {
    STANDARD,
    SPACEBAR,
    DELETE,
    RETURN
  }

  public static KeySoundType getKeySoundType(KeyValue kv)
  {
    if (kv == null) return KeySoundType.STANDARD;

    if (kv.getKind() == KeyValue.Kind.Editing)
    {
      KeyValue.Editing editing = kv.getEditing();
      if (editing == KeyValue.Editing.SPACE_BAR)
      {
        return KeySoundType.SPACEBAR;
      }
      else if (editing == KeyValue.Editing.BACKSPACE ||
               editing == KeyValue.Editing.DELETE_WORD ||
               editing == KeyValue.Editing.FORWARD_DELETE_WORD)
      {
        return KeySoundType.DELETE;
      }
    }
    else if (kv.getKind() == KeyValue.Kind.Keyevent)
    {
      int code = kv.getKeyevent();
      if (code == KeyEvent.KEYCODE_ENTER)
      {
        return KeySoundType.RETURN;
      }
      else if (code == KeyEvent.KEYCODE_DEL || code == KeyEvent.KEYCODE_FORWARD_DEL)
      {
        return KeySoundType.DELETE;
      }
    }

    // Check enter key
    if (kv.equals(KeyValue.ENTER))
    {
      return KeySoundType.RETURN;
    }

    return KeySoundType.STANDARD;
  }

  public void play(KeyValue kv, Config config)
  {
    Config globalCfg = Config.globalConfig();
    if (globalCfg != null)
    {
      config = globalCfg;
    }
    if (config == null || !config.sound_on_keypress)
      return;

    KeySoundType type = getKeySoundType(kv);
    float volume = (config.sound_volume > 0) ? Math.max(0.1f, Math.min(1.0f, config.sound_volume / 100.0f)) : 0.6f;

    String effectType = (config.sound_effect_type != null) ? config.sound_effect_type.trim().toLowerCase() : "default";

    if ("system".equals(effectType))
    {
      playSystemSound(type, volume);
      return;
    }

    playPackSound(effectType, type, volume);
  }

  private void playSystemSound(KeySoundType type, float volume)
  {
    if (_audioManager == null) return;
    try
    {
      int fx;
      switch (type)
      {
        case SPACEBAR:
          fx = AudioManager.FX_KEYPRESS_SPACEBAR;
          break;
        case DELETE:
          fx = AudioManager.FX_KEYPRESS_DELETE;
          break;
        case RETURN:
          fx = AudioManager.FX_KEYPRESS_RETURN;
          break;
        case STANDARD:
        default:
          fx = AudioManager.FX_KEYPRESS_STANDARD;
          break;
      }
      _audioManager.playSoundEffect(fx, volume);
    }
    catch (Throwable ignored) {}
  }

  private void playPackSound(String packName, KeySoundType type, float volume)
  {
    SoundPack pack = getOrLoadPack(packName);
    int soundId = 0;
    if (pack != null)
    {
      switch (type)
      {
        case SPACEBAR:
          soundId = (pack.spacebar != 0) ? pack.spacebar : pack.standard;
          break;
        case DELETE:
          soundId = (pack.delete != 0) ? pack.delete : pack.standard;
          break;
        case RETURN:
          soundId = (pack.ret != 0) ? pack.ret : pack.standard;
          break;
        case STANDARD:
        default:
          soundId = pack.standard;
          break;
      }
    }

    int streamId = 0;
    if (soundId != 0 && _soundPool != null)
    {
      try
      {
        streamId = _soundPool.play(soundId, volume, volume, 1, 0, 1.0f);
      }
      catch (Throwable ignored) {}
    }

    // If SoundPool playback did not produce a stream (e.g. still loading or stream issue),
    // immediately fall back to system audio click so sound ALWAYS works!
    if (streamId == 0)
    {
      playSystemSound(type, volume);
    }
  }

  public synchronized void release()
  {
    if (_soundPool != null)
    {
      try
      {
        _soundPool.release();
      }
      catch (Throwable ignored) {}
      _soundPool = null;
      _loadedPacks.clear();
      _readySoundIds.clear();
    }
  }
}
