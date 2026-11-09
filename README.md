# EVFinder

EVFinder 是一个面向美国电动车车主的充电站检索与推荐平台。用户可以按位置和充电需求查找站点，比较站点详情，并基于距离、接口类型、功率和评分等信息选择更适合自己的充电站。

## 为什么做它？

公共充电站信息常分散在不同平台，且站点状态、接口信息和用户体验未必完整一致。EVFinder 的目标是将公开站点数据与用户评价集中展示，减少车主比较站点时的搜索和决策成本。

同时，这是一个从零构建的 Java 后端学习项目：每项技术都应对应真实功能，并具备可重复的测试或验证方式。

## 初版功能

- 导入一批美国公共充电站数据；
- 按城市查询充电站，并按接口类型筛选；
- 提供至少一种可解释的排序方式；
- 查看站点的基本信息与充电设施信息；
- 提交和查看站点评价；
- 提供服务健康检查接口。

按坐标和半径搜索、多种排序、性能优化与缓存属于初版后的增强功能。

完整范围、阶段与验收标准见 [需求文档](docs/requirements.md)。

## 当前状态

后端 MVP 主链路已经完成并通过用户闭环验收：Docker MySQL、AFDC CSV 幂等导入、城市与接口类型搜索、Cursor Pagination、站点详情、评价提交与列表、评分聚合，以及按评分和功率排序。

当前 AFDC EV Charging Units 快照包含 7,208 条设备记录，聚合为 2,118 个唯一 Illinois 站点。开发数据库还保留了一条较早快照中的站点，因此本地验收时共有 2,119 个站点。原始数据文件和 API Key 不提交到 Git。前端、地理半径搜索、Redis 和部署属于 MVP 后续阶段。

## 规划技术栈

- Java 21、Spring Boot、Maven
- MySQL、Redis
- Docker Compose
- 前端框架待在前端阶段决定

## Local MySQL

EVFinder uses Docker Compose to run MySQL 8.4 in an isolated local environment.

```bash
cp .env.example .env
docker compose up -d mysql
docker compose ps
```

The container exposes MySQL at `localhost:3307` to avoid conflicting with a local MySQL server on port `3306`.

Run the test suite:

```bash
./mvnw test
```

Start the API:

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080` to use the demo frontend. The responsive page is served directly by Spring Boot and supports station search, connector filtering, power/rating sorting, cursor pagination, station details, and review submission. No separate Node.js frontend server is required.

Import an AFDC CSV file. The file is intentionally not committed to Git:

```bash
set -a
source .env
set +a
curl --fail --location \
  "https://developer.nlr.gov/api/alt-fuel-stations/v1/ev-charging-units.csv?api_key=${NLR_API_KEY}&state=IL" \
  --output data/raw/afdc_ev_charging_units_il.csv

./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--evfinder.import.file=data/raw/afdc_ev_charging_units_il.csv --server.port=0"
```

The import is idempotent: rerunning the same file updates existing stations instead of creating duplicates. Connector counts are summed across equipment rows, while connector power uses the maximum reported kW value. Missing source values remain `null`.

Core endpoints:

- `GET /api/v1/health`
- `GET /api/v1/stations?city=Chicago`
- `GET /api/v1/stations?city=Chicago&connectorType=CCS&sort=rating&order=desc`
- `GET /api/v1/stations?city=Chicago&connectorType=CCS&sort=power&order=desc`
- `GET /api/v1/stations/{stationId}`
- `GET /api/v1/stations/{stationId}/reviews`
- `POST /api/v1/stations/{stationId}/reviews`

## MVP Acceptance Flow

The MVP is accepted through a complete user-visible loop:

1. Search for a station.
2. Open its details.
3. Submit a review.
4. List the station's reviews.
5. Open the details again and verify that `averageRating` and `reviewCount` changed.
6. Submit the same user/station review again and verify that the existing review is returned without being overwritten.

This flow was verified against the real local MySQL database. The temporary acceptance user and review were removed afterward, and the database returned to its original state.

Example requests:

```bash
curl 'http://localhost:8080/api/v1/stations?city=Chicago&connectorType=CCS&sort=power&order=desc&limit=5'

STATION_ID=3596 # Replace this with an ID returned by the search request.
curl "http://localhost:8080/api/v1/stations/${STATION_ID}"

curl -i -X POST "http://localhost:8080/api/v1/stations/${STATION_ID}/reviews" \
  -H 'Content-Type: application/json' \
  -d '{"userId":"demo-user-1","rating":5,"comment":"Fast and easy to access."}'

curl "http://localhost:8080/api/v1/stations/${STATION_ID}/reviews"
curl "http://localhost:8080/api/v1/stations/${STATION_ID}"
```

Automated verification:

```bash
./mvnw test
```

The current suite contains 54 tests covering health checks, station search and details, connector filtering, cursor pagination, reviews, rating aggregation, import normalization, and power sorting.

Stop the database while keeping its data:

```bash
docker compose stop
```

Remove the container while keeping the named volume:

```bash
docker compose down
```

Do not run `docker compose down -v` unless the local database data should be permanently deleted.

## 文档

- [产品需求与开发范围](docs/requirements.md)
- [API Contract](docs/api-contract.md)
- [Database Design](docs/database/README.md)
- [Data Source Plan](docs/database/data-source-plan.md)
- [AFDC Import Mapping](docs/database/afdc-import-mapping.md)
- [Station Search Query Plan](docs/database/query-plan.md)
- [学习笔记](EVFinder学习笔记.txt)
