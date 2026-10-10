# Explore With Me

Java 21, Maven, Spring Boot 3.3.13, Spring Cloud 2023.0.

## Структура

- `core/main-service` — основной API.
- `stats/stats-server`, `stats/stats-client`, `stats/stats-dto` — статистика.
- `infra/discovery-server` — Eureka, порт 8761.
- `infra/config-server` — централизованная конфигурация, случайный порт.
- `infra/gateway-server` — API Gateway, порт 8080.

Main Service и Stats Server используют случайные порты. Клиент статистики получает
текущий адрес Stats Server через DiscoveryClient и повторяет обнаружение до трёх раз
с паузой 3 секунды. Gateway использует маршрут `lb://main-service`.

## Запуск

```bash
mvn verify
docker compose up -d --build
```

Eureka: http://localhost:8761. API: http://localhost:8080.

Для совместимости с проверками CI внешний порт 9090 также направлен в Gateway.
Маршруты /hit и /stats используют lb://stats-server: сам Stats Server сохраняет
случайный порт и обнаруживается через Eureka. Gateway запускается после успешных
проверок готовности Main Service и Stats Server. В Docker их служебные health endpoints
доступны на внутреннем порту 8081; бизнес API остаются на случайных портах.

Config Server использует профиль native и конфигурации из
`infra/config-server/src/main/resources/config/{main-service,stats-server,gateway-server}`.
Каждый клиент находит Config Server через Eureka по имени `config-server`.
Загрузка конфигурации обязательна; при запуске предусмотрены повторные попытки,
пока Config Server регистрируется в Eureka.

Для запуска из IDE сначала поднимите базы:
`docker compose up -d stats-db ewm-db`.
Затем запускайте DiscoveryServerApplication, ConfigServerApplication,
EWMStatsApplication, EWMApplication и GatewayServerApplication.
Дождитесь появления CONFIG-SERVER в Eureka перед запуском клиентов.
Для локального запуска используются PostgreSQL на портах 6541 и 6542.
В Docker адреса баз и Eureka задаются переменными окружения.

## Проверка

```bash
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
curl http://localhost:8080/categories
curl http://localhost:8080/events
```

В реестре должны быть CONFIG-SERVER, MAIN-SERVICE, STATS-SERVER и GATEWAY-SERVER.
Запросы categories/events проходят через Gateway; events также записывает обращение
в Stats Server. Конфигурации доступны по адресу обнаруженного Config Server:
`/main-service/default`, `/stats-server/default`, `/gateway-server/default`.

Автоматическая проверка после регистрации сервисов (PowerShell):

```powershell
./scripts/verify-infrastructure.ps1
```

Она проверяет статусы Eureka, отдельные конфигурации, случайные порты,
HTTP 200 через Gateway и сохранение статистики обращения к /events.
