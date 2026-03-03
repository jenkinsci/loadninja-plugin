# Changelog

All notable changes to the LoadNinja Jenkins Plugin will be documented in this file.

## [1.5] - 2026-03-02

### Breaking Changes
- **Requires Jenkins 2.414.3 or later** (upgraded from 2.7.3)
- Users on older Jenkins versions must upgrade Jenkins before installing this version
- Version 1.4 remains available for legacy Jenkins installations

### Changed
- Upgraded parent plugin from 3.4 to 4.42
- Upgraded Jenkins baseline from 2.7.3 to 2.414.3
- Modernized build infrastructure and dependencies
- Removed deprecated Java 7 target (now uses Java 11)

### Migration Guide
1. Backup your Jenkins instance
2. Upgrade Jenkins to version 2.414.3 or later
3. Install LoadNinja Plugin 1.5
4. Test your existing LoadNinja build configurations