import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;
import static org.apache.logging.log4j.core.util.Loader.getClassLoader;

public class Main {
//    Сделать несколько торговых коинов, например, 5 : проблема ограничения количества запросов в минуту. надо подумать оптимальное количество пар и запросов
//    Добавить логгер и писать логи в соответствующие файлы логов : посмотреть, может можно сделать читаемее
//    Еще нормально обрабатывать Exceptions и тоже писать в отдельный лог
//    А изюминкой, но, на мой взгляд обязательной, стоит из всего этого сделать, наверное, джарник и кинуть на какой-нибудь сервак, \
//    \ чтобы использовать ноут, не боясь, запороть процесс. Как вариант виртуалку от яндекса
//    от субд еще не отказался

    public static void main(String[] args) throws InterruptedException {
        LinkedHashMap cp = null;
        try {
            cp = getPairArgumentsMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Integer pairsCount = cp.entrySet().size();
        Map<String, String> filePattern = (Map<String, String>) cp.get("file_pattern");
        ExecutorService pool  = Executors.newFixedThreadPool(pairsCount);
        for (Object pair: cp.entrySet()) {
            String currencyPair = ((Map.Entry<String, HashMap<String, Double>>) pair).getKey();
            if (currencyPair.equals("file_pattern")){
                break;
            }
            Map<String, Double> arguments = ((Map.Entry<String, HashMap<String, Double>>) pair).getValue();
            pool.submit(() -> initWorkWithCurrencyPair(currencyPair, arguments.get("qty"), arguments.get("max_orders_count"),
                    arguments.get("delta"), arguments.get("persent"), filePattern.get("file_pattern_" + currencyPair)));
            sleep(5000);
        }
    }

    private static LinkedHashMap getPairArgumentsMap() throws IOException {
        Yaml yaml = new Yaml();
        InputStream is = getClassLoader().getResourceAsStream("pairs_parameters.yml");
        LinkedHashMap cp = yaml.load(is);
        is.close();
        return cp;
    }

    private static void initWorkWithCurrencyPair(String currencyPair, Double qty, Double maxOrdersCount, Double delta,
                                                 Double persent, String filePattern) {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        TradesPrices tradesPrices = new TradesPrices(currencyPair);
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
        OrderBookPrices orderBookPrices = new OrderBookPrices(currencyPair);
        pool.submit(() -> {
            try {
                orderBookPrices.execute();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
                System.exit(-1);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
                new WorkAlgoritm(tradesPrices, orderBookPrices, ps, maxOrdersCount, delta, currencyPair, persent, qty, filePattern)
                        .start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}