# Building and debugging TypoDev

## Building the app

The application uses Gradle and can be used with Android Studio, but using
Android Studio is not required. The build dependencies are:
- OpenJDK 17
- Android SDK: build tools, platform `35`

Python 3 is required to update generated files but not to build the app.

Make sure the Git submodules are initialized and point to the right revision:

```sh
git submodule update --init
```

For Android Studio users, no more setup is needed.

For Nix users, the right environment can be obtained with `nix-shell ./shell.nix`.
Instructions to install Nix are [here](https://wiki.nixos.org/wiki/Nix_Installation_Guide).

If you don't use Android Studio or Nix, you have to inform Gradle about the
location of your Android SDK by either:
- Setting the `ANDROID_HOME` environment variable to point to the android sdk or
- Creating the file `local.properties` and writing
  `sdk.dir=<location_of_android_home>` into it.

Building the debug apk:

```sh
./gradlew assembleDebug
```

Nix users can call gradle directly: `gradle assembleDebug`.

If the build succeeds, the debug apk is located in `build/outputs/apk/debug/app-debug.apk`.

To build a smaller APK that updates an existing debug installation, run
`./gradlew assembleOptimizedDebug`. This variant enables release minification
and resource shrinking, keeps the `typodev.keyboard.debug` package, and uses
the debug signing configuration. Set `DEBUG_KEYSTORE` to the same keystore
used for the installed APK; a different signing key cannot update that app.
The APK is written to `build/outputs/apk/optimizedDebug/`.

The normal release build (`./gradlew assembleRelease`) uses the production
package identity `typodev.keyboard`. Configure `RELEASE_KEYSTORE`,
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`
with the signing key used for the installed APK before building an update.

## Debugging on your phone

First [Enable adb debugging on your device](https://developer.android.com/studio/command-line/adb#Enabling).
Then connect your phone to your computer using an USB cable or via wireless
debugging.

If you use Android Studio, this process will be automatic and you don't have to
follow this guide anymore.

And finally, install the application with:
```sh
./gradlew installDebug
```

Debug builds use `typodev.keyboard.debug` and release builds use
`typodev.keyboard`, so they can be installed alongside each other. An update
must use the same package name and signing key as the installed app.

## Debugging the application: INSTALL_FAILED_UPDATE_INCOMPATIBLE

`./gradlew installDebug` can fail with the following error message:

```
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':installDebug'.
> java.util.concurrent.ExecutionException: com.android.builder.testing.api.DeviceException: com.android.ddmlib.InstallException: INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package typodev.keyboard.debug signatures do not match newer version; ignoring!
```

The application can't be "updated" because the temporary certificate has been
lost. The solution is to uninstall and install again.
The application must be enabled again in the settings.

```sh
adb uninstall typodev.keyboard.debug
./gradlew installDebug
```

## Specifying a debug signing certificate on Github Actions

It's possible to specify the signing certificate that the automated build
should use.
After you successfully run `./gradlew asssembleDebug`, (thus a debug.keystore
exists) you can use this second command to generate a base64 stringified
version of it:

```sh
gpg -c --armor --pinentry-mode loopback --passphrase debug0 --yes "debug.keystore"
```

This will create the file `debug.keystore.asc`, paste its content into a new
Github secret named `DEBUG_KEYSTORE`.
