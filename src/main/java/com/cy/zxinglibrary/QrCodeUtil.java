package com.cy.zxinglibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * Created by dell on 2018/5/11.
 * <p>
 * 二维码工具类
 * <p>
 * 1、支持将字符串转化为二维码图片
 * <p>
 * 2、支持从图片中单独解析二维码，支持从图片中单独解析条码，支持从图片中解析二维码或条码
 */

public class QrCodeUtil {


    /**
     * 根据字符串生成二维码图片，有的生成不了，所以在后面加了空格，获取时要注意去掉
     *
     * @param str
     * @return
     * @throws WriterException
     */
    public static Bitmap encodeQrBitmap(String str) throws WriterException {

        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        //指定纠错等级
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        //指定编码格式
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = null;
        try {
            matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, 400, 400, hints);
        } catch (Exception e) {
            matrix = new MultiFormatWriter().encode(str + " ", BarcodeFormat.QR_CODE, 400, 400, hints);
        }
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                } else {  // 此处不加else 保存二维码到本地会变成黑色的图片
                    pixels[y * width + x] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }


    /**
     * 绘制条形码
     *
     * @param content       要生成条形码包含的内容
     * @param widthPix      条形码的宽度
     * @param heightPix     条形码的高度
     * @param isShowContent 否则显示条形码包含的内容
     * @return 返回生成条形的位图
     */
    public static Bitmap encodeBarBitmap(String content, int widthPix, int heightPix, boolean isShowContent) {
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        //配置参数  
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 容错级别 这里选择最高H级别  
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        MultiFormatWriter writer = new MultiFormatWriter();

        try {
            // 图像数据转换，使用了矩阵转换 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数  
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, widthPix, heightPix, hints);
            int[] pixels = new int[widthPix * heightPix];
//             下面这里按照二维码的算法，逐个生成二维码的图片，  
            // 两个for循环是图片横列扫描的结果  
            for (int y = 0; y < heightPix; y++) {
                for (int x = 0; x < widthPix; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * widthPix + x] = 0xff000000; // 黑色  
                    } else {
                        pixels[y * widthPix + x] = 0xffffffff;// 白色  
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(widthPix, heightPix, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, widthPix, 0, 0, widthPix, heightPix);
            if (isShowContent) {
                bitmap = showContent(bitmap, content);
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 显示条形的内容
     *
     * @param bCBitmap 已生成的条形码的位图
     * @param content  条形码包含的内容
     * @return 返回生成的新位图, 返回的位图与新绘制文本content的组合
     */
    private static Bitmap showContent(Bitmap bCBitmap, String content) {
        if (TextUtils.isEmpty(content) || null == bCBitmap) {
            return null;
        }
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);//设置填充样式
        paint.setTextSize(14);
//                paint.setTextAlign(Paint.Align.CENTER);    
        //测量字符串的宽度
        int textWidth = (int) paint.measureText(content);
        Paint.FontMetrics fm = paint.getFontMetrics();
        //绘制字符串矩形区域的高度
        int textHeight = (int) (fm.bottom - fm.top);
        //  x  轴的缩放比率
        float scaleRateX = bCBitmap.getWidth() / textWidth;
        paint.setTextScaleX(scaleRateX);
        //绘制文本的基线
        int baseLine = bCBitmap.getHeight() + textHeight;
        //创建一个图层，然后在这个图层上绘制bCBitmap、content
        Bitmap bitmap = Bitmap.createBitmap(bCBitmap.getWidth(), bCBitmap.getHeight() + 2 * textHeight, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas();
        canvas.drawColor(Color.WHITE);
        canvas.setBitmap(bitmap);
        canvas.drawBitmap(bCBitmap, 0, 0, null);
        canvas.drawText(content, bCBitmap.getWidth() / 10, baseLine, paint);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        return bitmap;
    }


    /**
     * 从图片中解析二维码内容
     *
     * @param bitmap
     * @return
     */
    public static String decodeByZXing(Bitmap bitmap) {
        return decodeByZXing(bitmap, 0);
    }

    /**
     * 从图片中解析二维码或条码内容
     *
     * @param bitmap 图片
     * @param type   0：只识别二维码  1：只识别条码  2：两者都识别
     * @return
     */
    public static String decodeByZXing(Bitmap bitmap, int type) {
        MultiFormatReader multiFormatReader = new MultiFormatReader();

        // 解码的参数
        Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(2);
        // 可以解析的编码类型
        Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = new Vector<BarcodeFormat>();

            // 这里设置可扫描的类型
            switch (type) {
                case 0:
                    decodeFormats.addAll(MyDecodeFormatManager.QR_CODE_FORMATS);
                    decodeFormats.addAll(MyDecodeFormatManager.DATA_MATRIX_FORMATS);
                    break;
                case 1:
                    decodeFormats.addAll(MyDecodeFormatManager.ONE_D_FORMATS);
                    break;
                case 2:
                    decodeFormats.addAll(MyDecodeFormatManager.ONE_D_FORMATS);
                    decodeFormats.addAll(MyDecodeFormatManager.QR_CODE_FORMATS);
                    decodeFormats.addAll(MyDecodeFormatManager.DATA_MATRIX_FORMATS);
                    break;
            }
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        // 设置继续的字符编码格式为UTF8
        // hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
        // 设置解析配置参数
        multiFormatReader.setHints(hints);

        // 开始对图像资源解码
        Result rawResult = null;
        try {
            rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(bitmap))));
            try {
                bitmap.recycle();
                bitmap = null;
            } catch (Exception e) {
            }
            return rawResult.toString();
        } catch (Exception e) {
            e.printStackTrace();
            if (type == 1 || type == 2) {//需要扫一维码的，我们旋转90度再解析一遍
                Log.e("xxxxxxxx", "xxxxxxxx");
                Bitmap bitmap2 = rotateToDegrees(bitmap, 90);
                try {
                    rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(bitmap2))));
                    try {
                        bitmap2.recycle();
                        bitmap2 = null;
                    } catch (Exception ee) {

                    }
                    return rawResult.toString();
                } catch (NotFoundException e1) {
                    e1.printStackTrace();

                }

            }
            return null;
        }
    }


    /**
     * 从图片数据中，按指定区域解析二维码内容
     *
     * @param data 图片数据
     * @param w    横向起始位置
     * @param h    纵向起始位置
     * @param size 扫描区域大小（正方形）
     * @param type 0：只识别二维码  1：只识别条码  2：两者都识别
     * @return
     */
    public static String decodeByZXing(byte[] data, int w, int h, int size, int type) {

        ByteArrayOutputStream baos;
        Bitmap bitmap;

        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(
                data,
                ImageFormat.NV21,
                w,
                h,
                null);
        baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect((w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2), 100, baos);// 80--JPG图片的质量[0-100],100最高
        byte[] rawImage = baos.toByteArray();
        //将rawImage转换成bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        return decodeByZXing(bitmap, type);
    }


    /**
     * 图片旋转
     *
     * @param tmpBitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return tmpBitmap =
                Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                        true);
    }
}
