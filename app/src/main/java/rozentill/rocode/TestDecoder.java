package rozentill.rocode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.util.List;
/**
 * Created by yaoyuan on 16/5/28.
 */
public class TestDecoder extends Activity implements SurfaceHolder.Callback { // 定义对象
    private SurfaceView mSurfaceview = null; // SurfaceView 对象:(视图组件)视频 显示
    private SurfaceHolder mSurfaceHolder = null; // SurfaceHolder 对象:(抽象接口)SurfaceView 支持类
    private Camera mCamera = null; // Camera 对象,相机预览
    private boolean bIfPreview = false, storeFlag = true, stored = false;
    private int mPreviewWidth = 720, mPreviewHeight = 1280, colorNum, unitC;
    //private int mPreviewWidth = 480, mPreviewHeight = 800, colorNum, unitC;
    //用于接收解码好的数据
    private Handler uiHandler;
    private Bundle mBundle;
    private boolean isFirstFrame = true;
    private byte[] tmpBytes;
    private int frameNum, totalFrm, byteLenth, frameCnt = 0;
    // Map<Integer, byte[]> myMap = new SparseArray<Integer, byte[]>();
// SparseArray<int[]> myMap = new SparseArray<int[]>();
    SparseArray<byte[]> myMap = new SparseArray<byte[]>();
    static Context context;

    // InitSurfaceView
    private void initSurfaceView() {
        mSurfaceview = (SurfaceView) this.findViewById(R.id.preview_view);
        mSurfaceHolder = mSurfaceview.getHolder(); // 绑定 SurfaceView,取得 SurfaceHolder 对象
        mSurfaceHolder.addCallback(TestDecoder.this); // SurfaceHolder 加入回调接
//mSurfaceHolder.setFixedSize(720, 1280); // 预览大小設置
// mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 設置顯 示器類型,setType 必须设置
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //无 title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.decoder);
        initSurfaceView();
    }

    /*【SurfaceHolder.Callback 回调函数】*/
    public void surfaceCreated(SurfaceHolder holder) {
// SurfaceView 启动时/初次实例化,预览界面被创建时,该方法被调用。
        mCamera = Camera.open();// 开启摄像头(2.3 版本后支持多摄像头,需传入参数)
        try {
            Log.i("TAG", "SurfaceHolder.Callback:surface Created");
            mCamera.setPreviewDisplay(mSurfaceHolder);//set the surface to be used for live preview
        } catch (Exception ex) {
            if (null != mCamera) {
                mCamera.release();
                mCamera = null;
            }
            Log.i("TAG" + "initCamera", ex.getMessage());
        }
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                int previewWidth = camera.getParameters().getPreviewSize().width;
                int previewHeight = camera.getParameters().getPreviewSize().height;
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, previewWidth, previewHeight, 0, 0, previewWidth, previewHeight, false);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Reader reader = new QRCodeReader();
                try {
                    Result result = reader.decode(bitmap);
                    String text = result.getText();
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result.toString());
                    intent.putExtras(bundle);
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 当 SurfaceView/预览界面的格式和大小发生改变时,该方法被调用
        Log.i("TAG", "SurfaceHolder.Callback:Surface Changed");
        //mPreviewHeight = height;
        //mPreviewWidth = width;
        initCamera();
        mCamera.cancelAutoFocus();//只有加上了这一句,才会自动对焦。
    }

    public void surfaceDestroyed(SurfaceHolder holder) // SurfaceView 销毁时,该方法被调用
    {
        Log.i("TAG", "SurfaceHolder.Callback:Surface Destroyed");
        if (null != mCamera) {
            mCamera.setPreviewCallback(null); //!!这个必须在前,不然退出出错
            mCamera.stopPreview();
            bIfPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }

    /*【2】【相机预览】*/
    private void initCamera() {
        //surfaceChanged 中调用

        Log.i("TAG", "going into initCamera");
        if (bIfPreview) {
            mCamera.stopPreview();//stopCamera();
        }
        if (null != mCamera) {
            try {
/* Camera Service settings*/
                Camera.Parameters parameters = mCamera.getParameters();
//            parameters.setFlashMode("off");// 无闪光灯
                parameters.setPictureFormat(PixelFormat.JPEG);
                // Sets the image format for picture 设定相片格式为 JPEG,默认为 NV21
                parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP); //Sets the image format for preview picture,默认为 NV21
                // /*【ImageFormat】JPEG/NV16(YCrCb format,used for Video)/NV21(YCrCb format,used for Image)/RGB_565/YUY2/YU12*/
                List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
                List<String> focusModes = parameters.getSupportedFocusModes();
                Log.i("TAG" + "initCamera", "cyy support parameters is ");
                Camera.Size psize = null;
                for (int i = 0; i < pictureSizes.size(); i++) {
                    psize = pictureSizes.get(i);
                    Log.i("TAG" + "initCamera", "PictrueSize,width: " + psize.width + " height" + psize.height);
                }
                for (int i = 0; i < previewSizes.size(); i++) {
                    psize = previewSizes.get(i);
                    Log.i("TAG" + "initCamera", "PreviewSize,width: " + psize.width + " height" + psize.height);
                }
                Integer pf = null;
                for (int i = 0; i < previewFormats.size(); i++) {
                    pf = previewFormats.get(i);
                    Log.i("TAG" + "initCamera", "previewformates:" + pf);
                }
                String fm;
                for (int i = 0; i < focusModes.size(); i++) {
                    fm = focusModes.get(i);
                    Log.i("TAG" + "initCamera", "previewformates:" + fm);
                }
                List<Integer> supportedPreviewFrameRates =
                        mCamera.getParameters().getSupportedPreviewFrameRates();
                int fr;
                for (int i = 0; i < supportedPreviewFrameRates.size(); i++) {
                    fr = supportedPreviewFrameRates.get(i);
                    Log.i("TAG" + "initCamera", "previewformates:" + fr);
                }
                List<int[]> range = mCamera.getParameters().getSupportedPreviewFpsRange(); // Log.i("TAG", "range:"+range.size());
                for (int j = 0; j < range.size(); j++) {
                    int[] r = range.get(j);
                    for (int k = 0; k < r.length; k++) {
                        Log.i("TAG" + "initCamera", "supportedPreviewFps:" + r[k]);
                    }
                }
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                parameters.setPictureSize(1280, 720);
                parameters.setPreviewSize(mPreviewHeight, mPreviewWidth);
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    parameters.set("rotation", 90); // 镜头角度转 90 度(默认摄像头
                    mCamera.setDisplayOrientation(90);
                } else// 如果是横屏
                {
                    parameters.set("orientation", "landscape"); //
                    mCamera.setDisplayOrientation(0); // 在 2.2 以上可以使用
                }
                mCamera.setParameters(parameters); // 将 Camera.Parameters 设定予
                mCamera.startPreview(); // 打开预览画面加上
                mCamera.cancelAutoFocus();// 2 如果要实现连续的自动对焦,这一句必须
                bIfPreview = true;
                Camera.Size csize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = csize.height; //
                mPreviewWidth = csize.width;
                Log.i("TAG" + "initCamera", "after setting, previewSize:width: " + csize.width + " height: " + csize.height);
                csize = mCamera.getParameters().getPictureSize();
                Log.i("TAG" + "initCamera", "after setting, pictruesize:width: " + csize.width + " height: " + csize.height);
                Log.i("TAG" + "initCamera", "after setting, previewformate is " + mCamera.getParameters().getPreviewFormat());
                Log.i("TAG" + "initCamera", "after setting, previewframerate is " + mCamera.getParameters().getPreviewFrameRate());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

