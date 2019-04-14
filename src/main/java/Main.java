import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException, URISyntaxException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        PostRequests postRequests = new PostRequests("user_info", null,
                "K-cfa9fc252e2f0b57b17786ee119efa10392c6686",
                "S-1261694bb16beecf19c2eadac2b23ed4711996e3");
        postRequests.execute();
//        pool.submit(() -> {
//            try {
//                new TradesPrices().exetute();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//        pool.submit(() -> {
//            try {
//                new OrderBookPrices().execute();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//        System.out.println("Конец");

//        System.out.println(System.nanoTime());
//
//        String str = "curl -X POST https://api.exmo.me/v1/user_info/ " +
//                "-H 'Content-Type: application/x-www-form-urlencoded' " +
//                "-H 'Key: K-ffb834e1e4763124b7b6912d4537efd81c44e79f' " +
//                "-H 'Sign: S-574448e074467030ebbe502bff02cb5278ab85ee' " +
//                "-d 'nonce=25685808926699'\n";
//        String result = "";
//        String line;
//        Process process = Runtime.getRuntime().exec(str);
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        while ((line = reader.readLine()) != null) {
//            result += line + "\n";
//        }
//
//        System.out.println(result);

//        https://api.exmo.com/v1/user_info?key=K-ffb834e1e4763124b7b6912d4537efd81c44e79f&sign=35bfa3a99981f6bcba5f7964ca97a883bc7445bfa4aa1c56c4da63ef9907d9ad&nonce=31944723047806
    }
}