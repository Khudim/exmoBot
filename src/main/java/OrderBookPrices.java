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

    private void method() throws IOException {
        String url = "https://api.exmo.me/v1/order_book/?pair=BTC_USD&limit=1";

        FileOutputStream buyFos = new FileOutputStream("/root/askStat.txt");
        FileOutputStream sellFos = new FileOutputStream("/root/bidStat.txt");
        PrintStream buyPs = new PrintStream(buyFos);
        PrintStream sellPs = new PrintStream(sellFos);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getRequest;
        HttpResponse<String> getResponse;
        JSONObject jsonObject;

        ArrayList<String> askPriceList = new ArrayList<>();
        ArrayList<String> bidPriceList = new ArrayList<>();

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

            jsonObject = new JSONObject(getResponse.body()).getJSONObject("BTC_USD");
            askPriceList.add(jsonObject.getJSONArray("ask").getJSONArray(0).get(0).toString());
            bidPriceList.add(jsonObject.getJSONArray("bid").getJSONArray(0).get(0).toString());

            finish = System.currentTimeMillis();
            if ((finish - start) > 60000) {
                break;
            } else {
                System.out.println((finish - start) / 1000 + "." + (finish - start) % 1000);
            }

        }

        buyPs.println(askPriceList);
        sellPs.println(bidPriceList);

        buyPs.close();
        sellPs.close();
        buyFos.close();
        sellFos.close();
    }

    public void execute() throws IOException {
        method();
    }
}
