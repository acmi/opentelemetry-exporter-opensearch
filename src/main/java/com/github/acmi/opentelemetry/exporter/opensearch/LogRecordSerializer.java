package com.github.acmi.opentelemetry.exporter.opensearch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.logs.data.LogRecordData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogRecordSerializer {
    private final String index;
    private final JsonFactory jf;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public LogRecordSerializer(String index) {
        this.index = index;
        this.jf = new JsonFactory();
        this.jf.setRootValueSeparator(null);
    }

    public byte[] serialize(Collection<LogRecordData> events) throws IOException {
        baos.reset();
        serializeEvents(jf.createGenerator(baos), events);
        return baos.toByteArray();
    }

    protected void serializeEvents(JsonGenerator gen, Collection<LogRecordData> events) throws IOException {
        for (LogRecordData event : events) {
            serializeIndexString(gen, event);
            gen.writeRaw('\n');
            serializeEvent(gen, event);
            gen.writeRaw('\n');
        }
        gen.flush();
    }

    protected void serializeIndexString(JsonGenerator gen, LogRecordData event) throws IOException {
        gen.writeStartObject();
        gen.writeObjectFieldStart("index");
        gen.writeObjectField("_index", index);
        gen.writeEndObject();
        gen.writeEndObject();
    }

    protected void serializeEvent(JsonGenerator gen, LogRecordData event) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("@timestamp", getTimestamp(event.getTimestampEpochNanos()));
        gen.writeObjectField("message", event.getBody().asString());
        gen.writeObjectField("log.level", event.getSeverityText());
        gen.writeObjectField("log.logger", event.getInstrumentationScopeInfo().getName());
        Map<AttributeKey<?>, Object> attributes = new LinkedHashMap<>();
        attributes.putAll(event.getResource().getAttributes().asMap());
        attributes.putAll(event.getAttributes().asMap());
        serializeAttributes(gen, attributes);
        if (event.getSpanContext().isValid()) {
            gen.writeObjectField("trace.id", event.getSpanContext().getTraceId());
            gen.writeObjectField("span.id", event.getSpanContext().getSpanId());
        }
        gen.writeEndObject();
    }

    protected void serializeAttributes(JsonGenerator gen, Map<AttributeKey<?>, Object> attributes) throws IOException {
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
            String key = entry.getKey().getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                gen.writeArrayFieldStart(key);
                for (Object o : (List<?>) value) {
                    gen.writeObject(o);
                }
                gen.writeEndArray();
            } else {
                gen.writeObjectField(key, value);
            }
        }
    }

    protected String getTimestamp(long nanos) {
        Instant instant = Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(nanos));
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
