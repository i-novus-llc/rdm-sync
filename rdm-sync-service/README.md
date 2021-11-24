# Сервис синхронизации справочников

## Общая информация

Сервис предназначен для синхронизации справочников НСИ с использованием библиотеки-[стартера](../rdm-sync-spring-boot-starter/README.md).

## Информация для разработчика

### Описание свойств в файле ```application.properties```

###### Местонахождение таблиц Quartz в БД:

- `rdm.sync.liquibase.param.quartz_schema_name` -- наименование схемы, в которой находятся или будут созданы таблицы Quartz (по умолчанию -- `rdm_sync_qz`).
- `rdm.sync.liquibase.param.quartz_table_prefix` -- префикс, используемый при наименовании таблиц Quartz (по умолчанию -- `rdm_sync_qrtz_`).

## Настройки

Обязательно должны быть переопределены следующие настройки:
```
rdm.sync.liquibase.param.quartz_schema_name=
rdm.sync.liquibase.param.quartz_table_prefix=
```
