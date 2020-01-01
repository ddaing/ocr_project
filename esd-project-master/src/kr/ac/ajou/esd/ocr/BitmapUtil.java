package kr.ac.ajou.esd.ocr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Bitmap 을 byte array 나 Uri 로 conversion 시켜주기 위한 Util Class. Main Activity 에서
 * Result Activity 로 Bitmap 을 넘겨주려면 사이즈가 너무 크기에 Uri 로 만들어 Intent 에 담아 넘겨준다.
 */
public class BitmapUtil {

	// Bitmap 을 Uri 로 변환.
	public static Uri bitmapToUri(Context context, Bitmap inImage) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
		return Uri.parse(path);
	}

	// Uri 를 Bitmap 으로 변환.
	public static Bitmap uriToBitmap(Context context, Uri uri) throws IOException {
		return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
	}
}
