use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::jbyteArray;
use image::{load_from_memory, ImageOutputFormat};
use std::io::Cursor;

#[no_mangle]
pub extern "system" fn Java_com_builder_utils_NativeLib_processVividEnhance(
    mut env: JNIEnv,
    _class: JClass,
    input_data_obj: JByteArray,
) -> jbyteArray {
    let input_bytes = env.convert_byte_array(&input_data_obj).unwrap();

    if let Ok(img) = load_from_memory(&input_bytes) {
        // Logika Vivid yang kamu mau: Smart Contrast & Brightness
        let processed_img = img.adjust_contrast(20.0).brighten(5);

        let mut out_data = Vec::new();
        // Pakai kualitas 95 agar tetap tajam tanpa setting ribet
        processed_img.write_to(&mut Cursor::new(&mut out_data), ImageOutputFormat::Jpeg(95)).unwrap();

        let output = env.byte_array_from_slice(&out_data).unwrap();
        output.into_raw()
    } else {
        input_data_obj.into_raw()
    }
}
