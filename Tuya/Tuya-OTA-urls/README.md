# Tuya OTA URL catalogue

This directory is a channel- and platform-organised catalogue of Tuya OTA offerings. The channel number selects the component being updated; it does not identify the chipset or firmware format by itself.

| Channel | Tuya component label | Updates/devices covered |
|---:|---|---|
| `0` | Main Module | Firmware for the product's primary network/application module, commonly a Wi-Fi SoC or gateway main module. |
| `1` | Bluetooth Module | Firmware for a Bluetooth/BLE module or Bluetooth component. |
| `3` | ZigBee Module | Firmware for a Zigbee module, coordinator, or Zigbee component. |
| `9` | MCU Module | Firmware for the external/application MCU in a TuyaMCU product, separate from the main network module. |

These names are the `type_desc` values observed in captured Tuya OTA responses. No meaning is assigned here to channel numbers that have not been observed in the local evidence corpus.

## Layout

- `channel-N/` contains only offerings reported for OTA channel `N`.
- `urls-<platform>.txt` is used only when the chipset or MCU family has supporting evidence from the firmware, module identity, or previously verified catalogue analysis.
- `urls-<family>-unclassified.txt` records a known vendor/family where the exact device is not established.
- `urls-unclassified.txt` retains correctly channelled offerings whose chipset is not yet known confidently. They are deliberately not guessed into a platform file.

The first line is the firmware/package identifier. Prefer the semantic stem from the original Tuya filename. When Tuya supplied only an opaque numeric filename, a directly recovered compiled application name (`APP_BIN_NAME`) or explicit internal application-class marker may be used instead; the associated PID remains in square brackets and the original Tuya filename remains visible in the URL. Entries then contain the target version, captured date/timestamp where available, and the original Tuya URL. A small number of archived channel-9 payloads have no captured original URL; those entries are retained with their SHA-256 so the catalogue still accounts for the file.
