package com.itheima.mp;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * @Author 泽
 * @Date 2024/12/12 16:48
 */
public class ConmposeImg {


    // 基础常量设置

    //拿取数据地址
    private static final String API_URL = "";
    //图片存储地址
    private static final String DESKTOP_PATH = "E:/img/";

    public static void main(String[] args) {
        try {
            //1 拿到接口中的图片 存储到本地文件夹中
            getImgFromJson();

            //2. 传入图片和文件夹进行比较
            String inputImagePath = "E:/img/图片0.jpg";
            String folderPath = "E:/img/";
            double similarityThreshold = 0.6;  // 60%相似度

            // 3.调用图片算法
            List<File> similarImage = findSimilarImage(inputImagePath, folderPath, similarityThreshold);

            // 分析结果
            if(similarImage != null) {
                for (File file : similarImage) {
                    System.out.println("Found similar image: " + file.getName());
                }
            }else {
                System.out.println("No similar image found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从接口中拿到图片存储本地的方法
     */
    public static void getImgFromJson() {
        try {
            int j = 0;//记录图片名的变量
            // 从URL获取JSON数据
            String jsonString = fetchJsonFromUrl(API_URL);

            // 解析JSON数据
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray productsArray = jsonObject.getJSONArray("products");

            // 遍历每个产品
            for (int i = 0; i < productsArray.length(); i++) {
                JSONObject product = productsArray.getJSONObject(i);
                // 获取variants数组中的第一个元素
                JSONArray variantsArray = product.getJSONArray("variants");
                if (variantsArray.length() > 0) {
                    JSONObject firstVariant = variantsArray.getJSONObject(0);
                    JSONObject featuredImage = firstVariant.getJSONObject("featured_image");
                    String imageUrl = featuredImage.getString("src");
                    // 下载并保存图片
                    saveImage(imageUrl, "图片" +j+ ".jpg");
                    j++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从URL获取JSON字符串
     * @param urlString
     * @return
     * @throws Exception
     */
    private static String fetchJsonFromUrl(String urlString) throws Exception {
        //传入url相关信息拿到json对象
        String result = "";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        //使用StringBuilder存储响应结果
        if (responseCode == HttpURLConnection.HTTP_OK) { // 成功
            try (InputStream inputStream = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
                 result = sb.toString().trim(); // 去掉末尾的换行符
                // 现在你可以使用 result 字符串了
            } catch (IOException e) {
                e.printStackTrace();
                // 处理异常
            }
        }
        return result;
    }

    /**
     * 下载并保存图片
     * @param imageUrl
     * @param fileName
     */
    private static void saveImage(String imageUrl, String fileName) {
        try {
            // 确保本地文件夹存在
            File folder = new File(DESKTOP_PATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // 从URL下载图片
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();
            File outputFile = new File(folder, fileName);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
            System.out.println("图片已保存: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // 读取图像并转换为灰度图像，缩小到指定尺寸
    public static BufferedImage preprocessImage(String imagePath, int newWidth, int newHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(imagePath));
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        return resizedImage;
    }

    // 灰度化并生成二值图像
    public static int[][] convertToBinary(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] binaryImage = new int[width][height];

        // 计算灰度均值
        int sum = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getRGB(x, y);
                int gray = (int)(0.299 * ((color >> 16) & 0xFF) + 0.587 * ((color >> 8) & 0xFF) + 0.114 * (color & 0xFF));
                sum += gray;
            }
        }
        int mean = sum / (width * height);

        // 根据均值进行二值化
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getRGB(x, y);
                int gray = (int)(0.299 * ((color >> 16) & 0xFF) + 0.587 * ((color >> 8) & 0xFF) + 0.114 * (color & 0xFF));
                binaryImage[x][y] = (gray > mean) ? 1 : 0;
            }
        }
        return binaryImage;
    }

    // 计算哈希值：通过将二值图像转换为一个字符串
    public static String calculateHash(int[][] binaryImage) {
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < binaryImage.length; i++) {
            for (int j = 0; j < binaryImage[i].length; j++) {
                hash.append(binaryImage[i][j]);
            }
        }
        return hash.toString();
    }

    // 计算两个哈希值的相似度
    public static double calculateSimilarity(String hash1, String hash2) {
        int matchingBits = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) == hash2.charAt(i)) {
                matchingBits++;
            }
        }
        return (double) matchingBits / hash1.length();
    }

    /**
     * 传入的图片和文件夹中的图片进行比对
     * @param imagePath
     * @param folderPath
     * @param threshold
     * @return
     * @throws IOException
     */
    // 在本地文件夹中查找相似图片
    public static List<File> findSimilarImage(String imagePath, String folderPath, double threshold) throws IOException {
        List<File> similarImages = new ArrayList<>();

        // 读取输入图片并进行预处理
        BufferedImage inputImage = preprocessImage(imagePath, 8, 8);
        int[][] inputBinary = convertToBinary(inputImage);
        String inputHash = calculateHash(inputBinary);

        // 查找相似的图片
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        int i = 0;
        for (File file : files) {
            BufferedImage folderImage = preprocessImage(file.getAbsolutePath(), 8, 8);
            int[][] folderBinary = convertToBinary(folderImage);
            String folderHash = calculateHash(folderBinary);

            // 计算相似度
            double similarity = calculateSimilarity(inputHash, folderHash);
            if (similarity >= threshold) {
                i++;
                similarImages.add(file);
            }
        }
        return similarImages;
    }

}
