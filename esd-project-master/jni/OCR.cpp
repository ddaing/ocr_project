#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>
#include <string.h>
#include "letterData.h"
//번호판 문자 수
#define MAX_COUNT_DATA            7
//#define RANGE_OF_COLOR_TO_CHECK      80
//번호판 너비 시작
#define RATE_START_FOR_PARSING      0.3
//번호판 너비 끝
#define RATE_END_FOR_PARSING      0.7

extern "C" {
JNIEXPORT jintArray JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jniGrayScale(
		JNIEnv * env, jobject obj, jintArray originArray, jint width,
		jint height);
//jni로 비트맵 이미지를 받아와서 처리
JNIEXPORT jintArray JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jniProcess(
		JNIEnv * env, jobject obj, jintArray originArray, jint width,
		jint height);
//처리하고 매칭한 결과를 자바로 보냄
JNIEXPORT jcharArray JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jniTextResult(
		JNIEnv * env, jobject obj);

}



//전역변수로 번호판 데이터 선언
AllData allData;
Data *data;
int imageWidth;
int imageHeight;
int imageArray[640 * 480];
void grayScale();
void ParsingStepFirst();
void ParsingStepSecond(int xPlateLeft, int yTop, int yBottom);
void ParsingStepThird(Rect *rect);
void MakeImageData();
void MakeLetterData(Rect *rect);
void FindValue();
void FindNumberValue(Data *aData);
void FindLetterValue(Data *aData);
bool IsLetterRect(Rect *rect);

//---------------------------------------------------------------------------------------------------
JNIEXPORT jint JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jnitest(JNIEnv * env,
		jobject obj) {

	return allData.data[0].letter.image[23];
//	return standardNumber.letter[6].image[15];
}
JNIEXPORT jcharArray JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jniTextResult(
		JNIEnv * env, jobject obj) {
	int i;
	char s1[7];
	jcharArray newCharArray = env->NewCharArray(MAX_COUNT_DATA);
	jchar *nchararr = env->GetCharArrayElements(newCharArray, NULL);
	for (i = 0; i < MAX_COUNT_DATA; i++) {
		s1[i] = allData.data[i].letter.value;
	}
	for (i = 0; i < MAX_COUNT_DATA; i++) {
		nchararr[i] = s1[i];
	}
	env->ReleaseCharArrayElements(newCharArray, nchararr, 0);
	return newCharArray;
}
JNIEXPORT jintArray JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jniGrayScale(
		JNIEnv * env, jobject obj, jintArray originArray, jint width,
		jint height) {
	//전역변수에 너비와 높이 저장
	imageWidth = width;
	imageHeight = height;
	//파싱 후 결과 이미지를 자바로 넘겨주기 위한 int array 생성
	const jsize length = env->GetArrayLength(originArray);
	jintArray newArray = env->NewIntArray(length);
	//int 포인터 생성
	jint *originInt = env->GetIntArrayElements(originArray, NULL);
	jint *newInt = env->GetIntArrayElements(newArray, NULL);

	int i;
	//이미지를 처리하기 위해
	for (i = 0; i < length - 1; i++) {
		imageArray[i] = originInt[i];
	}
	grayScale();

	for (i = 0; i < length - 1; i++) {
		newInt[i] = imageArray[i];
	}
	env->ReleaseIntArrayElements(newArray, newInt, 0);
	env->ReleaseIntArrayElements(originArray, originInt, 0);

	return newArray;
}
void grayScale() {
	int x, y, pixel;
	
	for (y = 0; y < imageHeight; y++) {
		for (x = 0; x < imageWidth; x++) {

			pixel = imageArray[(y * imageWidth) + x];
			//R
			int gray = ((((pixel >> 16) & 0xff) * 30)
					+ (((pixel >> 8) & 0xff) * 30) + ((pixel & 0xff) * 30))
					/ 100;
			int alpha = (pixel >> 24) & 0xff;
			if (gray > 100)
				gray = 255;
			else
				gray = 0;
			imageArray[(y * imageWidth) + x] = (alpha << 24) + (gray << 16)
					+ (gray << 8) + (gray);

		}
	}

}
JNIEXPORT jintArray JNICALL Java_kr_ac_ajou_esd_ocr_JNIDriver_jniProcess(
		JNIEnv * env, jobject obj, jintArray originArray, jint width,
		jint height) {
	//전역변수에 너비와 높이 저장
	imageWidth = width;
	imageHeight = height;
	//파싱 후 결과 이미지를 자바로 넘겨주기 위한 int array 생성
	const jsize length = env->GetArrayLength(originArray);
	jintArray newArray = env->NewIntArray(length);
	//int 포인터 생성
	jint *originInt = env->GetIntArrayElements(originArray, NULL);
	jint *newInt = env->GetIntArrayElements(newArray, NULL);

	int i;
	//이미지를 처리하기 위해
	for (i = 0; i < length - 1; i++) {
		imageArray[i] = originInt[i];
	}
	//문자 영역 추출
	ParsingStepFirst();

	//추출한 각 문자 영역에서 이미지 문자 데이터를 생성
	MakeImageData();

	//생성한 문자 데이터와 기준 문자 데이터를 비교 연산하여 유사도가 높은 결과 저장
	FindValue();

	for (i = 0; i < length - 1; i++) {
		newInt[i] = imageArray[i];
	}
	env->ReleaseIntArrayElements(newArray, newInt, 0);
	env->ReleaseIntArrayElements(originArray, originInt, 0);

	return newArray;
}

