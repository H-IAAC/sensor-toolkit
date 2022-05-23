package br.org.eldorado.hiaac.datacollector;

import android.Manifest;
import android.app.Activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.util.ImageUtil;

public class PhotoCollectorActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "PhotoCollectorActivity";
    //private Camera mCamera;
    private SurfaceView mPreview;
    private ImageView mPhotoView;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private String mCameraID;

    private ImageView iv_show;
    private Handler childHandler, mainHandler;
    private ImageReader mImageReaderJPG;
    private ImageReader mImageReaderPreview;
    private CameraCaptureSession mCameraCaptureSession;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_collector);

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        mPhotoView = (ImageView)findViewById(R.id.photo);

        mPreview = (SurfaceView)findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        //mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onPause() {
        super.onPause();
        //mCamera.stopPreview();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mCamera.release();
        Log.d("CAMERA","Destroy");
    }

    public void onCancelClick(View v) {
        finish();
    }

    public void onSnapClick(View v) {
        //mCamera.takePicture(this, null, null, this);
        takePicture();
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        //Here, we chose internal storage
        try {
            FileOutputStream out = openFileOutput("picture.jpg", Activity.MODE_PRIVATE);
            out.write(data);
            out.flush();
            out.close();

            //camera.startPreview();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /*Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size selected = sizes.get(0);
        params.setPreviewSize(selected.width,selected.height);
        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();*/
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            initCamera2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//rear camera
        mImageReaderJPG = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG,1);
        mImageReaderJPG.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {//Temporary photos obtained by taking pictures can be processed here, for example, written to local
            @Override
            public void onImageAvailable(ImageReader reader) {
                //mCameraDevice.close();
                mPreview.setVisibility(View.GONE);
                //First verify if the phone has sdcard
                /*String status = Environment.getExternalStorageState();
                if (!status.equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(getApplicationContext(), "Your sd card is not available.", Toast.LENGTH_SHORT).show();
                    return;
                }*/
                //Get captured photo data
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                //Mobile phone photos are stored in this path
                String filePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/";
                String picturePath = System.currentTimeMillis() + ".jpg";
                File file = new File(filePath, picturePath);
                try {
                    //Save to local album
                    /*FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(data);
                    fileOutputStream.close();*/
                    mPhotoView.setVisibility(View.VISIBLE);
                    //display image
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    mPhotoView.setImageBitmap(bitmap);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        mPhotoView.setRotation(90);
                    }
                /*} catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();*/
                } finally {
                    image.close();
                }

            }
        }, mainHandler);
        mImageReaderPreview = ImageReader.newInstance(1080, 1920, ImageFormat.YUV_420_888,1);
        mImageReaderPreview.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {//Temporary photos obtained by taking pictures can be processed here, for example, written to local
            @Override
            public void onImageAvailable(ImageReader reader) {

                //Get captured photo data
                Image image = reader.acquireNextImage();
                //Log.i(TAG,"image format: "+image.getFormat());
                //Get three planes from the image
                Image.Plane[] planes = image.getPlanes();

                for (int i = 0; i <planes.length; i++) {
                    ByteBuffer iBuffer = planes[i].getBuffer();
                    int iSize = iBuffer.remaining();
                    /*Log.i(TAG, "pixelStride "+ planes[i].getPixelStride());
                    Log.i(TAG, "rowStride "+ planes[i].getRowStride());
                    Log.i(TAG, "width "+ image.getWidth());
                    Log.i(TAG, "height "+ image.getHeight());
                    Log.i(TAG, "Finished reading data from plane "+ i);*/
                }
                int n_image_size = image.getWidth()*image.getHeight()*3/2;
                final byte[] yuv420pbuf = new byte[n_image_size];
                System.arraycopy(ImageUtil.getBytesFromImageAsType(image, 0), 0, yuv420pbuf, 0, n_image_size);

                image.close();
            }
        }, null);

        //Get camera management
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //Turn on the camera
            mCameraManager.openCamera(mCameraID, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//Open the camera
            mCameraDevice = camera;
            //Open preview
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//Close the camera
            if (null != mCameraDevice) {
                mCameraDevice.close();
                PhotoCollectorActivity.this.mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {//An error occurred
            //Toast.makeText(MainActivity.this, "Camera opening failed", Toast.LENGTH_SHORT).show();
        }
    };

    private void takePreview() {
        try {
            //Create CaptureRequest.Builder needed for preview
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //Use the surface of SurfaceView as the target of CaptureRequest.Builder
            previewRequestBuilder.addTarget(mPreview.getHolder().getSurface());
            previewRequestBuilder.addTarget(mImageReaderPreview.getSurface());
            //Create CameraCaptureSession, which is responsible for managing and processing preview requests and camera requests
            mCameraDevice.createCaptureSession(Arrays.asList(mPreview.getHolder().getSurface(), mImageReaderPreview.getSurface(), mImageReaderJPG.getSurface()), new CameraCaptureSession.StateCallback()//③
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    //When the camera is ready, start to display the preview
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        //auto focus
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //Turn on the flash
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        //Show preview
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(PhotoCollectorActivity.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (mCameraDevice == null) {
            return;
        }
        //Create CaptureRequest.Builder needed for taking pictures
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //Use the surface of imageReader as the target of CaptureRequest.Builder
            captureRequestBuilder.addTarget(mImageReaderJPG.getSurface());
            //auto focus
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //auto exposure
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //Get the phone direction
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //Calculate the direction of the photo according to the device
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //Photograph
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG,"surfaceDestroyed");
    }
}
