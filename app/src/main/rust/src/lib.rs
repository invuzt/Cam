use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::jbyteArray;
use image::{load_from_memory, ImageBuffer, Rgb, DynamicImage, ImageOutputFormat};
use rayon::prelude::*;
use std::io::Cursor;

#[no_mangle]
pub extern "system" fn Java_com_builder_utils_NativeLib_processVividEnhance(
    mut env: JNIEnv,
    _class: JClass,
    input_data_obj: JByteArray,
) -> jbyteArray {
    // 1. Ambil data 1 foto mentah
    let input_bytes = env.convert_byte_array(&input_data_obj).unwrap();

    // 2. Decode di RAM Rust
    let img = load_from_memory(&input_bytes).unwrap().to_rgb8();
    let (width, height) = img.dimensions();
    let mut output_buffer: ImageBuffer<Rgb<u8>, Vec<u8>> = ImageBuffer::new(width, height);

    // 3. Proses Paralel (Multi-core) untuk Vivid Color Grading
    output_buffer.chunks_mut(width as usize * 3)
        .enumerate()
        .par_bridge()
        .for_each(|(y, row)| {
            for x in 0..width {
                let pixel_idx = (x as usize) * 3;
                let p = img.get_pixel(x, y as u32);
                
                let mut r = p[0] as f32;
                let mut g = p[1] as f32;
                let mut b = p[2] as f32;

                // Konversi HSL sederhana untuk deteksi warna
                let max = r.max(g).max(b);
                let min = r.min(g).min(b);
                let l = (max + min) / 2.0;
                let d = max - min;
                
                if d > 10.0 { // Jangan proses pixel grayscale
                    let h = if max == r {
                        (g - b) / d + (if g < b { 6.0 } else { 0.0 })
                    } else if max == g {
                        (b - r) / d + 2.0
                    } else {
                        (r - g) / d + 4.0
                    };
                    let hue_degrees = h * 60.0;

                    // TARGET WARNA 1: BIRU (Langit, Tiang Rak)
                    // Rentang Hue Biru: ~180 hingga ~260 derajat
                    if hue_degrees > 170.0 && hue_degrees < 270.0 {
                        // Kerjakan Biru: Naikkan Saturasi & Kecerahan (Vivid Blue)
                        r = (r * 0.8).clamp(0.0, 255.0); // Kurangi merah sedikit biar birunya murni
                        g = (g * 1.1).clamp(0.0, 255.0); // Naikkan hijau sedikit (turkis)
                        b = (b * 1.5).clamp(0.0, 255.0); // BOST BIRU ekstrem ala langit cerah
                    }

                    // TARGET WARNA 2: ORANYE (Rak Kayu)
                    // Rentang Hue Oranye: ~15 hingga ~45 derajat
                    if hue_degrees > 10.0 && hue_degrees < 50.0 {
                        // Kerjakan Oranye: Buat lebih Vivid & Hangat
                        r = (r * 1.4).clamp(0.0, 255.0); // BOOST MERAH
                        g = (g * 1.2).clamp(0.0, 255.0); // BOOST HIJAU (agar jadi oranye murni, bukan merah)
                        b = (b * 0.7).clamp(0.0, 255.0); // Kurangi biru (buat lebih hangat)
                    }
                }

                // Global Contrast & Brightness ala iPhone (Curves S)
                r = (r - 128.0) * 1.15 + 135.0;
                g = (g - 128.0) * 1.15 + 135.0;
                b = (b - 128.0) * 1.15 + 135.0;

                row[pixel_idx] = r.clamp(0.0, 255.0) as u8;
                row[pixel_idx + 1] = g.clamp(0.0, 255.0) as u8;
                row[pixel_idx + 2] = b.clamp(0.0, 255.0) as u8;
            }
        });

    // 4. Kompres hasil grading ke JPEG High Quality
    let mut compressed_data = Vec::new();
    let dynamic_img = DynamicImage::ImageRgb8(output_buffer);
    dynamic_img.write_to(&mut Cursor::new(&mut compressed_data), ImageOutputFormat::Jpeg(95)).unwrap();

    // 5. Kembalikan 1 biner foto Vivid ke Kotlin
    let output = env.byte_array_from_slice(&compressed_data).unwrap();
    output.into_raw()
}
