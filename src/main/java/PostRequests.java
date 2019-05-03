import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostRequests {

    private String eKey;
    private String secret;
    private String nonceName = "nonce";
    private HttpClient client = HttpClientBuilder.create().build();

    PostRequests(String eKey, String secret) {
        this.eKey = eKey;
        this.secret = secret;
    }

    private JSONObject sendPostRequest(String method, Map<String, String> arguments) {

        if (arguments == null) {
            arguments = new HashMap<>();
        }

        String nonceNum = "" + System.nanoTime();
        arguments.put(nonceName, nonceNum);
        String postData = "";

        for (Map.Entry<String, String> stringStringEntry : arguments.entrySet()) {
            Map.Entry argument = stringStringEntry;

            if (postData.length() > 0) {
                postData += "&";
            }
            postData += argument.getKey() + "=" + argument.getValue();
        }

        SecretKeySpec keySpec;

        try {
            keySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
        } catch (UnsupportedEncodingException ee) {
            System.err.println("Unsupported encoding exception: " + ee.toString());
            return null;
        }

        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA512");
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

        String sign;
        try {
            sign = Hex.encodeHexString(mac.doFinal(postData.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException uee) {
            System.err.println("Unsupported encoding exception: " + uee.toString());
            return null;
        }

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

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        } catch (UnsupportedEncodingException ee) {
            System.err.println("Unsupported encoding exception: " + ee.toString());
            return null;
        }
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "iso-8859-1"), 8);
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return new JSONObject(sb.toString());
    }

    JSONArray getResponse(String method, String currencyPair, Map<String, String> arguments) {
        return sendPostRequest(method, arguments).getJSONArray(currencyPair);
    }

    JSONObject getResponse(String method, Map<String, String> arguments) {
        return sendPostRequest(method, arguments);
    }

    Integer getOpenOrdersNum(String method) {
        return getResponse(method, null)
                .length();
    }


}
