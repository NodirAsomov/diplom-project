# Рекомендательная система — третий этап

## Поток данных

`event-service` и `request-service` отправляют действия через `CollectorClient`.
Collector принимает gRPC `UserActionController.CollectUserAction`, преобразует Protobuf в Avro
и подтверждает запрос после подтверждения Kafka. Aggregator читает `stats.user-actions.v1`
и записывает сходства в `stats.events-similarity.v1`. Analyzer читает оба топика,
обновляет PostgreSQL и предоставляет три потоковых gRPC-метода рекомендаций.

Схемы находятся в `stats-proto/src/main/proto` и `stats-avro/src/main/avro`;
Java-классы генерируются Maven. Сериализация Kafka — бинарный Avro без Schema Registry.

## Расчёт

Веса действий: VIEW = 0.4, REGISTER = 0.8, LIKE = 1.0.
Для каждой пары пользователь–мероприятие учитывается максимум, поэтому повторные и менее весомые
действия не увеличивают рейтинг. В задании значения весов не указаны; выбранные значения
централизованы в `ActionWeight`.

Сходство: `sum(min(weight[u,A], weight[u,B])) / sqrt(sum(weight[u,A]) * sum(weight[u,B]))`.
Суммы обновляются по разнице старого и нового максимума. Каждая пара упорядочена:
`eventA < eventB`. Даже если пользователь не взаимодействовал с B, изменение суммы A
меняет знаменатель и требует пересчёта пары A/B.

Analyzer хранит максимальный вес и наиболее позднее время взаимодействия независимо друг от друга.
Рекомендации выбираются по последним N взаимодействиям, исключают все ранее посещённые страницы
и оцениваются по K ближайшим просмотренным мероприятиям. K задаётся
`stats.recommendations.neighbors` (по умолчанию 10). Нулевые сходства не включаются.
HTTP-поле `rating` — сумма максимальных весов из `GetInteractionsCount`, а не предсказанная оценка.

## Запуск

Из корня проекта:

```sh
mvn verify -Pcheck
docker compose up -d --build
```

Kafka доступна на `localhost:9092`, внутри Docker — `kafka:29092`.
Топики создаёт одноразовый контейнер kafka-init. Данные Kafka и PostgreSQL хранятся в томах.
Config Server регистрируется в Eureka. Новые сервисы находят Config Server через
discovery, а Collector и Analyzer регистрируют случайный gRPC-порт.
Core-клиенты используют `discovery:///collector` и `discovery:///analyzer`.
Настройка случайного порта и discovery соответствует
[документации grpc-spring](https://grpc-ecosystem.github.io/grpc-spring/en/client/configuration.html).
Kafka запускается в режиме KRaft с
[официальным образом Apache](https://kafka.apache.org/37/getting-started/quickstart/).

Aggregator работает с одной партицией топика действий и одним активным экземпляром.
Его состояние восстанавливается чтением истории от начала при назначении партиции;
для этого топик действий сохраняет всю историю (`retention.ms=-1`).
Во время восстановления сходства последовательно пересчитываются, затем сходятся
к актуальным значениям. Масштабирование требует общего хранилища состояния или Kafka Streams.
При временной ошибке потребители повторяют обработку, не пропуская запись.

## HTTP

- `GET /events`: не отправляет действие просмотра.
- `GET /events/{id}`: требует `X-EWM-USER-ID`, отправляет VIEW для существующего опубликованного мероприятия.
- `GET /events/recommendations?size=10`: требует тот же заголовок, сохраняет порядок Analyzer и выдаёт опубликованные мероприятия.
- `PUT /events/{eventId}/like`: требует тот же заголовок; LIKE разрешён после даты мероприятия при подтверждённой заявке пользователя, иначе 400.
- `POST /users/{userId}/requests?eventId=...`: после успешного создания заявки отправляет REGISTER.

Отправка REGISTER выполняется после фиксации заявки в БД. При недоступности Collector
заявка остаётся сохранённой; гарантированная доставка между БД и gRPC потребует transactional outbox.

Старые REST-модули статистики сохранены для совместимости, core-сервисы больше
не используют их для просмотров и рейтинга.

## Сквозная проверка

```sh
docker compose -p ewm-recommendations-check -f docker-compose.yml -f scripts/recommendations-smoke.compose.yaml up -d --build collector aggregator analyzer
mvn test -pl stats/stats-client -am "-Dtest=RecommendationSmokeTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dstats.smoke=true"
docker compose -p ewm-recommendations-check -f docker-compose.yml -f scripts/recommendations-smoke.compose.yaml down
```

Override открывает только для этой проверки gRPC на localhost:9095 и localhost:9096.
Тест отправляет действия двух пользователей, ждёт асинхронную обработку и проверяет рейтинг,
похожие мероприятия и персональные рекомендации. В обычном запуске gRPC-порты случайные.