void ParsingStepFirst() {
	// 픽셀 데이터
	int pixel, pixel1, pixel2;
	int i, x, y, count, count1, count2;
	int yTop, yBottom;
	int yPlateTop, yPlateBottom;
	//문자라인 확인
	bool isLetterLine, isPlateLine;
	//번호판 문자 데이터 초기화
	allData.count = 0;
	data = &allData.data[0];
	for (i = 0; i < MAX_COUNT_DATA; i++) {
		allData.data[i].letter.value = '\0';
	}

	int xMax = imageWidth;
	int yMax = imageHeight;
	int yMiddle = (int) (yMax / 2);
	//스캔 시작, 종료값
	int xStart = (int) (xMax * RATE_START_FOR_PARSING);
	int xEnd = (int) (xMax * RATE_END_FOR_PARSING);

	//번호판의 중앙부터 위쪽으로 스캔시작
	for (y = yMiddle; y > 0; y--) {

		isLetterLine = false;
		count = 0;
		//행을 검색했을 때 검은색 픽셀의 수가 3개 미만일경우 문자 영역의 경계
		for (x = xStart; x < xEnd; x++) {

			pixel = imageArray[(y * xMax) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				isLetterLine = true;
				break;
			}
		}
		//문자영역 위쪽 경계 저장
		if (!isLetterLine) {
			yTop = y + 1;
			break;
		}
	}
	//번호판 위쪽 경계 검출
	for (y = yTop - 2; y > 0; y--) {
		isPlateLine = false;
		count = 0;
		for (x = xStart; x < xEnd; x++) {

			pixel = imageArray[(y * xMax) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 10) {
				isPlateLine = true;
				break;
			}
		}
		if (isPlateLine) {
			yPlateTop = y + 1;
			break;
		}
	}

	for (y = yMiddle; y < yMax; y++) {

		isLetterLine = false;
		count = 0;

		for (x = xStart; x < xEnd; x++) {

			pixel = imageArray[(y * xMax) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				isLetterLine = true;
				break;
			}
		}
		//문자영역 아래쪽 경계 저장
		if (!isLetterLine) {
			yBottom = y - 1;
			break;
		}
	}
	//번호판 아래쪽 경계 검출
	for (y = yBottom + 2; y < yMax; y++) {
		isPlateLine = false;
		count = 0;
		for (x = xStart; x < xEnd; x++) {

			pixel = imageArray[(y * xMax) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 10) {
				isPlateLine = true;
				break;
			}
		}
		if (isPlateLine) {
			yPlateBottom = y - 1;
			break;
		}
	}
	int xPlateLeft, xPlateRight;
	int yTopMiddle = (yTop + yPlateTop) / 2;
	int yBottomMiddle = (yBottom + yPlateBottom) / 2;
	isPlateLine = false;
	count1 = 0;
	count2 = 0;
	//번호판 왼쪽 경계 검출
	for (x = xStart; x > 0; x--) {

		pixel1 = imageArray[(yTopMiddle * xMax) + x];
		pixel2 = imageArray[(yBottomMiddle * xMax) + x];
		if (((pixel1 >> 16) & 0xff) < 80) {
			count1 += 1;
		}
		if (((pixel2 >> 16) & 0xff) < 80) {
			count2 += 1;
		}
		if (count1 > 2 && count2 > 2) {
			isPlateLine = true;
			xPlateLeft = x;
			break;
		}
	}
	isPlateLine = false;
	count1 = 0;
	count2 = 0;
	//번호판 오른쪽 경계 검출
	for (x = xEnd; x < xMax; x++) {

		pixel1 = imageArray[(yTopMiddle * xMax) + x];
		pixel2 = imageArray[(yBottomMiddle * xMax) + x];
		if (((pixel1 >> 16) & 0xff) < 80) {
			count1 += 1;
		}
		if (((pixel2 >> 16) & 0xff) < 80) {
			count2 += 1;
		}
		if (count1 > 2 && count2 > 2) {
			isPlateLine = true;
			xPlateRight = x;
			break;
		}
	}
	//번호판 영역 표시
	for (x = xPlateLeft; x < xPlateRight; x++) {
		imageArray[(yTopMiddle * xMax) + x] = 0xFFFF0000;
		imageArray[((yTopMiddle+1) * xMax) + x] = 0xFFFF0000;
		imageArray[(yBottomMiddle * xMax) + x] = 0xFFFF0000;
		imageArray[((yBottomMiddle+1) * xMax) + x] = 0xFFFF0000;
	}
	for(y = yTop; y<yBottom; y++){
		imageArray[(y*xMax) + xPlateLeft] = 0xFFFF0000;
		imageArray[(y*xMax) + xPlateLeft+1] = 0xFFFF0000;
		imageArray[(y*xMax) + xPlateRight] = 0xFFFF0000;
		imageArray[(y*xMax) + xPlateRight+1] = 0xFFFF0000;
	}
	//문자영역 좌우 경계 검색을 위해 parsingstepsecond
	ParsingStepSecond(xPlateLeft, yTop, yBottom);
}

