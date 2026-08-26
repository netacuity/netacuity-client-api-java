# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This changelog starts at the initial public release on GitHub; changes prior to that are not tracked here.

## [7.0.0]

### Added
- Initial public release of the NetAcuity Java Client API on GitHub.
- XML UDP query protocol, covering all current NetAcuity feature codes (Geo, Edge, Pulse, Mobile, VPN Proxy, Demographics, CBSA, and more — see the [NetAcuity documentation](https://docs.netacuity.com/) for the full list), with API ID, feature-code, and transaction-ID validation; response-echo verification (transaction ID and IP, the latter required to be an address literal and then compared as an `InetAddress`) to reject spoofed or stale replies without resolving server-supplied text; and a `SecureRandom`-backed transaction ID generator.
- Apache License 2.0 (see [LICENSE](LICENSE) and [NOTICE](NOTICE)).
