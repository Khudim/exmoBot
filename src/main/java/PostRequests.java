import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Mac.getInstance;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.http.impl.client.HttpClientBuilder.create;

class PostRequests {

    private String eKey;
    private String secret;
    private Logger logger;

    private HttpClient client = create().build();

    private String currencyPair;

    PostRequests(String eKey, String secret, String currencyPair) {
        this.eKey = eKey;
        this.secret = secret;
        this.currencyPair = currencyPair;
        logger = Main.initLogger("PostRequestsLog", "JsonExceptions");
    }

    private JSONObject sendPostRequest(String method, Map<String, String> arguments) throws BotException {

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
        } catch (NoSuchAlgorithmException e) {
            throw new BotException("Error in PostPequests in thread with currency pair - " + currencyPair + " in method - " + method,
                                    Arrays.toString(e.getStackTrace()));
        }

        try {
            mac.init(keySpec);
        } catch (InvalidKeyException e) {
            throw new BotException("Error in PostPequests in thread with currency pair - " + currencyPair + " in method - " + method,
                                    Arrays.toString(e.getStackTrace()));
        }

        String sign = encodeHexString(mac.doFinal(postData.toString().getBytes(UTF_8)));

        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("api.exmo.me")
                    .setPath("/v1/" + method)
                    .build();
        } catch (URISyntaxException e) {
            throw new BotException("Error in PostPequests in thread with currency pair - " + currencyPair + " in method - " + method,
                                    Arrays.toString(e.getStackTrace()));
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
            throw new BotException("Error in PostPequests in thread with currency pair - " + currencyPair + " in method - " + method,
                                    Arrays.toString(e.getStackTrace()));
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
            throw new BotException("Error in PostPequests in thread with currency pair - " + currencyPair + " in method - " + method,
                                    Arrays.toString(e.getStackTrace()));
        }

        return new JSONObject(sb.toString());
    }

    JSONArray getUserOpenOrders(String currencyPair) throws BotException {
        JSONObject jsonObject = sendPostRequest("user_open_orders", null);
        try {
            return jsonObject.getJSONArray(currencyPair);
        } catch (JSONException e) {
            logger.severe("Exception in parse response json for currency pair - " + currencyPair);
            logger.severe(e.getMessage());
            return new JSONArray();
        }
    }

    JSONObject sendPostRequestAndGetResponse(String method, Map<String, String> arguments) throws BotException {
        return sendPostRequest(method, arguments);
    }

    Integer getUserOpenOrdersNum() throws BotException {
        return sendPostRequestAndGetResponse("user_open_orders", null).length();
    }
}