/*
 *  CameraSurface.java
 *  ARToolKit5
 *
 *  Disclaimer: IMPORTANT:  This Daqri software is supplied to you by Daqri
 *  LLC ("Daqri") in consideration of your agreement to the following
 *  terms, and your use, installation, modification or redistribution of
 *  this Daqri software constitutes acceptance of these terms.  If you do
 *  not agree with these terms, please do not use, install, modify or
 *  redistribute this Daqri software.
 *
 *  In consideration of your agreement to abide by the following terms, and
 *  subject to these terms, Daqri grants you a personal, non-exclusive
 *  license, under Daqri's copyrights in this original Daqri software (the
 *  "Daqri Software"), to use, reproduce, modify and redistribute the Daqri
 *  Software, with or without modifications, in source and/or binary forms;
 *  provided that if you redistribute the Daqri Software in its entirety and
 *  without modifications, you must retain this notice and the following
 *  text and disclaimers in all such redistributions of the Daqri Software.
 *  Neither the name, trademarks, service marks or logos of Daqri LLC may
 *  be used to endorse or promote products derived from the Daqri Software
 *  without specific prior written permission from Daqri.  Except as
 *  expressly stated in this notice, no other rights or licenses, express or
 *  implied, are granted by Daqri herein, including but not limited to any
 *  patent rights that may be infringed by your derivative works or by other
 *  works in which the Daqri Software may be incorporated.
 *
 *  The Daqri Software is provided by Daqri on an "AS IS" basis.  DAQRI
 *  MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 *  THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE, REGARDING THE DAQRI SOFTWARE OR ITS USE AND
 *  OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
 *
 *  IN NO EVENT SHALL DAQRI BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 *  MODIFICATION AND/OR DISTRIBUTION OF THE DAQRI SOFTWARE, HOWEVER CAUSED
 *  AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 *  STRICT LIABILITY OR OTHERWISE, EVEN IF DAQRI HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  Copyright 2015 Daqri, LLC.
 *  Copyright 2011-2015 ARToolworks, Inc.
 *
 *  Author(s): Julian Looser, Philip Lamb
 *
 */

package org.artoolkit.ar.samples.nftSimple;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;