//문자영역 좌우 경계 검색
void ParsingStepSecond(int xPlateLeft, int yTop, int yBottom) {

	int xMax = imageWidth;

	int x, y, count;
	int pixel;
	bool isLetterLine;
	bool flagPrevLine;

	flagPrevLine = false;
	//parsingstepfirst에서 구한 y축 경계를 x축을 따라가면서 문자열 영역 탐색
	for (x = xPlateLeft; x < xMax; x++) {

		isLetterLine = false;
		count = 0;
		//문자열 영역 탐색
		for (y = yTop; y <= yBottom; y++) {
			pixel = imageArray[(y * xMax) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				isLetterLine = true;
				break;
			}
		}
		//검은색 픽셀값을 만난경우 문자 영역이라고 예상하고 start좌표를 저장
		if (isLetterLine) {
			if (!flagPrevLine) {
				data->rect.start.x = x;
				data->rect.start.y = yTop;
			}
			//문자 영역을 벗어날 때 end좌표를 저장
		} else {
			if (flagPrevLine) {
				data->rect.end.x = x - 1;
				data->rect.end.y = yBottom;
				//정확한 문자영역을 추출하기 위해 parssingstepthird
				ParsingStepThird(&data->rect);
				//문자영역인지 다른 영역을 문자영역으로 착각한 것인지 확인, 통과할 경우 문자열 데이터에 저장
				if (IsLetterRect(&data->rect)) {
					allData.count += 1;
					data = &allData.data[allData.count];
				}

				if (allData.count == MAX_COUNT_DATA)
					break;
			}

		}
		flagPrevLine = isLetterLine;
	}
	//번호판 각 문자영역 표시
	int j;
	for (j = 0; j < allData.count; j++) {
		data = &allData.data[j];
		for (x = data->rect.start.x; x < data->rect.end.x; x++) {
			imageArray[(data->rect.start.y * xMax) + x] = 0xFFFF0000;
			imageArray[(data->rect.end.y * xMax) + x] = 0xFFFF0000;
		}
		for (y = data->rect.start.y; y < data->rect.end.y; y++) {
			imageArray[(y * xMax) + data->rect.end.x] = 0xFF0000FF;
			imageArray[(y * xMax) + data->rect.start.x] = 0xFF0000FF;
		}
	}
}
// 개별 문자 높이 맞추기
void ParsingStepThird(Rect *rect) {

	int x, y, count;
	int pixel;
	bool isLetterLine;
	int height = imageHeight;
	//개별 문자 위쪽 경계를 위, 아래로 움직이며 맞춤
	for (y = rect->start.y; y >= 0; y--) {
		isLetterLine = false;
		count = 0;
		for (x = rect->start.x; x <= rect->end.x; x++) {
			pixel = imageArray[(y * imageWidth) + x];
			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				rect->start.y = y;
				isLetterLine = true;
				break;
			}
		}
		if (!isLetterLine)
			break;
	}
	for (y = rect->start.y; y < height; y++) {

		isLetterLine = false;
		count = 0;

		for (x = rect->start.x; x <= rect->end.x; x++) {

			pixel = imageArray[(y * imageWidth) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				rect->start.y = y;
				isLetterLine = true;
				break;
			}
		}
		if (isLetterLine)
			break;
	}
	//개별 문자 아래쪽 경계를 위, 아래로 움직이며 맞춤
	for (y = rect->end.y; y < height; y++) {

		isLetterLine = false;
		count = 0;

		for (x = rect->start.x; x <= rect->end.x; x++) {

			pixel = imageArray[(y * imageWidth) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				rect->end.y = y;
				isLetterLine = true;
				break;
			}
		}
		if (!isLetterLine)
			break;
	}
	for (y = rect->end.y; y >= 0; y--) {

		isLetterLine = false;
		count = 0;

		for (x = rect->start.x; x <= rect->end.x; x++) {

			pixel = imageArray[(y * imageWidth) + x];

			if (((pixel >> 16) & 0xff) < 80) {
				count += 1;
			}
			if (count > 3) {
				rect->end.y = y;
				isLetterLine = true;
				break;
			}
		}
		if (isLetterLine)
			break;
	}
}
//가정한 문자영역이 문자인지 검사 높이가 35 이상이어야 하고, 높이가 너비보다 크고, 너비의 4배가 높이보다 크면 안된다.
bool IsLetterRect(Rect *rect) {

	int width = rect->end.x - rect->start.x;
	int height = rect->end.y - rect->start.y;

	if (height < 35)
		return false;
	if ((height > width) && (height < (4 * width)))
		return true;

	return false;
}
//경계를 결정한 문자들을 기준문자와 매칭해서 문자값을 구한다. 한글은 3번째에 있으므로 따로 처리를 한다.
void FindValue() {

	int i;

	for (i = 0; i < allData.count; i++) {

		Data *aData = &allData.data[i];

		if (i == 2)
			FindLetterValue(aData);//한글데이터
		else
			FindNumberValue(aData);//숫자 데이터

	}
}
//
void FindNumberValue(Data *aData) {

	int count, maxCount;
	unsigned int buffer, bit;
	int i, x, y;

	Letter *letter = &(aData->letter);

	maxCount = 0;

	for (i = 0; i < standardNumber.count; i++) {

		count = 0;
		//aData->letter->image에 있는 값을 기준 숫자와 xor연산으로 비교한다. count가 높을 수록 일치할 확률이 높음.
		for (y = 0; y < 48; y++) {

			buffer = letter->image[y] ^ standardNumber.letter[i].image[y];

			for (x = 0; x < 32; x++) {
				bit = 0x80000000;
				bit >>= x;
				bit = bit & buffer;

				if (!bit)
					count += 1;
			}
		}
		//가장 count가 높은 값을 letter의 문자값으로 저장.
		if (count > maxCount) {
			letter->value = standardNumber.letter[i].value;
			maxCount = count;
		}
	}
}

