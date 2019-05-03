import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradesPrices {

    private final List<Double> buyPrice = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> sellPrice = Collections.synchronizedList(new ArrayList<>());
    private String currencyPair;

    TradesPrices(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    private void method() throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClientBuilder.create().build();

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("api.exmo.me")
                .setPath("/v1/trades")
                .setParameter("pair", currencyPair)
                .build();

        getFirstSellAndBuyPrice(client, uri);
        synchronized (buyPrice) {
            buyPrice.notifyAll();
        }
        getActualSellOrBuyPrice(client, uri);
        return;
    }

    private void getFirstSellAndBuyPrice(HttpClient client, URI uri) throws IOException, InterruptedException {
        Thread.sleep(3000);
        HttpGet httpGet = new HttpGet(uri);
        HttpResponse response = client.execute(httpGet);
        JSONArray jsonArray = new JSONObject(EntityUtils.toString(response.getEntity())).getJSONArray(currencyPair);
        boolean checkFirstSellPrice = false;
        boolean checkFirstBuyPrice = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            String type = jsonArray.getJSONObject(i)
                    .get("type")
                    .toString();
            Double price = Double.parseDouble(jsonArray.getJSONObject(i)
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

    private void getActualSellOrBuyPrice(HttpClient client, URI ur) throws IOException, URISyntaxException {
        JSONObject jsonObject;
        String sellOrBuy;
        Double price;
        HttpGet httpGet;
        HttpResponse response;

        URI uri = new URIBuilder(ur)
                .setParameter("limit", "1")
                .build();

        while (true) {

            httpGet = new HttpGet(uri);
            response = client.execute(httpGet);
            jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()))
                    .getJSONArray(currencyPair)
                    .getJSONObject(0);
            sellOrBuy = jsonObject.get("type").toString();
            price = Double.parseDouble(jsonObject.get("price").toString());

            if (sellOrBuy.equals("buy")) {
                synchronized (buyPrice) {
                    if (!price.equals(getActualBuyPrice())) {
                        buyPrice.add(price);
                    }
                }
            } else if (sellOrBuy.equals("sell")) {
                synchronized (sellPrice) {
                    if (!price.equals(getActualSellPrice())) {
                        sellPrice.add(price);
                    }
                }
            }
        }
    }

    void exetute() throws IOException, URISyntaxException, InterruptedException {
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
}