public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraSurface";
    private Camera camera;

    private boolean mustAskPermissionFirst = false;
    public boolean gettingCameraAccessPermissionsFromUser()
    {
        return mustAskPermissionFirst;
    }

    public void resetGettingCameraAccessPermissionsFromUserState()
    {
        mustAskPermissionFirst = false;
    }

    @SuppressWarnings("deprecation")
    public CameraSurface(Context context) {
        super(context);
        Log.i(TAG, "CameraSurface(): ctor called");
        Activity activityRef = (Activity)context;

        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                                                                           activityRef,
                                                                           Manifest.permission.CAMERA))
                {
                    mustAskPermissionFirst = true;
                    if (activityRef.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    {
                        // Will drop in here if user denied permissions access camera before.
                        // Or no uses-permission CAMERA element is in the
                        // manifest file. Must explain to the end user why the app wants
                        // permissions to the camera devices.
                        Toast.makeText(activityRef.getApplicationContext(),
                                       "App requires access to camera to be granted",
                                       Toast.LENGTH_SHORT).show();
                    }
                    // Request permission from the user to access the camera.
                    Log.i(TAG, "CameraSurface(): must ask user for camera access permission");
                    activityRef.requestPermissions(new String[]
                                                       {
                                                           Manifest.permission.CAMERA
                                                       },
                                                   nftSimpleActivity.REQUEST_CAMERA_PERMISSION_RESULT);
                    return;
                }
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "CameraSurface(): exception caught, " + ex.getMessage());
            return;
        }

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Deprecated in API level 11. Still required for API levels <= 10.
    }

    // SurfaceHolder.Callback methods

    @SuppressLint("NewApi")
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolderInstance) {
        int cameraIndex = Integer.parseInt(PreferenceManager.
                                      getDefaultSharedPreferences(getContext()).
                                           getString("pref_cameraIndex", "0"));
        Log.i(TAG, "surfaceCreated(): called, will attempt open camera \"" + cameraIndex +
                       "\", set orientation, set preview surface");
        openCamera(surfaceHolderInstance, cameraIndex);
    }

    private void openCamera(SurfaceHolder surfaceHolderInstance, int cameraIndex) {
        Log.i(TAG, "openCamera(): called");
        try
        {
            camera = Camera.open(cameraIndex);
        }
        catch (RuntimeException ex) {
            Log.e(TAG, "openCamera(): RuntimeException caught, " + ex.getMessage() + ", abnormal exit");
            return;
        }
        //catch (CameraAccessException ex) {
        //      Log.e(TAG, "openCamera(): CameraAccessException caught, " + ex.getMessage() + ", abnormal exit");
        //      return;
        //  }
        catch (Exception ex) {
            Log.e(TAG, "openCamera()): exception caught, " + ex.getMessage() + ", abnormal exit");
            return;
        }

        if (!SetOrientationAndPreview(surfaceHolderInstance, cameraIndex)) {
            Log.e(TAG, "openCamera(): call to SetOrientationAndPreview() failed, openCamera() failed");
        }
        else
            Log.i(TAG, "openCamera(): succeeded");
    }

    private boolean SetOrientationAndPreview(SurfaceHolder surfaceHolderInstance, int cameraIndex)
    {
        Log.i(TAG, "SetOrientationAndPreview(): called");
        boolean success = true;
        try {
            //setCameraDisplayOrientation(cameraIndex, camera);
            camera.setPreviewDisplay(surfaceHolderInstance);
            camera.setPreviewCallbackWithBuffer(this); // API level 8 (Android 2.2)
        }
        catch (IOException ex) {
            Log.e(TAG, "SetOrientationAndPreview(): IOException caught, " + ex.toString());
            success = false;
        }
        catch (Exception ex) {
            Log.e(TAG, "SetOrientationAndPreview(): Exception caught, " + ex.toString());
            success = false;
        }
        if (!success)
        {
            if (null != camera)
            {
                camera.release();
                camera = null;
            }
            Log.e(TAG, "SetOrientationAndPreview(): released camera due to caught exception");
        }
        return success;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (camera != null) {
            Log.i(TAG, "surfaceDestroyed(): closing camera");
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    @SuppressLint("NewApi") // CameraInfo
    @SuppressWarnings("deprecation") // setPreviewFrameRate
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (camera != null) {

            String camResolution = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraResolution", getResources().getString(R.string.pref_defaultValue_cameraResolution));
            String[] dims = camResolution.split("x", 2);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
            parameters.setPreviewFrameRate(30);
            camera.setParameters(parameters);

            parameters = camera.getParameters();
            int capWidth = parameters.getPreviewSize().width;
            int capHeight = parameters.getPreviewSize().height;
            int pixelformat = parameters.getPreviewFormat(); // android.graphics.imageformat
            PixelFormat pixelinfo = new PixelFormat();
            PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);
            int cameraIndex = 0;
            boolean frontFacing = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraIndex = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraIndex", "0"));
                Camera.getCameraInfo(cameraIndex, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) frontFacing = true;
            }

            int bufSize = capWidth * capHeight * pixelinfo.bitsPerPixel / 8; // For the default NV21 format, bitsPerPixel = 12.

            for (int i = 0; i < 5; i++) camera.addCallbackBuffer(new byte[bufSize]);

            camera.startPreview();

            nftSimpleActivity.nativeVideoInit(capWidth, capHeight, cameraIndex, frontFacing);
        }
    }

    // Camera.PreviewCallback methods.
    @Override
    public void onPreviewFrame(byte[] data, Camera cam) {

        nftSimpleActivity.nativeVideoFrame(data);

        cam.addCallbackBuffer(data);
    }
} // end: public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
