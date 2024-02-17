package com.example.opencvcamera;

import android.graphics.Point;
import android.os.Bundle;

import org.opencv.android.CameraActivity;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase openCvCameraView;
    private Mat mat;
    Mat im;
    CascadeClassifier faceDetector;
    MatOfRect faceDetections;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        //! [keep_screen]
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);

        openCvCameraView.setVisibility(SurfaceView.VISIBLE);

        openCvCameraView.setCvCameraViewListener(this);

//        faceDetector = new CascadeClassifier("haarcascade_frontalface_alt_tree.xml");
//        faceDetections = new MatOfRect();
//        im = Imgcodecs.imread("input.png");
//        int faceNum=0;
//        // カスケード分類器で顔探索
////        CascadeClassifier faceDetector = new CascadeClassifier("haarcascade_frontalface_alt_tree.xml");
//        MatOfRect faceDetections = new MatOfRect();
//        faceDetector.detectMultiScale(im, faceDetections);
//        System.out.println("a");
//        for (Rect rect : faceDetections.toArray()) {
//            faceNum++;
//            System.out.println("faceNum: " + faceNum);
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (openCvCameraView != null)
            openCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(openCvCameraView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mat = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mat.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mat = inputFrame.rgba();    //color
        //mat= inputFrame.gray();    //grayscale
        //Core.bitwise_not(inputFrame.rgba(), mat); //reversed
        //Core.bitwise_not(inputFrame.gray(), mat); //grayscale reversed
        //Imgproc.Canny(inputFrame.gray(), mat, 100, 200); //grayscale canny filtering
        //Imgproc.threshold(inputFrame.gray(), mat, 0.0, 255.0, Imgproc.THRESH_OTSU); //grayscale binarization with Ohtsu


        return mat;
    }
}