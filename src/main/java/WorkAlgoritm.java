import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Double.parseDouble;
import static java.lang.Thread.sleep;

class WorkAlgoritm {

    private TradesPrices tradesPrices;
    private OrderBookPrices orderBookPrices;
    private PostRequests postRequests;
    private Double maxOrdersCount;
    private Double orderPriceDelta;
    private String currencyPair;
    private Double persent;
    private Double qty;

    private Double upperBorder;
    private Double lowerBorder;
    private Double uppestBorder;
    private Double lowestBorder;

    private Integer ordersCount = 0;
    private Double lastOrderPrice = 0.0;

    private Logger logger;
    private Logger borderLogger;

    WorkAlgoritm(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests, Double maxOrdersCount,
                 Double orderPriceDelta, String currencyPair, Double persent, Double qty, String filePattern) {
        this.tradesPrices = tradesPrices;
        this.orderBookPrices = orderBookPrices;
        this.postRequests = postRequests;
        this.maxOrdersCount = maxOrdersCount;
        this.orderPriceDelta = orderPriceDelta;
        this.currencyPair = currencyPair;
        this.persent = persent;
        this.qty = qty;

        this.logger = Main.initLogger("WorkStatLog", filePattern);
        this.borderLogger = Main.initLogger("BorderStatLog", "borders_" + filePattern);
    }

    void start() throws BotException {

        while (true) {
            createCorridors();
            boolean needNewCorridors = false;
            Double lastActualPrice;
            while (!needNewCorridors) {
                lastActualPrice = tradesPrices.getLastActualPrice();
//                logger.info("In main menu: sell price - " + actualSellPrice + ", buy price - " + actualBuyPrice);
                logger.info("In main menu: last actual price - " + lastActualPrice);
                checkOpenOrders();
                if (lastActualPrice > lowerBorder && lastActualPrice < upperBorder) {
                    actionInMainCorridor();
                }
                checkOpenOrders();
                if (lastActualPrice < lowerBorder && lastActualPrice > lowestBorder) {
                    needNewCorridors = actionInLowCorridor();
                }
                checkOpenOrders();
                if (lastActualPrice > upperBorder && lastActualPrice < uppestBorder) {
                    needNewCorridors = actionInHighCorridor();
                }
            }
        }
    }

    private void createCorridors() throws BotException {
        if (upperBorder == null) {
            waitCondition();
        }
        Double actualPrice = tradesPrices.getLastActualPrice();
        upperBorder = actualPrice * (1 + persent / 100);
        lowerBorder = actualPrice * (1 - persent / 100);
        uppestBorder = actualPrice * (1 + 2 * persent / 100);
        lowestBorder = actualPrice * (1 - 2 * persent / 100);
//        logger.info("");
        borderLogger.info("Корридор сделали: ");
        borderLogger.info("uppest border - " + uppestBorder);
        borderLogger.info("upper border - " + upperBorder);
        borderLogger.info("lower border - " + lowerBorder);
        borderLogger.info("lowest border - " + lowestBorder);
//        logger.info("");
    }

