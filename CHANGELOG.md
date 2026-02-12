
# CHANGELOG

## 1.0.0-alpha02 (2026-02-12)

### Features

- Added `dependencyGuardBaseline` to generate a file `dependencyguard.yml` with the suppressions for the existing restrictions
- `dependencyGuardCheck` now generates the HTML report by default
- `dependencyGuardCheck` can now be used for individual modules
- Include transitive dependency verifications by default
- Renamed `setReason` to `reason` in the kotlin DSL
- Improved html report to allow filtering by module and dependency

### Bug fixes

- Fixed duplicate dependencies being shown when they're included in tests
- Fixed reason for dependency restriction not being shown correctly

## 1.0.0-alpha01 (2026-02-08)

- Initial alpha release