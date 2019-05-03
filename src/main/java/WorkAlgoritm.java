import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class WorkAlgoritm {

    private TradesPrices tradesPrices;
    private OrderBookPrices orderBookPrices;
    private PostRequests postRequests;

    private Double upperBorder;
    private Double lowBorder;
    private Double newUpperBorder;
    private Double newLowBorder;
    private Integer ordersCount = 0;
    private Integer maxOrdersCount;
//    Sell - это покупка  битков, Buy - это продажа битков
//    Подумать о том, чтобы каж дый вечер закрывать все открытые ордера????
//    НЕ ЗАТУПИ С SELL И BUY

    WorkAlgoritm(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests, Integer maxOrdersCount) {
        this.tradesPrices = tradesPrices;
        this.orderBookPrices = orderBookPrices;
        this.postRequests = postRequests;
        this.maxOrdersCount = maxOrdersCount;
    }

    void start() throws Exception {

        while (true) {
            createCorridors();
            boolean needNewCorridors = false;
            while (!needNewCorridors) {
                Double actualSellPrice = tradesPrices.getActualSellPrice();
                Double actualBuyPrice = tradesPrices.getActualBuyPrice();
                System.out.println("В основном под меню: sell price - " + actualSellPrice + ", buy price - " + actualBuyPrice);
                if (actualBuyPrice > lowBorder && actualSellPrice < upperBorder) {
                    actionInMainCorridor();
                }
                if (actualBuyPrice < lowBorder && actualBuyPrice > newLowBorder) {
                    needNewCorridors = actionInLowCorridor();
                }
                if (actualSellPrice > upperBorder && actualSellPrice < newUpperBorder) {
                    needNewCorridors = actionInHighCorridor();
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
        // После, в идеале будет торговля на нескольких коинах, в пропертях определять процент и лимит торгов
        System.out.println("Корридор сделали: " + upperBorder + ", " + lowBorder + ", " + newLowBorder + ", " + newUpperBorder);
    }

    private void actionInMainCorridor() throws Exception {
        int count = 0;
        Double prevBuyPrice = null;
        Double prevSellPrice = null;
        Double actualSellPrice;
        Double actualBuyPrice;
        Double upperAskPrice;
        Double upperBidPrice;
        while (true) {
            System.out.println();
            actualSellPrice = tradesPrices.getActualSellPrice();
            actualBuyPrice = tradesPrices.getActualBuyPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
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
            if (postRequests.getOpenOrdersNum("user_open_orders") > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "TRX_USD",
                        null)) {
                    parameters = getJsonValues(jsonObject);
                    price = Double.parseDouble(parameters.get("price"));
                    String type = parameters.get("type");
                    if (price < upperBorder) {
                        if (type.equals("sell")) {
                            if (prevSellPrice >= actualSellPrice && upperAskPrice > price) {
                                replaceOrderOnTopOfGlass(parameters, upperAskPrice);
                            } else if (prevSellPrice < actualSellPrice) {
                                cancelOrder(parameters);
                            }
                        }
                    } else if (price > lowBorder) {
                        if (type.equals("buy")) {
                            if (prevBuyPrice <= actualBuyPrice && upperBidPrice > price) {
                                replaceOrderOnTopOfGlass(parameters, upperBidPrice);
                            } else if (prevBuyPrice > actualBuyPrice) {
                                cancelOrder(parameters);
                            }
                        }
                    } else {
                        cancelOrder(parameters);
                    }
                }
            }
            prevBuyPrice = actualBuyPrice;
            prevSellPrice = actualSellPrice;
            System.out.println("Крутим main корридор - " + count);
            count++;
        }
    }


    private boolean actionInLowCorridor() throws Exception {
        int count = 0;
        Double prevBuyPrice = null;
        Double actualBuyPrice;
        Double upperAskPrice;
        Double upperBidPrice;
        while (true) {
            actualBuyPrice = tradesPrices.getActualBuyPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
            System.out.println("in low corridor sell price - " + actualBuyPrice);
            System.out.println("in low corridor upper ask price - " + upperAskPrice);
            System.out.println("in low corridor upper bid price - " + upperBidPrice);
            if (actualBuyPrice > lowBorder) {
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            if (actualBuyPrice > prevBuyPrice && ordersCount < maxOrdersCount) {
                System.out.println("Create buy order");
                createOrder(1.0, upperBidPrice + 0.0000001, "buy", "TRX_USD");
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "TRX_USD",
                        null)) {
                    parameters = getJsonValues(jsonObject);
                    price = Double.parseDouble(parameters.get("price"));
                    if (price < upperBorder && price < upperAskPrice) {
                        replaceOrderOnTopOfGlass(parameters, upperAskPrice);
                    } else if (actualBuyPrice >= prevBuyPrice && price < upperBidPrice) {
                        replaceOrderOnTopOfGlass(parameters, upperBidPrice);
                    } else if (actualBuyPrice < prevBuyPrice) {
                        cancelOrder(parameters);
                    }

                }
            }
            if (actualBuyPrice < newLowBorder) {
                if (ordersCount > -maxOrdersCount) {
                    createOrder(1.0, upperAskPrice + 0.0000001, "sell", "TRX_USD");
                }
                return true;
            }
            prevBuyPrice = actualBuyPrice;
            System.out.println("Крутим нижний корридор - " + count);
            count++;
        }
    }

    private boolean actionInHighCorridor() throws Exception {
        int count = 0;
        Double prevSellPrice = null;
        Double actualSellPrice;
        Double upperAskPrice;
        Double upperBidPrice;
        while (true) {
            actualSellPrice = tradesPrices.getActualSellPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
            System.out.println("in high corridor buy price - " + actualSellPrice);
            System.out.println("in high corridor upper ask price - " + upperAskPrice);
            System.out.println("in high corridor upper bid price - " + upperBidPrice);
            if (actualSellPrice < upperBorder) {
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (actualSellPrice < prevSellPrice && ordersCount > -maxOrdersCount) {
                System.out.println("Create sell order");
                createOrder(1.0, upperAskPrice + 0.0000001, "sell", "TRX_USD");
            }
            if (postRequests.getOpenOrdersNum("user_open_orders") > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getResponse("user_open_orders", "TRX_USD",
                        null)) {
                    parameters = getJsonValues(jsonObject);
                    price = Double.parseDouble(parameters.get("price"));
                    if (price > lowBorder && price < upperBidPrice) {
                        replaceOrderOnTopOfGlass(parameters, upperBidPrice);
                    } else if (actualSellPrice <= prevSellPrice && price < upperAskPrice) {
                        replaceOrderOnTopOfGlass(parameters, upperAskPrice);
                    } else if (actualSellPrice > prevSellPrice) {
                        cancelOrder(parameters);
                    }
                }
            }
            if (actualSellPrice > newUpperBorder) {
                if (ordersCount < maxOrdersCount) {
                    createOrder(1.0, upperBidPrice + 0.0000001, "buy", "TRX_USD");
                }
                return true;
            }
            prevSellPrice = actualSellPrice;
            System.out.println("Крутим верхний корридор - " + count);
            count++;
        }
    }

    private JSONObject createOrder(Double qty, Double price, String type, String currencyPair) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", currencyPair);
        arguments.put("quantity", qty.toString());
        arguments.put("price", price.toString());
        arguments.put("type", type);
        if (type.equals("buy")) {
            ordersCount++;
        } else if (type.equals("sell")) {
            ordersCount--;
        } else {
            throw new Exception("Wrong type");
        }
        return postRequests.getResponse("order_create", arguments);
    }

    private Map<String, String> getJsonValues(Object jsonObject) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", ((JSONObject) jsonObject).get("type").toString());
        parameters.put("price", ((JSONObject) jsonObject).get("price").toString());
        parameters.put("orderId", ((JSONObject) jsonObject).get("order_id").toString());
        return parameters;
    }

    private void replaceOrderOnTopOfGlass(Map <String, String> parameters, Double price) throws Exception {
        cancelOrder(parameters);
        createOrder(1.0, price + 0.0000001, parameters.get("type"), "TRX_USD");
    }

    private JSONObject cancelOrder(Map<String, String> parameters) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("order_id", parameters.get("orderId"));
        String type = parameters.get("type");
        if (type.equals("buy")) {
            ordersCount--;
        } else if (type.equals("sell")) {
            ordersCount++;
        } else {
            throw new Exception("Wrong type");
        }
        return postRequests.getResponse("order_cancel", arguments);
    }
}