# WhyScan privacy policy

**Last updated: 27 August 2026.**

WhyScan is a barcode and QR reader. This policy explains what data it handles, where that data
stays, and what you can do with it. It is written to be checkable: every claim here corresponds to
something you can verify in the app's source code, and where there is a caveat, the caveat is
stated.

## The short version

**WhyScan does not collect, store on any server, or transmit anything you scan.** There is no
account, no sign-up, no advertising, no analytics and no crash reporting.

The app **does not request the internet permission**. On Android that is not a promise we make: it
is the operating system that stops a process without that permission from opening a network
connection. You can check it from the phone itself, in the permission list your system settings
show.

One detail, because you may look at the source and find it confusing: the manifest does mention
`INTERNET` — with `tools:node="remove"`, which is the instruction that **takes it out**. It is
there because the Google scanning libraries declare that permission in their own manifests and
Android's build merges them into the app unless it is told not to. Until 31 August 2026 nobody was
telling it not to, so builds before that date did carry the permission. It was found by a check
written for exactly that case, and the decision is recorded in
[ADR-0020](../adr/ADR-0020-el-permiso-de-internet-se-quita-no-solo-se-omite.md).

## What data exists, and where

### What you scan

Every scan is stored in the **history**, a database inside the app's private storage on your
device. For each scan it keeps:

- the code's content, exactly as it came;
- the format (QR, EAN-13, Code 128…);
- when it was read, and which scanning engine read it;
- the note you wrote on it, if you wrote one.

That content can be anything a code can carry, **including sensitive information**: a Wi-Fi QR
carries the network password inside it, and a ticket or parcel code can identify you. It is stored
because the history is the feature; it does not leave the device.

The **system's automatic backup is switched off** (`allowBackup="false"` plus
`dataExtractionRules`). This matters more than it looks: without it, Android would copy the history
database to the user's Google Drive account, and it would be done by a system process, for which our
lack of an internet permission means nothing.

The trade-off is stated and accepted: **when you change phones, the history does not travel.** If
you want to keep it, export it yourself.

### The camera

WhyScan asks for camera permission in order to read codes. Images are analysed **on the device**,
frame by frame, and are not stored anywhere: the app takes no pictures, records no video and keeps
no frame after analysing it.

If you would rather not grant the permission, the app still works: you can scan an image you already
have, or type a code by hand.

### The images you pick

To scan from an image, the app uses the system picker — Android's photo picker — which runs outside
the app and hands it only the file you chose. That is why WhyScan **does not ask for permission to
read your photos**: it has no access to your gallery, only to what you hand it. The image is decoded
and discarded; it is not copied or stored.

### Your settings

Theme, language, dyslexia mode and advanced mode are stored in the app's private storage. Nothing
else.

## Third parties

Here is the one caveat in this whole document, which is why it gets its own name.

One of WhyScan's scanning engines on Android is the **Google Play Services code scanner** (*Google
Code Scanner*). When that engine is used, the camera is opened by Play Services — not by WhyScan —
in its own screen, and the only thing that comes back to the app is the text of the code. That
component belongs to Google, and its data handling is governed by [Google's privacy
policy](https://policies.google.com/privacy).

WhyScan sends it nothing of its own accord and receives nothing from it beyond the scan result. If
you would rather not use it, in **Settings → Advanced** you can turn on the engine workbench and
pick, by hand, any of the engines that run entirely inside the app.

Beyond that, WhyScan **integrates no analytics, advertising, attribution or crash-reporting SDK**.

## When anything leaves the device

Only when you send it, and always with an explicit gesture:

- **Copy** puts the content on the system clipboard. WhyScan marks it as sensitive so Android does
  not show it in the floating preview it displays on top of any app.
- **Share** opens the system sheet and you choose where it goes. From there on the app you pick is
  in charge, not this one.
- **Export** writes the history to a CSV, JSON or text file wherever you say.
- **Open** a link from a scanned code takes you to the browser or the matching app. WhyScan only
  opens `http`, `https`, `mailto`, `tel`, `sms` and `geo`; any other scheme is rejected. What
  happens afterwards happens in that other app, under its policy, not ours.

## Your rights, in practice

You do not need to write to us to exercise them, because we hold nothing of yours:

- **See** your data: it is on the History screen.
- **Export it**: the export buttons, on that same screen.
- **Delete one**: the delete button on each row, with an undo option.
- **Delete all of it**: "Clear" in the history bar. It is final, and there is no copy anywhere.
- **Delete everything for good**: uninstall the app. It takes the database and the settings with it.

## Children

WhyScan is not directed at children under 13, and it collects data from nobody, of any age.

## Changes to this policy

If what the app does with data ever changes, this document changes with it and the date above says
so. The change history is public: it lives in the same repository as the code.

## Contact

For any privacy question, or to exercise the rights in the previous section, write to
**<david@faro.net.ar>**.

WhyScan is also an open-source project: if yours is a defect rather than personal data, a public
issue at <https://github.com/dgfigueroa29/WhyScan/issues> gets there sooner and helps more people.
What does **not** belong in a public issue is anything that identifies you.
