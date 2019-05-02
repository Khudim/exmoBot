import org.json.JSONObject;

public class WorkAlgoritm {

    private Double upperBorder;
    private Double lowBorder;
    private Double newUpperBorder;
    private Double newLowBorder;
//    Sell - это покупка  битков, Buy - это продажа битков
//    Добавить ограничения по сумме
//    Подумать о том, чтобы каждый вечер закрывать все открытые ордера????

    public void start(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests) {

        while (true) {
            System.out.println("Ща бум создавать корридор");
            createCorridors(tradesPrices);
            boolean needNewCorridors = false;
            while (!needNewCorridors) {
                if (tradesPrices.getActualSellPrice() > lowBorder
                        && tradesPrices.getActualBuyPrice() < upperBorder) {
                    actionInMainCorridor(tradesPrices, postRequests);
                }
                if (tradesPrices.getActualSellPrice() < lowBorder
                        && tradesPrices.getActualSellPrice() > newLowBorder) {
                    needNewCorridors = actionInLowCorridor(tradesPrices, postRequests);
                }
                if (tradesPrices.getActualBuyPrice() > upperBorder
                        && tradesPrices.getActualBuyPrice() < newUpperBorder) {
                    needNewCorridors = actionInHighCorridor(tradesPrices, postRequests);
                }
            }
        }
    }

    private void createCorridors(TradesPrices tradesPrices) {
        Double actualPrice;
        if (upperBorder == null) {
            synchronized (tradesPrices.getBuyPrice()) {
                try {
                    System.out.println("Ща бум блокировать по цене покупки");
                    tradesPrices.getBuyPrice().wait();
                    System.out.println("Разбудили");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("interr");
                }
            }
            System.out.println("Считаем");
            actualPrice = (tradesPrices.getBuyPrice().get(0) + tradesPrices.getSellPrice().get(0)) / 2;
        } else {
            actualPrice = (tradesPrices.getActualBuyPrice() + tradesPrices.getActualSellPrice()) / 2;
        }
        upperBorder = actualPrice * 1.01;
        lowBorder = actualPrice * 0.9900990099;
        newUpperBorder = actualPrice * 1.02;
        newLowBorder = actualPrice * 0.98039215686;
        // Чтобы протестировать, надо будет сделать вообще мизерный доход, чтобы посмотреть как исполняются ордера
        // После, в идеале будет торговля на нескольких коинах, в пропертях определять процент и лимит торгов
        System.out.println("Корридор сделали: " + upperBorder + ", " + lowBorder + ", " + newLowBorder + ", " + newUpperBorder);
    }

