# Требования

- OpenJDK 14
- PostgreSQL 11
- Artemis или ActiveMQ

# Стек технологий

- Java 14
- JDBC
- JAX-RS
- JMS
- Spring Boot 2.1
- Spring Cloud Greenwich
- Liquibase 3.6.2
- N2O Platform 4

# Структура проекта

- [rdm-sync-spring-boot-starter](rdm-sync-spring-boot-starter/README.md) - стартер для синхронизации с RDM.
- [rdm-sync-service](rdm-sync-service/README.md) - запускаемый модуль для синхронизации с RDM на основе стартера.

# СИНХРОНИЗАЦИЯ СПРАВОЧНИКОВ

## Общая информация

Синхронизация справочников предназначена для переноса данных справочников НСИ в БД клиента синхронизации. На данный момент в качестве НСИ реализованная поддержка RDM и ФНСИ.
Синхронизация справочника реализована в виде библиотеки стартера для spring-boot приложений и в виде отдельного микросервиса.
Клиент синхронизации в своей БД создаёт таблицы соответствующей структуры, в которые будет происходить копирование данных НСИ.
Также добавлена возможность экспорта данных в НСИ(только для RDM) в синхронном/асинхронном режиме и периодически по расписанию.

##Реализация в виде библиотеки стартера для spring-boot приложений

Синхронизатор предоставляет API для запуска копирования. Клиент либо сам определяет момент запуска процесса синхронизации, либо включает автоматическое обновление на основе событий публикации справочника(только для RDM).

### Требования к клиентскому приложению

Клиентское приложение:
1) должно быть реализовано на java 14, spring-boot 2 и платформе n2o-platform версии 4.4.0;
2) должно использовать библиотеку Liquibase для автоматического обновления базы данных;
3) должно использовать фреймворк для автоматизации сборки проектов maven3.

### Подключение

1. Добавить в pom зависимость.
    ```xml
    <dependency>
        <groupId>ru.i-novus.ms.rdm</groupId>
        <artifactId>rdm-sync-spring-boot-starter</artifactId>
        <version>${rdm.sync.version}</version>
    </dependency>
    ```
2. Настроить маппинг.

3. Указать адрес API НСИ в `rdm.client.sync.url`

3. Запустить клиентское приложение.

В базе данных должна создаться схема `rdm_sync` с таблицами:
- `version` -- список справочников, которые необходимо синхронизировать с НСИ;
- `field_mapping` -- маппинг полей;
- `log` -- журнал обновления.

**Важно:**
liquibase в rdm-sync запускается ПОСЛЕ общего liquibase, сконфигурированного по умолчанию.
Поэтому, если нужно добавить скрипт, который производит изменения в схеме rdm_sync,
необходимо добавить его в каталог `/rdm-sync-db/changelog`.

После запуска у клиентского приложения будет доступно API:
1. `POST {CLIENT_SERVICE_URL}/rdm/update` -- обновление всех справочников, которые ведутся в системе клиента.
2. `POST {CLIENT_SERVICE_URL}/rdm/update?refbookCode=A001` -- обновление конкретного справочника, где A001 -- код справочника.

Для того, чтобы отключить синхронизацию полностью, надо указать `rdm_sync.enabled=false` (по умолчанию -- включено).

