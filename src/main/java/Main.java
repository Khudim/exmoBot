import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws ExecutionException,
            InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        DataChart chart = pool.submit(DataChart::new).get();
        double xData = 55;
        long start = System.currentTimeMillis();
        long finish = System.currentTimeMillis();
        int i = 0;
        while (finish - start < 120000) {
            xData++;
            i++;
            chart.updateGraph(chart, "aine", xData, 55.0 + i);
            chart.updateGraph(chart, "pine2", xData, 50.0 - i);
            chart.updateGraph(chart, "line3", xData, 52.5 + i * 0.5);
            chart.updateGraph(chart, "dine4", xData, 52.5 - i * 0.5);
            finish = System.currentTimeMillis();
        }
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
        System.out.println("Конец");
    }
}