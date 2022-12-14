package me.pqpo.smartcamera;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.cameraview.CameraImpl;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import me.pqpo.smartcameralib.MaskView;
import me.pqpo.smartcameralib.SmartCameraView;
import me.pqpo.smartcameralib.SmartScanner;


public class MainActivity extends AppCompatActivity {

    private SmartCameraView mCameraView;
    private ImageView ivPreview;
    private AlertDialog alertDialog;
    private ImageView ivDialog;
    private boolean granted = false;

    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.camera_view);
        ivPreview = findViewById(R.id.image);

        ivPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.takePicture();
                mCameraView.stopScan();
            }
        });

        initMaskView();
        initScannerParams();
        initCameraView();

        new RxPermissions(this).request(Manifest.permission.CAMERA)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean granted) {
                        MainActivity.this.granted = granted;
                        if (granted) {
                            MaskView maskView = (MaskView) mCameraView.getMaskView();
                            maskView.setShowScanLine(true);
                            mCameraView.start();
                            mCameraView.startScan();
                        } else {
                            Toast.makeText(MainActivity.this, "????????????????????????", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void initScannerParams() {
        SmartScanner.DEBUG = true;
        /*
          canny ????????????
          1. ????????????1???????????????????????????????????????
          2. ????????????2????????????????????????????????????
          3. ?????????1?????????2??????????????????,?????????2??????????????????????????????????????????????????????????????????????????????????????????
         */
        SmartScanner.cannyThreshold1 = 20; //canny ????????????1
        SmartScanner.cannyThreshold2 = 50; //canny ????????????2
        /*
         * ??????????????????????????????
         * 1. threshold: ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         * 2. minLinLength: ??????????????????????????????????????????, ???????????????????????????????????????
         * 3. maxLineGap: ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         */
        SmartScanner.houghLinesThreshold = 130;
        SmartScanner.houghLinesMinLineLength = 80;
        SmartScanner.houghLinesMaxLineGap = 10;
        /*
         * ???????????????????????????????????????????????????????????????
         */
        SmartScanner.gaussianBlurRadius = 3;

        // ??????????????????, ???????????????????????????????????????????????????
        SmartScanner.detectionRatio = 0.1f;
        // ??????????????????????????????
        SmartScanner.checkMinLengthRatio = 0.8f;
        // ???????????????????????????????????????????????????????????????
        SmartScanner.maxSize = 300;
        // ??????????????????
        SmartScanner.angleThreshold = 5;
        // don't forget reload params
        SmartScanner.reloadParams();
    }

    private void initCameraView() {
        mCameraView.getSmartScanner().setPreview(true);
        mCameraView.setOnScanResultListener(new SmartCameraView.OnScanResultListener() {
            @Override
            public boolean onScanResult(SmartCameraView smartCameraView, int result, byte[] yuvData) {
                Bitmap previewBitmap = smartCameraView.getPreviewBitmap();
                if (previewBitmap != null) {
                    ivPreview.setImageBitmap(previewBitmap);
                }
//                if (result == 1) {
//                    Size pictureSize = smartCameraView.getPreviewSize();
//                    int rotation = smartCameraView.getPreviewRotation();
//                    Rect maskRect = mCameraView.getAdjustPreviewMaskRect();
//                    Bitmap bitmap = mCameraView.cropYuvImage(yuvData, pictureSize.getWidth(), pictureSize.getHeight(), maskRect, rotation);
//                    if (bitmap != null) {
//                        showPicture(bitmap);
//                    }
//                }
                return false;
            }
        });

        mCameraView.addCallback(new CameraImpl.Callback() {

            @Override
            public void onCameraOpened(CameraImpl camera) {
                super.onCameraOpened(camera);
            }

            @Override
            public void onPictureTaken(CameraImpl camera, byte[] data) {
                super.onPictureTaken(camera, data);
                mCameraView.cropJpegImage(data, new SmartCameraView.CropCallback() {
                    @Override
                    public void onCropped(Bitmap cropBitmap) {
                        if (cropBitmap != null) {
                            showPicture(cropBitmap);
                        }
                    }
                });
            }

        });
    }

    private void initMaskView() {
        final MaskView maskView = (MaskView) mCameraView.getMaskView();
        maskView.setMaskLineColor(0xff00adb5);
        maskView.setShowScanLine(false);
        maskView.setScanLineGradient(0xff00adb5, 0x0000adb5);
        maskView.setMaskLineWidth(2);
        maskView.setMaskRadius(5);
        maskView.setScanSpeed(6);
        maskView.setScanGradientSpread(80);
        mCameraView.post(new Runnable() {
            @Override
            public void run() {
                int width = mCameraView.getWidth();
                int height = mCameraView.getHeight();
                if (width < height) {
                    maskView.setMaskSize((int) (width * 0.6f), (int) (width * 0.6f / 0.63));
                    maskView.setMaskOffset(0, -(int)(width * 0.1));
                } else {
                    maskView.setMaskSize((int) (width * 0.6f), (int) (width * 0.6f * 0.63));
                }
            }
        });
        mCameraView.setMaskView(maskView);
    }

    private void showPicture(Bitmap bitmap) {
        if (alertDialog == null) {
            ivDialog = new ImageView(this);
            alertDialog = new AlertDialog.Builder(this).setView(ivDialog).create();
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mCameraView.startScan();
                }
            });
            Window window = alertDialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.color.colorTrans);
            }
        }
        ivDialog.setImageBitmap(bitmap);
        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // request Camera permission first!
        if (granted) {
            mCameraView.start();
            mCameraView.startScan();
        }
    }


    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        mCameraView.stopScan();
    }
}