    private void waitCondition() throws BotException {
        synchronized (tradesPrices.getActualPrice()) {
            try {
                tradesPrices.getActualPrice().wait();
            } catch (InterruptedException e) {
                throw new BotException("Error in main algoritm in waiting trade prices in thread for currency pair - "
                                        + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }
        }
        synchronized (orderBookPrices.getAskPriceList()) {
            try {
                orderBookPrices.getAskPriceList().wait();
            } catch (InterruptedException e) {
                throw new BotException("Error in main algoritm in waiting order book prices in thread for currency pair - "
                                        + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void actionInMainCorridor() throws BotException {
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
            logger.info("in main corridor last actual price - " + lastActualPrice + " for cuurency pair - " + currencyPair);
            logger.info("in main corridor buy price - " + actualBuyPrice + " for cuurency pair - " + currencyPair);
            logger.info("in main corridor upper bid price - " + upperBidPrice + " for cuurency pair - " + currencyPair);
            logger.info("in main corridor sell price - " + actualSellPrice + " for cuurency pair - " + currencyPair);
            logger.info("in main corridor upper ask price - " + upperAskPrice + " for cuurency pair - " + currencyPair);
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
            if (ordersCount != 0) {
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
            logger.info("Runway number in main corridor - " + count + ", with order count - " + ordersCount +
                    ", with currency pair - " + currencyPair);
            System.out.println("Поехали на паре: " + currencyPair);
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                throw new BotException("Error in main algoritm for currency pair - " + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }
        }
    }


    private boolean actionInLowCorridor() throws BotException {
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
            logger.info("in low corridor sell price - " + actualBuyPrice + " for cuurency pair - " + currencyPair);
            logger.info("in low corridor upper ask price - " + upperAskPrice + " for cuurency pair - " + currencyPair);
            logger.info("in low corridor upper bid price - " + upperBidPrice + " for cuurency pair - " + currencyPair);
//            logger.info("");
            if (lastActualPrice > lowerBorder) {
                logger.info("Exit in main corridor from low corridor due price moved through lower border");
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = actualBuyPrice;
                continue;
            }
            if (actualBuyPrice > prevBuyPrice && ordersCount <= maxOrdersCount
                    && !actualBuyPrice.equals(lastOrderPrice)) {
                createOrder(qty, upperBidPrice + orderPriceDelta, "buy", currencyPair);
                logger.info("Create order in low corridor due good condition");
            }
            if (ordersCount != 0) {
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
                if (ordersCount >= -maxOrdersCount) {
                    createOrder(qty, upperAskPrice + orderPriceDelta, "sell", currencyPair);
                    logger.info("Create order in low corridor due price below lowestBorder");
                }
                logger.info("Exit in main menu for creating new corridors from low corridor due price moved through lowest border");
                return true;
            }
            prevBuyPrice = actualBuyPrice;
            count++;
            logger.info("Runway number in low corridor - " + count + ", with order count - " + ordersCount +
                    ", with currency pair - " + currencyPair);
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                throw new BotException("Error in main algoritm for currency pair - " + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private boolean actionInHighCorridor() throws BotException {
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
            logger.info("in high corridor sell price - " + actualSellPrice + " for cuurency pair - " + currencyPair);
            logger.info("in high corridor upper ask price - " + upperAskPrice + " for cuurency pair - " + currencyPair);
            logger.info("in high corridor upper bid price - " + upperBidPrice + " for cuurency pair - " + currencyPair);
//            logger.info("");
            if (lastActualPrice < upperBorder) {
                logger.info("Exit in main corridor from high corridor due price moved through upper border");
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = actualSellPrice;
                continue;
            }
            if (actualSellPrice < prevSellPrice && ordersCount >= -maxOrdersCount
                    && !actualSellPrice.equals(lastOrderPrice)) {
                createOrder(qty, upperAskPrice + orderPriceDelta, "sell", currencyPair);
                logger.info("Create order in high corridor due good condition");
            }
            if (ordersCount != 0) {
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
                if (ordersCount <= maxOrdersCount) {
                    createOrder(qty, upperBidPrice + orderPriceDelta, "buy", currencyPair);
                    logger.info("Create order in high corridor price upper uppestBorder");
                }
                logger.info("Exit in main menu for creating new corridors from high corridor due price moved through hoghest border");
                return true;
            }
            prevSellPrice = actualSellPrice;
            count++;
            logger.info("Runway number in high corridor - " + count + ", with order count - " + ordersCount +
                    ", with currency pair - " + currencyPair);
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                throw new BotException("Error in main algoritm for currency pair - " + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private Map<String, String> getJsonValues(Object jsonObject) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("type", ((JSONObject) jsonObject).get("type").toString());
        parameters.put("price", ((JSONObject) jsonObject).get("price").toString());
        parameters.put("orderId", ((JSONObject) jsonObject).get("order_id").toString());
        return parameters;
    }

    private void replaceOrderOnTopOfGlass(Map<String, String> parameters, Double qty, Double price) throws BotException {
        cancelOrder(parameters);
        createOrder(qty, price, parameters.get("type"), currencyPair);
    }

    private void cancelOrder(Map<String, String> parameters) throws BotException {
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
            ordersCount--;
        } else if (type.equals("sell")) {
            ordersCount++;
        } else {
            throw new BotException("Error in cancel order in main algoritm for currency pair - " + currencyPair +
                                    ". Wrong type of order", null);
        }
        logger.info("Number of open buy orders - " + ordersCount);
        logger.info("Number of open sell orders - " + -ordersCount);
    }

    private void createOrder(Double qty, Double price, String type, String currencyPair) throws BotException {
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
            ordersCount++;
        } else if (type.equals("sell")) {
            ordersCount--;
        } else {
            throw new BotException("Error in cancel order in main algoritm for currency pair - " + currencyPair +
                                    ". Wrong type of order", null);
        }
        logger.info("Number of open buy orders - " + ordersCount);
        logger.info("Number of open sell orders - " + -ordersCount);
    }

    private void checkOpenOrders() throws BotException {
        int orders = 0;
        if (postRequests.getUserOpenOrdersNum() > 0) {
            for (Object jsonObject : postRequests.getUserOpenOrders(currencyPair)) {
                String type = ((JSONObject) jsonObject).get("type").toString();
                if (type.equals("buy")) {
                    orders++;
                } else if (type.equals("sell")) {
                    orders--;
                }
            }
        }
        ordersCount = orders;
    }
}