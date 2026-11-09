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

后端 MVP 主链路已经完成：Docker MySQL、AFDC CSV 幂等导入、城市与接口类型搜索、Cursor Pagination、站点详情、评价提交与列表、评分聚合及评分排序。当前本地数据集包含 2,079 个 Illinois 充电站，完整自动化测试连接本地 MySQL 运行。前端、地理半径搜索、Redis 和部署尚未实现。

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

Core endpoints:

- `GET /api/v1/health`
- `GET /api/v1/stations?city=Chicago`
- `GET /api/v1/stations?city=Chicago&connectorType=CCS&sort=rating&order=desc`
- `GET /api/v1/stations?city=Chicago&connectorType=CCS&sort=power&order=desc`
- `GET /api/v1/stations/{stationId}`
- `GET /api/v1/stations/{stationId}/reviews`
- `POST /api/v1/stations/{stationId}/reviews`

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
