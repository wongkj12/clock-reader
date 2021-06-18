package com.example.clockreaderopencv3410;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;

    //Basically loads OpenCV, checks if it's connected properly
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        };
    };

    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        javaCameraView = (JavaCameraView) findViewById(R.id.CameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(MainActivity.this);

        tv = (TextView) findViewById(R.id.text1);
        tv.setText("Hello!");
        }

    boolean startCrop = false;
    boolean startWarp = false;
    boolean startDil = false;
    boolean startThresh = false;

    public void set_crop(View v) {
        startCrop = true;
        startWarp = false;
        startDil = false;
        startThresh = false;
    }

    public void set_warp(View v) {
        startCrop = false;
        startWarp = true;
        startDil = false;
        startThresh = false;
    }

    public void set_dil(View v) {
        startCrop = false;
        startWarp = false;
        startDil = true;
        startThresh = false;
    }

    public void set_thresh(View v) {
        startCrop = false;
        startWarp = false;
        startDil = false;
        startThresh = true;
    }

    public void set_default(View v) {
        startCrop = false;
        startWarp = false;
        startDil = false;
        startThresh = false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    //to handle setText outside of main thread
    private void setText(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private double abs_diff(double x, double y){
        return Math.abs(x-y);
    }

    //onCameraFrame is a callback
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

        Mat frame_in = frame.clone();

        Mat circles = new Mat();

        Mat img_crop = new Mat();

        Mat warped = new Mat();

        Mat dilated = new Mat();

        Mat threshed = new Mat();

        /*int thresh = 100;
        int maxValue = 255;
        Imgproc.threshold(gray, gray, thresh, maxValue, 1); //inverse binary threshold?
        //Finding contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //Sorting by max area contours
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null && o2 != null) {
                    return 1;
                }
                if (o1 != null && o2 != null) {
                    return -1;
                }
                return (int) (Imgproc.contourArea(o1) - Imgproc.contourArea(o2));
            }
        });

        Imgproc.drawContours(frame, contours, 0, new Scalar(0, 255, 0), -1, Core.LINE_8);
        Rect rect = Imgproc.boundingRect(contours.get(0));
        Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(255, 0, 0), 3);
        R = rect.br().x;
        return frame;*/

        //if (startDetect) {
            Imgproc.cvtColor(frame_in, frame_in, Imgproc.COLOR_RGB2GRAY);
            Imgproc.medianBlur(frame_in, frame_in, 5);

            Imgproc.HoughCircles(frame_in, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                    (double) frame_in.rows() / 16, // change this value to detect circles with different distances to each other
                    100.0, 30.0, 50, 100); // change the last two parameters
            // (min_radius & max_radius) to detect larger circles

            double c[] = circles.get(0, 0); //c is the best circle detected
            if (c != null) {
                Point center = new Point(Math.round(c[0]), Math.round(c[1]));
                // circle center
                //***ADDING WHITE CIRCLE AT CENTRE HELPS TO FILTER OUT AND SEPERATE CLOCK HANDS AFTER DOING BINARY THRESHOLDING!!!
                Imgproc.circle(frame, center, 2, new Scalar(2555, 255, 255), 5, 8, 0);
                // circle outline
                int radius = (int) Math.round(c[2]);
                Imgproc.circle(frame, center, radius, new Scalar(255, 0, 255), 3, 8, 0);

                //start process of detecting clock hands
                int c_x = (int) Math.round(c[0]);
                int c_y = (int) Math.round(c[1]);

                if (c_x - radius >= 0 && c_x + radius <= frame.cols() && c_y - radius >= 0 && c_y + radius <= frame.rows()){
                    Rect rectCrop = new Rect(c_x - radius, c_y - radius, 2 * radius, 2 * radius);
                    Imgproc.rectangle(frame,new Point(c_x-radius,c_y-radius),new Point(c_x+radius,c_y+radius),new Scalar(0,255,0), 3);
                    img_crop = frame.submat(rectCrop);


                    //WarpPolar
                    Imgproc.warpPolar(img_crop, warped, new Size(0, 0), new Point(radius, radius), radius, Imgproc.WARP_POLAR_LINEAR);
                    int kernel_width = (int) Math.round(0.15 * warped.cols());

                    Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(kernel_width, 1));
                    Imgproc.dilate(warped, dilated, element);

                    int thresh = 100;
                    int maxValue = 255;
                    Imgproc.cvtColor(dilated, gray, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.threshold(gray, threshed, thresh, maxValue, 1); //inverse binary threshold?

                    //Finding contours
                    List<MatOfPoint> contours = new ArrayList<>();
                    Mat hierarchy = new Mat();
                    Imgproc.findContours(threshed, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

                    //Imgproc.drawContours(gray,contours,-1,new Scalar(0,255,0),-1,Core.LINE_8);

                    //Sorting by max area contours
                    //Sorting by max area contours
                    Collections.sort(contours, new Comparator<MatOfPoint>() {
                        @Override
                        public int compare(MatOfPoint o1, MatOfPoint o2) {
                            if (o1 == null && o2 == null) {
                                return 0;
                            }
                            if (o1 == null) {
                                return 1;
                            }
                            if (o2 == null) {
                                return -1;
                            }
                            return (int) (Imgproc.contourArea(o2) - Imgproc.contourArea(o1));
                        }
                    });

                    if (contours.size() >= 2) {
                        //finding extreme x-values of the biggest 2 contours
                        Rect rect0 = Imgproc.boundingRect(contours.get(0));
                        double R0 = rect0.br().x;
                        Rect rect1 = Imgproc.boundingRect(contours.get(1));
                        double R1 = rect1.br().x;

                        //Imgproc.drawContours(dilated,contours,0,new Scalar(0,255,0),-1,Core.LINE_8);
                        //Imgproc.drawContours(dilated,contours,1,new Scalar(0,255,0),-1,Core.LINE_8);
                        //Imgproc.rectangle(dilated,rect0.tl(),rect0.br(),new Scalar(0,255,0),-1);
                        //Imgproc.rectangle(dilated,rect1.tl(),rect1.br(),new Scalar(0,255,0),-1);


                        Rect minute = new Rect();
                        Rect hour = new Rect();

                        if (R0 >= R1){
                            minute = rect0;
                            hour = rect1;
                        }
                        else{
                            minute = rect1;
                            hour = rect0;
                        }

                        double h_y = (hour.br().y + hour.tl().y)/2;
                        double m_y = (minute.br().y + minute.tl().y)/2;
                        int img_y = warped.rows();
                        double h_val = (h_y/img_y * 12 + 3) % 12;
                        double m_val = ((m_y/img_y * 12 + 3)%12)*5;

                        //Correct for errors by considering the minute hand position to deduce relative hour hand position
                        double rel = m_val/60;
                        if (abs_diff(h_val,Math.floor(h_val)+rel) < abs_diff(h_val,Math.ceil(h_val)+rel))
                        {
                            h_val = Math.floor(h_val);
                            h_val = Math.ceil(h_val);
                            m_val = Math.floor(m_val);
                        }

                        //Log.e(TAG,"Hours: " + Double.toString(h_val) + " Minutes: " + Double.toString(m_val));

                        //Drawing clock hands
                        double h_angle =  2*Math.PI - 2*Math.PI*(h_y/img_y);
                        double m_angle = 2*Math.PI - 2*Math.PI*(m_y/img_y);
                        double r_h = 0.2*img_crop.cols();
                        double r_m = 0.4*img_crop.cols();


                        Imgproc.line(frame, center, new Point(center.x + r_h*Math.cos(h_angle), center.y - r_h*Math.sin(h_angle)), new Scalar(255,255,0), 10);
                        Imgproc.line(frame, center, new Point(center.x + r_m*Math.cos(m_angle), center.y - r_m*Math.sin(m_angle)), new Scalar(0,165,265), 10);

                        String m,h;
                        if ((int)Math.round(m_val) < 10){
                            m = "0" + Integer.toString((int)Math.round(m_val));
                        }
                        else
                            m = Integer.toString((int)Math.round(m_val));
                        h = Integer.toString((int)Math.round(h_val));
                        setText(tv, h + ":" + m + " PM");

                    }
                }
            }
        //}
        Mat finish = new Mat();

        if(startCrop){
            if(!img_crop.empty()){
                Imgproc.resize(img_crop,finish,new Size(frame.cols(),frame.rows()));
                return finish;
            }
            else
                return frame;
        }

        if (startWarp){
            if (!warped.empty()) {
                Imgproc.resize(warped, finish, new Size(frame.cols(), frame.rows()));
                return finish;
            }
            else
                return frame;
        }

        if(startDil) {
            if (!dilated.empty()) {
                Imgproc.resize(dilated, finish, new Size(frame.cols(), frame.rows()));
                return finish;
                }
            else
                return frame;
        }

        if(startThresh) {
            if (!threshed.empty()) {
                Imgproc.resize(threshed, finish, new Size(frame.cols(), frame.rows()));
                return finish;
            }
            else
                return frame;
        }

        return frame;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override

    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            Log.d(TAG,"OpenCV is connected successfully.");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
        else {
            Log.d(TAG,"wtf opencv not working");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
        }
    }
}
