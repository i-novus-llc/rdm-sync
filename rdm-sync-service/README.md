# Сервис синхронизации справочников НСИ

## Общая информация

Сервис предназначен для синхронизации справочников НСИ с использованием библиотеки-[стартера](../rdm-sync-spring-boot-starter/README.md).

## Информация для разработчика

### Запуск

1. Собрать модуль rdm-sync-service командой
```
mvn clean package -Pproduction
```

2. Определить любым доступным для spring-boot приложений настройки (properties).

3. [Настроить маппинг](../README.md#Настройка-маппинга) и создать таблицы для справочников,
   либо воспользоваться [автосозданием](../README.md#Создание-таблиц-клиента-в-автоматическом-режиме).

4. Запустить `java -jar rdm-sync-service.jar`

### Описание настроек в файле ```application.properties```

#### Создание таблиц Quartz в БД

- `rdm.sync.liquibase.param.quartz_schema_name` -- наименование схемы, в которой находятся или будут созданы таблицы Quartz (по умолчанию -- `rdm_sync_qz`).
- `rdm.sync.liquibase.param.quartz_table_prefix` -- префикс, используемый при наименовании таблиц Quartz (по умолчанию -- `rdm_sync_qrtz_`).

#### Рекомендуемые настройки Quartz

```properties
## Spring Quartz
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=<value>

spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.scheduler.instanceName=RdmSyncScheduler

# jobStore
spring.quartz.properties.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
spring.quartz.properties.org.quartz.jobStore.tablePrefix=${rdm.sync.liquibase.param.quartz_schema_name}.${rdm.sync.liquibase.param.quartz_table_prefix}
spring.quartz.properties.org.quartz.jobStore.isClustered=true
```

Здесь значение `spring.quartz.jdbc.initialize-schema` определяется клиентским приложением.

Если приложение уже использует Quartz, то значение не нужно менять, т.к. liquibase не будет создавать таблицы для Quartz.

Если приложение не использует Quartz, то значение должно быть `never`, т.к. liquibase только один раз создаст таблицы для Quartz.
Если необходимо заново создавать таблицы каждый раз при запуске, необходимо создать кастомный скрипт и задать его наименование в настройке `spring.quartz.jdbc.schema`.

## Настройки

Обязательно должны быть определены следующие настройки:
```properties

rdm.sync.liquibase.param.quartz_schema_name=
rdm.sync.liquibase.param.quartz_table_prefix=

spring.quartz.jdbc.initialize-schema=
spring.quartz.properties.org.quartz.jobStore.tablePrefix=${rdm.sync.liquibase.param.quartz_schema_name}.${rdm.sync.liquibase.param.quartz_table_prefix}
```
