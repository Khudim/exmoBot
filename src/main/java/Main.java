import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        pool.submit(() -> new DataChart().createGraph());
        pool.submit(() -> {
            try {
                new TradesPrices().exetute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        pool.submit(() -> {
            try {
                new OrderBookPrices().execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}