package com.vinda.ucvcamera;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usbcameracommon.UvcCameraDataCallBack;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;
import com.yuan.camera.R;

import java.io.File;
import java.util.List;

/**
 * 显示多路摄像头
 */
public class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;
    private static final String TAG = "MainActivity";

    private static final float[] BANDWIDTH_FACTORS = {0.5f, 0.5f};
    UvcCameraDataCallBack firstDataCallBack = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data) {
            Log.v(TAG, "数据回调:" + data.length);
        }
    };
    UvcCameraDataCallBack firstDataCallBack2 = new UvcCameraDataCallBack() {
        @Override
        public void getData(byte[] data) {
            Log.v(TAG, "数据回调2:" + data.length);
        }
    };
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCameraHandler mHandlerFirst;
    private CameraViewInterface mUVCCameraViewFirst;
    private ImageButton mCaptureButtonFirst;
    private Surface mFirstPreviewSurface;
    private UVCCameraHandler mHandlerSecond;
    private CameraViewInterface mUVCCameraViewSecond;
    private Surface mSecondPreviewSurface;
    private List<UsbDevice> usbDeviceList;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                default:
                    break;

                case R.id.camera_view_first:
                    if (mHandlerFirst != null && mHandlerSecond != null) {
                        if (!mHandlerFirst.isOpened() && !mHandlerSecond.isOpened()) {
                            //                CameraDialog.showDialog(MainActivity.this);
                            mUSBMonitor.requestPermission(usbDeviceList.get(0));
                            mUSBMonitor.requestPermission(usbDeviceList.get(1));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setCameraButton();
                                }
                            }, 0);
                        } else {
                            mHandlerFirst.close();
                            mHandlerSecond.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.capture_button_first:
                    if (mHandlerFirst != null && mHandlerSecond != null) {
                        if (mHandlerFirst.isOpened() && mHandlerSecond.isOpened()) {
                            //检查是否有读写sd权限以及录制权限
                            if (checkPermissionWriteExternalStorage()) {
                                mCaptureButtonFirst.setColorFilter(0xffff0000);  // turn red
                                //                                    mHandlerFirst.startRecording();
                                // 拍照
                                String picPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + File.separator + System.currentTimeMillis()
                                        + ".png";
                                mHandlerFirst.captureStill(picPath);
                                mHandlerSecond.captureStill(picPath);
                            }
                            mCaptureButtonFirst.setColorFilter(0);    // return to default color
                            //                                mHandlerFirst.stopRecording();
                        }
                    }
                    break;
                case R.id.camera_view_second:
                    if (mHandlerSecond != null) {
                        if (!mHandlerSecond.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mHandlerSecond.close();
                            setCameraButton();
                        }
                    }
                    break;
            }
        }
    };
    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE:" + device.getDeviceName(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            //设备连接成功
            Log.v(TAG, "onConnect:" + device);
            if (!mHandlerFirst.isOpened()) {
                Log.v(TAG, "mHandlerFirst.isOpened false" );
                mHandlerFirst.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewFirst.getSurfaceTexture();
                mHandlerFirst.startPreview(new Surface(st));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureButtonFirst.setVisibility(View.VISIBLE);
                        Log.v(TAG, "mCaptureButtonFirst VISIBLE" );
                    }
                });
            } else if (!mHandlerSecond.isOpened()) {
                mHandlerSecond.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraViewSecond.getSurfaceTexture();
                mHandlerSecond.startPreview(new Surface(st));
                Log.v(TAG, "mHandlerSecond startPreview" );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //                        mCaptureButtonSecond.setVisibility(View.VISIBLE);
                    }
                });
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            Log.v(TAG, "onDisconnect:" + device);
            if (ctrlBlock.getDeviceName().equals(usbDeviceList.get(0).getDeviceName())) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run:关闭1");
                        mHandlerFirst.close();
                        if (mFirstPreviewSurface != null) {
                            mFirstPreviewSurface.release();
                            mFirstPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if (ctrlBlock.getDeviceName().equals(usbDeviceList.get(1).getDeviceName())) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerSecond.close();
                        Log.i(TAG, "run:关闭2");
                        if (mSecondPreviewSurface != null) {
                            mSecondPreviewSurface.release();
                            mSecondPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            }
            //            if (!mHandlerFirst.isEqual(device)) {
            //                queueEvent(new Runnable() {
            //                    @Override
            //                    public void run() {
            //                        Log.i(TAG, "run:关闭1");
            //                        mHandlerFirst.close();
            //                        if (mFirstPreviewSurface != null) {
            //                            mFirstPreviewSurface.release();
            //                            mFirstPreviewSurface = null;
            //                        }
            //                        setCameraButton();
            //                    }
            //                }, 0);
            //            } else if (!mHandlerSecond.isEqual(device)) {
            //                queueEvent(new Runnable() {
            //                    @Override
            //                    public void run() {
            //                        mHandlerSecond.close();
            //                        Log.i(TAG, "run:关闭2");
            //                        if (mSecondPreviewSurface != null) {
            //                            mSecondPreviewSurface.release();
            //                            mSecondPreviewSurface = null;
            //                        }
            //                        setCameraButton();
            //                    }
            //                }, 0);
            //            }
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Log.v(TAG, "onDettach:" + device);
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            Log.v(TAG, "onCancel:");
        }
    };
    private ToggleButton mBtnOpen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_view_camera);
        initView();
        findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        resultFirstCamera();
        resultSecondCamera();
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        usbDeviceList = mUSBMonitor.getDeviceList(filter.get(0));


    }

    /**
     * 带有回调数据的初始化
     */
    private void resultFirstCamera() {
        mUVCCameraViewFirst = (CameraViewInterface) findViewById(R.id.camera_view_first);
        //设置显示宽高
        mUVCCameraViewFirst.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        ((UVCCameraTextureView) mUVCCameraViewFirst).setOnClickListener(mOnClickListener);
        mCaptureButtonFirst = (ImageButton) findViewById(R.id.capture_button_first);
        mCaptureButtonFirst.setOnClickListener(mOnClickListener);
        mCaptureButtonFirst.setVisibility(View.INVISIBLE);
        mHandlerFirst = UVCCameraHandler.createHandler(this, mUVCCameraViewFirst
                , UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT
                , BANDWIDTH_FACTORS[0], firstDataCallBack);
    }

    private void resultSecondCamera() {
        mUVCCameraViewSecond = (CameraViewInterface) findViewById(R.id.camera_view_second);
        mUVCCameraViewSecond.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        ((UVCCameraTextureView) mUVCCameraViewSecond).setOnClickListener(mOnClickListener);
        mHandlerSecond = UVCCameraHandler.createHandler(this, mUVCCameraViewSecond, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1]);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        if (mUVCCameraViewSecond != null) {
            mUVCCameraViewSecond.onResume();
        }
        if (mUVCCameraViewFirst != null) {
            mUVCCameraViewFirst.onResume();
        }
    }

    @Override
    protected void onStop() {
        mHandlerFirst.close();
        if (mUVCCameraViewFirst != null) {
            mUVCCameraViewFirst.onPause();
        }
        mCaptureButtonFirst.setVisibility(View.INVISIBLE);

        mHandlerSecond.close();
        if (mUVCCameraViewSecond != null) {
            mUVCCameraViewSecond.onPause();
        }

        mUSBMonitor.unregister();//usb管理器解绑
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mHandlerFirst != null) {
            mHandlerFirst = null;
        }
        if (mHandlerSecond != null) {
            mHandlerSecond = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraViewFirst = null;
        mCaptureButtonFirst = null;

        mUVCCameraViewSecond = null;
        super.onDestroy();
    }

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setCameraButton();
                }
            }, 0);
        }
    }

    private void setCameraButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((mHandlerFirst != null) && !mHandlerFirst.isOpened() && (mCaptureButtonFirst != null) && (mHandlerSecond != null) && !mHandlerSecond.isOpened()) {
                    mCaptureButtonFirst.setVisibility(View.INVISIBLE);
                }
                //                if ((mHandlerSecond != null) && !mHandlerSecond.isOpened() && (mCaptureButtonSecond != null)) {
                //                    mCaptureButtonSecond.setVisibility(View.INVISIBLE);
                //                }
            }
        }, 0);
    }

    private void initView() {
        mBtnOpen = (ToggleButton) findViewById(R.id.btn_open);
        mBtnOpen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mHandlerFirst != null && mHandlerSecond != null) {
                    if (!mHandlerFirst.isOpened() && !mHandlerSecond.isOpened()) {
                        Log.i(TAG, "onCheckedChanged:  关闭状态");
                        //                CameraDialog.showDialog(MainActivity.this);
                        mUSBMonitor.requestPermission(usbDeviceList.get(0));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCameraButton();
                            }
                        }, 0);
                        SystemClock.sleep(100);
                        mUSBMonitor.requestPermission(usbDeviceList.get(1));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCameraButton();
                            }
                        }, 0);
                    } else {
                        mHandlerFirst.close();
                        mHandlerSecond.close();
                        setCameraButton();
                    }
                }
            }
        });
    }

}
