import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Double.parseDouble;

class WorkAlgoritm {

    private TradesPrices tradesPrices;
    private OrderBookPrices orderBookPrices;
    private PostRequests postRequests;
    private Integer maxOrdersCount;
    private Double orderPriceDelta;
    private String currencyPair;

    private Double upperBorder;
    private Double lowerBorder;
    private Double uppestBorder;
    private Double lowestBorder;
    private Integer sellOrdersCount = 0;
    private Integer buyOrdersCount = 0;
//    Sell - это покупка  битков, Buy - это продажа битков
//    НЕ ЗАТУПИ С SELL И BUY
//    После, в идеале будет торговля на нескольких коинах, в пропертях определять процент и лимит торгов - вроде решил, еще на свежую голову посмотреть
//    Подумать о том, чтобы каж дый вечер закрывать все открытые ордера???? - думаю нет

//    in parameters: border persent?, qty!

    WorkAlgoritm(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests,
                 Integer maxOrdersCount, Double orderPriceDelta, String currencyPair) {
        this.tradesPrices = tradesPrices;
        this.orderBookPrices = orderBookPrices;
        this.postRequests = postRequests;
        this.maxOrdersCount = maxOrdersCount;
        this.orderPriceDelta = orderPriceDelta;
        this.currencyPair = currencyPair;
    }

