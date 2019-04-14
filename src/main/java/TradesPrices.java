import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class TradesPrices {

    private ArrayList<Double> buyPrice = new ArrayList<>();
    private ArrayList<Double> sellPrice = new ArrayList<>();

    private void method() throws IOException {

        String url = "https://api.exmo.me/v1/trades/?pair=BTC_USD&limit=1";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest getRequest;
        HttpResponse<String> getResponse;
        JSONObject jsonObject;
        String sellOrBuy;
        Double price;

        while (true) {

            getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json")
                    .build();

            try {
                getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new IOException();
            }

            jsonObject = new JSONObject(getResponse.body()).getJSONArray("BTC_USD").getJSONObject(0);
            sellOrBuy = jsonObject.get("type").toString();
            price = Double.parseDouble(jsonObject.get("price").toString());

            if (sellOrBuy.equals("buy")) {
                buyPrice.add(price);
            } else if (sellOrBuy.equals("sell")) {
                sellPrice.add(price);
            }
        }
    }

    public void exetute() throws IOException {
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
