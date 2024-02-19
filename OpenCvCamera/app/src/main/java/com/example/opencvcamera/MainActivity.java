package com.example.opencvcamera;

import android.os.Bundle;
import android.view.WindowManager;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.FaceDetectorYN;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private final Scalar BOX_COLOR = new Scalar(0, 255, 0);
    private final Scalar RIGHT_EYE_COLOR = new Scalar(255, 0, 0);
    private final Scalar LEFT_EYE_COLOR = new Scalar(0, 0, 255);
    private final Scalar NOSE_TIP_COLOR = new Scalar(0, 255, 0);
    private final Scalar MOUTH_RIGHT_COLOR = new Scalar(255, 0, 255);
    private final Scalar MOUTH_LEFT_COLOR = new Scalar(0, 255, 255);

    private Mat rgbaMat;
    private Mat bgrMat;
    private Mat transposeRbga;
    private Mat transposeBgr;
    private Mat bgrScaledMat;
    private Size inputSize = null;
    private final float SCALE = 2.f;
    private FaceDetectorYN faceDetectorYN;
    private Mat facesMat;
    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initLocal()) return;
        byte[] buffer;
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.face_detection_yunet_2023mar);
            int size = inputStream.available();
            buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
        } catch (IOException e) {
            return;
        }
        MatOfByte modelBuffer = new MatOfByte(buffer);
        MatOfByte configBuffer = new MatOfByte();

        faceDetectorYN = FaceDetectorYN.create("onnx", modelBuffer, configBuffer, new Size(300, 300));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        openCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
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
        if (openCvCameraView != null) openCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(openCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        openCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        rgbaMat = new Mat();
        bgrMat = new Mat();
        bgrScaledMat = new Mat();
        facesMat = new Mat();
    }

    public void onCameraViewStopped() {
        rgbaMat.release();
        bgrMat.release();
        bgrScaledMat.release();
        facesMat.release();
    }

    public void visualize(Mat rgba, Mat faces) {
        int thickness = 2;
        float[] faceData = new float[faces.cols() * faces.channels()];
        for (int i = 0; i < faces.rows(); i++) {
            faces.get(i, 0, faceData);
            // Draw bounding box
            Imgproc.rectangle(rgba, new Rect(Math.round(SCALE * faceData[0]), Math.round(SCALE * faceData[1]), Math.round(SCALE * faceData[2]), Math.round(SCALE * faceData[3])), BOX_COLOR, thickness);
            // Draw landmarks
            Imgproc.circle(rgba, new Point(Math.round(SCALE * faceData[4]), Math.round(SCALE * faceData[5])), 2, RIGHT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(SCALE * faceData[6]), Math.round(SCALE * faceData[7])), 2, LEFT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(SCALE * faceData[8]), Math.round(SCALE * faceData[9])), 2, NOSE_TIP_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(SCALE * faceData[10]), Math.round(SCALE * faceData[11])), 2, MOUTH_RIGHT_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(SCALE * faceData[12]), Math.round(SCALE * faceData[13])), 2, MOUTH_LEFT_COLOR, thickness);
        }
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgbaMat = inputFrame.rgba();
        if (inputSize == null) {
            inputSize = new Size(Math.round(rgbaMat.cols() / SCALE), Math.round(rgbaMat.rows() / SCALE));
            faceDetectorYN.setInputSize(inputSize);
        }
        Imgproc.cvtColor(rgbaMat, bgrMat, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(bgrMat, bgrScaledMat, inputSize);
        faceDetectorYN.detect(bgrScaledMat, facesMat);
        visualize(rgbaMat, facesMat);

        return rgbaMat;
    }
}

