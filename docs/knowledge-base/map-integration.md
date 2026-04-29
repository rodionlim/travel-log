# Map Integration

## Main Responsibilities

The map feature is responsible for:

- showing itinerary items with resolved coordinates on a Google Map
- plotting a polyline through the visible stops
- filtering flight markers so the map stays focused on the trip destination
- backfilling missing coordinates for imported or loosely matched places when possible
- providing a direct Google Maps launch path from itinerary items with an address

## Key Implementation Files

- `presentation/map/MapScreen.kt`
- `presentation/map/MapViewModel.kt`
- `data/remote/places/PlacesDataSource`
- `domain/repository/PlacesRepository`

## Runtime Map Behavior

- `MapScreen` renders a `GoogleMap` composable.
- The initial camera centers on the first resolved itinerary item.
- Markers are added for every item that has both latitude and longitude.
- A polyline is drawn when two or more points are available.

## Coordinate Resolution Strategy

Imported items do not need to arrive with coordinates already populated.

`MapViewModel` attempts a best-effort resolution path for items whose `place` is present but coordinates are missing:

- try the saved address first
- try a combined `name + address` query when helpful
- fall back to the place name alone
- persist the resolved place back onto the itinerary item when a coordinate-bearing match is found

This keeps imported trips usable on the map even when the parser only produced names or addresses.

## Flight Filtering

Flights are handled differently from regular itinerary items:

- non-flight items are shown whenever they have coordinates
- flight items are only plotted when their place text matches the trip destination closely enough

This avoids cluttering the trip map with unrelated airports outside the trip’s destination context.

## Address-To-Maps Convenience Action

The itinerary item card exposes a direct Google Maps action for items with an address.

- the launch URL now prefers the item's address first
- if no address exists, it falls back to the place name
- coordinates are only used as a fallback after those text-based options

This keeps the Google Maps launch behavior aligned with what the user expects to see, rather than opening an opaque coordinate search when a human-readable address is available.

## Maps API Key Rules

There are two separate Maps/Places concerns in the app:

- the Android Maps SDK key in the manifest
- the runtime Settings key used for Places-based search/fetch behavior

The map screen specifically depends on the manifest `MAPS_API_KEY` coming from `local.properties` and an app rebuild. If that key is missing or unresolved, `MapViewModel` surfaces a clear error explaining that the runtime settings key is not enough for the map screen itself.

## Related References

- [Project Overview](./project-overview.md)
- [Architecture](./architecture.md)
- [Development and Operations](./development-and-operations.md)
- [Trip Sync](../TripSync.md)
