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
import java.util.Arrays;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Thread.sleep;
import static java.util.Collections.synchronizedList;
import static org.apache.http.impl.client.HttpClientBuilder.create;

public class OrderBookPrices {

    private final List<Double> askPriceList = synchronizedList(new ArrayList<>());
    private final List<Double> bidPriceList = synchronizedList(new ArrayList<>());
    private HttpClient client = create().build();

    private String currencyPair;

    OrderBookPrices(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    private void method() throws BotException {

        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme("http")
                    .setHost("api.exmo.me")
                    .setPath("/v1/order_book")
                    .setParameter("pair", currencyPair)
                    .setParameter("limit", "15")
                    .build();
        } catch (URISyntaxException e) {
            throw new BotException("Error in OrderBookPrices in thread with currency pair - " + currencyPair,
                                    Arrays.toString(e.getStackTrace()));
        }

        JSONObject fullJsonObject;
        HttpGet httpGet;
        HttpResponse httpResponse;
        Double askPrice;
        Double bidPrice;
        boolean threadsWait = true;

        while (true) {

            httpGet = new HttpGet(uri);

            try {
                httpResponse = client.execute(httpGet);
            } catch (IOException e) {
                throw new BotException("Error in OrderBookPrices in thread with currency pair - " + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }
            try {
                fullJsonObject = new JSONObject(EntityUtils.toString(httpResponse.getEntity()))
                        .getJSONObject(currencyPair);
            } catch (IOException e) {
                throw new BotException("Error in OrderBookPrices in thread with currency pair - " + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }

            for (int i = 0; i < 15; i++) {
                askPrice = parseDouble(fullJsonObject.getJSONArray("ask")
                        .getJSONArray(i)
                        .get(0)
                        .toString());
                synchronized (askPriceList) {
                    if (askPriceList.size() == 0
                            || !askPrice.equals(getActualAskPrice())) {
                        askPriceList.add(askPrice);
                    } else {
                        break;
                    }
                }
            }

            for (int i = 0; i < 15; i++) {
                bidPrice = parseDouble(fullJsonObject.getJSONArray("bid")
                        .getJSONArray(i)
                        .get(0)
                        .toString());
                synchronized (bidPriceList) {
                    if (bidPriceList.size() == 0
                            || !bidPrice.equals(getActualBidPrice())) {
                        bidPriceList.add(bidPrice);
                    } else {
                        break;
                    }
                }
            }

            try {
                sleep(3500);
            } catch (InterruptedException e) {
                throw new BotException("Error in OrderBookPrices in thread with currency pair - " + currencyPair,
                                        Arrays.toString(e.getStackTrace()));
            }

            if (threadsWait){
                synchronized (askPriceList){
                    askPriceList.notifyAll();
                }
                threadsWait = false;
            }
        }
    }

    void execute() throws BotException {
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

    synchronized List<Double> getAskPriceList() {
        return askPriceList;
    }

    Double getActualAskPrice() {
        synchronized (askPriceList) {
            int index = askPriceList.size() - 1;
            return askPriceList.get(index);
        }
    }
}