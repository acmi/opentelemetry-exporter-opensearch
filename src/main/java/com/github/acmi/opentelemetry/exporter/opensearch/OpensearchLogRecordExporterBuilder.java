package com.github.acmi.opentelemetry.exporter.opensearch;

import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.internal.TlsConfigHelper;
import io.opentelemetry.exporter.internal.auth.Authenticator;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OpensearchLogRecordExporterBuilder {
    private static final String DEFAULT_ENDPOINT_URL = "http://localhost:9200";
    private static final long DEFAULT_TIMEOUT_SECS = 10;

    private String index;

    private String endpoint = DEFAULT_ENDPOINT_URL;

    private long timeoutNanos = TimeUnit.SECONDS.toNanos(DEFAULT_TIMEOUT_SECS);
    private Headers.Builder headersBuilder;

    private final TlsConfigHelper tlsConfigHelper = new TlsConfigHelper();
    private Authenticator authenticator;

    public static OpensearchLogRecordExporterBuilder builder() {
        return new OpensearchLogRecordExporterBuilder();
    }

    public OpensearchLogRecordExporterBuilder setIndex(String index) {
        this.index = index;
        return this;
    }

    public OpensearchLogRecordExporterBuilder setEndpoint(String endpoint) {
        URI uri = ExporterBuilderUtil.validateEndpoint(endpoint);
        this.endpoint = uri.toString();
        return this;
    }

    public OpensearchLogRecordExporterBuilder addHeader(String key, String value) {
        if (headersBuilder == null) {
            headersBuilder = new Headers.Builder();
        }
        headersBuilder.add(key, value);
        return this;
    }

    public OpensearchLogRecordExporterBuilder setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
        return this;
    }

    public OpensearchLogRecordExporterBuilder setTimeout(long timeout, TimeUnit unit) {
        timeoutNanos = unit.toNanos(timeout);
        return this;
    }

    public OpensearchLogRecordExporterBuilder setTimeout(Duration timeout) {
        return setTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public OpensearchLogRecordExporterBuilder setTrustedCertificates(byte[] trustedCertificatesPem) {
        tlsConfigHelper.setTrustManagerFromCerts(trustedCertificatesPem);
        return this;
    }

    public OpensearchLogRecordExporterBuilder setClientTls(byte[] privateKeyPem, byte[] certificatePem) {
        tlsConfigHelper.setKeyManagerFromCerts(privateKeyPem, certificatePem);
        return this;
    }

    public OpensearchLogRecordExporterBuilder setSslContext(SSLContext sslContext, X509TrustManager trustManager) {
        tlsConfigHelper.setSslContext(sslContext, trustManager);
        return this;
    }

    public OpensearchLogRecordExporter build() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(
                        new ThreadPoolExecutor(
                                0,
                                Integer.MAX_VALUE,
                                60,
                                TimeUnit.SECONDS,
                                new SynchronousQueue<>(),
                                new DaemonThreadFactory("okhttp-dispatch"))))
                .callTimeout(Duration.ofNanos(timeoutNanos));

        SSLContext sslContext = tlsConfigHelper.getSslContext();
        X509TrustManager trustManager = tlsConfigHelper.getTrustManager();
        if (sslContext != null && trustManager != null) {
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        }

        Headers headers = headersBuilder == null ? null : headersBuilder.build();

        if (authenticator != null) {
            clientBuilder.authenticator((route, response) -> {
                Request.Builder requestBuilder = response.request().newBuilder();
                authenticator.getHeaders().forEach(requestBuilder::header);
                return requestBuilder.build();
            });
        }

        return new OpensearchLogRecordExporter(
                index,
                clientBuilder.build(),
                endpoint,
                headers
        );
    }
}
