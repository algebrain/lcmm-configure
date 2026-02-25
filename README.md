# configure

A small Clojure library for deterministic module configuration. It loads TOML files, overlays environment variables, validates required keys, and provides safe config output with secret masking.

## Highlights

- TOML + env with predictable precedence
- Required key validation
- Optional env type parsing
- Safe config dumping with secret masking
- Support for env-only setups in production

## Documentation

- Developer guide: [docs/CONFIGURE.md](docs/CONFIGURE.md)
- Admin guide: [docs/CONFIGURE_ADMIN.md](docs/CONFIGURE_ADMIN.md)
- Architecture notes: [docs/ARCH.md](docs/ARCH.md)

## Status

Early version, focused on correctness and clarity. Public API is stable enough for internal use but may evolve.
