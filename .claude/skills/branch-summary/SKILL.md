---
name: branch-summary
description: Summarize the work done on the current branch compared to main (or another base branch) as a commit message, with a one-sentence theme line followed by a bullet list of changes. Use when the user asks to summarize a branch, describe what changed vs main, or draft a squash/merge commit message.
argument-hint: "[base-branch]"
allowed-tools: Bash(git log:*), Bash(git diff:*), Bash(git status:*), Bash(git rev-parse:*), Bash(git merge-base:*), Bash(git branch:*), Bash(git show:*)
---

# Branch summary

Write a commit message that summarizes everything this branch changes relative to its base.

## 1. Pick the base

Use `$ARGUMENTS` as the base branch if given. Otherwise use `main`; if that doesn't exist, try `master`, then `origin/main`, `origin/master`. Check with `git rev-parse --verify <name>`.

## 2. Gather the changes

Run these (independent, so in parallel):

- `git log --no-merges --format='%h %s%n%b' <base>..HEAD`: commits on the branch
- `git diff --stat <base>...HEAD`: files touched (three dots = since the merge base, so later commits on the base aren't included)
- `git diff <base>...HEAD`: the actual changes
- `git status --short`: uncommitted work

If the full diff is large, start from `--stat` and read the diffs of the most significant files individually (`git diff <base>...HEAD -- <path>`) rather than skimming everything.

Base the summary on **what the diff actually does**, not just on commit subjects. Commit messages can be vague, stale, or describe work that was later reverted on the same branch.

If the branch has no commits ahead of the base, say so and stop. If there are uncommitted changes, mention them separately after the message, and don't fold them into it unless the user asks.

## 3. Write the message

Format:

```
<Theme: one short sentence describing the overall change>

- <change>
- <change>
- <change>
```

Rules:

- **Theme line**: one sentence, imperative mood ("Add…", "Migrate…", "Fix…"), ideally ≤ 72 characters, no trailing period. It captures *why the branch exists*, the common thread, not a list.
- Blank line between the theme and the bullets.
- **Bullets**: one per meaningful change, imperative mood, concise (one line each where possible). Group related file edits into a single bullet. Order from most to least significant.
- Mention concrete names (classes, endpoints, config keys, dependencies and versions) when they help a reader; skip trivia like whitespace or import reordering.
- Don't list file paths alone as bullets; describe what changed.
- Include tests, build, and docs changes as their own bullets when present.

## 4. Output

Print the message in a fenced code block so it can be copied. Do **not** create a commit, amend, or push unless the user asks. If they do, pass the message via a heredoc (`git commit -F - <<'EOF' … EOF`) so formatting survives.
