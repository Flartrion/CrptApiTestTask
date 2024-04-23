import javax.json.Json;
import javax.json.JsonObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final int requestLimitUpdateTime;
    private final int requestLimit;
    private final JsonObject testBody = Json.createReader(new StringReader(
                    "{\"description\": { \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", 109 \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}"))
            .readObject();
    private HttpsURLConnection httpsClient;
    private static final AtomicInteger requestCount = new AtomicInteger(0);
    private static final Timer resetTimer = new Timer("UpdateTimer");
    private static final Timer overflowTimer = new Timer("OverflowTimer");
    private static final TimerTask resetTimerTask = new TimerTask() {
        @Override
        public void run() {
            synchronized (requestCount) {
                requestCount.set(0);
            }
        }
    };

    public CrptApi(int requestLimitUpdateTime, int requestLimit) {
        this.requestLimitUpdateTime = requestLimitUpdateTime;
        this.requestLimit = requestLimit;

        resetTimer.schedule(resetTimerTask, 0, requestLimitUpdateTime);

        try {
            httpsClient = (HttpsURLConnection) new URL("https://ismp.crpt.ru/api/v3/lk/documents/create")
                    .openConnection();
            httpsClient.connect();
            httpsClient.setRequestMethod("POST");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int createDocumentNewProduct(JsonObject requestBody, String sign) {
        synchronized (requestCount) {
            if (requestCount.get() < requestLimit)
                requestCount.incrementAndGet();
            else overflowTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    createDocumentNewProduct(requestBody, sign);
                }
            }, resetTimerTask.scheduledExecutionTime());
        }

        httpsClient.setRequestProperty("Content-Length", String.valueOf(requestBody.toString().length()));
        httpsClient.setRequestProperty("Content-Type", "application/json");
        httpsClient.setDoInput(true);
        httpsClient.setDoOutput(true);

        try {
            httpsClient.connect();
            DataOutputStream outputStream = new DataOutputStream(httpsClient.getOutputStream());
            outputStream.writeBytes(requestBody.toString());
            outputStream.close();
            httpsClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}
