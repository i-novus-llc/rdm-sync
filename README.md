# Синхронизация справочников НСИ

## Общая информация

Данный модуль предназначен для переноса данных справочников между НСИ и БД прикладной системы.
На данный момент в качестве НСИ реализована поддержка [RDM](https://github.com/i-novus-llc/rdm) и ФНСИ.

Модуль реализован в виде в двух вариантах: 
1. В виде библиотеки стартера для spring-boot приложений. 
2. В виде отдельного микросервиса.

Синхронизация включает в себя:
- Импорт и обновление данных из НСИ справочников из НСИ;
- Экспорт данных в НСИ](#Экспорт-данных-в-НСИ) справочников в НСИ (только RDM) в синхронном/асинхронном режиме;

Также добавлена возможность синхронизации [по расписанию](#синхронизация-по-расписанию).

### Требования

- OpenJDK 14
- PostgreSQL 11 и выше
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

- rdm-sync-spring-boot-starter - стартер для синхронизации справочников НСИ.
- rdm-sync-service - микросервис для синхронизации справочников НСИ на основе стартера.


## Как использовать, 2 варианта

### 1. Подключение в виде стартера
#### Требования к модулю где будет использоваться стартер
1. Модуль должен быть реализован на java 14, spring-boot 2 и платформе n2o-platform версии 4.4.0;
2. Модуль в качестве СУБД должен использовать Postgresql 11 или выше и настроен DataSource стандартным для spring-boot приложений образом
3. Модуль должен использовать библиотеку Liquibase для автоматического обновления базы данных;
4. Модуль должен использовать фреймворк для автоматизации сборки проектов maven3.

#### Шаги подключения
1. Добавить в pom прикладного модуля зависимость.
    ```xml
    <dependency>
        <groupId>ru.i-novus.ms.rdm.sync</groupId>
        <artifactId>rdm-sync-spring-boot-starter</artifactId>
        <version>${rdm.sync.version}</version>
    </dependency>
    ```
2. Указать адрес API НСИ в properties 
    <br/>Для RDM `rdm.backend.path` - адрес API RDM'a 
    <br/>Для ФНСИ `rdm-sync.source.fnsi.values[0].url` - адрес API ФНСИ, 
   `rdm-sync.source.fnsi.values[0].userKey` - ключ API ФНСИ,
   `rdm-sync.source.fnsi.values[0].code` - код источника ФНСИ, произвольная уникальная строка
   `rdm-sync.source.fnsi.values[0].name` - наименование источника ФНСИ, понятное для пользователя наименование/описание, например "ФНСИ продуктивная среда" или "ФНСИ тестовая среда".
   Настройки фнси допускают множественность источника, это удобно когда хотят одновременно иметь в качестве источника несколько инстансов ФНСИ, прод, тест.
   
3. [Настроить маппинг](#настройка-маппинга) 

4. [Настроить расписание](#cинхронизация-по-расписанию) запуска если нужна синхронизация по расписанию

5. Запустить приложение.

### 2. Запуск отдельным микросервисом
Микросервис использует вышеописанный стартер. Выполнить следующие шаги. 
1. Собрать модуль rdm-sync-service командой
```
mvn package -Pproduction
```
2. Сконфигурировать подключение к бд любым доступным для spring-boot приложений способом
3. Выполнить пункты 2,3 и 4 из [описания стартера](#шаги-подключения)
5. Запустить `java -jar rdm-sync-service.jar`
<br/> 
Микросервис допускает конфигурирование  с помощью Config server'a стандартным для spring-boot приложений способом  

### Пример файла настроек

```properties
#  Подключение к БД
spring.datasource.url=jdbc:postgresql://localhost:5432/rdm_sync
spring.datasource.username=myuser
spring.datasource.password=mypass

# Адрес API RDM'a (если не нужна синхронизация с RDM, то указывать не надо)
rdm.backend.path=http://yandex.stable:9904/rdm/service/rdm/api

rdm-sync.source.fnsi.values[0].url=https://fnsi.rt-eu.ru/port
rdm-sync.source.fnsi.values[0].userKey=d053d531-9ac6-40fc-b345343-xxx0ba0a14
rdm-sync.source.fnsi.values[0].code=FNSI
rdm-sync.source.fnsi.values[0].name=Тестовая среда ФНСИ


rdm-sync.auto-create.refbooks[1].code=1.2.643.5.1.13.2.1.1.725
rdm-sync.auto-create.refbooks[1].source=FNSI
rdm-sync.auto-create.refbooks[1].type=NOT_VERSIONED
rdm-sync.auto-create.refbooks[0].code=EK002
rdm-sync.auto-create.refbooks[0].source=RDM
rdm-sync.auto-create.refbooks[0].type=NOT_VERSIONED
rdm-sync.auto-create.refbooks[2].code=1.2.643.5.1.13.13.99.2.115
rdm-sync.auto-create.refbooks[2].source=FNSI
rdm-sync.auto-create.refbooks[2].type=NOT_VERSIONED
```

## Полный список настроек

Настройка|Значение по умолчанию|Описание|
|---|---|---|
|rdm.backend.path| -| Адрес API RDM'a |
|rdm-sync.auto_create.schema| rdm| Схема, в которой будут создаваться таблицы в режиме автосоздания|
|rdm-sync.scheduling| true| Запуск по расписанию, true -- включено|
|rdm-sync.import.from_rdm.cron| 0 * * * * ?| Крон для загрузки данных из НСИ|
|rdm-sync.export.to_rdm.cron| 0 * * * * ?| Крон для загрузки данных в НСИ (только для RDM)|
|rdm-sync.load.size| 1000| Кол-во записей на странице при получении данных из НСИ|
|rdm-sync.threads.count| 3| Кол-во потоков в пуле на синхронизацию справочников. Один поток выделяется на один справочник|
|rdm-sync.auto-create.refbooks[<порядковый номер справочника>].code| -| Код справочника
|rdm-sync.auto-create.refbooks[<порядковый номер справочника>].source| -| Источник справочника
|rdm-sync.auto-create.refbooks[<порядковый номер справочника>].type| -| Тип синхронизации, сейчас только один тип NOT_VERSIONED
|rdm-sync.auto-create.refbooks[<порядковый номер справочника>].name| -| Наименование справочника
|rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].url| -| URL ФНСИ
|rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].userKey| -| Ключ АПИ ФНСИ
|rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].code| -| Код среды ФНСИ
|rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].name| -| Наименование среды ФНСИ
|rdm-sync.liquibase.param.quartz_schema_name| rdm_sync_qz| Наименование схемы, в которой находятся или будут созданы таблицы Quartz. Доступно только для микросервиса, для стартера не работает
|rdm-sync.liquibase.param.quartz_table_prefix| rdm_sync_qrtz_| префикс, используемый при наименовании таблиц Quartz. Доступно только для микросервиса, для стартера не работает

## Описание таблиц

В базе данных создаётся схема `rdm_sync` с таблицами:
- `refbook` -- список справочников, которые необходимо синхронизировать;
- `version` -- список версий справочников;
- `mapping` -- маппинг версий справочников;
- `field_mapping` -- список полей при маппинге;
- `loaded_version` -- список загруженных версий справочников;
- `log` -- журнал обновления.

## Настройка маппинга

**Маппинг** - это соответствие полей справочника в системе НСИ и колонок таблицы в БД прикладной системы.
Маппинг можно задать 3-мя способами.

#### 1.Маппинг через properties файлы
Этот способ самый простой, при котором не надо создавать таблицы для справочников и маппинг полей. Стандартным для spring-boot приложений способом задать следующие проперти для каждого справочника
<br/>`rdm-sync.auto-create.refbooks[<порядковый номер справочника>].code` - код, оид справочника
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].source` - источник, RDM или значение из  `rdm-sync.source.fnsi.values[*].code` для ФНСИ
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].name` - человекочитаемое наименование справочника, по умолчанию будет равно `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].code`
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].type` - тип синхронизации, сейчас реализованно только `NOT_VERSIONED`
<br/>
Тут <порядковый номер справочника> порядковый номер справочник начиная с 0 и далее, т.е если справочника 2 то нумерация до 1.
<br/>
Пример:
 ```properties
