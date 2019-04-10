import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpResponse.BodyHandlers;

public class Main {

    public static void main(String[] args) throws IOException {
        String url = "https://api.exmo.me/v1/trades/?pair=BTC_USD";

        FileOutputStream fos = new FileOutputStream("/root/stat.txt");
        PrintStream ps = new PrintStream(fos);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest getRequest;
        HttpResponse<String> getResponse;

        long start = System.currentTimeMillis();
        long finish;
        int num = 0;
        while (true) {
            getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            try {
                getResponse = client.send(getRequest, BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new IOException();
            }

            ps.println(getResponse.body());

            num++;
            finish = System.currentTimeMillis();
            if ((finish - start) > 60000){
                break;
            }
        }
        System.out.println((finish - start)/1000 + "." + (finish - start)%1000);
        System.out.println(num);
        linesNum();

        ps.close();
        fos.close();
    }

    public static void linesNum(){
        final File file = new File("/root/stat.txt");
        try {
            final LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            int linesCount = 0;
            while(null != lnr.readLine()) {
                linesCount++;
            }
            System.out.println("Количество строк в файле: " + linesCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}