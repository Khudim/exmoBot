import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WorkAlgoritm {

    private Double upperBorder;
    private Double lowBorder;
    private Double newUpperBorder;
    private Double newLowBorder;
    private String historyTradeId = "";
//    Sell - это покупка  битков, Buy - это продажа битков
//    Добавить ограничения по сумме, т.е. запрашивать баланс
//    Подумать о том, чтобы каждый вечер закрывать все открытые ордера????
    // НЕ ЗАТУПИ С SELL И BUY
    // TradePrices, OrderBookPrices, PostRequests сделать глобальными и в конструктор??????

    public void start(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests) {

        while (true) {
            System.out.println("Ща бум создавать корридор");
            createCorridors(tradesPrices);
            boolean needNewCorridors = false;
            while (!needNewCorridors) {
                Double actualSellPrice = tradesPrices.getActualSellPrice();
                Double actualBuyPrice = tradesPrices.getActualBuyPrice();
                if (actualSellPrice > lowBorder && actualBuyPrice < upperBorder) {
                    actionInMainCorridor(tradesPrices, postRequests, orderBookPrices);
                }
                if (actualSellPrice < lowBorder && actualSellPrice > newLowBorder) {
                    needNewCorridors = actionInLowCorridor(tradesPrices, postRequests, orderBookPrices);
                }
                if (actualBuyPrice > upperBorder && actualBuyPrice < newUpperBorder) {
                    needNewCorridors = actionInHighCorridor(tradesPrices, postRequests, orderBookPrices);
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

    private void actionInMainCorridor(TradesPrices tradesPrices, PostRequests postRequests, OrderBookPrices orderBookPrices) {
        // Проверять ордера даже когда прайс не менятся? Вроде сделал - перерпроверить
//        Не запутайся с sell, buy и условием pric, перепроверь перед коммитом
        // Может использовать в этом корридоре и buy, и sell price?
        Double prevBuyPrice = null;
        boolean needNewHistoryTradeId = true;
        while (true) {
            Double actualSellPrice = tradesPrices.getActualSellPrice();
            Double actualBuyPrice = tradesPrices.getActualBuyPrice();
            Double upperAskPrice = orderBookPrices.getActualAskPrice();
            Double upperBidPrice = orderBookPrices.getActualBidPrice();
            if (actualSellPrice < lowBorder || actualBuyPrice > upperBorder) {
                return;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            JSONArray jsonArray = getUserTrades(postRequests);
            if (jsonArray.length() > 0) {
                for (Object jsonObject : jsonArray) {
                    String type = ((JSONObject) jsonObject).getJSONObject("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).getJSONObject("price")
                            .toString());
                    String tradeId = ((JSONObject) jsonObject).getJSONObject("trade_id")
                            .toString();
                    if (tradeId.equals(historyTradeId)) {
                        break;
                    }
                    if (prevBuyPrice > actualBuyPrice) {
                        if (type.equals("sell")) {
                            if (price < newLowBorder) {
                                createOrder(postRequests, 0.0, 0.0, "sell");
                                if (needNewHistoryTradeId) {
                                    historyTradeId = tradeId;
                                    needNewHistoryTradeId = false;
                                }
                            }
                        }
                    } else if (prevBuyPrice < actualBuyPrice) {
                        if (type.equals("buy")) {
                            if (price > newUpperBorder) {
                                createOrder(postRequests, 0.0, 0.0, "buy");
                                if (needNewHistoryTradeId) {
                                    historyTradeId = tradeId;
                                    needNewHistoryTradeId = false;
                                }
                            }
                        }
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
                    String orderId = ((JSONObject) jsonObject).getJSONObject("order_id").toString();
                    if (type.equals("buy")) {
                        if (price >= newUpperBorder) {
                            if (upperAskPrice < price) {
                                cancelOrder(postRequests, orderId);
                                createOrder(postRequests, 0.0, 0.0, "buy");
                            }
                        } else {
                            if (prevBuyPrice > actualBuyPrice) {
                                cancelOrder(postRequests, orderId);
                            } else if (prevBuyPrice < actualBuyPrice) {
                                cancelOrder(postRequests, orderId);
                            }
                        }
                    } else if (type.equals("sell")) {
                        if (price < newLowBorder) {
                            if (upperBidPrice > price) {
                                cancelOrder(postRequests, orderId);
                                createOrder(postRequests, 0.0, 0.0, "sell");
                            }
                        } else {
                            if (prevBuyPrice > actualBuyPrice) {
                                cancelOrder(postRequests, orderId);
                            } else if (prevBuyPrice < actualBuyPrice) {
                                cancelOrder(postRequests, orderId);
                            }
                        }
                    }
                }
            }
        }
    }


    private boolean actionInLowCorridor(TradesPrices tradesPrices, PostRequests postRequests, OrderBookPrices orderBookPrices) {
        // Проверять ордера даже когда прайс не меняется? Вроде сделал - перепроверить
        boolean needNewBorders = false;
        Double prevSellPrice = null;
        while (true) {
            Double actualSellPrice = tradesPrices.getActualSellPrice();
            Double upperAskPrice = orderBookPrices.getActualAskPrice();
            Double upperBidPrice = orderBookPrices.getActualBidPrice();
            if (actualSellPrice > lowBorder) {
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") == 0
                    && actualSellPrice > newLowBorder) {
                if (prevSellPrice < actualSellPrice) {
                    createOrder(postRequests, 0.0, 0.0, "buy");
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "BTC_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).getJSONObject("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).getJSONObject("price")
                            .toString());
                    String orderId = ((JSONObject) jsonObject).getJSONObject("order_id").toString();
                    if (actualSellPrice <= newLowBorder) {
                        if (type.equals("buy")) {
                            if (upperAskPrice < price) {
                                cancelOrder(postRequests, orderId);
                                createOrder(postRequests, 0.0, 0.0, "buy");
                            }
                        } else if (type.equals("sell")) {
                            cancelOrder(postRequests, orderId);
                        }
                        needNewBorders = true;
                    } else if (actualSellPrice > newLowBorder) {
                        if (prevSellPrice > actualSellPrice) {
                            if (type.equals("sell")) {
                                cancelOrder(postRequests, orderId);
                            }
                        }
                        if (prevSellPrice < actualSellPrice) {
                            if (type.equals("buy")) {
                                cancelOrder(postRequests, orderId);
                            }
                        }
                        if (type.equals("buy") && upperAskPrice < price) {
                            cancelOrder(postRequests, orderId);
                            createOrder(postRequests, 0.0, 0.0, "buy");
                        }
                        if (type.equals("sell") && upperBidPrice > price) {
                            cancelOrder(postRequests, orderId);
                            createOrder(postRequests, 0.0, 0.0, "sell");
                        }
                    }
                }
            }
            if (needNewBorders) {
                createOrder(postRequests, 0.0, 0.0, "sell");
                return true;
            }
        }
    }

    private boolean actionInHighCorridor(TradesPrices tradesPrices, PostRequests postRequests, OrderBookPrices orderBookPrices) {
        // Проверять ордера даже когда прайс не меняется, вроде сделал - перерпроверить
        boolean needNewBorders = false;
        Double prevBuyPrice = null;
        while (true) {
            Double actualBuyPrice = tradesPrices.getActualBuyPrice();
            Double upperAskPrice = orderBookPrices.getActualAskPrice();
            Double upperBidPrice = orderBookPrices.getActualBidPrice();
            if (actualBuyPrice < upperBorder) {
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") == 0
                    && actualBuyPrice < newUpperBorder) {
                if (prevBuyPrice > actualBuyPrice) {
                    createOrder(postRequests, 0.0, 0.0, "sell");
                }
            }
            if (postRequests.getOpenOrdersNum("user_open_orders", "BTC_USD") > 0) {
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "BTC_USD",
                        null)) {
                    String type = ((JSONObject) jsonObject).getJSONObject("type")
                            .toString();
                    Double price = Double.parseDouble(((JSONObject) jsonObject).getJSONObject("price")
                            .toString());
                    String orderId = ((JSONObject) jsonObject).getJSONObject("order_id").toString();
                    if (actualBuyPrice >= newUpperBorder) {
                        if (type.equals("sell")) {
                            if (upperBidPrice > price) {
                                cancelOrder(postRequests, orderId);
                                createOrder(postRequests, 0.0, 0.0, "sell");
                            }
                        } else if (type.equals("buy")) {
                            cancelOrder(postRequests, orderId);
                        }
                        needNewBorders = true;
                    } else if (actualBuyPrice < newUpperBorder) {
                        if (prevBuyPrice < actualBuyPrice) {
                            if (type.equals("buy")) {
                                cancelOrder(postRequests, orderId);
                            }
                        }
                        if (prevBuyPrice > actualBuyPrice) {
                            if (type.equals("sell")) {
                                cancelOrder(postRequests, orderId);
                            }
                        }
                        if (type.equals("buy") && upperAskPrice < price) {
                            cancelOrder(postRequests, orderId);
                            createOrder(postRequests, 0.0, 0.0, "buy");
                        }
                        if (type.equals("sell") && upperBidPrice > price) {
                            cancelOrder(postRequests, orderId);
                            createOrder(postRequests, 0.0, 0.0, "sell");
                        }
                    }
                }
            }
            if (needNewBorders) {
                createOrder(postRequests, 0.0, 0.0, "buy");
                return true;
            }
        }
    }

    private JSONArray getUserTrades(PostRequests postRequests) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", "BTC_USD");
        arguments.put("limit", "50");
        return postRequests.getResponse("user_trades", "BTC_USD", arguments);
    }

    private JSONArray createOrder(PostRequests postRequests, Double qty, Double price, String type) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", "BTC_USD");
        arguments.put("quantity", qty.toString());
        arguments.put("price", price.toString());
        arguments.put("type", type);
        return postRequests.getResponse("order_create", "BTC_USD", arguments);
    }

    private JSONArray cancelOrder(PostRequests postRequests, String orderId) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("order_id", orderId);
        return postRequests.getResponse("order_cancel", "BTC_USD", arguments);
    }
}