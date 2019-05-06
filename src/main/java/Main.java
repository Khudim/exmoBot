import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.Thread.sleep;
import static org.apache.logging.log4j.core.util.Loader.getClassLoader;

public class Main {
// ** Может быть мне не надо открывать туннели для каждого типа запросов? Надо посмотреть. Что-то мне подсказывает, что достаточно \
//    \ будет одного туннеля для всех типов запросов и валютных пар. Но это не точно... Сделал, ща увидим. \
//    \ Не, ну его нахер. Его надо делать потокобезопасным, а это опять ограничения по времени
// ** Сделать несколько торговых коинов, например, 5 : проблема ограничения количества запросов в минуту. надо подумать оптимальное количество пар и запросов \
//    \ В результате 3 пары могут торговаться за раз, но через какое-то время происходит превышение количества запросов, \
//    \ из-за чего потоки по 2-м парам падают. Необходимо подумать и оптимизировать код так, чтобы по 3-м парам в минуту было не больше 180 запросов, \
//    \ т.е. 60 запросов в минуту на одну пару. Учитывая скорость торговли, я думаю этого количества запросов будет достаточно. \
//    \ Сделал попытку решить эту проблему путем сокращения запросов в основном алгоритме только проверкой открытых ордеров в основном \
//    \ меню перерд вхождением в цикл корридора, а в корридоре ориентируюсь по каунтам открытых ордеров. Посмотрим...
//  * Добавить логгер и писать логи в соответствующие файлы логов : посмотреть, может можно сделать читаемее. \
//    \ Логгер есть, но надо сделать читаемее. Пишу в разные файлы. Теперь бы сам лог читаемее сделать и все ок )
// ** Еще нормально обрабатывать Exceptions и тоже писать в отдельный лог
//  * А изюминкой, но, на мой взгляд обязательной, стоит из всего этого сделать, наверное, джарник и кинуть на какой-нибудь сервак, \
//    \ чтобы использовать ноут, не боясь, запороть процесс. Как вариант виртуалку от яндекса
//  * От субд еще не отказался
//  * И сделать наконец из всего этого дерьма джарник
//  * Рефачить и делать все красиво будем в последнюю очередь, когда сделаем остальные задачи
//  * С реплэйсом в стакане проблемы. Надо подебажить что ли...

    public static void main(String[] args) throws InterruptedException {

        LinkedHashMap apiParams = null;
        try {
            apiParams = getPairArgumentsMap("api.yml");
        } catch (IOException ae) {
            ae.printStackTrace();
        }

        if (apiParams == null) {
            throw new IllegalStateException("apiParams not init");
        }

        final LinkedHashMap finalApiParams = apiParams;

        LinkedHashMap pairsParameters = null;
        try {
            pairsParameters = getPairArgumentsMap("pairs_parameters.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int pairsCount = pairsParameters.entrySet().size();
        Map<String, String> filePattern = (Map<String, String>) pairsParameters.get("file_pattern");
        ExecutorService pool = Executors.newFixedThreadPool(pairsCount);

        for (Object pair : pairsParameters.entrySet()) {
            String currencyPair = ((Map.Entry<String, HashMap<String, Double>>) pair).getKey();
            if (currencyPair.equals("file_pattern")) {
                break;
            }
            Map<String, Double> arguments = ((Map.Entry<String, HashMap<String, Double>>) pair).getValue();

            pool.submit(() -> initWorkWithCurrencyPair(
                    finalApiParams,
                    currencyPair,
                    arguments.get("qty"),
                    arguments.get("max_orders_count"),
                    arguments.get("delta"),
                    arguments.get("persent"),
                    filePattern.get("file_pattern_" + currencyPair)
            ));

            sleep(5000);
        }
    }

    private static void initWorkWithCurrencyPair(LinkedHashMap apiParams, String currencyPair, Double qty,
                                                 Double maxOrdersCount, Double delta, Double persent,
                                                 String filePattern) {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        TradesPrices tradesPrices = new TradesPrices(currencyPair);
        pool.submit(() -> {
            try {
                tradesPrices.exetute();
            } catch (BotException e) {
                e.printStackTrace();
            }
        });
        OrderBookPrices orderBookPrices = new OrderBookPrices(currencyPair);
        pool.submit(() -> {
            try {
                orderBookPrices.execute();
            } catch (BotException e) {
                e.printStackTrace();
            }
        });
        PostRequests postRequests = new PostRequests(
                (String) apiParams.get("key"),
                (String) apiParams.get("secret"),
                currencyPair
        );
        pool.submit(() -> {
            try {
                new WorkAlgoritm(tradesPrices, orderBookPrices, postRequests, maxOrdersCount, delta, currencyPair,
                        persent, qty, filePattern).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static LinkedHashMap getPairArgumentsMap(String parametersName) throws IOException {
        Yaml yaml = new Yaml();
        InputStream is = getClassLoader().getResourceAsStream(parametersName);
        LinkedHashMap cp = yaml.load(is);
        is.close();
        return cp;
    }

    public static Logger initLogger(String loggerName, String filePattern) {
        Logger logger = Logger.getLogger(loggerName);

        try {
            FileHandler fhandler = new FileHandler("./logs/" + filePattern, 10000000, 15);
            SimpleFormatter sformatter = new SimpleFormatter();
            fhandler.setFormatter(sformatter);
            logger.addHandler(fhandler);
        } catch (SecurityException | IOException e) {
            logger.severe("log error");
        }
        return logger;
    }
}