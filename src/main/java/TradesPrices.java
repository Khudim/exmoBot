import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class TradesPrices {

    private ArrayList<Double> buyPrice = new ArrayList<>();
    private ArrayList<Double> sellPrice = new ArrayList<>();
    private String method;
    private String currencyPair;

    TradesPrices(String method, String currencyPair){
        this.method = method;
        this.currencyPair = currencyPair;
    }

    TradesPrices(){}

    private void method() throws IOException, URISyntaxException {

        HttpClient client = HttpClientBuilder.create().build();

        JSONObject jsonObject;
        String sellOrBuy;
        Double price;

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("api.exmo.me")
                .setPath("/v1/" + method)
                .setParameter("pair", currencyPair)
                .setParameter("limit", "1")
                .build();

        HttpGet httpGet;
        HttpResponse response;

        while (true) {

            httpGet = new HttpGet(uri);
            response = client.execute(httpGet);
            jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()))
                    .getJSONArray(currencyPair)
                    .getJSONObject(0);
            sellOrBuy = jsonObject.get("type").toString();
            price = Double.parseDouble(jsonObject.get("price").toString());

            if (sellOrBuy.equals("buy")) {
                buyPrice.add(price);
            } else if (sellOrBuy.equals("sell")) {
                sellPrice.add(price);
            }
            System.out.println(sellOrBuy + " - " + price);
        }
    }

    public void exetute() throws IOException, URISyntaxException {
        method();
    }

    public ArrayList<Double> getBuyPrice() {
        return buyPrice;
    }

    public Double getActualBuyPrice() {
        return buyPrice.get(buyPrice.size());
    }

    public ArrayList<Double> getSellPrice() {
        return sellPrice;
    }

    public Double getActualSellPrice() {
        return sellPrice.get(sellPrice.size());
    }
}
