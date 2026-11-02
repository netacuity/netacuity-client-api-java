# NetAcuity Client API — Java

Java client library for querying the [NetAcuity](https://www.digitalelement.com/solutions/netacuity/) Server for IP geolocation and intelligence data. Supports the XML UDP query protocol.

## Requirements

- **Maven** 3.6+ (for building from source)
- **Java** 8 or later
- A running **NetAcuity Server** accessible on UDP port 5400
- An **API ID** (customer-provided integer, range 0–127; default 0)

## Installation / Build

```bash
git clone https://github.com/netacuity/netacuity-client-api-java.git
cd netacuity-client-api-java
mvn install
```

## Adding as a Dependency

**Maven:**
```xml
<dependency>
    <groupId>digitalenvoy</groupId>
    <artifactId>netacuity-client-api-java</artifactId>
    <version>7.0.1</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'digitalenvoy:netacuity-client-api-java:7.0.1'
```

## Quick Start

### XML UDP Query

The XML UDP protocol supports multiple feature codes in a single query.

```java
import net.digitalenvoy.netacuity.api.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Sockets are pooled at the class level and shared across all accessors
InetAddress server = InetAddress.getByName("your-netacuity-server");
int apiId = 0; // API ID, range 0-127, default 0
int timeoutMillis = 3000; // socket timeout for each query, in milliseconds
DbAccessor accessor = DbAccessorFactory.getAccessor(server, apiId, timeoutMillis);

try {
    // Multi-feature query — single network round-trip, returns a flat map
    // 198.51.100.1 is a placeholder; replace with the IP address you want to look up
    List<NetAcuityFeature> features = Arrays.asList(NetAcuityFeature.PULSE, NetAcuityFeature.VPN_PROXY);
    Map<String, String> results = accessor.queryXml(features, InetAddress.getByName("198.51.100.1")).getResponseMap();
    System.out.println(results);
} catch (NetAcuityException e) {
    System.err.println("Server error: " + e.getMessage());
} catch (Exception e) {
    System.err.println("I/O error: " + e.getMessage());
}
```

## API Reference

### `DbAccessorFactory.getAccessor(serverAddress, apiId, timeoutMillis)`

Returns a new `DbAccessor` instance. A new instance is created on each call; sockets are pooled at the class level and shared across all accessors, so reuse is cheap and safe.

| Parameter | Type | Description |
|---|---|---|
| `serverAddress` | `InetAddress` | Address of the NetAcuity Server |
| `apiId` | `int` | Customer-assigned API ID, range 0–127, default 0 |
| `timeoutMillis` | `int` | Time in milliseconds to wait for a response before aborting the call |

### `DbAccessorFactory.getAccessor(serverAddress, apiId)`

Same as `getAccessor(serverAddress, apiId, timeoutMillis)`, but defaulting `timeoutMillis` to the default (`DEFAULT_TIMEOUT_MILLIS`, 2000ms).

| Parameter | Type | Description |
|---|---|---|
| `serverAddress` | `InetAddress` | Address of the NetAcuity Server |
| `apiId` | `int` | Customer-assigned API ID, range 0–127, default 0 |

### `DbAccessorFactory.getAccessor(serverAddress)`

Same as `getAccessor(serverAddress, apiId, timeoutMillis)`, but defaulting `apiId` to 0 and `timeoutMillis` to the default (`DEFAULT_TIMEOUT_MILLIS`, 2000ms).

| Parameter | Type | Description |
|---|---|---|
| `serverAddress` | `InetAddress` | Address of the NetAcuity Server |

### `DbAccessor.queryXml(features, queryAddress)`

XML UDP query. Queries multiple databases in a single round-trip and returns an `XmlResponse`. The transaction ID is auto-generated (via a `SecureRandom`-backed generator); use the `queryXml(featureCodes, queryAddress, transactionId)` overload below if you need to supply your own.

| Parameter | Type | Description |
|---|---|---|
| `features` | `List<NetAcuityFeature>` | The databases to query |
| `queryAddress` | `InetAddress` | The IP address to look up |

### `DbAccessor.queryXml(featureCodes, queryAddress, transactionId)`

XML UDP query by integer feature code, with a caller-supplied transaction ID.

| Parameter | Type | Description |
|---|---|---|
| `featureCodes` | `List<Integer>` | The feature codes of the databases to query |
| `queryAddress` | `InetAddress` | The IP address to look up |
| `transactionId` | `String` | Caller-supplied transaction ID, verified against the value echoed back in the response |

### `XmlResponse`

Returned by the XML UDP `queryXml` methods.

| Method | Returns | Description |
|---|---|---|
| `getResponseMap()` | `Map<String, String>` | Flat map of response field name to value, including `trans-id` and `ip` |
| `getRawResponse()` | `String` | The raw, unparsed response text, available alongside the parsed fields |

### Statistics (optional)

`DbAccessor` also exposes opt-in round-trip/socket-acquisition statistics collection, off by default:

| Method | Returns | Description |
|---|---|---|
| `startStatsManager()` | — | Begin collecting statistics for calls made through this accessor |
| `stopStatsManager()` | — | Stop collecting statistics and discard any counters collected so far |
| `dumpStats()` | `String` | A human-readable dump of the statistics collected since start (or last reset), or an empty string if not running |
| `resetStats()` | — | Reset the statistics counters; no effect if the stats manager isn't running |

## Feature Codes

For the complete, up-to-date list of feature codes and their response fields, see the [NetAcuity documentation](https://docs.digitalelement.com/).

## Examples

Runnable examples are provided in the `examples/` directory:

```bash
mvn package -pl netacuity-client-api-java,examples -am
```

(On Windows, use `;` instead of `:` to separate classpath entries below.)

```bash
java -cp "examples/target/*:netacuity-client-api-java/target/*" \
     net.digitalenvoy.netacuityapi.example.XmlQueryExample \
     <server_ip> <query_ip> <feature_code>[,<feature_code>...]

# e.g. query the Geo, Pulse, and VPN Proxy databases (feature codes 3, 26, 33):
java ... 192.0.2.1 198.51.100.1 3,26,33
```

## Running the Tests

```bash
mvn test -pl netacuity-client-api-java
```

## Upgrading from Older Versions

If you are upgrading from a version of this library that was distributed as `netacuity-api.jar`, remove that jar from your classpath before adding this one.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## Support

Technical Support is only available to those under active contract with Digital Element. To contact Support, use the contact information provided at contract initiation.

- Documentation: [docs.digitalelement.com](https://docs.digitalelement.com/)
- Issues: [GitHub Issues](https://github.com/netacuity/netacuity-client-api-java/issues)

## License

Copyright 2026 Digital Envoy, Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full license text.

This repository contains no third-party source code or binaries. The only third-party packages referenced are test-scope dependencies (JUnit 5, EPL-2.0) and Maven build plugins, resolved by Maven at build time and never included in the published jar, which contains only this repository's own code.
