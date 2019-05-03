import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Mac.getInstance;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.http.impl.client.HttpClientBuilder.create;

class PostRequests {

    private String eKey;
    private String secret;
    private HttpClient client = create().build();

    PostRequests(String eKey, String secret) {
        this.eKey = eKey;
        this.secret = secret;
    }

    private JSONObject sendPostRequest(String method, Map<String, String> arguments) {

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        String nonceNum = "" + System.nanoTime();
        String nonceName = "nonce";
        arguments.put(nonceName, nonceNum);
        StringBuilder postData = new StringBuilder();

        for (Map.Entry<String, String> stringStringEntry : arguments.entrySet()) {

            if (postData.length() > 0) {
                postData.append("&");
            }
            postData.append(((Map.Entry) stringStringEntry).getKey())
                    .append("=")
                    .append(((Map.Entry) stringStringEntry).getValue());
        }

        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA512");

        Mac mac;
        try {
            mac = getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException ee) {
            System.err.println("No such algorithm exception: " + ee.toString());
            return null;
        }

        try {
            mac.init(keySpec);
        } catch (InvalidKeyException ike) {
            System.err.println("Invalid key exception: " + ike.toString());
            return null;
        }

        String sign = encodeHexString(mac.doFinal(postData.toString().getBytes(UTF_8)));

        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("api.exmo.me")
                    .setPath("/v1/" + method)
                    .build();
        } catch (URISyntaxException ee) {
            System.err.println("Bad URI request " + ee.toString());
            return null;
        }

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Key", eKey);
        httpPost.setHeader("Sign", sign);

        ArrayList<NameValuePair> postParameters = new ArrayList<>();
        for (Map.Entry<String, String> param : arguments.entrySet()) {
            postParameters.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }

        httpPost.setEntity(new UrlEncodedFormEntity(postParameters, UTF_8));
        HttpResponse httpResponse;
        HttpEntity httpEntity;
        InputStream is;

        try {
            httpResponse = client.execute(httpPost);
            httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();
        } catch (IOException e) {
            System.err.println("IOException " + e.toString());
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, ISO_8859_1), 8);
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return new JSONObject(sb.toString());
    }

    JSONArray getUserOpenOrders(String currencyPair) {
        return sendPostRequest("user_open_orders", null).getJSONArray(currencyPair);
    }

    JSONObject sendPostRequestAndGetResponse(String method, Map<String, String> arguments) {
        return sendPostRequest(method, arguments);
    }

    Integer getUserOpenOrdersNum() {
        return sendPostRequestAndGetResponse("user_open_orders", null).length();
    }
}