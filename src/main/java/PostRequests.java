import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.DecompressingEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PostRequests {

    private String method;
    private String currencyPair;
    private String eKey;
    private String secret;

    PostRequests(String method, String currencyPair, String eKey, String secret){
        this.method = method;
        this.currencyPair = currencyPair;
        this.eKey = eKey;
        this.secret = secret;
    }

    PostRequests(){}

    private void method() throws URISyntaxException, IOException, NoSuchAlgorithmException, InvalidKeyException {

        String nonceName = "nonce";
        Long nonceNum = System.nanoTime();
        String postData = nonceName + "=" + ++nonceNum;

        System.out.println(nonceNum);

        HttpClient client = HttpClientBuilder.create().build();

//        URI uri = new URIBuilder()
//                .setScheme("https")
//                .setHost("api.exmo.me")
//                .setPath("/v1/" + method)
//                .setParameter(nonceName, nonceNum.toString())
//                .build();



        String hexString = Long.toHexString(nonceNum);
        Mac mac512;

        mac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(nonceNum.toString().getBytes(), "HmacSHA512");
        mac512.init(secretKeySpec);

        byte[] mac_data = mac512.doFinal(secret.getBytes("UTF-8"));
        String sign = Base64.getEncoder().encodeToString(mac_data);









//        byte[] keyBytes = secret.getBytes();
//        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA512");
//        Mac mac = Mac.getInstance("HmacSHA512");
//        mac.init(signingKey);
//
//        byte[] rawHmac = mac.doFinal(postData.getBytes());
//        byte[] hexBytes = new Hex().encode(rawHmac);
//        String sign = new String(hexBytes, "UTF-8");







//        SecretKeySpec key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
//        Mac mac = Mac.getInstance("HmacSHA512");
//        mac.init(key);
//        String sign = Hex.encodeHexString(mac.doFinal(postData.getBytes("UTF-8")));


//        String sign;
//        eKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
//        mac =
//        mac.init(key);
//        sign = Hex.encodeHexString(mac.doFinal(("nonce" + System.nanoTime()).getBytes("UTF-8")));


        URI uri = new URIBuilder()
                .setScheme("https")
                .setHost("api.exmo.me")
                .setPath("/v1/" + method)
                .setParameter("Key", "K-cfa9fc252e2f0b57b17786ee119efa10392c6686")
                .setParameter("Sign", sign)
                .setParameter(nonceName, nonceNum.toString())
                .build();

        HttpPost httpPost = new HttpPost(uri);

        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
//        httpPost.setHeader("Key", "K-ffb834e1e4763124b7b6912d4537efd81c44e79f");
//        httpPost.addHeader("Sign", "S-574448e074467030ebbe502bff02cb5278ab85ee");
//        httpPost.setHeader("Sign", sign);


        System.out.println(httpPost.toString());
        HttpResponse httpResponse = client.execute(httpPost);
        String response = EntityUtils.toString(httpResponse.getEntity());

        System.out.println(response);
    }

    public void execute() throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException {
        method();
    }
}
