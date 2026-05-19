# API Client SDK (MVP)

This folder is scaffolded from `openapi-mvp.yaml`.

## Layout
- `src/core`: generic http client and shared types
- `src/modules`: grouped endpoint wrappers
- `src/index.ts`: composed SDK facade

## Next
1. Replace manual module files with generated code from your preferred OpenAPI tool.
2. Keep request headers aligned with platform requirements:
   - `X-Tenant-Id`
   - `X-Request-Id`
   - `Idempotency-Key`