void FindLetterValue(Data *aData) {

	int count, maxCount;
	unsigned int buffer, bit;
	int i, x, y;

	Letter *letter = &(aData->letter);

	maxCount = 0;

	for (i = 0; i < standardLetter.count; i++) {

		count = 0;
		//aData->letter->image에 있는 값을 기준 문자와 xor연산으로 비교한다. count가 높을 수록 일치할 확률이 높음.
		for (y = 0; y < 48; y++) {

			buffer = letter->image[y] ^ standardLetter.letter[i].image[y];

			for (x = 0; x < 32; x++) {

				bit = 0x80000000;
				bit >>= x;
				bit = bit & buffer;

				if (!bit)
					count += 1;
			}
		}
		//가장 count가 높은 값을 letter의 문자값으로 저장.
		if (count > maxCount) {
			letter->value = standardLetter.letter[i].value;
			maxCount = count;
		}
	}
}
//찾은 문자 영역을 기준 문자와 매칭할 수 있도록 바이너리화 해줌
void MakeImageData() {

	int i, j;

	for (i = 0; i < allData.count; i++) {

		Data *data = &allData.data[i];
		Letter *letter = &data->letter;
		Rect *rect = &data->rect;

		float xRate, yRate;
		int x, y, pixel;
		unsigned int buffer;
		//기준매칭할 데이터의 넓이는 48*32이다.
		for (j = 0; j < 48; j++)
			letter->image[j] = 0x00000000;
		//기준 데이터와 크기를 맞춰주기 위해 같은 비율로 맞춰준다.
		xRate = (float) (rect->end.x - rect->start.x) / (32 - 1);
		yRate = (float) (rect->end.y - rect->start.y) / (48 - 1);

		for (y = 0; y < 48; y++) {
			for (x = 0; x < 32; x++) {
				int w = (int) (rect->start.x + (x * xRate));
				int h = (int) (rect->start.y + (y * yRate));

				pixel = imageArray[(h * imageWidth) + w];
				//픽셀값이 검정일 경우 맨왼쪽 비트를 on하고 x의 길이만큼 right shift하고 or연산으로 값을 넣어준다.
				if (((pixel >> 16) & 0xff) < 80) {
					buffer = 0x80000000;
					buffer >>= x;
					letter->image[y] |= buffer;
				}
			}
		}
	}
}
