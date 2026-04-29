# Maintenance Rules

## Goal

The knowledge base should stay useful for fast retrieval, not become a second unstructured wiki.

## What Belongs Here

- stable project knowledge
- implementation patterns that are likely to matter again
- feature behavior that is easy to forget later
- links to deeper canonical docs when those already exist

## What Should Not Accumulate Here

- long raw changelogs
- transient debugging transcripts
- repeated copies of README or CLAUDE content
- low-value implementation trivia that is already obvious from the code

## Topic File Rules

- Keep one topic file focused on one area of the project.
- Split when a file starts covering two unrelated concerns.
- Prefer adding a new topic page over letting one topic become the default dumping ground.
- Add cross-links instead of copying the same explanation into multiple files.

## Chronology Rules

- Use `NOTES.md` as the short overview only.
- Put dated history under `chronology/` using monthly files.
- Chronology should answer "when did this behavior land?" rather than "how does this subsystem work?"
- If a chronology note needs a deep explanation, summarize it briefly and link to the owning topic page.

## Recommended Update Pattern

When a meaningful feature or structural change lands:

1. Update the owning topic page.
2. Add a dated note to the current monthly chronology file.
3. Update the index only if a new topic or chronology file was added.

## Query-Friendliness

This structure is designed so future queries can use two paths:

- topic-first when the question is conceptual
- chronology-first when the question is about when or why something changed

That split is what prevents the knowledge base from bloating into a single ambiguous source.
