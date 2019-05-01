import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        TradesPrices tradesPrices = new TradesPrices("BTC_USD");
        System.out.println("Начинаем запускать потоки");
        pool.submit(() -> {
            try {
                tradesPrices.exetute();
            } catch (URISyntaxException | IOException e){
                e.printStackTrace();
                System.exit(-1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("Запустили TradesPrices");
        OrderBookPrices orderBookPrices = new OrderBookPrices("BTC_USD");
        pool.submit(() -> {
            try {
                orderBookPrices.execute();
            } catch (URISyntaxException | IOException e){
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
        pool.submit(() -> new WorkAlgoritm().start(tradesPrices, orderBookPrices, ps));
        System.out.println("Запустили алгоритм");
    }
}