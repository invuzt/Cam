package com.builder.utils

object NativeLib {
    init {
        // Nama biner native sesuai dengan Cargo.toml
        System.loadLibrary("rust_engine")
    }

    /**
     * Mengirim 3 biner foto dengan exposure berbeda ke Rust
     * untuk diproses menggunakan algoritma HDR Stacking & kompresi paralel.
     */
    external fun processHDRAndCompress(
        imgDark: ByteArray,
        imgNormal: ByteArray,
        imgBright: ByteArray
    ): ByteArray
}
