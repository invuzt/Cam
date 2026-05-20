package com.builder.utils

object NativeLib {
    init {
        // Nama biner native sesuai dengan Cargo.toml
        System.loadLibrary("rust_engine")
    }

    /**
     * Mengirim 1 biner foto ke Rust untuk diwarnai ulang
     * (Vivid Color Grading) agar warna Biru & Oranye lebih Standout.
     */
    external fun processVividEnhance(inputData: ByteArray): ByteArray
}
