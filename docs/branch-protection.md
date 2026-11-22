# Branch protection for `main`

Needs a repository admin (the repo is `c-madalin/IAPC`). Settings, then Rules, then Rulesets, then New branch
ruleset (or Settings, then Branches, then Add rule on older UIs):

- Target: the default branch (`main`).
- Restrict deletions; block force pushes.
- Require a pull request before merging:
  - Required approvals: 1.
  - Dismiss stale approvals when new commits are pushed.
  - Require review from Code Owners (once `.github/CODEOWNERS` has real handles, see docs/open-items.md).
  - Allowed merge method: squash.
- Require status checks to pass, with "require branches to be up to date":
  - `format, build, tests`
  - `compose and dashboards`
  - `secret scan`
  - `eval report is current, no regression`
- Require linear history.

The status check names only appear in the picker after each workflow has run once, so open a first PR before
setting them.
