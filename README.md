# Синхронизация справочников НСИ

## Содержание
[Общая информация](#общая-информация)<br/>
[Как использовать](#как-использовать-2-варианта)<br/>
[Полный список настроек](#полный-список-настроек)<br/>
[Описание таблиц](#описание-таблиц)<br/>
[Настройка маппинга](#настройка-маппинга)<br/>
[Типы синхронизации](#типы-синхронизации)<br/>
[Задание диапазона версий справочника](#задание-диапазона-версий-справочника)<br/>
[Создание таблиц](#создание-таблиц)<br/>
[Синхронизация по расписанию](#синхронизация-по-расписанию)<br/>
[Подробнее об экспорте данных в НСИ](#подробнее-об-экспорте-данных-в-НСИ)<br/>
[Получение синхронизированных данных](#получение-синхронизированных-данных)<br/>


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
Микросервис допускает конфигурирование с помощью Config server'a стандартным для spring-boot приложений способом  

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

| Настройка                                                            | Значение по умолчанию | Описание                                                                                                                                |
|----------------------------------------------------------------------|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| rdm-sync.enabled                                                     | true                  | Включение/выключение синхронизации                                                                                                      |
| rdm.backend.path                                                     | -                     | Адрес API RDM'a                                                                                                                         |
| rdm-sync.auto-create.schema                                          | rdm                   | Схема, в которой будут создаваться таблицы в режиме автосоздания, если наименование таблицы в [маппинге](Настройка-маппинга) не содержит схему |
| rdm-sync.auto-create.ignore-case                                     | true                  | Игнорирование регистра букв в названиях колонок и таблиц, если в [маппинге](Настройка-маппинга) не указано наименование таблицы         |
| rdm-sync.scheduling                                                  | true                  | Запуск по расписанию, true -- включено.  Значение по умолчанию есть только у микросервиса                                               |
| rdm-sync.import.from_rdm.cron                                        | 0 0/10 * * * ?        | Крон для импорта данных из НСИ. Значение по умолчанию есть только у микросервиса                                                        |
| rdm-sync.import.from_rdm.delay                                       | 0                     | Задержка импорта данных из НСИ после запуска. Значение по умолчанию -- 0                                                                |
| rdm-sync.change_data.mode                                            | -                     | Режим экспорта данных в НСИ (только для RDM). Значения sync/async - синхронный и асинхронный                                            |
| rdm-sync.export.to_rdm.cron                                          | 0 0/20 * * * ?        | Крон для экспорта данных в НСИ (только для RDM).  Значение по умолчанию есть только у микросервиса                                      |
| rdm-sync.export.to_rdm.delay                                         | 0                     | Задержка экспорта данных в НСИ (только для RDM) после запуска. Значение по умолчанию -- 0                                               |
| rdm-sync.load.size                                                   | 1000                  | Кол-во записей на странице при получении данных из НСИ                                                                                  |
| rdm-sync.threads.count                                               | 3                     | Кол-во потоков в пуле на синхронизацию справочников. Один поток выделяется на один справочник                                           |
| rdm-sync.auto-create.refbooks[<порядковый номер справочника>].code   | -                     | Код справочника                                                                                                                         |
| rdm-sync.auto-create.refbooks[<порядковый номер справочника>].source | -                     | Источник справочника                                                                                                                    |
| rdm-sync.auto-create.refbooks[<порядковый номер справочника>].type   |                       | Тип синхронизации, подробнее [тут](Типы-синхронизации)                                                                                  |
| rdm-sync.auto-create.refbooks[<порядковый номер справочника>].table  | -                     | Наименование таблицы справочника                                                                                                        |
| rdm-sync.auto-create.refbooks[<порядковый номер справочника>].name   | -                     | Наименование справочника                                                                                                                |
| rdm-sync.auto-create.refbooks[<порядковый номер справочника>].range  | -                     | Диапазон версий справочника                                                                                                             |
| rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].url       | -                     | URL ФНСИ                                                                                                                                |
| rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].userKey   | -                     | Ключ АПИ ФНСИ                                                                                                                           |
| rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].code      | -                     | Код среды ФНСИ                                                                                                                          |
| rdm-sync.source.fnsi.values[<порядковый номер среды ФНСИ>].name      | -                     | Наименование среды ФНСИ                                                                                                                 |
| rdm-sync.liquibase.param.quartz_schema_name                          | rdm_sync_qz           | Наименование схемы, в которой находятся или будут созданы таблицы Quartz. Доступно только для микросервиса, для стартера не работает    |
| rdm-sync.liquibase.param.quartz_table_prefix                         | rdm_sync_qrtz_        | префикс, используемый при наименовании таблиц Quartz. Доступно только для микросервиса, для стартера не работает                        |
| rdm-sync.init.delay                                                  | -                     | настройка таймера инициализации rdm-sync лоадеров после старта приложения (в миллисекундах), по умолчанию настройка отключена           |
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
Для автоматического создания таблиц необходимо стандартным для spring-boot приложений способом задать следующие проперти для каждого справочника:
<br/>`rdm-sync.auto-create.refbooks[<порядковый номер справочника>].code` - код, оид справочника
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].source` - источник, RDM или значение из  `rdm-sync.source.fnsi.values[*].code` для ФНСИ
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].name` - человекочитаемое наименование справочника, по умолчанию будет равно `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].code`
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].type` - тип синхронизации, подробнее [тут](Типы-синхронизации)
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].range` - диапазон версий справочника, подробнее [тут](Задание-диапазона-версий-справочника)
<br/> `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].table` - наименование таблицы справочника, подробнее [тут](создание-таблиц)
<br/>
Тут <порядковый номер справочника> порядковый номер справочник начиная с 0 и далее, т.е если справочника 2 то нумерация до 1.
<br/>
Пример:
 ```properties
rdm-sync.auto-create.refbooks[0].code=EK002
rdm-sync.auto-create.refbooks[0].name=Какой-то справочник из RDM
rdm-sync.auto-create.refbooks[0].source=RDM
rdm-sync.auto-create.refbooks[0].type=NOT_VERSIONED
rdm-sync.auto-create.refbooks[0].sysPkColumn=test_rdm_pk
rdm-sync.auto-create.refbooks[0].table=common.test_refbook

rdm-sync.auto-create.refbooks[1].code=1.2.643.5.1.13.2.1.1.725
rdm-sync.auto-create.refbooks[1].name=Какой-то справочник ФНСИ
rdm-sync.auto-create.refbooks[1].source=FNSI
rdm-sync.auto-create.refbooks[1].type=SIMPLE_VERSIONED
rdm-sync.auto-create.refbooks[1].sysPkColumn=test_fnsi_pk
rdm-sync.auto-create.refbooks[1].range=*-3.0

rdm-sync.auto-create.refbooks[2].code=EK003
rdm-sync.auto-create.refbooks[2].name=Какой-то неверсионный справочник из RDM
rdm-sync.auto-create.refbooks[2].source=RDM
rdm-sync.auto-create.refbooks[2].type=RDM_NOT_VERSIONED
```
#### 2. XML-конфигурация маппинга

Маппинг можно настроить через XML-конфигурацию.
В classpath (например, в папку resources) создаём файл с наименованием *rdm-mapping.xml*.
В случае изменения маппинга меняем в файле соответствующий элемент refbook и увеличиваем mapping-version на 1.
Таблицы для справочников могут быть созданы автоматически или предварительно. Как создавать таблицы [тут.](#создание-таблиц).

Для автоматического создания таблиц необходимо в xml-конфигурации указать значения code, sys-table, type, source, name
<br/>`code` - код, оид справочника
<br/> `source` - источник, RDM или значение из  `rdm-sync.source.fnsi.values[*].code` для ФНСИ
<br/> `name` - человекочитаемое наименование справочника, по умолчанию будет равно значнию атрибута `code`
<br/> `type` - тип синхронизации, подробнее [тут](Типы-синхронизации)
<br/> `range` - диапазон версий справочника, подробнее [тут](Задание-диапазона-версий-справочника)
<br/> `sys-table` - наименование таблицы справочника, подробнее [тут](создание-таблиц)  
<br/>
<br/>
Пример:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<mapping>

    <refbook code="T001" sys-table="rdm.test_rb" type="NOT_VERSIONED" unique-sys-field="code" deleted-field="deleted_ts" mapping-version="1" source="RDM" name="Какой-то справочник из RDM">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
        <field sys-field="doc_number" sys-data-type="integer" rdm-field="doc_num"/>
    </refbook>

    <refbook code="R001" sys-table="rdm.some_table" type="RDM_NOT_VERSIONED" unique-sys-field="code" deleted-field="deleted_ts" mapping-version="1" source="RDM" name="Еще какой-то справочник из RDM" range="*">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
    </refbook>

    <refbook code="1.2.643.5.1.13.2.1.1.725" sys-table="rdm.some_table2" type="SIMPLE_VERSIONED" unique-sys-field="code" mapping-version="1" source="FNSI" name="Еще какой-то справочник из ФНСИ" range="*">
        <field sys-field="code" sys-data-type="integer" rdm-field="ID"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="name"/>
    </refbook>
</mapping>
```

По умолчанию маппинг задается для всех версий справочника. Когда для какой-то версии нужен специфический маппинг, его можно переопределить указав в теге ```<refbook>``` атрибут ```refbook-version``` с необходимой версией. Как в примере ниже:
```xml
<mapping>
   <refbook code="EK001" sys-table="rdm.ref_ek001" refbook-version="2.5" type="SIMPLE_VERSIONED" unique-sys-field="code" mapping-version="1" source="RDM" name="Еще какой-то справочник из RDM версия 2.5">
      <field sys-field="code" sys-data-type="integer" rdm-field="ID"/>
      <field sys-field="name" sys-data-type="varchar" rdm-field="name"/>
   </refbook>
</mapping>
```

**Важно:**

Маппинг должен соответствовать тому справочнику, который есть в НСИ и таблице в бд прикладной системы.
То есть в маппинге должны быть только поля, которые существуют как в НСИ, так и в локальной таблице.

### Автогенерация XML-конфигурации

Для облегчения разработки предусмотрена генерация файла `rdm-mapping.xml` по существующему маппингу. Например, он был сгенерирован через автосоздание с маппингом через properties файлы.

Получить сгенерированный файл `rdm-mapping.xml` можно по адресу `<адрес вашего приложения>/api/rdm/xml-fm?code=REF_BOOK_CODE1&code=REF_BOOK_CODE2&...`
Полученный файл `rdm-mapping.xml` можно поправить и положить в classpath уже для использования по назначению.
   Например, можно убрать неиспользуемые поля справочника.

Вы также можете перейти по адресу `localhost:8080/api/rdm/xml-fm?code=all`.
Так вы получите файл, в котором перечислен маппинг для каждого справочника, ведущегося в вашем приложении.

### Ограничения маппинга

- Справочники без первичных ключей не смогут синхронизироваться.
- Строковый тип из НСИ можно маппить в "varchar", "text", "character varying", "smallint", "integer", "bigint", "serial", "bigserial", boolean(true/false), "numeric", "decimal",  "date(yyyy-MM-dd)".
- Дату из НСИ можно  маппить в "date", "varchar", "text", "character varying".
- Дробный из НСИ можно маппить в "numeric", "decimal", "varchar", "text", "character varying".
- Логический тип из НСИ можно маппить в boolean, "varchar", "text", "character varying".

## Типы синхронизации
При синхронизации данных из НСИ нужно указать один из типов синхронизации
1. NOT_VERSIONED - синхронизиция данных без версии, т.е данные не привязываются к версии, а характеризуются только как актуальные и удаленные(колонка дата удаления)
2. RDM_NOT_VERSIONED - синхронизация аналогичная первому пункту только для неверсионных справочников RDM
3. SIMPLE_VERSIONED - синхронизация данных с версией, т.е. вместе с данными хранится и паспорт(версия и даты действия версии). Паспорт в отдельной таблице.
4. NOT_VERSIONED_WITH_NATURAL_PK - синхронизация аналогичная пункту 1, с использованием первичного ключа справочника в качестве первичного ключа таблицы
5. RDM_NOT_VERSIONED_WITH_NATURAL_PK - синхронизация аналогичная пункту 2, с использованием первичного ключа справочника в качестве первичного ключа таблицы

## Задание диапазона версий справочника
По умолчанию при синхронизации из НСИ скачивается самая последняя версия. Если надо задать конкретный диапазон версий, то нужно указать атрибут `range` в xml-маппинге или `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].range` если используются проперти.
#### Примеры задания диапазона
`*` - все версии <br/>
`1.0-1.33` - все версии начиная от 1.0 до 1.33 включительно <br/>
`*-1.5` - все версии до 1.5 включительно <br/>
`1.5-*` - все версии начиная с 1.5 <br/>
> :warning: При указании версии не забывайте что маппинг и структура вашей таблицы должна соответствовать структуре версии справочника

## Создание таблиц
### Автосоздание таблиц
Схема и таблицы справочников могут быть созданы автоматически по [маппингу](Настройка-маппинга) при первом запуске приложения. 
Xml-mapping имеет приоритет выше, чем маппинг через properties файлы. 

Если в маппинге не задано наименование таблицы, оно будут сгенерировано по коду справочника с использованием [настроек](полный-список-настроек): `rdm-sync.auto-create.schema`, `rdm-sync.auto-create.ignore-case`.
Наименование таблицы в маппинге может не содержать схему, в этом случает она будет создана автоматически с использованием настройки `rdm-sync.auto-create.schema`.

### Создание таблиц вручную

Таблицы можно создавать в любой схеме. Важно в [маппинге](Настройка-маппинга) указать название таблицы со схемой. Структура таблицы зависит от типа синхронизации

#### Таблицы для типов `NOT_VERSIONED` и `RDM_NOT_VERSIONED`
Таблица должна содержать колонки для атрибутов справочника(те которые указаны в маппинге) и следующие технические колонки:
- `_sync_rec_id(по умолчанию) bigserial` -- внутренний первичный ключ таблицы, на него можно ссылаться внутри системы, можно задать другое имя через `rdm-sync.auto-create.refbooks[<порядковый номер справочника>].sysPkColumn` в *.properties или через поле `sys-pk-field` в теге `<reefbook>` rdm-mapping.xml.
- колонка с любым типом, совместимым с типом первичного ключа справочника НСИ. Например `code`.
  В эту колонку будет копироваться значение первичного ключа справочника из НСИ. Указывается в колонке `rdm_sync.version.unique_sys_field`.
- `deleted_ts timestamp without time zone` -- признак и дата удалённости записи. Указывается в колонке `rdm_sync.version.deleted_field`.
- `rdm_sync_internal_local_row_state character varying NOT NULL DEFAULT 'DIRTY'::character varying`

#### Таблицы для типов `NOT_VERSIONED_WITH_NATURAL_PK` и `RDM_NOT_VERSIONED_WITH_NATURAL_PK`
Таблица должна содержать колонки для атрибутов справочника(те которые указаны в маппинге), первичным ключом таблицы должна быть колонка указанная в мапинге в атрибуте unique-sys-field и следующие технические колонки:
- `deleted_ts timestamp without time zone` -- признак и дата удалённости записи. Указывается в колонке `rdm_sync.version.deleted_field`.
-  `rdm_sync_internal_local_row_state character varying NOT NULL DEFAULT 'DIRTY'::character varying`
;


#### Таблицы для типа `SIMPLE_VERSIONED` 
Таблица должна содержать колонки для атрибутов справочника(те которые указаны в маппинге) и следующие технические колонки:
- `_sync_rec_id bigserial` -- внутренний первичный ключ таблицы, на него можно ссылаться внутри системы.
- `version_id integer` -- ссылка на версию справочника в таблице `rdm-sync.loaded_version`.


Таблица должна содержать колонки для значений справочника, т.е. те колонки, в которые будут копироваться данные из полей справочника.
Их количество и наименование необязательно должны совпадать. Также наименование этих колонок не должно совпадать с наименованиями технических колонок .
Эти колонки участвуют в маппинге, т.е. прописываются в `rdm_sync.field_mapping`.

### Возможность ссылаться на созданные автоматически таблицы при первом запуске приложения
Стандартный механизм автосоздания таблиц через loader-ы не позволит ссылаться на них в liquibase-скриптах при первом запуске приложения. Но есть возможность добавить автосоздание через liquibase-скрипт, пример 
```xml
<changeSet id="21" author="iuser">
   <customChange class="ru.i_novus.ms.rdm.sync.service.init.InitCustomTaskChange"/>
</changeSet>
```
Во всех следующих после него скриптах можно ссылаться на созданные автоматически таблицы при первом запуске приложения.

## Синхронизация по расписанию
Периодическая синхронизация справочников НСИ реализована с помощью библиотеки Quartz.

### Настройка расписания при использовании стартера

#### Если в прикладном модуле уже есть Quartz
Если в прикладном модуле уже подключена и настроена библиотека Quartz, то стартер будет использовать существующую конфигурацию.
Нужно задать время запуска
```properties
rdm-sync.scheduling=true
#изменить как надо. время обновления данных из НСИ
rdm-sync.import.from_rdm.cron=0 0/10 * * * ?
#Включает экспорт данных в НСИ
rdm-sync.change_data.mode=async
#изменить как надо. время обновления данных в НСИ, можно не указывать если не указано rdm-sync.change_data.mode
rdm-sync.export.to_rdm.cron=0 0/20 * * * ?

```

#### Если Quartz не подключен
Если Quartz не подключен, то для его настройки нужно выполнить 
1. Добавить зависимость
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>
```
2. Задать настройки доступным для spring-boot приложений образом
```properties
rdm-sync.scheduling=true
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=always
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.scheduler.instanceName=RdmSyncScheduler
spring.quartz.properties.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
spring.quartz.properties.org.quartz.jobStore.isClustered=true
#изменить как надо. время обновления данных из НСИ
rdm-sync.import.from_rdm.cron=0 0/10 * * * ?
#Включает экспорт данных в НСИ
rdm-sync.change_data.mode=async
#изменить как надо. время обновления данных в НСИ, можно не указывать если не указано rdm-sync.change_data.mode
rdm-sync.export.to_rdm.cron=0 0/20 * * * ?
```
### Настройка расписания при использовании микросервиса
#### Если микросервис использует БД где уже есть таблицы для Quartz 
Нужно указать настройки 
```properties
rdm-sync.liquibase.param.quartz_schema_name=<указать схему где лежат таблицы Quartz'a>
rdm-sync.liquibase.param.quartz_table_prefix=<указать префикс таблиц Quartz'a>
#изменить как надо. время обновления данных из НСИ
rdm-sync.import.from_rdm.cron=0 0/10 * * * ?
#Включает экспорт данных в НСИ
rdm-sync.change_data.mode=async
#изменить как надо. время обновления данных в НСИ, можно не указывать если не указано rdm-sync.change_data.mode
rdm-sync.export.to_rdm.cron=0 0/20 * * * ?
```

## Подробнее об экспорте данных в НСИ

В classpath должен лежать Quartz-шедулер (в кластерном режиме).
Управление экспортом выполняется с помощью настроек: `rdm-sync.export.to_rdm.cron` и `rdm-sync.export.to_rdm.batch_size`.

Библиотека создаст Job, который по крону из настройки `rdm-sync.export.to_rdm.cron`,
будет периодически сканировать все клиентские таблички на предмет записей в состоянии `DIRTY`.
Из этих записей он будет отбирать `rdm-sync.export.to_rdm.batch_size` записей (по умолчанию -- 100) и экспортировать их в RDM.
На каждую пачку записей, отправленную в RDM, внутри RDM будет так же происходить публикация. То есть если у вас 500 "грязных записей" и batch_size = 100, то соответствующий справочник опубликуется 5 раз.
Поэтому batch_size вместе с крон-выражением нужно выбирать аккуратно.
Ещё раз стоит отметить, что очень желательно вместе с экспортом настроить также и импорт.

## Получение синхронизированных данных
После синхронизации данных имеется возможность получить их с помощью Rest API. 
1. Постраничное получение списка данных с фильтрами:
   ```
   GET <адрес>/rdm/data/{refBookCode}
   ```
   Здесь `<адрес>` -- адрес приложения или микросервиса синхронизации.
   
   Можно указать следующие Query-параметры:
   - getDeleted - значения true/false:
     если true, то показывает только удалённые, если false -- неудалённые, если не указывать, то все записи.
   - page - номер страницы, начиная с 0 (по умолчанию -- 0).
   - size - кол-во записей на странице (по умолчанию -- 10).
   - фильтр по колонкам: ключ -- название колонки, значение -- в формате: `$маска фильтрации|значение фильтра`.

   Поиск происходит в соответствии с маской:
   - eq - точное совпадение,
   - like - поиск по вхождению подстроки,
   - like/i - поиск по вхождению подстроки с игнорированием регистра символов,
   - like/q - поиск по вхождению подстроки с игнорированием одинарных и двойных кавычек,
   - like/iq - поиск по вхождению подстроки с игнорированием регистра и кавычек,
   - null - поиск по совпадению с null,
   - not-null - поиск по несовпадению с null.

   Если выполняется точный поиск по значению, не начинающемуся с символа `$`, то маску можно не указывать.  
    
   Например, таблице есть колонка name, то можно по ней фильтровать так:
   ```
   GET <адрес>/rdm/data/{refBookCode}?getDeleted=false&page=0&size=10&name=текст
     
   GET <адрес>/rdm/data/{refBookCode}?getDeleted=false&page=0&size=10&name=$eq|текст

   GET <адрес>/rdm/data/{refBookCode}?getDeleted=false&page=0&size=10&name=$eq|текст1&name=$eq|текст2

   GET <адрес>/rdm/data/{refBookCode}?getDeleted=false&page=0&size=10&name=$like|подстрока

   GET <адрес>/rdm/data/{refBookCode}?getDeleted=false&page=0&size=10&name=$eq|текст&name=$like/iq|подстрока
   ```
   > :warning: При выполнении в браузере не забываем об url encode

3. Получение одной записи по первичному ключу справочника (т.е колонки, которая указана в rdm_sync.mapping.unique_sys_field).

   ```
   GET <адрес>/rdm/data/{refBookCode}/{primaryKey}
   ```

4. Получение одной записи по системному идентификатору.

   ```
   GET <адрес>/rdm/data/{refBookCode}/record/{recordId}
   ```
