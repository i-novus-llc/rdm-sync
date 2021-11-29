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

3. [Настроить маппинг](../#Настройка-маппинга) и создать таблицы для справочников,
   либо воспользоваться [автосозданием](../#Создание-таблиц-клиента-в-автоматическом-режиме).

4. Запустить `java -jar rdm-sync-service.jar`

### Описание настроек в файле ```application.properties```

#### Создание таблиц Quartz в БД:

- `rdm.sync.liquibase.param.quartz_schema_name` -- наименование схемы, в которой находятся или будут созданы таблицы Quartz (по умолчанию -- `rdm_sync_qz`).
- `rdm.sync.liquibase.param.quartz_table_prefix` -- префикс, используемый при наименовании таблиц Quartz (по умолчанию -- `rdm_sync_qrtz_`).

## Настройки

Обязательно должны быть переопределены следующие настройки:
```
rdm.sync.liquibase.param.quartz_schema_name=
rdm.sync.liquibase.param.quartz_table_prefix=
```
