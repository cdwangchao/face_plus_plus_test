package http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpRequestHelper {
    private static final Log log = LogFactory.getLog(HttpRequestHelper.class);
    private HttpClient httpClient = HttpClientFactory.createHttpClient();

    public HttpResponse execPostRequest(Map<String, Object> paramMap, String url, Map<String, String> headerMap) throws Exception {
        HttpPost post = new HttpPost(url);
        for (String key : headerMap.keySet()) {
            post.setHeader(key, headerMap.get(key));
        }
        HttpEntity entity = createHttpEntity(paramMap);
        post.setEntity(entity);
        return handleHttpResponse(httpClient, post);
    }

    public HttpResponse execNoTslPostRequest(Map<String, Object> paramMap, String url, Map<String, String> headerMap) throws Exception {
        HttpClient NoTslhttpClient = HttpClientFactory.createNoTslHttpClient();
        HttpPost post = new HttpPost(url);
        for (String key : headerMap.keySet()) {
            post.setHeader(key, headerMap.get(key));
        }
        HttpEntity entity = createUrlEncodedFormEntity(paramMap);
        post.setEntity(entity);
        return handleHttpResponse(NoTslhttpClient, post);
    }

    public UrlEncodedFormEntity createUrlEncodedFormEntity(Map<String , Object> paramMap) throws UnsupportedEncodingException {
        List<NameValuePair> params=new ArrayList<NameValuePair>();
        for(String key : paramMap.keySet()){
            params.add(new BasicNameValuePair(key, paramMap.get(key).toString()));
        }
        return new UrlEncodedFormEntity(params);
    }

    private HttpEntity createHttpEntity(Map<String, Object> paramMap) {
        MultipartEntityBuilder multipartEntity = MultipartEntityBuilder.create();
//        multipartEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//        multipartEntity.setCharset(Charset.forName(Constants.CHARSET));
        for (String key : paramMap.keySet()) {
            if (paramMap.get(key) != null) {
                if(paramMap.get(key) instanceof File){
                    File file = (File) paramMap.get(key);
                    multipartEntity.addBinaryBody(key, file);
                }else{
                    multipartEntity.addTextBody(key, paramMap.get(key).toString());
                }
            }
        }
        return multipartEntity.build();
    }

    public HttpResponse execGetRequest(Map<String, String> paramMap, String basicUrl) throws Exception {
        String url = createNewUrl(paramMap, basicUrl);
        HttpGet get = new HttpGet(url);
        return handleHttpResponse(httpClient, get);
    }

    public HttpResponse execGetRequest(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        return handleHttpResponse(httpClient, get);
    }

    private String createNewUrl(Map<String, String> paramMap, String basicUrl) {
        StringBuffer sb = new StringBuffer();
        sb.append(basicUrl).append("?");
        for (String key : paramMap.keySet()) {
            sb.append(key).append("=").append(paramMap.get(key)).append("&");
        }
        return sb.substring(0, sb.length() - 1);//romove last & symbol
    }

    private HttpResponse handleHttpResponse(HttpClient httpClient, HttpRequestBase baseRequest) throws Exception {
        HttpResponse response = null;
        try {
            response = httpClient.execute(baseRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                String errorMsg = IOUtils.toString(response.getEntity().getContent());
                log.error("Request service error! Status code :" + statusCode + "\nurl : " + baseRequest.getURI());
                log.error(errorMsg);

//                throw new ApiUserUmException(statusCode, "Request service error!\n" + errorMsg);
            }
        } catch (Exception e) {
            baseRequest.releaseConnection();
            throw e;
        }
        log.info("request url " + baseRequest.getURI() + " successful!");
        return response;
    }
}