rdm-sync.auto-create.refbooks[0].code=EK002
rdm-sync.auto-create.refbooks[0].name=Какой-то справочник из RDM
rdm-sync.auto-create.refbooks[0].source=RDM
rdm-sync.auto-create.refbooks[0].type=NOT_VERSIONED

rdm-sync.auto-create.refbooks[1].code=1.2.643.5.1.13.2.1.1.725
rdm-sync.auto-create.refbooks[1].name=Какой-то справочник ФНСИ
rdm-sync.auto-create.refbooks[1].source=FNSI
rdm-sync.auto-create.refbooks[1].type=NOT_VERSIONED

```
#### 2. XML-конфигурация маппинга

Маппинг можно настроить через XML-конфигурацию.
В classpath (например, в папку resources) создаём файл с наименованием *rdm-mapping.xml*.
В случае изменения маппинга меняем в файле соответствующий элемент refbook и увеличиваем mapping-version на 1.
Таблицы для справочников должны быть предварительно созданы. Как создавать таблицы [тут.](#cоздание-таблиц)
<br/>
Пример:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<mapping>

    <refbook code="T001" sys-table="rdm.test_rb" unique-sys-field="code" deleted-field="deleted_ts" mapping-version="1" source="RDM" name="Какой-то справочник из RDM">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
        <field sys-field="doc_number" sys-data-type="integer" rdm-field="doc_num"/>
    </refbook>

    <refbook code="R001" sys-table="rdm.some_table" unique-sys-field="code" deleted-field="deleted_ts" mapping-version="1" source="RDM" name="Еще какой-то справочник из RDM">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
    </refbook>

</mapping>
```

