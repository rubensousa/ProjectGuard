
# CHANGELOG

## 1.0.0-alpha05 (2026-02-16)

- Rename plugin to `ProjectGuard` to avoid confusion with other similar plugins after community feedback

## 1.0.0-alpha04 (2026-02-16)

### API Changes

- Added `guardRule`, `restrictModuleRule` and `restrictDependencyRule` to allow re-using a set of rules across different configurations

## 1.0.0-alpha03 (2026-02-15)

### API Changes

- Added `restrictModule` to allow restricting all dependencies by default in some modules.

### Bug fixes

- Fixed baseline being regenerated when it is already there in some cases
- Fixed existing reasons in the baseline not being kept across task executions

## 1.0.0-alpha02 (2026-02-12)

### API Changes

- Added `projectGuardBaseline` to generate a yml file  with the suppressions for the existing restrictions. The file is `projectguard-baseline.yml`
- `projectGuardCheck` now generates the HTML report by default
- `projectGuardCheck` can now be used for individual modules
- Include transitive dependency verifications by default
- Renamed `setReason` to `reason` in the kotlin DSL
- Improved html report to allow filtering by module and dependency

### Bug fixes

- Fixed duplicate dependencies being shown when they're included in tests
- Fixed reason for dependency restriction not being shown correctly

## 1.0.0-alpha01 (2026-02-08)

- Initial alpha release