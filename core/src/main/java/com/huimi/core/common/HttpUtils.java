package com.huimi.core.common;

import com.google.common.io.CharStreams;
import com.huimi.common.utils.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpUtils {
    /**
     * get
     */
    public static HttpResponse doGet(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpGet request = new HttpGet(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    /**
     * post form
     */
    public static HttpResponse doPost(String host, String path, String method,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      Map<String, String> bodys)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (bodys != null) {
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

            for (String key : bodys.keySet()) {
                nameValuePairList.add(new BasicNameValuePair(key, bodys.get(key)));
            }
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
            formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            request.setEntity(formEntity);
        }

        return httpClient.execute(request);
    }

    /**
     * Post String
     */

    public static HttpResponse doPost(String host, String path, String method,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      String body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * Post stream
     */
    public static HttpResponse doPost(String host, String path, String method,
                                      Map<String, String> headers,
                                      Map<String, String> querys,
                                      byte[] body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPost request = new HttpPost(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Put String
     */
    public static HttpResponse doPut(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     String body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return httpClient.execute(request);
    }

    /**
     * Put stream
     */
    public static HttpResponse doPut(String host, String path, String method,
                                     Map<String, String> headers,
                                     Map<String, String> querys,
                                     byte[] body)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpPut request = new HttpPut(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return httpClient.execute(request);
    }

    /**
     * Delete
     */
    public static HttpResponse doDelete(String host, String path, String method,
                                        Map<String, String> headers,
                                        Map<String, String> querys)
            throws Exception {
        HttpClient httpClient = wrapClient(host);

        HttpDelete request = new HttpDelete(buildUrl(host, path, querys));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return httpClient.execute(request);
    }

    private static String buildUrl(String host, String path, Map<String, String> querys) throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(host);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }
        if (null != querys) {
            StringBuilder sbQuery = new StringBuilder();
            for (Map.Entry<String, String> query : querys.entrySet()) {
                if (0 < sbQuery.length()) {
                    sbQuery.append("&");
                }
                if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                    sbQuery.append(query.getValue());
                }
                if (!StringUtils.isBlank(query.getKey())) {
                    sbQuery.append(query.getKey());
                    if (!StringUtils.isBlank(query.getValue())) {
                        sbQuery.append("=");
                        sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
                    }
                }
            }
            if (0 < sbQuery.length()) {
                sbUrl.append("?").append(sbQuery);
            }
        }

        return sbUrl.toString();
    }

    private static HttpClient wrapClient(String host) {
        HttpClient httpClient = new DefaultHttpClient();
        if (host.startsWith("https://")) {
            sslClient(httpClient);
        }

        return httpClient;
    }

    private static void sslClient(HttpClient httpClient) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] xcs, String str) {

                }

                public void checkServerTrusted(X509Certificate[] xcs, String str) {

                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = httpClient.getConnectionManager();
            SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String POST_FORM(String url, Map<String, Object> map, String encoding) {
        try {
            String body = "";
            //??????httpclient??????
            CloseableHttpClient client = HttpClients.createDefault();
            //??????post??????????????????
            HttpPost httpPost = new HttpPost(url);

//        List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
//        if(map!=null){
//            for (Map.Entry<String, Object> entry : map.entrySet()) {
//                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
//            }
//        }
//        //??????????????????????????????
//        httpPost.setEntity(new UrlEncodedFormEntity(nvps, encoding));
//
//        //???????????????????????? ???3???
//        RequestConfig config = RequestConfig.custom().setConnectTimeout(3000).setConnectionRequestTimeout(3000).setSocketTimeout(5000).build();
//        httpPost.setConfig(config);
//        System.out.println("???????????????"+url);
//        System.out.println("???????????????"+nvps.toString());

//        1.????????????????????????   ?????????????????????????????????map??????
            List<BasicNameValuePair> pairList = new ArrayList<BasicNameValuePair>();
            if (map != null) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    pairList.add(new BasicNameValuePair(entry.getKey().toString(), entry.getValue().toString()));
                }
            }
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, "utf-8"));

            //2.json??????????????????
//        JSONObject jsonParam = new JSONObject();
//        jsonParam.put("name", "admin");
//        jsonParam.put("pass", "123456");
//        StringEntity entity = new StringEntity(jsonParam.toString(), "utf-8");// ????????????????????????
//        entity.setContentEncoding("UTF-8");
//        entity.setContentType("application/json");
//        httpPost.setEntity(entity);

            //????????????header?????? ??????????????????
            //??????????????????Content-type?????????User-Agent???
//        httpPost.setHeader("Content-type", "application/json");
//        httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

            //??????????????????????????????????????????????????????
            CloseableHttpResponse response = client.execute(httpPost);

            //??????????????????
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                //????????????????????????????????????String??????
                body = EntityUtils.toString(entity, encoding);
            }
            EntityUtils.consume(entity);
            //????????????
            response.close();
            return body;
        } catch (Exception e) {
            return e.getMessage();
        }
    }


    /**
     * ?????????????????????html?????????
     */
    public static String POST_FORM(String url, Map<String, Object> map) {
        try {
            String body;
            //??????httpclient??????
            CloseableHttpClient client = HttpClients.createDefault();
            //??????post??????????????????
            HttpPost httpPost = new HttpPost(url);

//        1.????????????????????????   ?????????????????????????????????map??????
            List<BasicNameValuePair> pairList = new ArrayList<BasicNameValuePair>();
            if (map != null) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    pairList.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
                }
            }
            httpPost.setHeader("authority", "webcast.amemv.com");
            httpPost.setHeader("upgrade-insecure-requests", "1");
            httpPost.setHeader("user-agent", " Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Mobile Safari/537.36");
            httpPost.setHeader("authority", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            httpPost.setHeader("sec-fetch-site", "none");
            httpPost.setHeader("sec-fetch-mode", "navigate");
            httpPost.setHeader("sec-fetch-user", "?1");
            httpPost.setHeader("sec-fetch-dest", "document");
            httpPost.setHeader("accept-language", "zh,zh-CN;q=0.9,en;q=0.8");
            httpPost.setHeader("cookie", "SLARDAR_WEB_ID=cd2c534b-95f1-4bac-8788-1a6d0303c99a");
            httpPost.setEntity(new UrlEncodedFormEntity(pairList, "utf-8"));


            //??????????????????????????????????????????????????????
            CloseableHttpResponse response = client.execute(httpPost);
            Header header = response.getFirstHeader("Location");
            body = header.getValue();
            System.out.println("???????????????" + response.toString());
            System.out.println("????????????????????????" + response.getFirstHeader("Location"));
            //??????????????????
            HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
            //????????????
            response.close();
            return body;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * ?????????URL??????GET???????????????
     *
     * @param url   ???????????????URL
     * @param param ???????????????????????????????????? name1=value1&name2=value2 ????????????
     * @return URL ????????????????????????????????????
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // ?????????URL???????????????
            URLConnection connection = realUrl.openConnection();
            // ???????????????????????????
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Mobile Safari/537.36");
            // ?????????????????????
            connection.connect();
            // ???????????????????????????
            Map<String, List<String>> map = connection.getHeaderFields();
            // ??????????????????????????????
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            // ?????? BufferedReader??????????????????URL?????????
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
//            while ((line = in.readLine()) != null) {
//                result += line;
//            }
            result = CharStreams.toString(new InputStreamReader(connection.getInputStream()));
        } catch (Exception e) {
            System.out.println("??????GET?????????????????????" + e);
            e.printStackTrace();
        }
        // ??????finally?????????????????????
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }
}