**Важно:**

Маппинг должен соответствовать тому справочнику, который есть в НСИ и таблице в бд прикладной системы.
То есть в маппинге должны быть только поля, которые существуют как в НСИ, так и в локальной таблице.

### Автогенерация XML-конфигурации

Для облегчения разработки предусмотрена генерация файла `rdm-mapping.xml` по существующим записям в БД.

1. Указываете настройки `rdm-sync.auto_create.refbook_codes` и `rdm-sync.auto_create.schema`.
2. Запускаете своё приложение. Стартер скачает справочники из НСИ и создаст таблицы.
3. Получаете сгенерированный файл `rdm-mapping.xml` по адресу `<адрес вашего приложения>/api/rdm/xml-fm?code=REF_BOOK_CODE1&code=REF_BOOK_CODE2&...`
4. Полученный файл `rdm-mapping.xml` можно поправить и положить в classpath уже для использования по назначению.
   Например, можно убрать неиспользуемые поля справочника.

Вы также можете перейти по адресу `localhost:8080/api/rdm/xml-fm?code=all`.
Так вы получите файл, в котором перечислен маппинг для каждого справочника, ведущегося в вашем приложении.

### Ограничения маппинга

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
- `_sync_rec_id bigserial` -- внутренний первичный ключ таблицы, на него можно ссылаться внутри системы.
- `code` -- колонка с любым типом, совместимым с типом первичного ключа справочника НСИ.
  В эту колонку будет копироваться значение первичного ключа справочника из НСИ. Указывается в колонке `rdm_sync.version.unique_sys_field`.
- `deleted_ts timestamp without time zone` -- признак и дата удалённости записи. Указывается в колонке `rdm_sync.version.deleted_field`.

Таблица версионного справочника должна также содержать технические колонки:
- `_versions text` -- версии справочника, в которых присутствует текущая запись.
- `_hash text` -- хеш записи.
  Для этой колонки должно быть создано unique-ограничение `unique_hash`.

Таблица должна содержать колонки для значений справочника, т.е. те колонки, в которые будут копироваться данные из полей справочника.
Их количество и наименование необязательно должны совпадать. Также наименование этих колонок не должно совпадать с наименованиями технических колонок (за исключением `code`).
Эти колонки участвуют в маппинге, т.е. прописываются в `rdm_sync.field_mapping`.


## Синхронизация по расписанию
Периодическая синхронизация справочников НСИ реализована с помощью библиотеки Quartz.

### Настройка расписания при использовании стартера

#### Если в прикладном модуле уже есть Quartz
Если в прикладном модуле уже подключена и настроена библиотека Quartz, то стартер будет использовать существующую конфигурацию.

#### Если Quartz не подключен
Если Quartz не подключен, то для его настройки нужно выполнить 
1. Добавить зависимость
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
            <optional>true</optional>
        </dependency>
