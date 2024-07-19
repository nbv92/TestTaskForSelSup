import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private int requestCount;
    private long lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
    }

    public synchronized void createDocument(ObjectNode document, String signature) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRequestTime;

        if (elapsedTime >= timeUnit.toMillis(1)) {
            requestCount = 0;
            lastRequestTime = currentTime;
        }

        if (requestCount >= requestLimit) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");

            ObjectNode requestNode = new ObjectMapper().createObjectNode();
            requestNode.set("document", document);
            requestNode.put("signature", signature);

            StringEntity entity = new StringEntity(requestNode.toString());
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(httpPost);

            requestCount++;

            notifyAll(); // Notify waiting threads

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
