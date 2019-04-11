import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import static java.net.http.HttpResponse.BodyHandlers;

public class Main {

    public static void main(String[] args) throws IOException {
//        String url = "https://api.exmo.me/v1/trades/?pair=BTC_USD&limit=1";

        String url = "https://api.exmo.me/v1/order_book/?pair=BTC_USD&limit=1";

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

        String askPrice;
        String bidPrice;
        ArrayList<String> askPriceList = new ArrayList<>();
        ArrayList<String> bidPriceList = new ArrayList<>();

        ArrayList<String> buyPrice = new ArrayList<>();
        ArrayList<String> sellPrice = new ArrayList<>();

        long start = System.currentTimeMillis();
        long finish;
        int num = 0;
        while (true) {

            getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json")
                    .build();

            try {
                getResponse = client.send(getRequest, BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new IOException();
            }

//            jsonObject = new JSONObject(getResponse.body()).getJSONArray("BTC_USD").getJSONObject(0);
//            tradeId = jsonObject.get("trade_id").toString();
//            sellOrBuy = jsonObject.get("type").toString();
//            price = jsonObject.get("price").toString();

            jsonObject = new JSONObject(getResponse.body()).getJSONObject("BTC_USD");
            askPriceList.add(jsonObject.getJSONArray("ask").getJSONArray(0).get(0).toString());
            bidPriceList.add(jsonObject.getJSONArray("bid").getJSONArray(0).get(0).toString());

//            if (!tradeId.equals(newTradeId)) {
//                newTradeId = tradeId;
//                if (sellOrBuy.equals("buy")) {
//                    buyPrice.add(price);
//                } else if (sellOrBuy.equals("sell")) {
//                    sellPrice.add(price);
//                }
//            }
            finish = System.currentTimeMillis();
            if ((finish - start) > 60000) {
                break;
            }

        }

        buyPs.println(askPriceList);
        sellPs.println(bidPriceList);

//            ps.println(getResponse.body());
//        System.out.println(getResponse.body());

//            num++;
//            finish = System.currentTimeMillis();
//            if ((finish - start) > 60000){
//                break;
//            }
//        }
//        System.out.println((finish - start)/1000 + "." + (finish - start)%1000);
//        System.out.println(num);
//        linesNum();

        buyPs.close();
        sellPs.close();
        buyFos.close();
        sellFos.close();
    }

    public static void linesNum() {
        final File file = new File("/root/stat.txt");
        try {
            final LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            int linesCount = 0;
            while (null != lnr.readLine()) {
                linesCount++;
            }
            System.out.println("Количество строк в файле: " + linesCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}