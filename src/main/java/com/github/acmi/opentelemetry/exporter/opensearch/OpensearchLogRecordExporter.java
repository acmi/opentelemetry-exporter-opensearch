package com.github.acmi.opentelemetry.exporter.opensearch;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpensearchLogRecordExporter implements LogRecordExporter {
    private final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final AtomicBoolean isShutdown = new AtomicBoolean();

    private final OkHttpClient client;
    private final HttpUrl url;
    private final Headers headers;

    private final LogRecordSerializer serializer;

    public OpensearchLogRecordExporter(
            String index,
            OkHttpClient client,
            String endpoint,
            Headers headers
    ) {
        this.client = client;
        this.url = HttpUrl.get(endpoint);
        this.headers = headers;
        this.serializer = new LogRecordSerializer(index);
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            requestBuilder.headers(headers);
        }

        byte[] bytes;
        try {
            bytes = serializer.serialize(logs);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to serialize logs: {0}", e.getMessage());
            return CompletableResultCode.ofFailure();
        }
        RequestBody requestBody = RequestBody.create(bytes, JSON_MEDIA_TYPE);
        requestBuilder.post(requestBody);

        CompletableResultCode result = new CompletableResultCode();

        client.newCall(requestBuilder.build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logger.log(Level.SEVERE, "Failed to export logs. The request could not be executed: {0}", e.getMessage());
                        result.fail();
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try (ResponseBody body = response.body()) {
                            if (response.isSuccessful()) {
                                result.succeed();
                                return;
                            }

                            int code = response.code();

                            String responseBody;
                            try {
                                responseBody = body != null ? body.string() : "null";
                            } catch (IOException e) {
                                responseBody = "unable to read response";
                            }

                            logger.log(Level.WARNING, "Failed to export logs. Response {0}: {1}", new Object[]{code, responseBody});
                            result.fail();
                        }
                    }
                });

        return result;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            logger.log(Level.INFO, "Calling shutdown() multiple times.");
            return CompletableResultCode.ofSuccess();
        }
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdownNow();
        client.connectionPool().evictAll();
        return CompletableResultCode.ofSuccess();
    }
}