```
2. Задать настройки доступным для spring-boot приложений образом
```properties
rdm-sync.scheduling=true
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.scheduler.instanceName=RdmSyncScheduler
```
Включение/выключение выполнения действий по расписанию задаётся настройкой: `rdm-sync.scheduling` значения true/false (по умолчанию true - включенно).
Если настройка имеет значение true, то при подключённом Quartz-шедулере выполняется подготовка и запуск заданий для выполнения требуемых действий.

Управление отдельными действиями (импортом и экспортом) выполняется соответствующими настройками (см. ниже).

### Импорт(данных из НСИ) по расписанию(не актуализированно)

Для того чтобы гарантировать, что локальные справочники со временем будут идентичны справочникам в НСИ, желательно для них настроить обновление по таймеру.
Желательно сделать это через Quartz-шедулер в кластерном режиме (и пометить Job по обновлению справочников Quartz-аннотацией org.quartz.DisallowConcurrentExecution).
Также cron-выражения нужно выбрать аккуратно, а не так, что у вас допустим 10 справочников и для каждого одно и то же выражение. Лучше распределить импорт этих 10 справочников, например, по часовому интервалу (то есть 6 минут на каждый).
Ручная настройка выполняется так:
1) Автовайрим интерфейс ru.i_novus.ms.rdm.sync.api.service.RdmSyncService через AutowiringSpringBeanJobFactory.
   (по примеру <a href="https://stackoverflow.com/questions/6990767/inject-bean-reference-into-a-quartz-job-in-spring/15211030">отсюда</a>).
2) И в методе org.quartz.Job#execute вызываем его метод ru.i_novus.ms.rdm.sync.api.service.RdmSyncService#update(String refBookCode).
   То есть желательно либо создать по джобу на каждый справочник (со своим cron-ом), либо как-то самим координировать в джобе, чтобы они не запускались разом. Или, если у вас несколько (скажем 3) экземпляров приложения, можно сделать по 3 concurrent джоба одновременно и т.д.

### Экспорт(данных в НСИ) по расписанию(только для RDM)(не актуализированно)

В classpath должен лежать Quartz-шедулер (в кластерном режиме).
Управление экспортом выполняется с помощью настроек: `rdm-sync.export.to_rdm.cron` и `rdm-sync.export.to_rdm.batch_size`.

Библиотека создаст Job, который по крону из настройки `rdm-sync.export.to_rdm.cron` (по умолчанию -- раз в 5 секунд),
будет периодически сканировать все клиентские таблички на предмет записей в состоянии `DIRTY`.
Из этих записей он будет отбирать `rdm-sync.export.to_rdm.batch_size` записей (по умолчанию -- 100) и экспортировать их в RDM.
На каждую пачку записей, отправленную в RDM, внутри RDM будет так же происходить публикация. То есть если у вас 500 "грязных записей" и batch_size = 100, то соответствующий справочник опубликуется 5 раз.
Поэтому batch_size вместе с крон-выражением нужно выбирать аккуратно.
Ещё раз стоит отметить, что очень желательно вместе с экспортом настроить также и импорт.

### Рекомендуемые настройки Quartz (не актуализированно)

Настройка Quartz-шедулера задаётся параметрами в файле `application.properties`:
```properties
## Spring Quartz
spring.quartz.job-store-type=jdbc

spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.scheduler.instanceName=RdmSyncScheduler

# jobStore
spring.quartz.properties.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
spring.quartz.properties.org.quartz.jobStore.tablePrefix=<schema_name>.<table_prefix>
spring.quartz.properties.org.quartz.jobStore.isClustered=true
```

Здесь значение `spring.quartz.properties.org.quartz.jobStore.tablePrefix` определяется клиентским приложением.

### Собственная реализация (не актуализированно)

Можно реализовать в клиентском приложении собственный вариант синхронизации по расписанию.

Для этого нужно выполнить следующие действия:

1. Подключить к приложению стартер.
   
2. Реализовать задания по импорту и экспорту данных. Примеры реализации:
   - импорт из НСИ: класс `RdmSyncImportRecordsFromRdmJob` в стартере.
   - экспорт изменённых данных в НСИ: класс `RdmSyncExportDirtyRecordsToRdmJob` в стартере.

3. Организовать периодический вызов заданий в соответствии с собственными требованиями.
