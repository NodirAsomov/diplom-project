# Клиенты рекомендательной системы

`CollectorClient.collect(userId, eventId, ActionTypeProto)` отправляет действие с текущим UTC-временем.
`AnalyzerClient` предоставляет `getRecommendationsForUser`, `getSimilarEvents` и
`getInteractionsCount`, возвращающие `Stream<RecommendedEventProto>`.

Клиенты — Spring-компоненты в пакете `ru.practicum.ewm.stats.client`.
Настройте `grpc.client.collector.address=discovery:///collector` и
`grpc.client.analyzer.address=discovery:///analyzer`; используйте plaintext внутри локальной сети Compose.
RPC имеют ограничение времени: 6 секунд для Collector, 5 секунд для Analyzer.
Потоки выполняются лениво; завершите их потребление внутри вызывающего метода.

Описание запуска и сквозного теста: [stats/README.md](../README.md).
