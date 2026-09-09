# OpenSpec change template

Copy this folder pattern when starting behavioral work:

```
openspec/changes/<kebab-id>/
  proposal.md
  tasks.md
  specs/<capability>/spec.md   # delta only
```

## proposal.md skeleton

```markdown
# Change: <title>

## Why

<at least a few sentences: problem and outcome>

## What Changes

- …

## Capabilities

- New: `<kebab>` (optional)
- Modified: `obd-session` | `android-bt` | `diag-archive`
```

## Delta spec skeleton

```markdown
## ADDED Requirements

### Requirement: …

The system SHALL …

#### Scenario: …

- GIVEN …
- WHEN …
- THEN …

## MODIFIED Requirements

### Requirement: <exact existing title>

<full updated requirement body + scenarios>

## REMOVED Requirements

### Requirement: <title>

**Reason:** …
**Migration:** …
```
