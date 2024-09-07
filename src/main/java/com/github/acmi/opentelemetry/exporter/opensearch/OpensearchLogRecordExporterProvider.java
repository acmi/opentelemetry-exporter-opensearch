package com.github.acmi.opentelemetry.exporter.opensearch;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

public class OpensearchLogRecordExporterProvider implements ConfigurableLogRecordExporterProvider {
    @Override
    public LogRecordExporter createExporter(ConfigProperties config) {
        OpensearchLogRecordExporterBuilder builder = OpensearchLogRecordExporterBuilder.builder();

        String index = config.getString("otel.exporter.opensearch.index");
        if (index == null) {
            throw new ConfigurationException("Index name must be set");
        }
        builder.setIndex(index);

        String endpoint = config.getString("otel.exporter.opensearch.endpoint");
        if (endpoint != null) {
            builder.setEndpoint(endpoint);
        }

        Map<String, String> headers = config.getMap("otel.exporter.opensearch.headers");
        headers.forEach(builder::addHeader);

        Duration timeout = config.getDuration("otel.exporter.opensearch.timeout");
        if (timeout != null) {
            builder.setTimeout(timeout);
        }

        String certificatePath = config.getString("otel.exporter.opensearch.certificate");
        String clientKeyPath = config.getString("otel.exporter.opensearch.client.key");
        String clientKeyChainPath = config.getString("otel.exporter.opensearch.client.certificate");
        byte[] certificateBytes = readFileBytes("certificate", certificatePath);
        if (certificateBytes != null) {
            builder.setTrustedCertificates(certificateBytes);
        }
        byte[] clientKeyBytes = readFileBytes("key", clientKeyPath);
        byte[] clientKeyChainBytes = readFileBytes("certificate", clientKeyChainPath);
        if (clientKeyBytes != null && clientKeyChainBytes != null) {
            builder.setClientTls(clientKeyBytes, clientKeyChainBytes);
        }

        return builder.build();
    }

    @Override
    public String getName() {
        return "opensearch";
    }

    private static byte[] readFileBytes(String fileType, String filePath) {
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ConfigurationException("Invalid " + fileType + " path: " + filePath);
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new ConfigurationException("Error reading content of file (" + filePath + ")", e);
        }
    }
}
