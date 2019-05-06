import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedList;
import static org.apache.http.impl.client.HttpClientBuilder.create;

class TradesPrices {

    private final List<Double> buyPrice = synchronizedList(new ArrayList<>());
    private final List<Double> sellPrice = synchronizedList(new ArrayList<>());
    private final List<Double> actualPrice = synchronizedList(new ArrayList<>());
    private HttpClient client = create().build();

    private String currencyPair;

    TradesPrices(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    private void method() throws BotException {

        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme("http")
                    .setHost("api.exmo.me")
                    .setPath("/v1/trades")
                    .setParameter("pair", currencyPair)
                    .build();
        } catch (URISyntaxException e) {
            throw new BotException("Error in TradesPrices in thread with currency pair - " + currencyPair,
                                    Arrays.toString(e.getStackTrace()));
        }

        getFirstSellAndBuyPrice(uri);
        synchronized (actualPrice) {
            actualPrice.notifyAll();
        }
        getActualSellOrBuyPrice(uri);
    }

    private void getFirstSellAndBuyPrice(URI uri) throws BotException {
        try {
            sleep(3000);
        } catch (InterruptedException e) {
            throw new BotException("Error in TradesPrices in thread with currency pair - " + currencyPair,
                                    Arrays.toString(e.getStackTrace()));
        }
        HttpGet httpGet = new HttpGet(uri);
        JSONArray jsonArray = getResponseJsonArray(httpGet);
        actualPrice.add(parseDouble(jsonArray.getJSONObject(0).get("price").toString()));
        boolean checkFirstSellPrice = false;
        boolean checkFirstBuyPrice = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            String type = jsonArray.getJSONObject(i)
                    .get("type")
                    .toString();
            Double price = parseDouble(jsonArray.getJSONObject(i)
                    .get("price")
                    .toString());
            if (type.equals("buy")
                    && !checkFirstBuyPrice) {
                buyPrice.add(price);
                checkFirstBuyPrice = true;
            } else if (type.equals("sell")
                    && !checkFirstSellPrice) {
                sellPrice.add(price);
                checkFirstSellPrice = true;
            } else if (checkFirstBuyPrice && checkFirstSellPrice) {
                return;
            }
        }
    }

    private void getActualSellOrBuyPrice(URI ur) throws BotException {
        JSONArray jsonArray;
        String sellOrBuy;
        Double price;
        HttpGet httpGet;
        JSONObject jsonObject;

        URI uri;
        try {
            uri = new URIBuilder(ur)
                    .setParameter("limit", "15")
                    .build();
        } catch (URISyntaxException e) {
            throw new BotException("Error in TradesPrices in thread with currency pair - " + currencyPair,
                                    Arrays.toString(e.getStackTrace()));
        }

        while (true) {

            httpGet = new HttpGet(uri);
            jsonArray = getResponseJsonArray(httpGet);

            for (Object object: jsonArray) {

                jsonObject = (JSONObject) object;

                sellOrBuy = jsonObject.get("type").toString();
                price = parseDouble(jsonObject.get("price").toString());

                synchronized (actualPrice) {
                    if (!price.equals(getLastActualPrice())) {
                        actualPrice.add(price);
                    }
                }
                if (sellOrBuy.equals("buy")) {
                    synchronized (buyPrice) {
                        if (!price.equals(getActualBuyPrice())) {
                            buyPrice.add(price);
                        } else {
                            break;
                        }
                    }
                } else if (sellOrBuy.equals("sell")) {
                    synchronized (sellPrice) {
                        if (!price.equals(getActualSellPrice())) {
                            sellPrice.add(price);
                        } else {
                            break;
                        }
                    }
                }

                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    throw new BotException("Error in TradesPrices in thread with currency pair - " + currencyPair,
                            Arrays.toString(e.getStackTrace()));
                }
            }
        }
    }

    void exetute() throws BotException {
        method();
    }

    List<Double> getBuyPrice() {
        return buyPrice;
    }

    Double getActualBuyPrice() {
        synchronized (buyPrice) {
            int index = buyPrice.size() - 1;
            return buyPrice.get(index);
        }
    }

    List<Double> getSellPrice() {
        return sellPrice;
    }

    Double getActualSellPrice() {
        synchronized (sellPrice) {
            int index = sellPrice.size() - 1;
            return sellPrice.get(index);
        }
    }

    List<Double> getActualPrice() {
        return actualPrice;
    }

    Double getLastActualPrice() {
        synchronized (actualPrice) {
            int index = actualPrice.size() - 1;
            return actualPrice.get(index);
        }
    }

    private JSONArray getResponseJsonArray(HttpGet httpGet) throws BotException {
        HttpResponse response;
        try {
            response = client.execute(httpGet);
        } catch (IOException e) {
            throw new BotException("Error in TradesPrices in thread with currency pair - " + currencyPair,
                    Arrays.toString(e.getStackTrace()));
        }
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONObject(EntityUtils.toString(response.getEntity())).getJSONArray(currencyPair);
        } catch (IOException e) {
            throw new BotException("Error in TradesPrices in thread with currency pair - " + currencyPair,
                    Arrays.toString(e.getStackTrace()));
        }
        return jsonArray;
    }
}