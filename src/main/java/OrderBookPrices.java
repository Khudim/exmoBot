import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class OrderBookPrices {

    private ArrayList<Double> askPriceList = new ArrayList<>();
    private ArrayList<Double> bidPriceList = new ArrayList<>();

    private void method() throws IOException {

        String url = "https://api.exmo.me/v1/order_book/?pair=BTC_USD&limit=1";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest getRequest;
        HttpResponse<String> getResponse;
        JSONObject jsonObject;

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

            jsonObject = new JSONObject(getResponse.body()).getJSONObject("BTC_USD");
            askPriceList.add(Double.parseDouble(
                    jsonObject.getJSONArray("ask")
                            .getJSONArray(0)
                            .get(0)
                            .toString())
            );
            bidPriceList.add(Double.parseDouble(
                    jsonObject.getJSONArray("bid")
                            .getJSONArray(0)
                            .get(0)
                            .toString())
            );
        }
    }

    public void execute() throws IOException {
        method();
    }

    public ArrayList<Double> getBidPriceList() {
        return bidPriceList;
    }

    public Double getActualBidPrice() {
        return bidPriceList.get(bidPriceList.size());
    }

    public ArrayList<Double> getAskPriceList() {
        return askPriceList;
    }

    public Double getActualAskPrice() {
        return askPriceList.get(askPriceList.size());
    }
}
