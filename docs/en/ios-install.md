# Installing on an iPhone (your own build with Xcode)

[Čeština](../ios-install.md) | **English**

Apple does not allow distributing a ready-made installer without a paid developer account, but anyone with a **Mac**
can build the app and put it on their own iPhone – a **free Apple ID** is enough.

> Note: the iOS project has not been tried on a real Mac yet. If something fails, send the error message from Xcode
> (Report navigator – the speech bubble icon on the left) and we will fix it.
>
> Alternative without a Mac: the **web version** in Safari (iOS 18.2+) → Share → *Add to Home Screen*.

## What you need

- A Mac with macOS 14 or newer
- **Xcode** (free from the App Store, ~10 GB)
- **Java 17+** – e.g. [Temurin 21](https://adoptium.net/) (`.pkg` installer), or `brew install --cask temurin@21`
- An iPhone with iOS 15+ and a USB cable
- An Apple ID (a regular free one is fine)

The Android SDK is **not** needed.

## Steps

1. **Download the project**
   ```bash
   git clone https://github.com/DejvHeHe/pathfinder-mobile-app.git
   cd pathfinder-mobile-app
   ```
2. **Add your Apple ID to Xcode:** Xcode → *Settings* → *Accounts* → **+** → Apple ID.
3. **Fill in the Team ID:** in Accounts click your account → the team *(Personal Team)* – copy its ID
   (10 characters, e.g. `AB12CD34EF`) into `iosApp/Configuration/Config.xcconfig`:
   ```
   TEAM_ID=AB12CD34EF
   ```
   This also gives the app its own bundle ID, so several people can build it at the same time.
4. **Open the project:** double-click `iosApp/iosApp.xcodeproj`.
   In *Signing & Capabilities* (target `iosApp`) your team should be selected – if not, choose it manually.
5. **Prepare the iPhone:**
   - connect it with the cable, confirm *Trust This Computer* on the phone,
   - turn on **Developer Mode**: *Settings → Privacy & Security → Developer Mode* → on → restart the phone
     (the option appears only after the first connection to Xcode).
6. **Run:** at the top of Xcode choose your iPhone as the target and press **▶**.
   The first build takes **10–20 minutes** (it downloads and compiles the Kotlin part of the app), later builds are fast.
7. **Trust the developer on the phone:** on first launch the iPhone reports *Untrusted Developer* →
   *Settings → General → VPN & Device Management* → your Apple ID → **Trust**.

## Free account limitations

- The signature is valid for **7 days** – then the app will not start. Just reconnect the phone and press ▶ in Xcode
  (your data – characters and inventories – stays unless you delete the app).
- A free account can have at most 3 of your own apps installed at once.
- To be safe, run *Export all* (menu ⋮) now and then – if the app gets deleted, the data can be imported back.

## Updating to a new version

```bash
git pull
```
and press **▶** in Xcode again.

## Troubleshooting

| Message | Fix |
|---|---|
| `Unable to locate a Java Runtime` / `JAVA_HOME is not set` | install Java 17+ (see above). Xcode does not see Terminal variables – if needed add `org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` to `~/.gradle/gradle.properties` |
| `Failed to register bundle identifier` / `… is not available` | fill in `TEAM_ID` in `Config.xcconfig` (step 3) |
| `No Account for Team` / `Signing requires a development team` | choose the team in *Signing & Capabilities* (step 4) |
| `Developer Mode disabled` | turn on Developer Mode (step 5) |
| `Untrusted Developer` | trust it in Settings (step 7) |
| an error in the *Compile Kotlin Framework* phase | send the whole log from the Report navigator to the author |
