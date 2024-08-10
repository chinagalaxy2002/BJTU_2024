package com.aiot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import android.util.Base64;
import java.util.List;
import java.util.Map;


public class ImageBase64Converter {

    public static String post(String path) {
        try {
            File file = new File(path);
            long fileSize = file.length();
            final long maxFileSize = (long) (1.5 * 1024 * 1024); // 1.5MB in bytes

            Bitmap bitmap;
            if (fileSize > maxFileSize) {
                // 如果文件大于1.5M，对其进行压缩
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);

                // 计算压缩比例
                int height = options.outHeight;
                int width = options.outWidth;
                int inSampleSize = 1;
                int halfHeight = height / 2;
                int halfWidth = width / 2;

                // 计算最大的 inSampleSize 值，该值是 2 的幂，并且保持高度和宽度大于目标高度和宽度
                while ((halfHeight / inSampleSize) >= 600 && (halfWidth / inSampleSize) >= 600) {
                    inSampleSize *= 2;
                }

                options.inSampleSize = inSampleSize;
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(path, options);

                // 进一步压缩图片
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos); // 压缩质量60%
            } else {
                // 如果文件大小合适，直接加载图片
                bitmap = BitmapFactory.decodeFile(path);
            }

            // 将Bitmap转换为Base64字符串
            String imgBase64Str = ImageBase64Converter.convertBitmapToBase64(bitmap);

            // 发送请求
            String contentType = "application/x-www-form-urlencoded";
            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imgBase64Str, "UTF-8");
//            String str = postGeneralUrl("http://49.232.151.58:10010/", contentType, params, "UTF-8");
            String str = postGeneralUrl("http://58.87.103.34:10010/ ", contentType, params, "UTF-8");
            return str;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "failed";
    }

    public static String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }


    public static String postGeneralUrl(String generalUrl, String contentType,
                                        String params, String encoding) throws Exception {
        URL url = new URL(generalUrl);
        // 打开和URL之间的连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        // 设置通用的请求属性
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);

        // 得到请求的输出流对象
        DataOutputStream out = new DataOutputStream(
                connection.getOutputStream());
        out.write(params.getBytes(encoding));
        out.flush();
        out.close();

        // 建立实际的连接
        connection.connect();
        // 获取所有响应头字段
        Map<String, List<String>> headers = connection.getHeaderFields();
        // 遍历所有的响应头字段
        for (String key : headers.keySet()) {
            System.err.println(key + "--->" + headers.get(key));
        }
        // 定义 BufferedReader输入流来读取URL的响应
        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), encoding));
        String result = "";
        String getLine;
        while ((getLine = in.readLine()) != null) {
            result += getLine;
        }
        in.close();
        System.err.println("result:" + result);
        return result;
    }


    public static void compress(String path ) {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sdaeu/";

        File dir = new File(filePath);
        if (!dir.exists()) {// 判断文件目录是否存在
            dir.mkdirs();
        }
//        File sdFile = Environment.getExternalStorageDirectory();
        File originFile = new File(path);
        Bitmap bitmap = BitmapFactory.decodeFile(originFile.getAbsolutePath());
        //设置缩放比
        int radio = 8;
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth() / radio, bitmap.getHeight() / radio, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        RectF rectF = new RectF(0, 0, bitmap.getWidth() / radio, bitmap.getHeight() / radio);
        //将原图画在缩放之后的矩形上
        canvas.drawBitmap(bitmap, null, rectF, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        try {
            FileOutputStream fos = new FileOutputStream(new File(dir, "sizeCompress.jpg"));
            fos.write(bos.toByteArray());
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}