import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
//    Отрефачить так, чтобы запускалось несколько потоков по разным парам
//    Сделать несколько торговых коинов, например, 5
//    Протестировать создание и отмену ордеров - ок. Теперь с учетом прайсов!
//    По-хорошему прикрутить пропертя и СУБД

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        TradesPrices tradesPrices = new TradesPrices("TRX_USD");
        System.out.println("Начинаем запускать потоки");
        pool.submit(() -> {
            try {
                tradesPrices.exetute();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                System.exit(-1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("Запустили TradesPrices");
        OrderBookPrices orderBookPrices = new OrderBookPrices("TRX_USD");
        pool.submit(() -> {
            try {
                orderBookPrices.execute();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
        System.out.println("Запустили OrderBookPrices");
        PostRequests postRequests = null;
        try {
            postRequests = pool.submit(() -> new PostRequests(
                    "K-cfa9fc252e2f0b57b17786ee119efa10392c6686",
                    "S-1261694bb16beecf19c2eadac2b23ed4711996e3"
            )).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Запустили PostRequests");
        final PostRequests ps = postRequests;
        pool.submit(() -> new WorkAlgoritm(tradesPrices, orderBookPrices, ps).start());
        System.out.println("Запустили алгоритм");
    }

    @Test
    public void test() {
        PostRequests postRequests = new PostRequests(
                "K-cfa9fc252e2f0b57b17786ee119efa10392c6686",
                "S-1261694bb16beecf19c2eadac2b23ed4711996e3"
        );
        JSONObject jsonArray = createOrder(postRequests, 1.0, 0.023, "buy");
        String orderId = jsonArray.get("order_id").toString();
        cancelOrder(postRequests, orderId);
    }

    private JSONObject createOrder(PostRequests postRequests, Double qty, Double price, String type) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", "TRX_USD");
        arguments.put("quantity", qty.toString());
        arguments.put("price", price.toString());
        arguments.put("type", type);
        return postRequests.getResponse("order_create", arguments);
    }

    private JSONObject cancelOrder(PostRequests postRequests, String orderId) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("order_id", orderId);
        return postRequests.getResponse("order_cancel", arguments);
    }
}