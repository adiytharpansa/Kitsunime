# Kitsunime

Aplikasi streaming anime untuk Android, dibangun dari nol dengan Kotlin + Jetpack Compose.
Mesin pemutarnya **mpv** (lewat libmpv) — dipilih karena dukungan subtitle ASS/SSA fansub-nya
yang paling lengkap, plus format video apa pun bisa diputar.

- Tema hitam / kuning (#FFD60A)
- Katalog dibaca dari gudang katalog yang sama dengan aplikasi-aplikasi pendahulunya
  (JSON polos lewat cermin raw GitHub / jsDelivr), video HLS diputar langsung oleh mpv
- Min SDK 26, target SDK 35

## Lisensi

Aplikasi ini didistribusikan di bawah **GPL-3.0-or-later**, karena memakai libmpv
(mpv + FFmpeg berlisensi GPL). Kode sumber libmpv yang dipakai:
https://github.com/midasvo/libmpv-android (artefak Maven `io.github.midasvo:libmpv`).
