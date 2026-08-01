# Per-PR changelog policy

Every pull request must add exactly one Markdown fragment to this directory,
named:

```text
YYYY-MM-DD-short-description.md
```

The fragment must describe user-visible and architectural changes under the
headings that apply:

```markdown
# Summary

## Added
## Changed
## Fixed
## Removed
## Validation
```

Empty headings should be omitted. Documentation-only PRs still require a
fragment. A release workflow may combine fragments into a versioned changelog,
but the original fragments remain the permanent PR-level history.

