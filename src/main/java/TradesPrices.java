import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class TradesPrices {

    private void method() throws IOException {

        String url = "https://api.exmo.me/v1/trades/?pair=BTC_USD&limit=1";

        FileOutputStream buyFos = new FileOutputStream("/root/buyStat.txt");
        FileOutputStream sellFos = new FileOutputStream("/root/sellStat.txt");
        PrintStream buyPs = new PrintStream(buyFos);
        PrintStream sellPs = new PrintStream(sellFos);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getRequest;
        HttpResponse<String> getResponse;
        JSONObject jsonObject;
        String tradeId;
        String newTradeId = "";
        String sellOrBuy;
        String price;

        ArrayList<String> buyPrice = new ArrayList<>();
        ArrayList<String> sellPrice = new ArrayList<>();

        long start = System.currentTimeMillis();
        long finish;
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
            tradeId = jsonObject.get("trade_id").toString();
            sellOrBuy = jsonObject.get("type").toString();
            price = jsonObject.get("price").toString();

            if (!tradeId.equals(newTradeId)) {
                newTradeId = tradeId;
                if (sellOrBuy.equals("buy")) {
                    buyPrice.add(price);
                } else if (sellOrBuy.equals("sell")) {
                    sellPrice.add(price);
                }
            }
            finish = System.currentTimeMillis();
            if ((finish - start) > 60000) {
                break;
            } else {
                System.out.println((finish - start) / 1000 + "." + (finish - start) % 1000);
            }

        }

        buyPs.close();
        sellPs.close();
        buyFos.close();
        sellFos.close();
    }

    public void exetute() throws IOException {
        method();
    }
}
