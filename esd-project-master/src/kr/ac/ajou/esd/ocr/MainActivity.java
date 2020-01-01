package kr.ac.ajou.esd.ocr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * OCR Application 의 Entry point
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    //Gallery 호출시 Request code
    private static final int REQ_GALLERY = 1000;

    //Gallery 호출시 이미지를 갖고오기 위한 file
    private File mFile;
    //mViewPicture 에 표현해주기 위한 Bitmap
    private Bitmap mPicture = null;
    private ImageView mViewPicture;
    private Camera mCamera;
    //OCR 실행 시 현재 진행 상태를 보여주기 위한 Progress dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing
        mViewPicture = (ImageView) findViewById(R.id.imgViewPicture);

        SurfaceHolder surfHolder = ((SurfaceView) findViewById(R.id.surfPreview)).getHolder();
        surfHolder.addCallback(mPreviewCallback);

        findViewById(R.id.btnGallery).setOnClickListener(clickListener);
        findViewById(R.id.btnTakePicture).setOnClickListener(clickListener);
        findViewById(R.id.btnNext).setOnClickListener(clickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_GALLERY) {
            if (resultCode == RESULT_OK) {
                try {
                    // 선택한 이미지에서 비트맵 생성
                    if (data.getData() == null)
                        return;
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    // OCR 처리 및 ImageView 에 표시하기 위해 저장
                    mPicture = BitmapFactory.decodeStream(in);
                    if (in == null)
                        return;

                    in.close();
                    // 이미지 표시
                    mViewPicture.setImageBitmap(mPicture);
                } catch (Exception e) {
                    Log.e(TAG, "Error!", e);
                }
            }
        }
    }

    //버튼 3개에 대한 Click listener
    View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnGallery: //Gallery 호출
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, REQ_GALLERY);
                    break;
                case R.id.btnTakePicture: //사진 촬영
                    //sd card 에 있는 file 선언
                    File sdcard = Environment.getExternalStorageDirectory();
                    //촬영한 사진의 파일명을 중복되지 않게 하기 위해 현재시간을 ms 단위로 갖고온다.
                    String fileName = String.format("%s/%s.jpg", sdcard.getAbsolutePath(), dateName(System.currentTimeMillis()));
                    mFile = new File(fileName);

                    Toast.makeText(getApplicationContext(), "loc: " + fileName, Toast.LENGTH_LONG).show();

                    //Camera 에서 auto focus 기능을 활용해 사진을 찍는다.
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                //auto focus 가 성공하면 사진을 찍고 mPictureCallback 을 통해 촬영 결과물을 받는다.
                                mCamera.takePicture(null, null, mPictureCallback);
                            }
                        }
                    });
                    break;
                case R.id.btnNext:
                	if(mPicture == null){
                        Toast.makeText(getApplicationContext(), "분석할 이미지가 없습니다!", Toast.LENGTH_LONG).show();
                		return;
                	}
                    new OCRTask(MainActivity.this).execute(mPicture);
                    break;
            }
        }
    };

    private SurfaceHolder.Callback mPreviewCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera = Camera.open(0);
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.set("jpeg-quality", 100);
                parameters.setPictureFormat(ImageFormat.JPEG);
                // OCR 을 하기 적당하다고 생각하는 이미지 사이즈인 640 X 480 으로 촬영한다.
                parameters.setPictureSize(640, 480);
                parameters.setPreviewSize(320, 240);
                parameters.setRotation(180);
                mCamera.setParameters(parameters);
                // 카메라가 거꾸로 설치되어 있는 것으로 판단하여 180 도 회전시킨다.
                mCamera.setDisplayOrientation(180);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error!", e);
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error!", e);
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.stopPreview();
            mCamera.release();
        }
    };

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // 사진이 촬영되면 이미지를 변환 및 저장하는 Work thread 로 넘겨준다.
            new PictureProcessTask(MainActivity.this).execute(data);
        }
    };

    private class PictureProcessTask extends AsyncTask<byte[], Integer, Bitmap> {
        // Memory leak 을 방지하기 위해 WeakReference 를 사용하여 Context 를 가져온다.
        private WeakReference<MainActivity> weakReference;

        //처리하는데 걸리는 시간을 확인하기 위한 변수
        private long elapsed;

        PictureProcessTask(MainActivity activity) {
            this.weakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Task 시작 초기화 및 알림. UI Thread 에 접근한다.
            weakReference.get().showProgress("Photo", "Processing...");
            elapsed = System.currentTimeMillis();
        }

        @Override
        protected Bitmap doInBackground(byte[]... data) {
            //background 에서 작업한다. Main Thread 를 접근 못 한다.
            try {
                //
                MainActivity mainActivity = weakReference.get();
                // bytearray 를 bitmap 으로 변환
                Bitmap originBitmap = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);

                // bitmap 을 180도 회전하여 rotatedBitmap 에 저장후 recycle
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                mainActivity.mPicture = Bitmap.createBitmap(originBitmap, 0, 0, originBitmap.getWidth(), originBitmap.getHeight(), matrix, true);
                originBitmap.recycle();

                // 회전된 이미지를 bytearray 로 변환
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mainActivity.mPicture.compress(Bitmap.CompressFormat.JPEG, 30, stream);
                byte[] mPictureArray = stream.toByteArray();

                // 회전된 이미지 저장
                FileOutputStream fos = new FileOutputStream(mFile);
                fos.write(mPictureArray);
                fos.flush();
                fos.close();
                // 이미지 저장됐다고 알림
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));
                mainActivity.mCamera.startPreview();
                return mPicture;
            } catch (Exception e) {
                Log.e(TAG, "Error!", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //촬영 완료 후 후처리하고 Progress dialog 닫음
            weakReference.get().hideProgress();
            Log.d("PictureCallback", "Elapsed: " + (System.currentTimeMillis() - elapsed));
            if (bitmap != null) {
                Toast.makeText(getApplicationContext(), "Write OK", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Write Error", Toast.LENGTH_SHORT).show();
            }
            mPicture = bitmap;
            //촬영한 사진 image view 에 표시
            weakReference.get().mViewPicture.setImageBitmap(bitmap);
        }
    }

    /**
     * OCR 처리를 Worker thread 에서 하기 위한 AsyncTask
     */
    private class OCRTask extends AsyncTask<Bitmap, Integer, ProcessedResult> {
        private WeakReference<MainActivity> weakReference;
        //OCR 진행 과정을 Progress dialog 에서 보여주기 위한 문자
        private String[] steps = {"(1/5) Preprocessing...", "(2/5) Binarization...", 
        		"(3/5) Segmentation...", "(4/5) Template matching...", "(5/5) Saving..."};
        //OCR 처리를 JNI 를 통해 해주기 위해 사용
        private JNIDriver jniDriver;

        public OCRTask(MainActivity activity) {
            this.weakReference = new WeakReference<MainActivity>(activity);
            this.jniDriver = new JNIDriver();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            weakReference.get().showProgress("OCR", "Starting...");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // doInBackground 에서 publishProgress 를 호출했을 때 UI 를 update 해준다.
            weakReference.get().progressDialog.setMessage(steps[values[values.length - 1]]);
        }

        @Override
        protected ProcessedResult doInBackground(Bitmap... bitmaps) {
            // background 에서 OCR 진행.
            return process(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(ProcessedResult results) {
            super.onPostExecute(results);
            // OCR 처리 완료하고 progress dialog 닫음.
            weakReference.get().hideProgress();
            if (results != null) {
                // 결과가 있다면 결과 dialog 띄워 줌.
                showResultDialog(results);
            }
        }

        private ProcessedResult process(Bitmap originBitmap) {
            // OCR 중간 처리 결과들을 저장하고 있는 List
            List<ProcessedData> result = new ArrayList<ProcessedData>();

            // 차량번호를 OCR 한 Text 결과
            String plateNumber = "";
            MainActivity mainActivity = weakReference.get();

            //각 단계별 처리 시간을 측정하기 위해 선언
            long startTime = System.currentTimeMillis();
            long processingTime = System.currentTimeMillis();
            try {
                //JNI 매개변수를 넣어주기 위해 너비와 높이를 변수로 둠
                int x = originBitmap.getWidth();
                int y = originBitmap.getHeight();
                result.add(new ProcessedData(mainActivity, "Original Image", originBitmap, System.currentTimeMillis() - startTime));
                //이미지를 Gaussian blur 로 전처리
                publishProgress(0);
                startTime = System.currentTimeMillis();
                Bitmap gaussianBlurBitmap = new GaussianBlur(mainActivity).transform(2, originBitmap);
                result.add(new ProcessedData(mainActivity, "Gaussian Blur Image", gaussianBlurBitmap, System.currentTimeMillis() - startTime));

                //이미지를 Gray scale
                publishProgress(1);
                startTime = System.currentTimeMillis();
                int[] gaussianIntArray = new int[x * y];
                gaussianBlurBitmap.getPixels(gaussianIntArray, 0, x, 0, 0, x, y);
                int[] grayResultArray = jniDriver.makeGrayScale(gaussianIntArray, x, y);
                Bitmap grayResultBitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
                grayResultBitmap.setPixels(grayResultArray, 0, x, 0, 0, x, y);
                result.add(new ProcessedData(mainActivity, "Gray scaled Image", grayResultBitmap, System.currentTimeMillis() - startTime));

                //이미지에서 번호판 추출
                publishProgress(2);
                startTime = System.currentTimeMillis();
                int[] intArray = new int[x * y];
                grayResultBitmap.getPixels(intArray, 0, x, 0, 0, x, y);
                int[] resultArray = jniDriver.sendBitmapByIntArray(intArray, x, y);

                //추출한 번호 Text 로 가져옴
                publishProgress(3);
                char[] resultText = jniDriver.sendBitmapReceivetext();
                plateNumber = new Mapper().transform(resultText);

                //최종 처리된 이미지 Bitmap 으로 만듬
                publishProgress(4);
                Bitmap resultBitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);

                // Set the pixels
                resultBitmap.setPixels(resultArray, 0, x, 0, 0, x, y);
                result.add(new ProcessedData(mainActivity, "Result Image", resultBitmap, System.currentTimeMillis() - startTime));

                // 이진화 이미지 SDcard에 저장
                FileOutputStream outStream = null;
                String extStorageDirectory = Environment.getExternalStorageDirectory().toString();

                File file = new File(extStorageDirectory, "binarized.jpg");

                outStream = new FileOutputStream(file);
                grayResultBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);

                outStream.flush();
                outStream.close();

                // 브로드캐스팅해서 갤러리에 보이도록 함
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            } catch (Exception e) {
                Log.e(TAG, "Error!", e);
            }
            // 전부 처리된 후 취합하여 return
            return new ProcessedResult(plateNumber, result, System.currentTimeMillis() - processingTime);
        }

        // 최종 결과 Dialog 로 확인
        private void showResultDialog(final ProcessedResult results) {
            new AlertDialog.Builder(weakReference.get()).setTitle("OCR 처리 결과").setMessage(String.format("분석 결과: %s\n총 처리 시간: %dms", results.getPlateNumber(), results.getProcessingTime())).setCancelable(true)
                    // Dialog 닫기
                    .setNegativeButton("닫기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    // 처리과정 상세보기
                    .setPositiveButton("상세보기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent resultIntent = new Intent(getApplicationContext(), ResultActivity.class);
                            //정리된 결과 intent 로 담아서 ResultActivity 로 전달
                            resultIntent.putExtra("RESULT", results);
                            startActivity(resultIntent);
                        }
                    })
                    .show();
        }
    }

    // progress dialog 보여줌.
    private void showProgress(String title, String message) {
        if (progressDialog == null || !progressDialog.isShowing())
            progressDialog = ProgressDialog.show(MainActivity.this, title, message, true, false);
    }

    // progress dialog 숨김.
    private void hideProgress() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    // dateToken 을 사람이 보기 좋게 바꿔서 return.
    private String dateName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(date);
    }
}