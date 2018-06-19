package com.flipcam.cameramanager;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;

import com.flipcam.camerainterface.CameraOperations;

import java.util.List;

public class Camera2Manager implements CameraOperations {

    private CameraDevice cameraDevice;
    private int cameraOrientation;
    private int cameraId;
    private CameraCharacteristics cameraCharacteristics;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;
    public final String TAG = "Camera2Manager";
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    private static Camera2Manager camera2Manager;
    public static Camera2Manager getInstance()
    {
        if(camera2Manager == null){
            camera2Manager = new Camera2Manager();
        }
        return camera2Manager;
    }

    @Override
    public int[] getPreviewSizes()
    {
        int[] sizes = new int[2];
        sizes[0] = VIDEO_WIDTH;
        sizes[1] = VIDEO_HEIGHT;
        return sizes;
    }

    @Override
    public Camera.CameraInfo getCameraInfo()
    {
        return null;
    }

    @Override
    public List<Camera.Size> getSupportedPictureSizes() {
        return null;
    }

    @Override
    public void openCamera(boolean backCamera, Context context) {
        CameraManager cameraManager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
             for (String camId : cameraManager.getCameraIdList()) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(camId);
                cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                 if (backCamera) {
                     if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = Integer.parseInt(camId);
                         break;
                    }
                }
                else{
                     if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraId = Integer.parseInt(camId);
                         break;
                    }
                }
            }
        }
        catch (CameraAccessException ac){
            ac.printStackTrace();
        }
    }
}
