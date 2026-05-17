# UICC Browser

UICC Browser is an Android application for viewing and editing the contents of UICC. A combination of this application and your older Android phone may be usable as an alternative to your PC/SC card reader and reader/writer software.

The application does not distinguish between physical SIM cards and eSIM profiles. Any active eSIM profile can therefore be accessed in the same way as a physical SIM card.

This project is intended for users who already understand SIM/UICC file structures and the risks of modifying card contents.

## Setup

UICC Browser requires Android system-level privileges, so it can only be used on rooted Android device. The author has tested the application on an older Google Pixel 4 with Magisk installed.

If you use Magisk, the easiest installation method is to install the ZIP file in the `release` directory from the Magisk "Modules" tab.

The application specifically requires the following privileged Android permissions:

- `READ_PRIVILEGED_PHONE_STATE`
- `MODIFY_PHONE_STATE`

Any installation method should work as long as it can grant these permissions to the application.

```
$ adb shell pm grant com.github.cheeriotb.uiccbrowser android.permission.READ_PRIVILEGED_PHONE_STATE
$ adb shell pm grant com.github.cheeriotb.uiccbrowser android.permission.MODIFY_PHONE_STATE
```

The minimum supported Android version is Android 13, which corresponds to SDK 33. The current target SDK version is 35.

## Build

Use the Gradle wrapper to build and test the project.

```
$ ./gradlew assembleDebug
$ ./gradlew testDebugUnitTest --stacktrace
```

## Functionalities

UICC Browser currently provides the following features:

- Select the target SIM card or eSIM profile from the icon at the top of the navigation view when multiple profiles are active on the device.
- Check whether the EFs described in the JSON resource files exist on the card, and list the EFs that are present.
- Read and display the data of the selected EF.
- Display the FCP template of the selected EF.
- Enable "Pro Mode" at the bottom of the navigation view to add "Edit" to the options menu on the EF data screen.
- Enter Edit Mode after selecting "Edit" and successfully verifying the required PIN code or ADM key.

The following screen shows Edit Mode for a selected EF.

![edit mode](https://github.com/user-attachments/assets/759b29a7-c9ad-4ba6-916a-43e41a029f5f)

The following screen shows an EF update operation after editing its contents.

![update](https://github.com/user-attachments/assets/86d41776-e3a2-445a-b8e1-2c66f0e49986)

## Warning

Editing UICC contents can break SIM functionality or make a profile unusable. Use this application only with cards and profiles that you are prepared to recover or replace. The author is not liable for any damages or consequences resulting from the use of this application.

## Limitations

Current limitations are listed below.

- Only USIM and ISIM are supported as ADF.
- The number of supported EFs is currently quite limited.
- Linear Fixed and Transparent EFs are supported, but Cyclic and BER-TLV EFs are not supported yet.
- Special EF operations such as create, delete, and resize are not supported.
- Some features have not been fully verified because the number of SIM cards and devices available for testing is limited.

## License

This software is released under the MIT License, see LICENSE.

## Author

Cheerio (cheerio.the.bear@gmail.com)

## References

- [ETSI TS 102 221 V18.4.0](https://www.etsi.org/deliver/etsi_ts/102200_102299/102221/18.04.00_60/ts_102221v180400p.pdf)
- [ETSI TS 131 102 V19.4.0](https://www.etsi.org/deliver/etsi_ts/131100_131199/131102/19.04.00_60/ts_131102v190400p.pdf)
- [ETSI TS 131 103 V19.0.0](https://www.etsi.org/deliver/etsi_ts/131100_131199/131103/19.00.00_60/ts_131103v190000p.pdf)
