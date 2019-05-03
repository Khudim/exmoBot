import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
//    Отрефачить так, чтобы запускалось несколько потоков по разным парам
//    Сделать несколько торговых коинов, например, 5
//    Протестировать создание и отмену ордеров - ок. Теперь с учетом прайсов! - вроде ок...
//    По-хорошему прикрутить пропертя (можно yml) и СУБД
//    Добавить логгер и писать логи в соответствующие файлы логов
//    Еще нормально обрабатывать Exceptions и тоже писать в отдельный лог

//    А изюминкой, но, на мой взгляд обязательной, стоит из всего этого сделать, наверное, джарник и кинуть на какой-нибудь сервак,
//    чтобы использовать ноут, не боясь, запороть процесс. Как вариант виртуалку от яндекса

//    Key and secret to property or yml file

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        initWorkWithCurrencyPair(pool, "TRX_USD", 7, 0.0000001);
    }

    private static void initWorkWithCurrencyPair(ExecutorService pool, String currencyPair, Integer maxOrdersCount,
                                                 Double delta) {
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
                new WorkAlgoritm(tradesPrices, orderBookPrices, ps, maxOrdersCount, delta, currencyPair).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}