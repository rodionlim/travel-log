---
name: create-knowledge-base
description: "Use when: creating a project knowledge base, documenting a codebase into topic files, adding an index.md, splitting docs to avoid bloat, adding NOTES.md or chronology files, or setting up maintainable internal project documentation for future reuse."
---

# Create Knowledge Base

## Purpose

Create a local, maintainable project knowledge base that stays useful as the codebase grows.

This skill is for building a documentation structure that helps with:

- answering future questions about how the project works
- preserving important implementation knowledge outside the code
- avoiding one giant documentation file that becomes noisy and unqueryable
- keeping stable topic knowledge separate from chronological change history

## When To Use

Use this skill when the user asks for any of the following:

- create a knowledge base
- document the project structure
- build internal docs for this repo
- create an `index.md` and split docs into multiple files
- add a `NOTES.md` or chronology log
- make the docs easier to query later
- prevent documentation bloat in the future

## Core Principles

### 1. Prefer topic files over one big document

Create separate files for distinct concerns instead of accumulating all knowledge in one page.

Good examples:

- project overview
- architecture
- AI / import behavior
- sync / storage
- settings / onboarding
- map integration
- operations / CI / release

### 2. Separate stable knowledge from history

Keep these concerns separate:

- **topic docs** explain how the project works now
- **chronology docs** explain when major changes landed

Do not turn a chronology file into a design document.

### 3. Make the entry point obvious

Always create an `index.md` that:

- lists the topic files
- links any chronology notes
- links canonical project docs such as `README.md`, `CLAUDE.md`, or release docs
- explains how the knowledge base should be maintained

### 4. Keep it query-friendly

Structure the knowledge base so future questions can be answered by two different paths:

- **topic-first** for conceptual questions
- **chronology-first** for “when did this change?” questions

This is what prevents the knowledge base from becoming a second README or a raw changelog dump.

## Recommended Structure

Prefer a structure like this:

```text
docs/
  knowledge-base/
    index.md
    project-overview.md
    architecture.md
    maintenance-rules.md
    NOTES.md
    chronology/
      index.md
      YYYY-MM.md
```

Add more topic files only when they reflect real separate concerns.

## Suggested Workflow

### Step 1. Inspect existing docs first

Before creating anything:

- read `README.md`
- read project guidance such as `CLAUDE.md`, `AGENTS.md`, or similar repo notes
- inspect `docs/`
- inspect any stored repo memory or local notes if available

The goal is to integrate with what already exists rather than creating a parallel documentation system.

### Step 2. Design the topic layout

Choose a small initial set of topic files. Do not over-fragment too early.

Start with the fewest files that separate the main concerns cleanly.

### Step 3. Write the index first

The index should:

- name the knowledge base
- explain why it is split by topic
- link all topic files
- link chronology files or overview notes
- link canonical docs elsewhere in the repo

### Step 4. Write focused topic files

Each topic file should:

- answer one class of future question well
- avoid copying large amounts of content from other docs
- link out when deeper docs already exist
- stay concise and stable

### Step 5. Add maintenance rules

Create a `maintenance-rules.md` file or equivalent that explains:

- what belongs in a topic file
- what belongs in chronology
- when to split a file
- how to keep future updates small and consistent

### Step 6. Add chronology only if it helps

If the project is active or features are evolving, create:

- a short `NOTES.md` overview
- a `chronology/` folder
- monthly note files such as `2026-04.md`

If the user only needs a static knowledge base, chronology can be omitted.

## Chronology Guidance

If chronology is needed, use this pattern:

- `NOTES.md` stays short and explains the chronology system
- `chronology/index.md` lists the monthly files
- `chronology/YYYY-MM.md` stores dated entries

Chronology entries should be:

- factual
- concise
- ordered by date
- linked back to stable topic docs when appropriate

Avoid long prose in chronology files.

## Maintenance Rules To Recommend

When creating or revising the knowledge base, include guidance like this:

- update the owning topic page when a stable behavior changes
- add a chronology entry when a notable feature or structural change lands
- update the index only when adding or removing a topic or chronology file
- prefer links over duplicated explanations

## Anti-Patterns

Avoid these:

- one massive `knowledge-base.md`
- copying the entire README into topic files
- turning chronology into a long-form design explanation
- creating too many files before the project actually needs them
- mixing temporary debugging notes with stable project knowledge

## Good Output Characteristics

A good knowledge base created with this skill should:

- be easy to scan
- be easy to extend
- stay small per file
- answer both conceptual and historical questions
- avoid duplication with existing repository docs

## Default Deliverables

When the user asks to create a knowledge base, default to producing:

1. `docs/knowledge-base/index.md`
2. a small set of topic files
3. `docs/knowledge-base/maintenance-rules.md`
4. `docs/knowledge-base/NOTES.md` if chronology is useful
5. `docs/knowledge-base/chronology/index.md` and the current month file if history tracking is useful

## Reuse Guidance

This skill is workspace-scoped. To reuse it in another project, copy the entire folder:

```text
.github/skills/create-knowledge-base/
```

Then adjust the generated topic files to fit that repo’s real concepts instead of forcing the exact same file list.