    private void actionInMainCorridor(TradesPrices tradesPrices, PostRequests postRequests) {
        // Проверять ордера даже когда прайс не менятся? Вроде сделал - перерпроверить
//        Не запутайся с sell, buy и условием pric, перепроверь перед коммитом
        Double prevBuyPrice = null;
        while (true) {
            if (tradesPrices.getActualSellPrice() < lowBorder
                    || tradesPrices.getActualBuyPrice() > upperBorder) {
                return;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = tradesPrices.getActualBuyPrice();
                continue;
            }
            if (prevBuyPrice > tradesPrices.getActualBuyPrice()) {
                if (false/*есть исполненные ордера на покупку*/) {
                    if (false/*исполненный ордер пришел из нижнего коридора*/) {
                        // выставляю ордер на продажу
                    }
                }
            } else if (prevBuyPrice < tradesPrices.getActualBuyPrice()) {
                if (false/*есть исполненные ордера на продажу*/) {
                    if (false/*исполненный ордер на продажу пришел из верхнего коридора*/) {
                        // выставляю ордер на покупку
                    }
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "BTC_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).getJSONObject("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).getJSONObject("price")
                            .toString());
                    if (type.equals("buy")) {
                        if (price >= newUpperBorder) {
                            if (false/*открытый ордер не в верху стакана на продажу*/) {
                                // переставляю ордер
                            }
                        } else {
                            if (prevBuyPrice > tradesPrices.getActualBuyPrice()) {
                                // отменяю ордер
                            } else if (prevBuyPrice < tradesPrices.getActualBuyPrice()) {
                                // отменяю ордер
                            }
                        }
                    } else if (type.equals("sell")) {
                        if (price < newLowBorder) {
                            if (false/*открытый ордер не в верху стакана на покупку*/) {
                                // переставляю ордер
                            }
                        } else {
                            if (prevBuyPrice > tradesPrices.getActualBuyPrice()) {
                                // отменяю ордер
                            } else if (prevBuyPrice < tradesPrices.getActualBuyPrice()) {
                                // отменяю ордер
                            }
                        }
                    }
                }
            }
        }
    }


    private boolean actionInLowCorridor(TradesPrices tradesPrices, PostRequests postRequests) {
        // Проверять ордера даже когда прайс не меняется? Вроде сделал - перепроверить
        boolean needNewBorders = false;
        Double prevSellPrice = null;
        while (true) {
            if (tradesPrices.getActualSellPrice() > lowBorder) {
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = tradesPrices.getActualSellPrice();
                continue;
            }
            if (/*есть исполненные ордера или * и * */postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") == 0
                    && tradesPrices.getActualSellPrice() > newLowBorder) {
                if (prevSellPrice < tradesPrices.getActualSellPrice()) {
                    // создаю ордер на покупку
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "BTC_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).getJSONObject("type")
                            .toString();
                    if (tradesPrices.getActualSellPrice() <= newLowBorder) {
                        if (type.equals("buy")) {
                            if (false/*открытый ордер не верхний в стакане на продажу*/) {
                                // переставляю ордер
                            }
                        } else if (type.equals("sell")) {
                            // отменяю ордер
                        }
                        needNewBorders = true;
                    } else if (tradesPrices.getActualSellPrice() > newLowBorder) {
                        if (prevSellPrice > tradesPrices.getActualSellPrice()) {
                            if (type.equals("sell")) {
                                // отменяю ордер
                            }
                        }
                        if (prevSellPrice < tradesPrices.getActualSellPrice()) {
                            if (type.equals("buy")) {
                                // отменяю ордер
                            }
                        }
                        if (type.equals("buy")/*и он не верхний в стакане на продажу*/) {
                            // переставляю ордер
                        }
                        if (type.equals("sell")/*и он нне верхний в стакане на покупку*/) {
                            // переставляю ордер
                        }
                    }

                }
            }
            if (needNewBorders) {
                // создаю ордер на продажу
                return true;
            }
        }
    }

    private boolean actionInHighCorridor(TradesPrices tradesPrices, PostRequests postRequests) {
        // Проверять ордера даже когда прайс не меняется, вроде сделал - перерпроверить
        boolean needNewBorders = false;
        Double prevBuyPrice = null;
        while (true) {
            if (tradesPrices.getActualBuyPrice() < upperBorder) {
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = tradesPrices.getActualBuyPrice();
                continue;
            }
            if (/*есть исполненные ордера или * и * */postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") == 0
                    && tradesPrices.getActualSellPrice() < newUpperBorder) {
                if (prevBuyPrice > tradesPrices.getActualBuyPrice()) {
                    // создаю ордер на продажу
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "BTC_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).getJSONObject("type")
                            .toString();
                    if (tradesPrices.getActualBuyPrice() >= newUpperBorder) {
                        if (type.equals("sell")) {
                            if (false/*открытый ордер не в верху стакана на продажу*/) {
                                // переставляю ордер
                            }
                        } else if (type.equals("buy")) {
                            // отменяю ордер
                        }
                        needNewBorders = true;
                    } else if (tradesPrices.getActualBuyPrice() < newUpperBorder) {
                        if (prevBuyPrice < tradesPrices.getActualBuyPrice()) {
                            if (type.equals("buy")) {
                                // отменяю ордер
                            }
                        }
                        if (prevBuyPrice > tradesPrices.getActualBuyPrice()) {
                            if (type.equals("sell")) {
                                // отменяю ордер
                            }
                        }
                        if (type.equals("buy")/*и он не верхний в стакане на продажу*/) {
                            // перерставляю ордер
                        }
                        if (type.equals("buy"/*и он не верхний в стакане на продажу*/)) {
                            // перерставляю ордер
                        }
                    }
                }
            }
            if (needNewBorders) {
                // создаю ордер на покупку
                return true;
            }
        }
    }
}