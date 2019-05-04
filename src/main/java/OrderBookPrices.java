import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedList;
import static org.apache.http.impl.client.HttpClientBuilder.create;

public class OrderBookPrices {

    private final List<Double> askPriceList = synchronizedList(new ArrayList<>());
    private final List<Double> bidPriceList = synchronizedList(new ArrayList<>());
    private String currencyPair;

    OrderBookPrices(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    private void method() throws IOException, URISyntaxException, InterruptedException {

        HttpClient client = create().build();

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("api.exmo.me")
                .setPath("/v1/order_book")
                .setParameter("pair", currencyPair)
                .setParameter("limit", "1")
                .build();

        JSONObject jsonObject;
        HttpGet httpGet;
        HttpResponse httpResponse;
        Double askPrice;
        Double bidPrice;
        boolean threadsWait = true;

        while (true) {

            httpGet = new HttpGet(uri);
            httpResponse = client.execute(httpGet);
            jsonObject = new JSONObject(EntityUtils.toString(httpResponse.getEntity()))
                    .getJSONObject(currencyPair);
            askPrice = parseDouble(jsonObject.getJSONArray("ask")
                    .getJSONArray(0)
                    .get(0)
                    .toString());
            bidPrice = parseDouble(jsonObject.getJSONArray("bid")
                    .getJSONArray(0)
                    .get(0)
                    .toString());
            synchronized (askPriceList) {
                if (askPriceList.size() == 0
                        || !askPrice.equals(getActualAskPrice())) {
                    askPriceList.add(askPrice);
                }
            }
            synchronized (bidPriceList) {
                if (bidPriceList.size() == 0
                        || !bidPrice.equals(getActualBidPrice())) {
                    bidPriceList.add(bidPrice);
                }
            }
            sleep(5000);
            if (threadsWait){
                synchronized (askPriceList){
                    askPriceList.notifyAll();
                }
                threadsWait = false;
            }
        }
    }

    void execute() throws IOException, URISyntaxException, InterruptedException {
        method();
    }

    public List<Double> getBidPriceList() {
        return bidPriceList;
    }

    Double getActualBidPrice() {
        synchronized (bidPriceList) {
            int index = bidPriceList.size() - 1;
            return bidPriceList.get(index);
        }
    }

    public synchronized List<Double> getAskPriceList() {
        return askPriceList;
    }

    Double getActualAskPrice() {
        synchronized (askPriceList) {
            int index = askPriceList.size() - 1;
            return askPriceList.get(index);
        }
    }
}