## Реализация в виде отдельного микросервиса
### Запуск
1. Собрать проект rdm-sync-service командой `mvn clean package -Pproduction`
2. Определить любым доступным для spring-boot приложений настройки(properties)  Пример
```properties
#  подключение к бд
spring.datasource.url=jdbc:postgresql://localhost:5432/rdm_sync
spring.datasource.username=postgres
spring.datasource.password=postgres

# Адрес RDM'a, если не нужна синхронизация с RDM, то не надо указывать
rdm.backend.path=http://yandex.stable:9904/rdm/service/rdm/api

# Адрес ФНСИ, если не нужна синхронизация с ФНСИ, то не надо указывать
rdm_sync.fnsi.url=https://fnsi-dev.rt-eu.ru/port
# Ключ ФНСИ, если не нужна синхронизация с ФНСИ, то не надо указывать
rdm_sync.fnsi.userKey=4191f0cf-b100-4d80-a392-6cee9432deea
```
3. [Настроить маппинг](#Настройка-маппинга) и создать таблицы для справочников, либо воспользоваться [автосозданием](#Создание-таблиц-клиента-в-автоматическом-режиме)

4. Запустит `java -jar rdm-sync-service.jar`

### Полный список настроек
Настройка|Значение по умолчанию|Описание|
|---|---|---|
|rdm.backend.path| -| Адрес АПИ RDM'a |
|rdm_sync.auto_create.schema| rdm| Схема в которой будут создаваться таблицы в режиме автосоздания|
|rdm_sync.scheduling| true| Запуск по расписанию, true - включено|
|rdm_sync.import.from_rdm.cron| 0 * * * * ?| Крон для загрузки данных из НСИ|
|rdm_sync.export.to_rdm.cron| 0 * * * * ?| Крон для загрузки данных в НСИ (только для RDM)|
|rdm-sync.load.size| 1000| Кол-во записей на странице при получении данных из нси|
|rdm.sync.threads.coun| 1| Кол-во потоков в пуле на синхронизацию справочников. 1 поток выделяется на один справочник|


## Настройка маппинга
Маппинг -- это соответствие полей справочника в системе НСИ и колонок таблицы в БД клиента.
Описание маппинга задаётся в таблице `rdm_sync.field_mapping` (см. комментарии к колонкам).
Описание, какой справочник копировать в какую таблицу, задаётся в таблице `rdm_sync.version`.
Маппинг можно настроить напрямую в БД и через XML-конфигурацию.

### Через БД (DEPRECATED)

Этот способ устаревший и не рекомендуется для использования, в будущем он будет удалён.

Добавляем запись на каждый справочник в rdm_sync.version. Заполняем колонки:
- code -- код справочника в НСИ,
- sys_table -- название таблицы в БД клиента,
- unique_sys_field -- заполняем значением "code",
- deleted_field -- заполняем значением "is_deleted".
  Добавляем запись на каждое поле справочника (которое нужно скопировать в БД клиента) в rdm_sync.field_mapping.
  Поле справочника, которое является первичным ключом справочника в НСИ, должно маппиться в колонку code.

Пример:
```sql
insert into rdm_sync.version(code, sys_table, unique_sys_field, deleted_field)
select 'S019', 'rdm.grade_test', 'code', 'is_deleted';

insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field)
select 'S019', 'code', 'varchar', 'id';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field)
select 'S019', 'test_text', 'varchar', 'test_text';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field)
select 'S019', 'sequence', 'integer', 'sequence';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field)
select 'S019', 'grade_request_id', 'varchar', 'grade_request_id';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field)
select 'S019', 'is_required', 'boolean', 'is_required';
```

### Через XML

Рекомендованный способ маппинга.

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
То есть в маппинге должно быть только поля, которые существуют как в НСИ, так и в локальной таблице.

## Из ограничений:

- Справочники без первичных ключей не смогут синхронизироваться.
- Строковый тип из НСИ можно маппить в "varchar", "text", "character varying", "smallint", "integer", "bigint", "serial", "bigserial", boolean(true/false), "numeric", "decimal",  "date(yyyy-MM-dd)".
- Дату из НСИ можно  маппить в "date", "varchar", "text", "character varying".
- Дробный из НСИ можно маппить в "numeric", "decimal", "varchar", "text", "character varying".
- Логический тип из НСИ можно маппить в boolean, "varchar", "text", "character varying".

## Создание таблиц для копирования данных из НСИ

Таблицы создавать в схеме rdm.
Таблица должна содержать технические колонки:
- `UUID id` -- внутренний первичный ключ таблицы, на него можно ссылаться внутри системы.
- `code` -- колонка с любым типом, совместимым с типом первичного ключа справочника НСИ.

  В эту колонку будет копироваться значение первичного ключа справочника из НСИ. Указывается в колонке `rdm_sync.version.unique_sys_field`.
- `is_deleted` -- признак удалённости записи. Указывается в колонке `rdm_sync.version.deleted_field`.

Таблица должна содержать колонки для значений справочника, т.е. те колонки, в которые будут копироваться данные из полей справочника.
Их количество и название необязательно должны совпадать.
Эти колонки участвуют в маппинге, т.е. прописываются в `rdm_sync.field_mapping`.

## Создание таблиц клиента в автоматическом режиме

Вам необходимо указать две настройки: `rdm_sync.auto_create.schema` и `rdm_sync.auto_create.refbook_codes`.
Первая указывает на то, в какой схеме будет создана таблица клиента (по умолчанию -- `rdm`). Если схемы нет, она также будет создана.
Вторая задаётся в формате `code1,code2,...,codeN`. Это коды справочников, для которых автоматом создадутся таблицы. Название таблицы будет кодом справочника, переведённым в нижний регистр, с символами `-` и `.`, заменёнными на `_`.
Вы также можете настроить создание таблиц с учётом маппинга. Таблицы создадутся уже не по структуре справочника из RDM, а по вашему маппингу.
Важно отметить, что таблицы только создаются. Она не подхватывает на лету изменения версий `rdm-mapping.xml` и изменения версий справочников в RDM.

## Сгенерировать файл `rdm-mapping.xml` по существующим записям в БД (для разработки)

Эту функциональность имеет смысл использовать с описанной выше.
То есть вы указываете настройки `rdm_sync.auto_create.refbook_codes` и `rdm_sync.auto_create.schema`. Запускаете своё приложение.
Стартер скачает справочники из RDM и сам создаст таблички. Но вам, скажем, не нужна половина полей из RDM. Вы можете получить сгенерированный файл rdm-mapping.xml по адресу `<адрес вашего приложения>/api/rdm/xml-fm?code=REF_BOOK_CODE1&code=REF_BOOK_CODE2&...`
Так вы скачаете себе сгенерированный файл `rdm-mapping.xml`, который можете поправить как вам нужно и положить в classpath уже для использования по назначению.
Вы также можете перейти по адресу `localhost:8080/api/rdm/xml-fm?code=all`. Так вы скачаете файл, в котором перечислен маппинг для каждого справочника, ведущегося в вашем приложении.


**Важно:**
Синхронизация не создаёт таблиц для копирования данных из НСИ, это должно делать само клиентское приложение.

## Автоматический импорт справочника по событию публикации:

Требуется наличие брокера сообщений для взаимодействия клиентского приложения и системы НСИ.
Это либо ActiveMQ, реализующий спецификацию JMS 1.1, либо Artemis, реализующий спецификацию JMS 2.0.

В клиентском приложении необходимо:
1. Задать значение свойств `rdm_sync.publish.listener.enable` и `spring.activemq.broker-url`.
   Первое включает (при значении true) возможность импортировать справочник по событию (по умолчанию - false, т.е. выключено).
   Второе свойство -- это адрес брокера ActiveMQ. Он должен совпадать с адресом брокера, на который уходят сообщения о публикации, т.е с аналогичной настройкой для rdm-rest.

2. Добавить зависимость maven
```xml
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>
```
или
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-artemis</artifactId>
</dependency>
```
Можно использовать либо JMS 1.1 (ActiveMQ), либо JMS 2.0 (Artemis), т.е. если указываем  `rdm_sync.publish.listener.enable` как `true` -- добавляем зависимости либо от ActiveMQ, либо от Artemis (но не обе!).
В случае с ActiveMQ справочник при событии публикации будет блокироваться (так как спецификация JMS 1.1 не поддерживает механизм SharedSubscription и сообщение о публикации примерно одновременно получат все узлы кластера).
В случае с Artemis блокировок не будет и можно параллельно с синхронизацией модифицировать таблицу со справочником (желательно ограничиться только добавлением записей, которых там точно никогда быть не могло, чтобы избежать конфликтов).

## Возможность экспортировать данные в НСИ

Если в вашем приложении есть какая-то сущность, которая отражена в НСИ, вы можете экспортировать её.
Сущности, которые вы синхронизируете с НСИ, должны реализовывать интерфейс `Serializable`. То же самое применяется для всех полей сущности рекурсивно.
Вы можете передать либо экземпляр какой-то конкретной сущности, либо `Map`. В первом случае все поля сущности (вплоть до `Object`-а) будут сложены в `Map` (нерелевантные поля будут отфильтрованы на стороне НСИ).
В НСИ в качестве первичного ключа записи используются идентификаторы, которые вам в общем случае неизвестны, поэтому все операции `Update/Delete` выполняются по первичному ключу **справочника**, который должен быть представлен в вашей сущности.

Базовый интерфейс для данной операции отражён в `ru.i_novus.ms.rdm.sync.service.change_data.RdmChangeDataClient`.
У данного интерфейса есть две реализации: синхронная и асинхронная. Синхронная блокирует вызывающего, асинхронная положит сообщение в очередь и сразу вернёт управление.
Чтобы явно указать синхронную реализацию, необходимо задать для свойства `rdm_sync.change_data.mode` значение `sync`, а чтобы указать асинхронную реализацию -- `async`.
Если свойство `rdm_sync.change_data.mode` не указано, то экспорт данных в НСИ выключен
Если вы решили использовать синхронную реализацию, необходимо убедиться, что вы сконфигурировали брокер сообщений.
Также в classpath должна быть реализация JMS спецификации (например, ActiveMQ).
Результаты и синхронной, и асинхронной операций можно перехватить через `ru.i_novus.ms.rdm.sync.service.change_data.RdmChangeDataRequestCallback` (по умолчанию лог просто пишется в консоль).

Стоит отметить, что в асинхронной реализации контейнер слушателя установлен как транзакционный.
Это значит, что spring-jms не отправит Acknowledge брокеру сообщений в случае, если метод слушателя (помеченный как @JmsListener) кинет исключение.
Поэтому необходимо выставить разумные значения для двух параметров: количество попыток и задержка между ними. Однако в спецификации JMS эти параметры не указаны и настраиваются на стороне брокера.
Например, если у вас Artemis, то в папке `${artemis-broker}/etc` лежит файл `broker.xml`.
В нём есть секция `<address-settings>`. Туда можно добавить `<address-setting>` с `match=jms.queue.${rdm_sync.change_data.queue}` (по умолчанию -- `rdmChangeData`).
Внутри прописываем `<redelivery-delay>DELAY</redelivery-delay>`. В `DELAY` подставляем разумное значение (5 секунд, например) в миллисекундах.
И туда же прописываем `<max-delivery-attempts>ATTEMPTS</max-delivery-attempts>`.
Аналогичные настройки можно найти у других брокеров.

## Возможность обновлять данные в локальной таблице (и со временем в НСИ)

Эта возможность является логическим продолжением предыдущей.
Локальные таблицы хранят в себе системное поле, указывающее в каком отношении с соответствующей записью в НСИ находится локальная запись.
Это поле может принимать 4 значения:
1) `SYNCED` -- указывает, что локальная запись была синхронизирована с НСИ и с тех пор не менялась (то есть при условии, что в НСИ не публиковали справочник, эта запись -- корректное отражение её в локальной таблице).
2) `DIRTY` -- указывает, что локальная запись была отредактирована (либо руками, либо программно) и она уже не отражает соответствующую запись в НСИ.
3) `PENDING` -- указывает, что локальная запись была поставлена в очередь на экспорт её в НСИ.
4) `ERROR` -- указывает, что экспорт в НСИ завершился с ошибкой. Это состояние также логически является расширением состояния `DIRTY`.
   Диаграмма переходов состояний, при условии, что запись не модифицируется во время пребывания её в состоянии `PENDING` (что не рекомендуется, иначе можно нарушить порядок доставки в НСИ), выглядит так:
   `SYNCED`→`DIRTY`
   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;↑&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;↙
   `PENDING`→`ERROR`
   Также надо отметить, что запись в случае использования асинхронной реализации `ru.i_novus.ms.rdm.sync.service.change_data.RdmChangeDataClient` может остаться в состоянии `PENDING` на неограниченное количество времени.
   Если вы настроили периодическую синхронизацию с НСИ, она со временем перейдёт в `SYNCED` (при условии, что запись с соответствующим первичным ключом присутствует в НСИ).
   Просто стоит помнить, что любое состояние, отличное от `SYNCED`, не отражает реальное положение вещей в НСИ.
   Чтобы узнать состояние записи, необходимо воспользоваться `ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowStateService`.

## Настройка периодического выполнения действий над справочниками

Включение/выключение периодического выполнения действий задаётся настройкой: `rdm_sync.scheduling` (по умолчанию -- null).
Если настройка имеет значение true, то при подключённом Quartz-шедулере выполняется подготовка и запуск заданий для выполнения требуемых действий.

Управление отдельными действиями (импортом и экспортом) выполняется соответствующими настройками (см. ниже).

### Настройка Quartz-шедулера

Настройка Quartz-шедулера задаётся параметрами
- в файле `application.properties`:
    ```properties
    rdm.sync.liquibase.param.quartz_schema_name=rdm_sync_qz
    rdm.sync.liquibase.param.quartz_table_prefix=rdm_sync_qrtz_
    ```
    Здесь:
    - `rdm.sync.liquibase.param.quartz_schema_name` -- наименование схемы, в которой находятся или будут созданы таблицы Quartz.
    - `rdm.sync.liquibase.param.quartz_table_prefix` -- префикс, используемый при наименовании таблиц Quartz.

- в файле `quartz.properties`:
    ```properties
    # main
    org.quartz.scheduler.instanceId=AUTO
    org.quartz.scheduler.instanceName=RdmSyncScheduler

    # jobStore
    org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
    org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
    org.quartz.jobStore.tablePrefix=rdm_sync_qz.rdm_sync_qrtz_
    org.quartz.jobStore.isClustered=true
    ```

### Настройка периодического импорта справочников

Для того чтобы гарантировать, что локальные справочники со временем будут идентичны справочникам в НСИ, желательно для них настроить обновление по таймеру.
Желательно сделать это через Quartz-шедулер в кластерном режиме (и пометить Job по обновлению справочников Quartz-аннотацией org.quartz.DisallowConcurrentExecution).
Также cron-выражения нужно выбрать аккуратно, а не так, что у вас допустим 10 справочников и для каждого одно и то же выражение. Лучше распределить импорт этих 10 справочников, например, по часовому интервалу (то есть 6 минут на каждый).
Обычная настройка всего этого будет такая:
1) Автовайрим интерфейс ru.i_novus.ms.rdm.sync.api.service.RdmSyncService через AutowiringSpringBeanJobFactory.
   (по примеру <a href="https://stackoverflow.com/questions/6990767/inject-bean-reference-into-a-quartz-job-in-spring/15211030">отсюда</a>).
2) И в методе org.quart.Job#execute вызываем его метод ru.i_novus.ms.rdm.sync.api.service.RdmSyncService#update(String refBookCode).
   То есть желательно либо создать по джобу на каждый справочник (со своим cron-ом), либо как-то самим координировать в джобе, чтобы они не запускались разом. Или, если у вас несколько (скажем 3) экземпляров приложения, можно сделать по 3 concurrent джоба одновременно и т.д.

### Настройка периодического экспорта справочников

В classpath должен лежать Quartz-шедулер (в кластерном режиме).
Управление экспортом выполняется с помощью настроек: `rdm_sync.export.to_rdm.cron` и `rdm_sync.export.to_rdm.batch_size`.

Библиотека создаст Job, который по крону, заданному настройкой `rdm_sync.export.to_rdm.cron` (по умолчанию -- раз в 5 секунд),
будет периодически сканировать все клиентские таблички на предмет записей в состоянии `DIRTY`.
Из этих записей он будет отбирать `rdm_sync.export.to_rdm.batch_size` записей (по умолчанию -- 100) и экспортировать их в RDM.
На каждую пачку записей, отправленную в RDM, внутри RDM будет так же происходить публикация. То есть если у вас 500 "грязных записей" и batch_size = 100, то соответствующий справочник опубликуется 5 раз.
Поэтому batch_size вместе с крон-выражением нужно выбирать аккуратно.
Ещё раз стоит отметить, что очень желательно вместе с экспортом настроить также и импорт.

