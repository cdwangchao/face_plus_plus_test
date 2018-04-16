import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import http.HttpRequestHelper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by Administrator on 2018/4/12 0012.
 */
public class FacePlusPlusThread {

    static final String url = "https://api-cn.faceplusplus.com/facepp/v3/compare";
    static final String api_key = "V4bGVjCYUzbQzwd5vA6GfbKxDDbuieG2";
    static final String api_secret = "jlBfsZEHP1GUVg0YOvuSqykVqnlkb_2N";
    static final String FACE_DIR =  "D:\\java\\taozi\\maoyan";
    static final Logger total_log = LoggerFactory.getLogger("face_total");
    static final Logger success_log = LoggerFactory.getLogger("face_success");
    static final Logger error_log = LoggerFactory.getLogger("face_error");
    static final Logger face_detect_error = LoggerFactory.getLogger("face_detect_error");
    static final Logger face_plus_plus = LoggerFactory.getLogger(FacePlusPlusThread.class.getClass());
    static AtomicInteger count = new AtomicInteger(0);
    static AtomicInteger error = new AtomicInteger(0);
    static AtomicInteger success = new AtomicInteger(0);
    static AtomicInteger confidenceNullCount = new AtomicInteger(0);
    static AtomicInteger httpResponseNullCount = new AtomicInteger(0);
    static float thresholds = 73.975f;
    static int total_time_used = 0;
    static final HttpRequestHelper httpRequestHelper = new HttpRequestHelper();
    static int index = 0;

    public static void main(String[] args) {
        String filePath = "D:\\java\\中移物联网\\猫眼\\猫眼提取照片v1.0\\猫眼提取照片v1.0\\pair_mismatch.txt";
        testFacePlusPlus(filePath);
    }

    private static void testFacePlusPlus(String filePath) {
        readFile(filePath);
    }

    private static void readFile(String filePath) {
        List<String> orgPaths = new ArrayList<>();
        List<String> desPaths = new ArrayList<>();
        try {
            FileReader reader = new FileReader(filePath);
            BufferedReader br = new BufferedReader(reader);
            String str = null;

            while((str = br.readLine()) != null) {
                String[] array = str.split(" ");
                traverseFolder(orgPaths,desPaths,array[0],array[2]);
            }
            br.close();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < orgPaths.size(); i++) {
            int index = i;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        oneByOneFaceRecognition(orgPaths.get(index),desPaths.get(index));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            threadPool.submit(runnable);
        }
        threadPool.shutdown();
    }

    //找到文件所在位置
    public static void traverseFolder(List<String> orgs, List<String> deses, String org, String des) {
        File file = new File(FACE_DIR);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files.length == 0) {
                System.out.println("文件夹为空");
            } else {
                for (File file2 : files) {
                    File desFile = null;
                    File orgFile = null;
                    if (file2.isDirectory()) {//文件夹
                        orgFile = new File(file2,org);
                        if (orgFile.exists()) {
                            for (File file1 : files) {
                                if (file1.isDirectory()) {//文件夹
                                    desFile = new File(file1, des);
                                    if (desFile.exists()) {
                                        orgs.add(orgFile.getAbsolutePath());
                                        deses.add(desFile.getAbsolutePath());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void oneByOneFaceRecognition(String orgFace, String desFace) throws Exception {
        long startTime = System.currentTimeMillis();
        HttpResponse httpResponse = requestFacePlusPlus(orgFace, desFace);
        long costTime = System.currentTimeMillis() - startTime;
        if(httpResponse == null){
            httpResponseNullCount.getAndIncrement();
            return;
        }
        String returnMsg = EntityUtils.toString(httpResponse.getEntity());

        JSONObject json = JSON.parseObject(returnMsg);
        Float confidence = json.getFloat("confidence");
        int time_used = json.getInteger("time_used");
        List<String> orgDirList = Splitter.on("\\").trimResults().
                omitEmptyStrings().splitToList(orgFace);
        List<String> desDirList = Splitter.on("\\").trimResults().
                omitEmptyStrings().splitToList(desFace);
        String orgFileName = orgDirList.get(5);
        String desFileName = desDirList.get(5);
        if (confidence == null) {
            face_detect_error.info("orgFace: {}, desFace: {}, face++ return meg: {}", orgFileName, desFileName, json);
            confidenceNullCount.getAndIncrement();
            return;
        }

        total_time_used += time_used;

        count.getAndIncrement();
        recodeResults(confidence, orgDirList, desDirList, orgFileName, desFileName);
        total_log.info("{} matches {} similarity is confidence: {} ", orgFileName, desFileName, confidence);
        total_log.info("Circle {} spent {} s", count.get(),(double)costTime/1000);
        total_log.info("count_right: {}",success.get());
        total_log.info("count_wrong:{}",error.get());
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        total_log.info("accuracy {}%",decimalFormat.format((double)success.get()*100/count.get()));
        System.out.println(String.format("count: %d, error: %d, success: %d,confidenceNullCount: %d, httpResponseNullCount:%d, ", count.get(), error.get(), success.get(),confidenceNullCount.get(),httpResponseNullCount.get(),time_used));
    }


    private static HttpResponse requestFacePlusPlus(String orgFace, String desFace) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("api_key", api_key);
        params.put("api_secret", api_secret);
        params.put("image_file1", new File(orgFace));
        params.put("image_file2", new File(desFace));
        HttpResponse httpResponse = httpRequestHelper.execPostRequest(params, url, new HashMap<String, String>());
        int i = 0;
        if (httpResponse == null) {
            System.err.println(orgFace + " and " + desFace + " httpResponse1 is null");
            face_plus_plus.error(orgFace + " and " + desFace + " httpResponse1 is null");
        }
        while (httpResponse.getStatusLine().getStatusCode() != 200) {
            httpResponse = httpRequestHelper.execPostRequest(params, url, new HashMap<String, String>());
            i++;
            face_plus_plus.error("{} and {}, i: {}", orgFace, desFace, i);
            if(i == 50){
                return null;
            }
        }
        return httpResponse;
    }

    private static void recodeResults(Float confidence, List<String> orgDirList, List<String> desDirList, String orgFileName, String desFileName) {
        if (orgDirList.get(4).equals(desDirList.get(4))) {
            if (confidence > thresholds) {
                success.getAndIncrement();
                success_log.info("{} matches {} similarity is {}", orgFileName, desFileName, confidence);
            } else {
                error.getAndIncrement();
                error_log.info("{} dismatches {} similarity is {}", orgFileName, desFileName, confidence);
            }
        } else {
            if (confidence > thresholds) {
                error.getAndIncrement();
                error_log.info("{} matches {} similarity is {}", orgFileName, desFileName, confidence);
            } else {
                success.getAndIncrement();
                success_log.info("{} dismatches {} similarity is {} ", orgFileName, desFileName, confidence);
            }
        }
    }

}
