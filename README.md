# Синхронизация справочников НСИ

## Общая информация

Синхронизация справочников предназначена для переноса данных справочников между НСИ и БД клиента синхронизации.
На данный момент в качестве НСИ реализована поддержка RDM и ФНСИ.

Синхронизация справочника реализована в виде библиотеки стартера для spring-boot приложений и в виде отдельного микросервиса.
Клиент синхронизации в своей БД создаёт таблицы соответствующей структуры, в которые будет выполняться копирование данных НСИ.

Синхронизация включает в себя:
- [импорт данных](#Импорт-данных-из-НСИ) справочников из НСИ;
- [экспорт данных](#Экспорт-данных-в-НСИ) справочников в НСИ (только RDM) в синхронном/асинхронном режиме;
- [обновление данных](#Обновление-данных-из-НСИ) справочников из НСИ.

Также добавлена возможность синхронизации [по расписанию](#Синхронизация-по-расписанию).

## Информация для разработчика

### Требования

- OpenJDK 14
- PostgreSQL 11
- Artemis или ActiveMQ

### Стек технологий

- Java 14
- JDBC
- JAX-RS
- JMS
- Spring Boot 2.1
- Spring Cloud Greenwich
- Liquibase 3.6.2
- N2O Platform 4

### Структура проекта

- [rdm-sync-spring-boot-starter](rdm-sync-spring-boot-starter/README.md) - стартер для синхронизации справочников НСИ.
- [rdm-sync-service](rdm-sync-service/README.md) - микросервис для синхронизации справочников НСИ на основе стартера.

### Настройка синхронизации

```properties
#  Подключение к БД
spring.datasource.url=jdbc:postgresql://localhost:5432/rdm_sync
spring.datasource.username=postgres
spring.datasource.password=postgres

# Адрес API RDM'a (если не нужна синхронизация с RDM, то указывать не надо)
rdm.backend.path=http://yandex.stable:9904/rdm/service/rdm/api

# Адрес API ФНСИ (если не нужна синхронизация с ФНСИ, то указывать не надо)
rdm_sync.fnsi.url=https://fnsi-dev.rt-eu.ru/port
# Ключ для API ФНСИ (если не нужна синхронизация с ФНСИ, то указывать не надо)
rdm_sync.fnsi.userKey=4191f0cf-b100-4d80-a392-6cee9432deea
```

#### Полный список настроек

Настройка|Значение по умолчанию|Описание|
|---|---|---|
|rdm.backend.path| -| Адрес API RDM'a |
|rdm_sync.auto_create.schema| rdm| Схема, в которой будут создаваться таблицы в режиме автосоздания|
|rdm_sync.scheduling| true| Запуск по расписанию, true -- включено|
|rdm_sync.import.from_rdm.cron| 0 * * * * ?| Крон для загрузки данных из НСИ|
|rdm_sync.export.to_rdm.cron| 0 * * * * ?| Крон для загрузки данных в НСИ (только для RDM)|
|rdm-sync.load.size| 1000| Кол-во записей на странице при получении данных из НСИ|
|rdm.sync.threads.coun| 1| Кол-во потоков в пуле на синхронизацию справочников. Один поток выделяется на один справочник|

### Описание таблиц

В базе данных создаётся схема `rdm_sync` с таблицами:
- `refbook` -- список справочников, которые необходимо синхронизировать;
- `version` -- список версий справочников;
- `mapping` -- маппинг версий справочников;
- `field_mapping` -- список полей при маппинге;
- `loaded_version` -- список загруженных версий справочников;
- `log` -- журнал обновления.

## Настройка маппинга

**Маппинг** -- это соответствие полей справочника в системе НСИ и колонок таблицы в БД клиента.
Описание маппинга задаётся в таблице `rdm_sync.field_mapping` (см. комментарии к колонкам).
Описание, какой справочник копировать в какую таблицу, задаётся в таблице `rdm_sync.version`.

### XML-конфигурация маппинга

Маппинг можно настроить через XML-конфигурацию.
В classpath (например, в папку resources) создаём файл с наименованием *rdm-mapping.xml*.
В случае изменения маппинга меняем в файле соответствующий элемент refbook и увеличиваем mapping-version на 1.

Пример:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<mapping>

    <refbook code="T001" sys-table="rdm.test_rb" unique-sys-field="code" deleted-field="is_deleted" mapping-version="1">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
        <field sys-field="doc_number" sys-data-type="integer" rdm-field="doc_num"/>
    </refbook>

    <refbook code="R001" sys-table="rdm.some_table" unique-sys-field="code" deleted-field="is_deleted" mapping-version="1">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
    </refbook>

</mapping>
```

**Важно:**

Маппинг должен соответствовать тому справочнику, который есть в НСИ и в клиентском приложении.
То есть в маппинге должны быть только поля, которые существуют как в НСИ, так и в локальной таблице.

### Автогенерация XML-конфигурации

Для облегчения разработки предусмотрена генерация файла `rdm-mapping.xml` по существующим записям в БД.

1. Указываете настройки `rdm_sync.auto_create.refbook_codes` и `rdm_sync.auto_create.schema`.
2. Запускаете своё приложение. Стартер скачает справочники из НСИ и создаст таблицы.
3. Получаете сгенерированный файл `rdm-mapping.xml` по адресу `<адрес вашего приложения>/api/rdm/xml-fm?code=REF_BOOK_CODE1&code=REF_BOOK_CODE2&...`
4. Полученный файл `rdm-mapping.xml` можно поправить и положить в classpath уже для использования по назначению.
   Например, можно убрать неиспользуемые поля справочника.

Вы также можете перейти по адресу `localhost:8080/api/rdm/xml-fm?code=all`.
Так вы получите файл, в котором перечислен маппинг для каждого справочника, ведущегося в вашем приложении.

## Ограничения маппинга

- Справочники без первичных ключей не смогут синхронизироваться.
- Строковый тип из НСИ можно маппить в "varchar", "text", "character varying", "smallint", "integer", "bigint", "serial", "bigserial", boolean(true/false), "numeric", "decimal",  "date(yyyy-MM-dd)".
- Дату из НСИ можно  маппить в "date", "varchar", "text", "character varying".
- Дробный из НСИ можно маппить в "numeric", "decimal", "varchar", "text", "character varying".
- Логический тип из НСИ можно маппить в boolean, "varchar", "text", "character varying".

## Создание таблиц

**Важно:**
Синхронизация не создаёт таблиц для копирования данных из НСИ, это должно делать само клиентское приложение.

### Создание таблиц вручную

Таблицы создавать в схеме rdm.
Таблица должна содержать технические колонки:
- `UUID id` -- внутренний первичный ключ таблицы, на него можно ссылаться внутри системы.
- `code` -- колонка с любым типом, совместимым с типом первичного ключа справочника НСИ.

  В эту колонку будет копироваться значение первичного ключа справочника из НСИ. Указывается в колонке `rdm_sync.version.unique_sys_field`.
- `is_deleted` -- признак удалённости записи. Указывается в колонке `rdm_sync.version.deleted_field`.

Таблица должна содержать колонки для значений справочника, т.е. те колонки, в которые будут копироваться данные из полей справочника.
Их количество и название необязательно должны совпадать.
Эти колонки участвуют в маппинге, т.е. прописываются в `rdm_sync.field_mapping`.

### Создание таблиц в автоматическом режиме

Вам необходимо указать две настройки: `rdm_sync.auto_create.schema` и `rdm_sync.auto_create.refbook_codes`.
Первая указывает на то, в какой схеме будет создана таблица клиента (по умолчанию -- `rdm`). Если схемы нет, она также будет создана.
Вторая задаётся в формате `code1,code2,...,codeN`. Это коды справочников, для которых автоматом создадутся таблицы. Название таблицы будет кодом справочника, переведённым в нижний регистр, с символами `-` и `.`, заменёнными на `_`.
Вы также можете настроить создание таблиц с учётом маппинга. Таблицы создадутся уже не по структуре справочника из RDM, а по вашему маппингу.
Важно отметить, что таблицы только создаются. Она не подхватывает на лету изменения версий `rdm-mapping.xml` и изменения версий справочников в RDM.

## Синхронизация справочников НСИ

Синхронизация справочников НСИ реализована в [стартере](rdm-sync-spring-boot-starter/README.md#Синхронизация-справочников).

## Синхронизация по расписанию

Периодическая синхронизация справочников НСИ реализована в [стартере](rdm-sync-spring-boot-starter/README.md#Синхронизация-по-расписанию) с помощью библиотеки Quartz.
При этом Quartz используется в самом стартере опционально.
Это позволяет реализовать в клиентском приложении собственный вариант синхронизации по расписанию.

В микросервисе библиотека Quartz уже [подключена](rdm-sync-service/README.md#Настройка-Quartz).
