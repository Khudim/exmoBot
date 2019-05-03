import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WorkAlgoritm {

    private TradesPrices tradesPrices;
    private OrderBookPrices orderBookPrices;
    private PostRequests postRequests;

    private Double upperBorder;
    private Double lowBorder;
    private Double newUpperBorder;
    private Double newLowBorder;
    private String historyTradeId = "";
//    Sell - это покупка  битков, Buy - это продажа битков
//    Добавить ограничения по сумме, т.е. запрашивать баланс
//    Подумать о том, чтобы каж дый вечер закрывать все открытые ордера????
//    НЕ ЗАТУПИ С SELL И BUY
//    Разобраться с определением прайса, количества и лимитов на ордера
//    Может еще сделать создание ордеров не только когда их нет, а еще если есть из других корридоров? В общем нужно перед запуском еще разок продумать варианты

    WorkAlgoritm(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests) {
        this.tradesPrices = tradesPrices;
        this.orderBookPrices = orderBookPrices;
        this.postRequests = postRequests;
    }

    public void start() {

        while (true) {
            createCorridors();
            System.out.println("Создали корридор и вернулись в основное меню");
            boolean needNewCorridors = false;
            System.out.println("Инициализировали необходимость нового корридора");
            while (!needNewCorridors) {
                System.out.println("Зашли в под меню");
                Double actualSellPrice = tradesPrices.getActualSellPrice();
                Double actualBuyPrice = tradesPrices.getActualBuyPrice();
                System.out.println("В основном под меню: sell price - " + actualSellPrice + ", buy price - " + actualBuyPrice);
                if (actualSellPrice > lowBorder && actualBuyPrice < upperBorder) {
                    System.out.println("Сейчас провалимся в main корридор");
                    actionInMainCorridor();
                    System.out.println("Вышли из main корридор");
                }
                if (actualSellPrice < lowBorder && actualSellPrice > newLowBorder) {
                    System.out.println("Сейчас провалимся в low корридор");
                    needNewCorridors = actionInLowCorridor();
                    System.out.println("Вышли из low корридор");
                }
                if (actualBuyPrice > upperBorder && actualBuyPrice < newUpperBorder) {
                    System.out.println("Сейчас провалимся в high корридор");
                    needNewCorridors = actionInHighCorridor();
                    System.out.println("Вышли из high корридор");
                }
            }
        }
    }

    private void createCorridors() {
        Double actualPrice;
        if (upperBorder == null) {
            synchronized (tradesPrices.getBuyPrice()) {
                try {
                    tradesPrices.getBuyPrice().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            actualPrice = (tradesPrices.getBuyPrice().get(0) + tradesPrices.getSellPrice().get(0)) / 2;
        } else {
            actualPrice = (tradesPrices.getActualBuyPrice() + tradesPrices.getActualSellPrice()) / 2;
        }
        upperBorder = actualPrice * 1.007;
        lowBorder = actualPrice * 0.993;
        newUpperBorder = actualPrice * 1.014;
        newLowBorder = actualPrice * 0.986;
        // Чтобы протестировать, надо будет сделать вообще мизерный доход, чтобы посмотреть как исполняются ордера
        // После, в идеале будет торговля на нескольких коинах, в пропертях определять процент и лимит торгов
        System.out.println("Корридор сделали: " + upperBorder + ", " + lowBorder + ", " + newLowBorder + ", " + newUpperBorder);
    }

    private void actionInMainCorridor() {
        // Проверять ордера даже когда прайс не менятся? Вроде сделал - перерпроверить
//        Не запутайся с sell, buy и условием price, перепроверь перед коммитом
        // Может использовать в этом корридоре и buy, и sell price?
        Double prevBuyPrice = null;
        Double prevSellPrice = null;
        boolean needNewHistoryTradeId = true;
        System.out.println("В main корридоре");
        int count = 0;
        while (true) {
            System.out.println();
            Double actualSellPrice = tradesPrices.getActualSellPrice();
            Double actualBuyPrice = tradesPrices.getActualBuyPrice();
            Double upperAskPrice = orderBookPrices.getActualAskPrice();
            Double upperBidPrice = orderBookPrices.getActualBidPrice();
            System.out.println("in main corridor buy price - " + actualBuyPrice);
            System.out.println("in main corridor sell price - " + actualSellPrice);
            System.out.println("in main corridor upper ask price - " + upperAskPrice);
            System.out.println("in main corridor upper bid price - " + upperBidPrice);
            if (actualSellPrice < lowBorder || actualBuyPrice > upperBorder) {
                return;
            }
            if (prevBuyPrice == null || prevSellPrice == null) {
                prevBuyPrice = actualBuyPrice;
                prevSellPrice = actualSellPrice;
                continue;
            }
            JSONArray jsonArray = getUserTrades("TRX_USD");
            if (jsonArray.length() > 0) {
                System.out.println("Смотрим историю");
                for (Object jsonObject : jsonArray) {
                    String type = ((JSONObject) jsonObject).get("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).get("price")
                            .toString());
                    String tradeId = ((JSONObject) jsonObject).get("trade_id")
                            .toString();
                    if (tradeId.equals(historyTradeId)) {
                        break;
                    }
                    if (prevBuyPrice > actualBuyPrice) {
                        if (type.equals("sell")) {
                            if (price < newLowBorder) {
//                                createOrder(0.0, 0.0, "sell", "TRX_USD");
                                System.out.println("Создал ордер sell из-за из нижнего корридора");
                                if (needNewHistoryTradeId) {
                                    historyTradeId = tradeId;
                                    needNewHistoryTradeId = false;
                                }
                            }
                        }
                    } else if (prevBuyPrice < actualBuyPrice) {
                        if (type.equals("buy")) {
                            if (price > newUpperBorder) {
//                                createOrder(0.0, 0.0, "buy", "TRX_USD");
                                System.out.println("Создал ордер buy из-за из верхнего корридора");
                                if (needNewHistoryTradeId) {
                                    historyTradeId = tradeId;
                                    needNewHistoryTradeId = false;
                                }
                            }
                        }
                    }
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") > 0) {
                System.out.println("Проверям открытые ордера");
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "TRX_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).get("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).get("price")
                            .toString());
                    String orderId = ((JSONObject) jsonObject).get("order_id").toString();
                    if (type.equals("buy")) {
                        if (price >= newUpperBorder) {
                            if (upperAskPrice < price) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер buy");
//                                createOrder(0.0, 0.0, "buy", "TRX_USD");
                                System.out.println("Создал ордер buy");
                            }
                        } else {
                            if (prevBuyPrice > actualBuyPrice) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер buy");
                            } else if (prevBuyPrice < actualBuyPrice) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер buy");
                            }
                        }
                    } else if (type.equals("sell")) {
                        if (price < newLowBorder) {
                            if (upperBidPrice > price) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер sell");
//                                createOrder(0.0, 0.0, "sell", "TRX_USD");
                                System.out.println("Создал ордер sell");
                            }
                        } else {
                            if (prevBuyPrice > actualBuyPrice) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер sell");
                            } else if (prevBuyPrice < actualBuyPrice) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер sell");
                            }
                        }
                    }
                }
            }
            System.out.println("Еще крутится - " + count);
            count++;
        }
    }


    private boolean actionInLowCorridor() {
        // Проверять ордера даже когда прайс не меняется? Вроде сделал - перепроверить
        boolean needNewBorders = false;
        Double prevSellPrice = null;
        int count = 0;
        while (true) {
            Double actualSellPrice = tradesPrices.getActualSellPrice();
            Double upperAskPrice = orderBookPrices.getActualAskPrice();
            Double upperBidPrice = orderBookPrices.getActualBidPrice();
            System.out.println("in low corridor sell price - " + actualSellPrice);
            System.out.println("in low corridor upper ask price - " + upperAskPrice);
            System.out.println("in low corridor upper bid price - " + upperBidPrice);
            if (actualSellPrice > lowBorder) {
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") == 0
                    && actualSellPrice > newLowBorder) {
                if (prevSellPrice < actualSellPrice) {
//                    createOrder(0.0, 0.0, "buy", "TRX_USD");
                    System.out.println("Создал ордер");
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "TRX_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).get("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).get("price")
                            .toString());
                    String orderId = ((JSONObject) jsonObject).get("order_id").toString();
                    if (actualSellPrice <= newLowBorder) {
                        if (type.equals("buy")) {
                            if (upperAskPrice < price) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер");
//                                createOrder(0.0, 0.0, "buy", "TRX_USD");
                                System.out.println("Создал ордер");
                            }
                        } else if (type.equals("sell")) {
//                            cancelOrder(orderId);
                            System.out.println("Заканселял ордер");
                        }
                        needNewBorders = true;
                    } else if (actualSellPrice > newLowBorder) {
                        if (prevSellPrice > actualSellPrice) {
                            if (type.equals("sell")) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер");
                            }
                        }
                        if (prevSellPrice < actualSellPrice) {
                            if (type.equals("buy")) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер");
                            }
                        }
                        if (type.equals("buy") && upperAskPrice < price) {
//                            cancelOrder(orderId);
                            System.out.println("Заканселял ордер");
//                            createOrder(0.0, 0.0, "buy", "TRX_USD");
                            System.out.println("Создал ордер");
                        }
                        if (type.equals("sell") && upperBidPrice > price) {
//                            cancelOrder(orderId);
                            System.out.println("Заканселял ордер");
//                            createOrder(0.0, 0.0, "sell", "TRX_USD");
                            System.out.println("Создал ордер");
                        }
                    }
                }
            }
            if (needNewBorders) {
//                createOrder(0.0, 0.0, "sell", "TRX_USD");
                System.out.println("Создал ордер");
                return true;
            }
            System.out.println("Еще крутится - " + count);
            count++;
        }
    }

    private boolean actionInHighCorridor() {
        // Проверять ордера даже когда прайс не меняется, вроде сделал - перерпроверить
        boolean needNewBorders = false;
        Double prevBuyPrice = null;
        int count = 0;
        while (true) {
            Double actualBuyPrice = tradesPrices.getActualBuyPrice();
            Double upperAskPrice = orderBookPrices.getActualAskPrice();
            Double upperBidPrice = orderBookPrices.getActualBidPrice();
            System.out.println("in high corridor buy price - " + actualBuyPrice);
            System.out.println("in high corridor upper ask price - " + upperAskPrice);
            System.out.println("in high corridor upper bid price - " + upperBidPrice);
            if (actualBuyPrice < upperBorder) {
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") == 0
                    && actualBuyPrice < newUpperBorder) {
                if (prevBuyPrice > actualBuyPrice) {
//                    createOrder(0.0, 0.0, "sell", "TRX_USD");
                    System.out.println("Создал ордер");
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "TRX_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).get("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).get("price")
                            .toString());
                    String orderId = ((JSONObject) jsonObject).get("order_id").toString();
                    if (actualBuyPrice >= newUpperBorder) {
                        if (type.equals("sell")) {
                            if (upperBidPrice > price) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер");
//                                createOrder(0.0, 0.0, "sell", "TRX_USD");
                                System.out.println("Создал ордер");
                            }
                        } else if (type.equals("buy")) {
//                            cancelOrder(orderId);
                            System.out.println("Заканселял ордер");
                        }
                        needNewBorders = true;
                    } else if (actualBuyPrice < newUpperBorder) {
                        if (prevBuyPrice < actualBuyPrice) {
                            if (type.equals("buy")) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер");
                            }
                        }
                        if (prevBuyPrice > actualBuyPrice) {
                            if (type.equals("sell")) {
//                                cancelOrder(orderId);
                                System.out.println("Заканселял ордер");
                            }
                        }
                        if (type.equals("buy") && upperAskPrice < price) {
//                            cancelOrder(orderId);

//                            createOrder(0.0, 0.0, "buy", "TRX_USD");
                            System.out.println("Создал ордер");
                        }
                        if (type.equals("sell") && upperBidPrice > price) {
//                            cancelOrder(orderId);
                            System.out.println("Заканселял ордер");
//                            createOrder(0.0, 0.0, "sell", "TRX_USD");
                            System.out.println("Создал ордер");
                        }
                    }
                }
            }
            if (needNewBorders) {
//                createOrder(0.0, 0.0, "buy", "TRX_USD");
                System.out.println("Создал ордер");
                return true;
            }
            System.out.println("Еще крутится - " + count);
            count++;
        }
    }

    private JSONArray getUserTrades(String currencyPair) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", currencyPair);
        arguments.put("limit", "50");
        return postRequests.getResponse("user_trades", currencyPair, arguments);
    }

    private JSONObject createOrder(Double qty, Double price, String type, String currencyPair) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", currencyPair);
        arguments.put("quantity", qty.toString());
        arguments.put("price", price.toString());
        arguments.put("type", type);
        return postRequests.getResponse("order_create", arguments);
    }

    private JSONObject cancelOrder(String orderId) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("order_id", orderId);
        return postRequests.getResponse("order_cancel", arguments);
    }
}