import java.io.IOException;
import java.net.URISyntaxException;
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
        OrderBookPrices orderBookPrices = new OrderBookPrices("TRX_USD");
        pool.submit(() -> {
            try {
                orderBookPrices.execute();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
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
        final PostRequests ps = postRequests;
        pool.submit(() -> {
            try {
                new WorkAlgoritm(tradesPrices, orderBookPrices, ps, 7).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}