    void start() throws Exception {

        while (true) {
            createCorridors();
            boolean needNewCorridors = false;
            Double actualSellPrice;
            Double actualBuyPrice;
            while (!needNewCorridors) {
                actualSellPrice = tradesPrices.getActualSellPrice();
                actualBuyPrice = tradesPrices.getActualBuyPrice();
                System.out.println("In main menu: sell price - " + actualSellPrice + ", buy price - " + actualBuyPrice);
                if (actualBuyPrice > lowerBorder && actualSellPrice < upperBorder) {
                    actionInMainCorridor();
                }
                if (actualBuyPrice < lowerBorder && actualBuyPrice > lowestBorder) {
                    needNewCorridors = actionInLowCorridor();
                }
                if (actualSellPrice > upperBorder && actualSellPrice < uppestBorder) {
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
        lowerBorder = actualPrice * 0.993;
        uppestBorder = actualPrice * 1.014;
        lowestBorder = actualPrice * 0.986;
        System.out.println();
        System.out.println("Корридор сделали: ");
        System.out.println("uppest border - " + uppestBorder);
        System.out.println("upper border - " + upperBorder);
        System.out.println("lower border - " + lowerBorder);
        System.out.println("lowest border - " + lowestBorder);
        System.out.println();
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
            actualSellPrice = tradesPrices.getActualSellPrice();
            actualBuyPrice = tradesPrices.getActualBuyPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
            System.out.println();
            System.out.println("in main corridor buy price - " + actualBuyPrice);
            System.out.println("in main corridor upper bid price - " + upperBidPrice);
            System.out.println("in main corridor sell price - " + actualSellPrice);
            System.out.println("in main corridor upper ask price - " + upperAskPrice);
            System.out.println();
            if (actualSellPrice < lowerBorder || actualBuyPrice > upperBorder) {
                System.out.println("Exit from main corridor due price moved through lower or higher border");
                return;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (postRequests.getUserOpenOrdersNum() > 0) {
                Map<String, String> parameters;
                Double price;
                String type;
                for (Object jsonObject : postRequests.getUserOpenOrders(currencyPair)) {
                    parameters = getJsonValues(jsonObject);
                    price = parseDouble(parameters.get("price"));
                    type = parameters.get("type");
                    if (price < upperBorder) {
                        if (type.equals("sell")) {
                            if (prevSellPrice >= actualSellPrice && upperAskPrice > price) {
                                replaceOrderOnTopOfGlass(parameters, 1.0, upperAskPrice + orderPriceDelta);
                                System.out.println("Replace order in main corridor due order from upper corridor");
                            } else if (prevSellPrice < actualSellPrice) {
                                cancelOrder(parameters);
                                System.out.println("Cancel order in main corridor due order from upper corridor");
                            }
                        }
                    } else if (price > lowerBorder) {
                        if (type.equals("buy")) {
                            if (prevBuyPrice <= actualBuyPrice && upperBidPrice > price) {
                                replaceOrderOnTopOfGlass(parameters, 1.0, upperBidPrice + orderPriceDelta);
                                System.out.println("Replace order in main corridor due order from lower corridor");
                            } else if (prevBuyPrice > actualBuyPrice) {
                                cancelOrder(parameters);
                                System.out.println("Cancel order in main corridor due order from lower corridor");
                            }
                        }
                    } else {
                        cancelOrder(parameters);
                        System.out.println("Cancel order in main corridor due order from main corridor");
                    }
                }
            }
            prevBuyPrice = actualBuyPrice;
            prevSellPrice = actualSellPrice;
            count++;
            System.out.println("Runway number in main corridor - " + count);
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
            System.out.println();
            System.out.println("in low corridor sell price - " + actualBuyPrice);
            System.out.println("in low corridor upper ask price - " + upperAskPrice);
            System.out.println("in low corridor upper bid price - " + upperBidPrice);
            System.out.println();
            if (actualBuyPrice > lowerBorder) {
                System.out.println("Exit in main corridor from low corridor due price moved through lower border");
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            if (actualBuyPrice > prevBuyPrice && buyOrdersCount <= maxOrdersCount) {
                createOrder(1.0, upperBidPrice + orderPriceDelta, "buy", currencyPair);
                System.out.println("Create order in low corridor due good condition");
            }
            if (postRequests.getUserOpenOrdersNum() > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getUserOpenOrders(currencyPair)) {
                    parameters = getJsonValues(jsonObject);
                    price = parseDouble(parameters.get("price"));
                    if (price < upperBorder && price < upperAskPrice) {
                        replaceOrderOnTopOfGlass(parameters, 1.0, upperAskPrice + orderPriceDelta);
                        System.out.println("Replace order in low corridor due order from upper corridor");
                    } else if (actualBuyPrice >= prevBuyPrice && price < upperBidPrice) {
                        replaceOrderOnTopOfGlass(parameters, 1.0, upperBidPrice + orderPriceDelta);
                        System.out.println("Replace order in low corridor due this order was created due good condition");
                    } else if (actualBuyPrice < prevBuyPrice) {
                        cancelOrder(parameters);
                        System.out.println("Cancel order in low corridor due bad condition");
                    }
                }
            }
            if (actualBuyPrice < lowestBorder) {
                if (sellOrdersCount <= maxOrdersCount) {
                    createOrder(1.0, upperAskPrice + orderPriceDelta, "sell", currencyPair);
                    System.out.println("Create order in low corridor due price below lowestBorder");
                }
                System.out.println("Exit in main menu for creating new corridors from low corridor due price moved through lowest border");
                return true;
            }
            prevBuyPrice = actualBuyPrice;
            count++;
            System.out.println("Runway number in low corridor - " + count);
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
            System.out.println();
            System.out.println("in high corridor buy price - " + actualSellPrice);
            System.out.println("in high corridor upper ask price - " + upperAskPrice);
            System.out.println("in high corridor upper bid price - " + upperBidPrice);
            System.out.println();
            if (actualSellPrice < upperBorder) {
                System.out.println("Exit in main corridor from high corridor due price moved through upper border");
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (actualSellPrice < prevSellPrice && sellOrdersCount <= maxOrdersCount) {
                createOrder(1.0, upperAskPrice + orderPriceDelta, "sell", currencyPair);
                System.out.println("Create order in high corridor due good condition");
            }
            if (postRequests.getUserOpenOrdersNum() > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getUserOpenOrders(currencyPair)) {
                    parameters = getJsonValues(jsonObject);
                    price = parseDouble(parameters.get("price"));
                    if (price > lowerBorder && price < upperBidPrice) {
                        replaceOrderOnTopOfGlass(parameters, 1.0, upperBidPrice + orderPriceDelta);
                        System.out.println("Replace order in high corridor due order from lower corridor");
                    } else if (actualSellPrice <= prevSellPrice && price < upperAskPrice) {
                        replaceOrderOnTopOfGlass(parameters, 1.0, upperAskPrice + orderPriceDelta);
                        System.out.println("Replace order in high corridor due order was created due good condition");
                    } else if (actualSellPrice > prevSellPrice) {
                        cancelOrder(parameters);
                        System.out.println("Cancel order in high corridor due bad condition");
                    }
                }
            }
            if (actualSellPrice > uppestBorder) {
                if (buyOrdersCount <= maxOrdersCount) {
                    createOrder(1.0, upperBidPrice + orderPriceDelta, "buy", currencyPair);
                    System.out.println("Create order in high corridor price upper uppestBorder");
                }
                System.out.println("Exit in main menu for creating new corridors from high corridor due price moved through hoghest border");
                return true;
            }
            prevSellPrice = actualSellPrice;
            count++;
            System.out.println("Runway number in high corridor - " + count);
        }
    }

    private Map<String, String> getJsonValues(Object jsonObject) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", ((JSONObject) jsonObject).get("type").toString());
        parameters.put("price", ((JSONObject) jsonObject).get("price").toString());
        parameters.put("orderId", ((JSONObject) jsonObject).get("order_id").toString());
        return parameters;
    }

    private void replaceOrderOnTopOfGlass(Map<String, String> parameters, Double qty, Double price) throws Exception {
        cancelOrder(parameters);
        createOrder(qty, price, parameters.get("type"), currencyPair);
    }

    private void cancelOrder(Map<String, String> parameters) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("order_id", parameters.get("orderId"));
        String type = parameters.get("type");
        JSONObject jsonObject = postRequests.sendPostRequestAndGetResponse("order_cancel", arguments);
        String result = jsonObject.get("result").toString();
        System.out.println("Result of cancel order - " + result);
        if (!result.equals("true")) {
            System.out.println("Error of cancel order - " + jsonObject.get("error"));
            return;
        }
        if (type.equals("buy")) {
            buyOrdersCount--;
        } else if (type.equals("sell")) {
            sellOrdersCount--;
        } else {
            throw new Exception("Wrong type");
        }
        System.out.println("Number of open buy orders - " + buyOrdersCount);
        System.out.println("Number of open sell orders - " + sellOrdersCount);
    }

    private void createOrder(Double qty, Double price, String type, String currencyPair) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", currencyPair);
        arguments.put("quantity", qty.toString());
        arguments.put("price", price.toString());
        arguments.put("type", type);
        JSONObject jsonObject = postRequests.sendPostRequestAndGetResponse("order_create", arguments);
        String result = jsonObject.get("result").toString();
        System.out.println("Result of create order - " + result);
        if (!result.equals("true")) {
            System.out.println("Error of create order - " + jsonObject.get("error"));
            return;
        }
        if (type.equals("buy")) {
            buyOrdersCount++;
        } else if (type.equals("sell")) {
            sellOrdersCount++;
        } else {
            throw new Exception("Wrong type");
        }
        System.out.println("Number of open buy orders - " + buyOrdersCount);
        System.out.println("Number of open sell orders - " + sellOrdersCount);
    }
}