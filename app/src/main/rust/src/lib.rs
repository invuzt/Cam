use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::jbyteArray;
use image::{load_from_memory, ImageBuffer, Rgb, DynamicImage, ImageOutputFormat};
use rayon::prelude::*; // Mengaktifkan otot multi-core HP via Rayon
use std::io::Cursor;

#[no_mangle]
pub extern "system" fn Java_com_builder_utils_NativeLib_processHDRAndCompress(
    mut env: JNIEnv,
    _class: JClass,
    img_dark_obj: JByteArray,
    img_normal_obj: JByteArray,
    img_bright_obj: JByteArray,
) -> jbyteArray {
    // 1. Ambil data byte mentah dari Java/Kotlin
    let byte_dark = env.convert_byte_array(&img_dark_obj).unwrap();
    let byte_normal = env.convert_byte_array(&img_normal_obj).unwrap();
    let byte_bright = env.convert_byte_array(&img_bright_obj).unwrap();

    // 2. Decode biner JPEG menjadi objek gambar di RAM Rust
    let img_dark = load_from_memory(&byte_dark).unwrap().to_rgb8();
    let img_normal = load_from_memory(&byte_normal).unwrap().to_rgb8();
    let img_bright = load_from_memory(&byte_bright).unwrap().to_rgb8();

    let (width, height) = img_normal.dimensions();
    
    // 3. Buat buffer kosong di RAM untuk menampung hasil foto "Iphone-Look"
    let mut output_buffer: ImageBuffer<Rgb<u8>, Vec<u8>> = ImageBuffer::new(width, height);

    // 4. OTOT UTAMA: Gabungkan pixel secara paralel memanfaatkan semua Core CPU HP
    // Kita lakukan iterasi baris demi baris pixel gambar
    output_buffer.chunks_mut(width as usize * 3)
        .enumerate()
        .par_bridge() // Jembatan magis Rayon untuk multithreading
        .for_each(|(y, row)| {
            for x in 0..width {
                let pixel_idx = (x as usize) * 3;
                
                let p_dark = img_dark.get_pixel(x, y as u32);
                let p_normal = img_normal.get_pixel(x, y as u32);
                let p_bright = img_bright.get_pixel(x, y as u32);

                // Algoritma HDR Sederhana (Exposure Blending):
                // Jika foto normal terlalu silau (overexposed), ambil detail dari foto gelap.
                // Jika foto normal terlalu gelap (underexposed), ambil detail dari foto terang.
                for c in 0..3 { // Loop warna R, G, B
                    let normal_val = p_normal[c] as f32;
                    
                    let final_color = if normal_val > 220.0 {
                        // Mengambil detail langit/lampu dari foto gelap
                        p_dark[c] as f32 * 0.6 + normal_val * 0.4
                    } else if normal_val < 45.0 {
                        // Mengangkat detail bayangan dari foto terang
                        p_bright[c] as f32 * 0.6 + normal_val * 0.4
                    } else {
                        // Jika pencahayaan normal, gunakan setelan seimbang
                        normal_val * 0.7 + p_dark[c] as f32 * 0.15 + p_bright[c] as f32 * 0.15
                    };

                    row[pixel_idx + c] = final_color.clamp(0.0, 255.0) as u8;
                }
            }
        });

    // 5. Kompres hasil stacking tadi ke JPEG dengan kualitas optimal (85%)
    let mut compressed_data = Vec::new();
    let dynamic_img = DynamicImage::ImageRgb8(output_buffer);
    dynamic_img.write_to(&mut Cursor::new(&mut compressed_data), ImageOutputFormat::Jpeg(85)).unwrap();

    // 6. Kembalikan biner matang berukuran efisien ke Kotlin
    let output = env.byte_array_from_slice(&compressed_data).unwrap();
    output.into_raw()
}
