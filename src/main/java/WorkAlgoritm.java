import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.Double.parseDouble;
import static java.lang.Thread.sleep;

class WorkAlgoritm {

    private TradesPrices tradesPrices;
    private OrderBookPrices orderBookPrices;
    private PostRequests postRequests;
    private Double maxOrdersCount;
    private Double orderPriceDelta;
    private String currencyPair;

    private Double upperBorder;
    private Double lowerBorder;
    private Double uppestBorder;
    private Double lowestBorder;

    private Double persent;
    private Double qty;

    private Integer sellOrdersCount = 0;
    private Integer buyOrdersCount = 0;

    private Double lastOrderPrice = 0.0;

    private Logger logger;

    WorkAlgoritm(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests,
                 Double maxOrdersCount, Double orderPriceDelta, String currencyPair, Double persent, Double qty, String filePattern) {
        this.tradesPrices = tradesPrices;
        this.orderBookPrices = orderBookPrices;
        this.postRequests = postRequests;
        this.maxOrdersCount = maxOrdersCount;
        this.orderPriceDelta = orderPriceDelta;
        this.currencyPair = currencyPair;
        this.persent = persent;
        this.qty = qty;

        this.logger = initLogger(filePattern);
    }

    void start() throws Exception {

        while (true) {
            createCorridors();
            boolean needNewCorridors = false;
            Double lastActualPrice;
            while (!needNewCorridors) {
                lastActualPrice = tradesPrices.getLastActualPrice();
//                logger.info("In main menu: sell price - " + actualSellPrice + ", buy price - " + actualBuyPrice);
                logger.info("In main menu: last actual price - " + lastActualPrice);
                if (lastActualPrice > lowerBorder && lastActualPrice < upperBorder) {
                    actionInMainCorridor();
                }
                if (lastActualPrice < lowerBorder && lastActualPrice > lowestBorder) {
                    needNewCorridors = actionInLowCorridor();
                }
                if (lastActualPrice > upperBorder && lastActualPrice < uppestBorder) {
                    needNewCorridors = actionInHighCorridor();
                }
            }
        }
    }

    private void createCorridors() {
        waitCondition();
        Double actualPrice = tradesPrices.getLastActualPrice();
        upperBorder = actualPrice * (1 + persent / 100);
        lowerBorder = actualPrice * (1 - persent / 100);
        uppestBorder = actualPrice * (1 + 2 * persent / 100);
        lowestBorder = actualPrice * (1 - 2 * persent / 100);
//        logger.info("");
        logger.info("Корридор сделали: ");
        logger.info("uppest border - " + uppestBorder);
        logger.info("upper border - " + upperBorder);
        logger.info("lower border - " + lowerBorder);
        logger.info("lowest border - " + lowestBorder);
//        logger.info("");
    }

