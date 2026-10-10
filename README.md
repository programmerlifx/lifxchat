# LIFX Chat — Android + Firebase

A dark, green-accented Android private-chat starter built with Kotlin, Jetpack Compose, Firebase Authentication, and Cloud Firestore snapshot listeners.

## Included
- Email/password registration and sign-in
- User directory
- Private 1-to-1 text messages
- Real-time message updates through Firestore listeners
- Dark modern UI
- Firestore rules starter
- Android Studio Gradle project

## Important
This is a functional starter, not a production-ready WhatsApp clone. It does not yet include voice/video calls, push notifications, end-to-end encryption, media uploads, group chats, message read receipts, or account recovery. Do not advertise it as end-to-end encrypted. The provided starter Firestore message rule allows any signed-in account to read messages; restrict chat membership before deploying for real users.

## 1. Create Firebase project
1. Open https://console.firebase.google.com/ and create a project named `LIFX Chat`.
2. Add an Android app with package name exactly `com.lifx.chat`.
3. Download `google-services.json`.
4. Put it at `LIFXChat/app/google-services.json` (beside `app/build.gradle.kts` folder).
5. In Firebase Console → Authentication → Sign-in method, enable **Email/Password**.
6. In Firebase Console → Firestore Database, create a database. Start in production mode, then publish the rules in `firebase/firestore.rules` after reviewing and tightening them.

## 2. Open and build APK
1. Install Android Studio on a PC, or use a compatible Android build environment.
2. Extract this ZIP.
3. Open the `LIFXChat` folder in Android Studio.
4. Use JDK 17 and allow Gradle sync to finish.
5. Add `app/google-services.json` as described above.
6. Build → Build Bundle(s) / APK(s) → Build APK(s).
7. Android Studio will show the APK location, normally `app/build/outputs/apk/debug/app-debug.apk`.

For a terminal build after Gradle is installed:
```bash
gradle assembleDebug
```
The output is normally `app/build/outputs/apk/debug/app-debug.apk`.

## 3. Publish Firestore rules
Copy the contents of `firebase/firestore.rules` into Firebase Console → Firestore Database → Rules and publish. Review the security note above first; for a public production app, use a membership model so users can only read chats they belong to.

## 4. Test with two users
1. Install the built APK on two Android devices.
2. Register different email accounts on each device.
3. After both users have registered, they should appear in each other's user list.
4. Tap a user and send a message. Messages update live when Firestore is connected.

## Termux note
Building Android projects directly in Termux is more complicated than using Android Studio because Android SDK, build-tools, JDK, and Gradle must be installed and configured. The easiest reliable path is Android Studio on a computer or a cloud Android build runner. Never upload your Firebase service-account private key to the APK or public repository; the client config `google-services.json` is not a service-account key.
