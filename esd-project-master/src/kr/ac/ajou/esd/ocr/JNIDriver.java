package kr.ac.ajou.esd.ocr;

public class JNIDriver {
	static {
		System.loadLibrary("OCR");
	}
	//jni번호판 영역을 검출하고 문자영역 검출, 이미지 매칭까지 하는 JNI함수
	private native int[] jniProcess(int[] intArray, int weight, int height);
	//grayscale로 변환하고 이진화처리까지 하는 함수
	private native int[] jniGrayScale(int[] intArray, int weight, int height);
	//이미지 매칭 결과를 반환하는 함수 
	private native char[] jniTextResult();

	public int[] makeGrayScale(int[] intArray, int x, int y) {
		return jniGrayScale(intArray, x, y);
	}

	public int[] sendBitmapByIntArray(int[] intArray, int x, int y) {
		return jniProcess(intArray, x, y);
	}

	public char[] sendBitmapReceivetext() {
		return jniTextResult();
	}

}