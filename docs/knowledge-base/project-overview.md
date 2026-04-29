# Project Overview

## Product Summary

Wanderlog is an offline-first Android travel planner built for a single user. The app combines itinerary planning, budgeting, packing, attachments, map views, AI itinerary assistance, and booking import workflows.

## Core User Workflows

- Create or edit a trip with destination, dates, traveller aliases, and optional budget.
- Plan daily itinerary items and reorder them within each day.
- View active accommodation separately from the rest of the selected day's itinerary.
- Ask AI for itinerary generation, whole-trip updates, or conversational trip Q&A.
- Import booking details from files or pasted text and review the extracted itinerary items before saving.
- Track expenses and optionally view them in a separate display currency.
- Manage packing lists for the whole trip or per traveller.
- Store and view trip-level and item-level attachments locally.

## Current Feature Highlights

- AI itinerary generation and Ask About Trip both use the current trip context.
- Booking imports support PDFs, images, text files, and clipboard-pasted text.
- Imported bookings can create linked transport, accommodation, or activity expenses.
- Multi-day car rentals can create separate pickup and return itinerary entries.
- Imported places without coordinates can be backfilled later through a best-effort Places lookup.
- Items with an address expose a direct Google Maps convenience action.

## Settings-Driven Capabilities

- AI features require an OpenAI API key.
- Separate OpenAI models can be chosen for general generation vs image/PDF parsing.
- Budget totals can be shown in a user-chosen display currency, defaulting to SGD.
- Runtime Google Maps / Places behavior can use the settings-provided Maps key, while the map screen still depends on the manifest key in `local.properties`.
