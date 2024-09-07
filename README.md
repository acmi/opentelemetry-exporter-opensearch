# OpenTelemetry Opensearch Exporter

This exporter sends logs to Opensearch. Logs are delivered in JSON format to [Bulk API](https://opensearch.org/docs/latest/api-reference/document-apis/bulk/) enpoint.

## Configuration

| Property                                    | Description                                                                                                                                                                  | Default               |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| otel.exporter.opensearch.endpoint           | The Opensearch endpoint to send logs to. Must be a URL with a scheme of either http or https based on the use of TLS.                                                        | http://localhost:9200 |
| otel.exporter.opensearch.index              | The Opensearch index name to publish logs to. (Required)                                                                                                                     |                       |
| otel.exporter.opensearch.headers            | Key-value pairs separated by commas to pass as request headers on requests.                                                                                                  |                       |
| otel.exporter.opensearch.timeout            | The maximum waiting time, in milliseconds, allowed to log batch.                                                                                                             | 10000                 |
| otel.exporter.opensearch.certificate        | The path to the file containing trusted certificates to use when verifying a server’s TLS credentials. The file should contain one or more X.509 certificates in PEM format. |                       |
| otel.exporter.opensearch.client.key         | The path to the file containing private client key to use when verifying a client’s TLS credentials. The file should contain one private key PKCS8 PEM format.               |                       |
| otel.exporter.opensearch.client.certificate | The path to the file containing trusted certificates to use when verifying a client’s TLS credentials. The file should contain one or more X.509 certificates in PEM format. |                       |

## Usage
Can be used as Opentelemetry javaagent extension.
```
java -javaagent:path/to/opentelemetry-javaagent.jar
     -Dotel.javaagent.extensions=path/to/opentelemetry-exporter-opensearch.jar
     -jar myapp.jar
```