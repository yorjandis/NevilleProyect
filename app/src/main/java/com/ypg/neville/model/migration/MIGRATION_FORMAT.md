# `YPGEXP-2` / `.ypgexp` format

Version: `2`. This is the only supported contract; `MYAPPEXPORT-1` and PBKDF2 files are intentionally rejected.

The file is portable between Android and iOS. It never contains a native SQLite/Core Data store.

Canonical records must contain decrypted/plain user data. Platform-specific encryption used inside SQLite, Room, Core Data, or other local persistence layers must be removed before writing `data.ndjson`, then reapplied by the target platform when importing into its own local store.

## Binary envelope

```text
YPGEXP-2\n
{header-json}\n
{aes-gcm-ciphertext-with-tag}
```

`header-json` is not secret. AES-GCM authenticates the complete original prefix, including both LF bytes:

```text
AAD = ASCII("YPGEXP-2") || 0A || header-json-bytes || 0A
```

The header contains exactly:

- `format`: `com.ypg.neville.ndjson.export`
- `formatVersion`: `2`
- `cipher`: `AES-256-GCM`
- `kdf`: `ARGON2ID`
- `kdfVersion`: `19` (Argon2 v1.3)
- `kdfMemoryKiB`: `65536`
- `kdfIterations`: `3`
- `kdfParallelism`: `1`
- `passwordNormalization`: `NFC`
- `salt`: Base64, 16 random bytes per export
- `nonce`: Base64, 12 random bytes per export
- `keyLengthBits`: `256`
- `tagLengthBits`: `128`

Passwords are normalized to NFC and encoded as UTF-8 without a NUL terminator. New exports require at least 15 Unicode code points and at most 1024 UTF-8 bytes. Imports enforce only the maximum.

The encrypted payload must contain ciphertext followed by the 16-byte GCM tag. Salt and nonce are generated independently with `SecureRandom` for every file.

## Plaintext package

After decrypting, the plaintext is:

```text
uint32_be manifest_json_byte_length
manifest.json bytes
data.ndjson bytes
```

The plaintext is kept in memory by the Android implementation and should not be written to disk.

Defensive limits are 4096 header bytes, 1 MiB manifest bytes, 512 MiB plaintext bytes, and 536875136 total file bytes. Header constants, JSON number types, canonical Base64, salt/nonce lengths, and payload length are checked before running Argon2id.

## `manifest.json`

Required fields:

- `format`
- `formatVersion`
- `createdAt`
- `sourcePlatform`: `ios` or `android`
- `sourceAppVersion`
- `schemaVersion`
- `contentSummary`
- `encryption`
- `exportId`

## `data.ndjson`

Each line is one canonical record:

```json
{"type":"note","id":"sha256:...","createdAt":"2026-07-05T10:00:00Z","updatedAt":"2026-07-05T10:15:00Z","schemaVersion":1,"payload":{"title":"Idea","body":"Texto","tags":["personal"]}}
```

Android currently exports:

- `note`
- `diary_entry`
- `agenda_entry`
- `goal`
- `archived_goal`
- `personal_phrase`
- `personal_reflection`
- `day_ritual_archive`
- `calm_personal_phrase`

Imports are idempotent. Existing identical records are skipped; same stable ID or duplicate content with different payload is reported as an `ImportConflict` and not overwritten by the default UI.

Duplicate content rules for auto-increment-backed local entities:

- `note`: `payload.title + payload.body`
- `diary_entry`: `payload.title + payload.body`
- `personal_phrase`: `payload.phrase`

Dates and native database IDs are not used for those duplicate checks.