    private void waitCondition() {
        if (upperBorder == null) {
            synchronized (tradesPrices.getActualPrice()) {
                try {
                    tradesPrices.getActualPrice().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (orderBookPrices.getAskPriceList()){
                try {
                    orderBookPrices.getAskPriceList().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void actionInMainCorridor() throws Exception {
        int count = 0;
        Double prevBuyPrice = null;
        Double prevSellPrice = null;
        Double actualSellPrice;
        Double actualBuyPrice;
        Double upperAskPrice;
        Double upperBidPrice;
        Double lastActualPrice;
        while (true) {
            actualSellPrice = tradesPrices.getActualSellPrice();
            actualBuyPrice = tradesPrices.getActualBuyPrice();
            lastActualPrice = tradesPrices.getLastActualPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
//            logger.info("");
            logger.info("in main corridor last actual price - " + lastActualPrice);
            logger.info("in main corridor buy price - " + actualBuyPrice);
            logger.info("in main corridor upper bid price - " + upperBidPrice);
            logger.info("in main corridor sell price - " + actualSellPrice);
            logger.info("in main corridor upper ask price - " + upperAskPrice);
//            logger.info("");
            if (lastActualPrice < lowerBorder || lastActualPrice > upperBorder) {
                logger.info("Exit from main corridor due price moved through lower or higher border");
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
                                replaceOrderOnTopOfGlass(parameters, qty, upperAskPrice + orderPriceDelta);
                                logger.info("Replace order in main corridor due order from upper corridor");
                            } else if (prevSellPrice < actualSellPrice) {
                                cancelOrder(parameters);
                                logger.info("Cancel order in main corridor due order from upper corridor");
                            }
                        }
                    } else if (price > lowerBorder) {
                        if (type.equals("buy")) {
                            if (prevBuyPrice <= actualBuyPrice && upperBidPrice > price) {
                                replaceOrderOnTopOfGlass(parameters, qty, upperBidPrice + orderPriceDelta);
                                logger.info("Replace order in main corridor due order from lower corridor");
                            } else if (prevBuyPrice > actualBuyPrice) {
                                cancelOrder(parameters);
                                logger.info("Cancel order in main corridor due order from lower corridor");
                            }
                        }
                    } else {
                        cancelOrder(parameters);
                        logger.info("Cancel order in main corridor due order from main corridor");
                    }
                }
            }
            prevBuyPrice = actualBuyPrice;
            prevSellPrice = actualSellPrice;
            count++;
            logger.info("Runway number in main corridor - " + count);
            System.out.println("Поехали на паре: " + currencyPair);
            sleep(2000);
        }
    }


    private boolean actionInLowCorridor() throws Exception {
        int count = 0;
        Double prevBuyPrice = null;
        Double lastActualPrice;
        Double actualBuyPrice;
        Double actualSellPrice;
        Double upperAskPrice;
        Double upperBidPrice;
        while (true) {
            actualBuyPrice = tradesPrices.getActualBuyPrice();
            actualSellPrice = tradesPrices.getActualSellPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
            lastActualPrice = tradesPrices.getLastActualPrice();
//            logger.info("");
            logger.info("in low corridor sell price - " + actualBuyPrice);
            logger.info("in low corridor upper ask price - " + upperAskPrice);
            logger.info("in low corridor upper bid price - " + upperBidPrice);
//            logger.info("");
            if (lastActualPrice > lowerBorder) {
                logger.info("Exit in main corridor from low corridor due price moved through lower border");
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            if (actualBuyPrice > prevBuyPrice && buyOrdersCount <= maxOrdersCount
                    && !actualBuyPrice.equals(lastOrderPrice)) {
                createOrder(qty, upperBidPrice + orderPriceDelta, "buy", currencyPair);
                logger.info("Create order in low corridor due good condition");
            }
            if (postRequests.getUserOpenOrdersNum() > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getUserOpenOrders(currencyPair)) {
                    parameters = getJsonValues(jsonObject);
                    price = parseDouble(parameters.get("price"));
                    if (price < upperBorder && price < upperAskPrice) {
                        replaceOrderOnTopOfGlass(parameters, qty, upperAskPrice + orderPriceDelta);
                        logger.info("Replace order in low corridor due order from upper corridor");
                    } else if (actualBuyPrice >= prevBuyPrice && price < upperBidPrice) {
                        replaceOrderOnTopOfGlass(parameters, qty, upperBidPrice + orderPriceDelta);
                        logger.info("Replace order in low corridor due this order was created due good condition");
                    } else if (actualBuyPrice < prevBuyPrice) {
                        cancelOrder(parameters);
                        logger.info("Cancel order in low corridor due bad condition");
                    }
                }
            }
            if (actualBuyPrice < lowestBorder || actualSellPrice < lowestBorder) {
                if (sellOrdersCount <= maxOrdersCount) {
                    createOrder(qty, upperAskPrice + orderPriceDelta, "sell", currencyPair);
                    logger.info("Create order in low corridor due price below lowestBorder");
                }
                logger.info("Exit in main menu for creating new corridors from low corridor due price moved through lowest border");
                return true;
            }
            prevBuyPrice = actualBuyPrice;
            count++;
            logger.info("Runway number in low corridor - " + count);
            sleep(2000);
        }
    }

    private boolean actionInHighCorridor() throws Exception {
        int count = 0;
        Double prevSellPrice = null;
        Double actualSellPrice;
        Double actualBuyPrice;
        Double upperAskPrice;
        Double upperBidPrice;
        Double lastActualPrice;
        while (true) {
            actualSellPrice = tradesPrices.getActualSellPrice();
            actualBuyPrice = tradesPrices.getActualBuyPrice();
            upperAskPrice = orderBookPrices.getActualAskPrice();
            upperBidPrice = orderBookPrices.getActualBidPrice();
            lastActualPrice = tradesPrices.getLastActualPrice();
//            logger.info("");
            logger.info("in high corridor buy price - " + actualSellPrice);
            logger.info("in high corridor upper ask price - " + upperAskPrice);
            logger.info("in high corridor upper bid price - " + upperBidPrice);
//            logger.info("");
            if (lastActualPrice < upperBorder) {
                logger.info("Exit in main corridor from high corridor due price moved through upper border");
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (actualSellPrice < prevSellPrice && sellOrdersCount <= maxOrdersCount
                    && !actualSellPrice.equals(lastOrderPrice)) {
                createOrder(qty, upperAskPrice + orderPriceDelta, "sell", currencyPair);
                logger.info("Create order in high corridor due good condition");
            }
            if (postRequests.getUserOpenOrdersNum() > 0) {
                Map<String, String> parameters;
                Double price;
                for (Object jsonObject : postRequests.getUserOpenOrders(currencyPair)) {
                    parameters = getJsonValues(jsonObject);
                    price = parseDouble(parameters.get("price"));
                    if (price > lowerBorder && price < upperBidPrice) {
                        replaceOrderOnTopOfGlass(parameters, qty, upperBidPrice + orderPriceDelta);
                        logger.info("Replace order in high corridor due order from lower corridor");
                    } else if (actualSellPrice <= prevSellPrice && price < upperAskPrice) {
                        replaceOrderOnTopOfGlass(parameters, qty, upperAskPrice + orderPriceDelta);
                        logger.info("Replace order in high corridor due order was created due good condition");
                    } else if (actualSellPrice > prevSellPrice) {
                        cancelOrder(parameters);
                        logger.info("Cancel order in high corridor due bad condition");
                    }
                }
            }
            if (actualSellPrice > uppestBorder || actualBuyPrice > uppestBorder) {
                if (buyOrdersCount <= maxOrdersCount) {
                    createOrder(qty, upperBidPrice + orderPriceDelta, "buy", currencyPair);
                    logger.info("Create order in high corridor price upper uppestBorder");
                }
                logger.info("Exit in main menu for creating new corridors from high corridor due price moved through hoghest border");
                return true;
            }
            prevSellPrice = actualSellPrice;
            count++;
            logger.info("Runway number in high corridor - " + count);
            sleep(2000);
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
        logger.info("Result of cancel order - " + result);
        if (!result.equals("true")) {
            logger.info("Error of cancel order - " + jsonObject.get("error"));
            return;
        }
        if (type.equals("buy")) {
            buyOrdersCount--;
        } else if (type.equals("sell")) {
            sellOrdersCount--;
        } else {
            throw new Exception("Wrong type");
        }
        logger.info("Number of open buy orders - " + buyOrdersCount);
        logger.info("Number of open sell orders - " + sellOrdersCount);
    }

    private void createOrder(Double qty, Double price, String type, String currencyPair) throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("pair", currencyPair);
        arguments.put("quantity", qty.toString());
        arguments.put("price", price.toString());
        arguments.put("type", type);
        JSONObject jsonObject = postRequests.sendPostRequestAndGetResponse("order_create", arguments);
        String result = jsonObject.get("result").toString();
        logger.info("Result of create order - " + result);
        if (!result.equals("true")) {
            logger.info("Error of create order - " + jsonObject.get("error"));
            return;
        }
        lastOrderPrice = price;
        if (type.equals("buy")) {
            buyOrdersCount++;
        } else if (type.equals("sell")) {
            sellOrdersCount++;
        } else {
            throw new Exception("Wrong type");
        }
        logger.info("Number of open buy orders - " + buyOrdersCount);
        logger.info("Number of open sell orders - " + sellOrdersCount);
    }

    private Logger initLogger(String filePattern) {
        Logger logger = Logger.getLogger("Work process");

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