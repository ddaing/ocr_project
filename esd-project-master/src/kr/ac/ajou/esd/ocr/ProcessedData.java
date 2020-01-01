package kr.ac.ajou.esd.ocr;

import java.io.Serializable;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

/**
 * OCR 처리될 때매다의 데이터를 저장해주기 위한 Data 클래스.
 */
public class ProcessedData implements Serializable {
	private static final long serialVersionUID = 3350032430747030834L;

	// 처리된 부분에 대한 이름
	private String title;
	// 처리된 Bitmap Uri
	private String imageUri;
	// 처리하는데 걸린 시간 (ms)
	private long elapsed;

	public ProcessedData(Context context, String title, Bitmap image, long elapsed) {
		this.title = title;
		this.imageUri = BitmapUtil.bitmapToUri(context, image).toString();
		this.elapsed = elapsed;
	}

	public String getTitle() {
		return title;
	}

	public Uri getImageUri() {
		return Uri.parse(imageUri);
	}

	public long getElapsed() {
		return elapsed;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ProcessedData{");
		sb.append("title='").append(title).append('\'');
		sb.append(", imageUri='").append(imageUri).append('\'');
		sb.append(", elapsed=").append(elapsed);
		sb.append('}');
		return sb.toString();
	}
}