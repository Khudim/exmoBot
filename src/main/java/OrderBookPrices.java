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

public class OrderBookPrices {

    private ArrayList<Double> askPriceList = new ArrayList<>();
    private ArrayList<Double> bidPriceList = new ArrayList<>();
    private String method;
    private String currencyPair;

    OrderBookPrices(String method, String currencyPair){
        this.method = method;
        this.currencyPair = currencyPair;
    }

    OrderBookPrices(){}

    private void method() throws IOException, URISyntaxException {

        HttpClient client = HttpClientBuilder.create().build();

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("api.exmo.me")
                .setPath("/v1/" + method)
                .setParameter("pair", currencyPair)
                .setParameter("limit", "1")
                .build();

        JSONObject jsonObject;
        HttpGet httpGet;
        HttpResponse httpResponse;

        while (true) {

            httpGet = new HttpGet(uri);
            httpResponse = client.execute(httpGet);
            jsonObject = new JSONObject(EntityUtils.toString(httpResponse.getEntity()))
                    .getJSONObject(currencyPair);
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

    public void execute() throws IOException, URISyntaxException {
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
