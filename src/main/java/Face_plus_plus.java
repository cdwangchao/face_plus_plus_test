package main.java;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import http.HttpRequestHelper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Face_plus_plus {

    static final String url = "https://api-cn.faceplusplus.com/facepp/v3/compare";
    static final String api_key = "V4bGVjCYUzbQzwd5vA6GfbKxDDbuieG2";
    static final String api_secret = "jlBfsZEHP1GUVg0YOvuSqykVqnlkb_2N";
    static final String face_dir = "E:/work/AI/train-image/positive_face";
    static final Logger total_log = LoggerFactory.getLogger("face_total");
    static final Logger success_log = LoggerFactory.getLogger("face_success");
    static final Logger error_log = LoggerFactory.getLogger("face_error");
    static final Logger face_detect_error = LoggerFactory.getLogger("face_detect_error");
    static final Logger face_plus_plus = LoggerFactory.getLogger(Face_plus_plus.class.getClass());
    static int count = 0;
    static int error = 0;
    static int success = 0;
    static float thresholds = 73.975f;
    static int total_time_used = 0;
    static final HttpRequestHelper httpRequestHelper = new HttpRequestHelper();

    public static void main(String[] args) throws Exception {
        testFacePlusPlus();
    }

//    @Test
    public static void testFacePlusPlus() throws Exception {
        ExecutorService threadPool =  Executors.newFixedThreadPool(10);
        List<String> orgList = new ArrayList<>();
        List<String> desList = new ArrayList<>();
//        万不得已采用按引用传递
        traverseFolder2(face_dir, orgList, desList);
        Set<String> oneByOneFaceSet = new HashSet<String>();
        for (final String orgFace : orgList) {
            for (final String desFace : orgList) {
                long start = System.currentTimeMillis();
                String oneByOneFaces = orgFace + desFace;
                oneByOneFaceSet.add(oneByOneFaces);
                if (!orgFace.equals(desFace) && oneByOneFaceSet.add(desFace + orgFace)) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                oneByOneFaceRecognition(orgFace, desFace);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    threadPool.submit(runnable);
                }
                System.out.println(String.format("used time: %d", System.currentTimeMillis() - start));
            }
        }
        threadPool.shutdown();
    }

    private static void oneByOneFaceRecognition(String orgFace, String desFace) throws Exception {
        HttpResponse httpResponse = requestFacePlusPlus(orgFace, desFace);
        if(httpResponse == null){
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
        String orgFileName = orgDirList.get(6);
        String desFileName = desDirList.get(6);
        if (confidence == null) {
            face_detect_error.info("orgFace: {}, desFace: {}, face++ return meg: {}", orgFileName, desFileName, json);
            return;
        }
        total_log.info("orgFace: {}, desFace: {}, confidence: {}, time used: {}", orgFileName, desFileName, confidence, time_used);
        count++;
        total_time_used += time_used;
        recodeResults(confidence, orgDirList, desDirList, orgFileName, desFileName);
        System.out.println(String.format("count: %d, error: %d, success: %d, total time used", count, error, success, time_used));

    }

    private static HttpResponse requestFacePlusPlus(String orgFace, String desFace) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("api_key", api_key);
        params.put("api_secret", api_secret);
        params.put("image_file1", new File(orgFace));
        params.put("image_file2", new File(desFace));
        HttpResponse httpResponse = httpRequestHelper.execPostRequest(params, url, new HashMap<String, String>());
        int i = 0;
        while (httpResponse.getStatusLine().getStatusCode() != 200) {
            httpResponse = httpRequestHelper.execPostRequest(params, url, new HashMap<String, String>());
            i++;
            face_plus_plus.error("orgFace: {}, desFace: {}, i: {}", orgFace, desFace, i);
            if(i == 30){
                return null;
            }
        }
        return httpResponse;
    }

    private static void recodeResults(Float confidence, List<String> orgDirList, List<String> desDirList, String orgFileName, String desFileName) {
        if (orgDirList.get(5).equals(desDirList.get(5))) {
            if (confidence > thresholds) {
                success++;
                success_log.info("orgFace: {}, desFace: {}, confidence: {}", orgFileName, desFileName, confidence);
            } else {
                error++;
                error_log.info("orgFace: {}, desFace: {}, confidence: {}", orgFileName, desFileName, confidence);
            }
        } else {
            if (confidence > thresholds) {
                error++;
                error_log.info("orgFace: {}, desFace: {}, confidence: {}", orgFileName, desFileName, confidence);
            } else {
                success++;
                success_log.info("orgFace: {}, desFace: {}, confidence: {}", orgFileName, desFileName, confidence);
            }
        }
    }


    static void traverseFolder2(String path, List<String> orgList, List<String> desList) {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files.length == 0) {
                System.out.println("文件夹是空的!");
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
//                        System.out.println("文件夹:" + file2.getAbsolutePath());
                        traverseFolder2(file2.getAbsolutePath(), orgList, desList);
                    } else {
//                        System.out.println("文件:" + file2.getAbsolutePath());
//                        System.out.println(file2.getPath());
                        orgList.add(file2.getPath());
                        desList.add(file2.getPath());
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }
}
