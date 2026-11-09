# EVFinder API Contract

Base path: `/api/v1`

Requests and responses use JSON. Field names use `camelCase`.

## Common Status Codes

| Status | Meaning |
| --- | --- |
| `200 OK` | Request succeeded |
| `201 Created` | Resource created |
| `400 Bad Request` | Invalid request |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Request conflicts with existing data |
| `500 Internal Server Error` | Unexpected server error |

Error response:

```json
{
  "code": "STATION_NOT_FOUND",
  "message": "Station 123 was not found"
}
```

## Health Check

### `GET /api/v1/health`

Response: `200 OK`

```json
{
  "status": "ok"
}
```

## Search Stations

### `GET /api/v1/stations`

Query parameters:

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `city` | string | Yes | City name, such as `Chicago` |
| `connectorType` | string | No | `CCS`, `CHAdeMO`, or `J1772` |
| `sort` | string | No | `rating`; omit for stable station-ID order |
| `order` | string | No | `asc` or `desc`; default: `desc` |
| `limit` | integer | No | 1 to 100; default: 20 |
| `cursor` | string | No | Cursor for the next page |

Example:

```http
GET /api/v1/stations?city=Chicago&connectorType=CCS&sort=rating&order=desc&limit=20
```

Response: `200 OK`

```json
{
  "items": [
    {
      "id": 123,
      "name": "Example Charging Station",
      "address": "100 Example Street, Chicago, IL",
      "city": "Chicago",
      "connectorTypes": ["CCS", "J1772"],
      "maxPowerKw": 150,
      "averageRating": 4.5,
      "reviewCount": 12
    }
  ],
  "nextCursor": null
}
```

If no stations match, return `200 OK` with an empty `items` array.

## Get Station Details

### `GET /api/v1/stations/{stationId}`

`stationId` must be a positive integer.

Response: `200 OK`

```json
{
  "id": 123,
  "name": "Example Charging Station",
  "address": "100 Example Street, Chicago, IL",
  "city": "Chicago",
  "state": "IL",
  "latitude": 41.8781,
  "longitude": -87.6298,
  "connectors": [
    {
      "type": "CCS",
      "count": 4,
      "maxPowerKw": 150
    }
  ],
  "averageRating": 4.5,
  "reviewCount": 12
}
```

If the station does not exist, return `404 Not Found`.

## List Station Reviews

### `GET /api/v1/stations/{stationId}/reviews`

Query parameters:

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `limit` | integer | No | 1 to 100; default: 20 |
| `cursor` | string | No | Cursor for the next page |

Response: `200 OK`

```json
{
  "items": [
    {
      "id": 456,
      "userId": "demo-user-1",
      "rating": 5,
      "comment": "Fast and easy to access.",
      "createdAt": "2026-11-07T12:00:00Z"
    }
  ],
  "nextCursor": null
}
```

## Create a Review

### `POST /api/v1/stations/{stationId}/reviews`

Request body:

```json
{
  "userId": "demo-user-1",
  "rating": 5,
  "comment": "Fast and easy to access."
}
```

Rules:

- `userId` is required.
- `rating` must be an integer from 1 to 5.
- `comment` is optional and may contain up to 500 characters.
- A user may create only one review per station.

Response for a new review: `201 Created`

```json
{
  "id": 456,
  "stationId": 123,
  "userId": "demo-user-1",
  "rating": 5,
  "comment": "Fast and easy to access.",
  "createdAt": "2026-11-07T12:00:00Z"
}
```

If the same user submits another review for the same station, return the existing review with `200 OK`.
