package com.cy.zxinglibrary;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;


/**
 * 说明：本功能使用Zxing开源库进行二维码扫描（注意跳转前先获取摄像头权限） 请不要在app中引入appcompat-v7 ，否则会冲突
 *
 * 1、ui可以在本activity的布局文件中修改
 *
 * 2、启动本页面：用startActivityForResult()的方式，    接受结果：在 RESULT_OK 的情况下，intent中获取 data.getStringExtra("resultStr")
 *
 * 3、本页面只支持二维码，要支持一维码，可以在 final String string = QrCodeUtil.decodeByZXing 这一句中修改最后一个参数
 *
 * 4、直接从图片中解析二维码：  QrDecoder.decodeByZXing(bitmap);
 *
 * 5、生成二维码:
 *
 *
 */
public class QrScanActivity extends AppCompatActivity {

    String resultString;


    SurfaceView sv;
    ImageView iv;
    LinearLayout ll;
    View viewBack;
    View viewPicture;

    SurfaceHolder surfaceHolder;

    Camera mCamera;


    /**
     * 扫描成功了，这里就不会继续扫了，结果已经拿到了
     */
    private void onSanResult(String resultStr) {
        setResult(RESULT_OK, new Intent().putExtra("resultStr", resultStr));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        init();

        startAnim();

    }


    void init() {
        sv = findViewById(R.id.sv_scan);
        iv = findViewById(R.id.iv_scan);
        ll = findViewById(R.id.ll_scan);
        viewBack = findViewById(R.id.view_back);
        viewPicture = findViewById(R.id.view_picture);

        viewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        viewPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImageFromAlbum();
            }
        });

        surfaceHolder = sv.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = Camera.open();

                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.setDisplayOrientation(90);

                mCamera.startPreview();
                startScan();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                try {
                    mCamera.setPreviewDisplay(null);
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    mCamera.release();
                } catch (Exception e) {
                }
            }
        });


    }


    /**
     * 开始扫描
     */
    private void startScan() {

        if (!TextUtils.isEmpty(resultString)) {
            onSanResult(resultString);
            return;
        }

        if (isFinishing()) {
            return;
        }
        try {
            mCamera.autoFocus(null);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {

                @Override
                public void onPreviewFrame(final byte[] data1, final Camera camera) {

                    mCamera.startPreview();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String string = QrCodeUtil.decodeByZXing(data1, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, ll.getWidth(),0);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!TextUtils.isEmpty(string)) {
                                        resultString = string;
                                    } else {
                                        startScan();
                                    }

                                }
                            });

                        }
                    }).start();
                }
            });
        } catch (Exception e) {
        }

    }


    /**
     * 播放扫描动画
     */
    private void startAnim() {
        ObjectAnimator oa = ObjectAnimator.ofFloat(iv, "translationY", dp2px(200 - 2 * 5));
        oa.setDuration(1500);
        oa.setRepeatCount(-1);
        oa.start();
    }

    /**
     * dp转px
     *
     * @param dp
     * @return
     */
    int dp2px(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp);
    }


    public void getImageFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            Uri uri;
            if (data != null && (uri = data.getData()) != null) {
                //从相册扫描到的结果
                String uri_str = ToReallyUri(uri.toString());

                Bitmap bm = BitmapFactory.decodeFile(uri_str);
                String code = QrCodeUtil.decodeByZXing(bm);
                if (!TextUtils.isEmpty(code)) {
                    onSanResult(code);
                }
            }
        }

    }

    //把content地址变成真实地址
    public String ToReallyUri(String content_image_uri) {
        Uri uri = Uri.parse(content_image_uri);
        String[] proj = {MediaStore.MediaColumns.DATA};
        @SuppressWarnings("deprecation")
        Cursor actualimagecursor = this.managedQuery(uri, proj, null, null, null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        actualimagecursor.moveToFirst();
        return actualimagecursor.getString(actual_image_column_index);
    }


}
