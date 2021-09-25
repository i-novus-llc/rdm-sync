package ru.i_novus.ms.rdm.sync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.service.RdmLoggingService;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.ms.rdm.sync.service.RdmSyncServiceImpl;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author lgalimova
 * @since 26.02.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class RdmSyncServiceTest {

    private static final int MAX_SIZE = 100;

    @InjectMocks
    private RdmSyncServiceImpl rdmSyncService;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private RdmMappingService mappingService;

    @Mock
    private SyncSourceService syncSourceService;

    @Mock
    private RdmLoggingService rdmLoggingService;


    @Before
    public void setUp() {
        reset(syncSourceService);
        rdmSyncService.setSelf(rdmSyncService);
        rdmSyncService.setSyncSourceServices(singleton(syncSourceService));
    }

    /**
     * Кейс: Обновление справочника в первый раз, версия в маппинге не указана. В таблице клиента уже есть запись с id=1, из НСИ приходят записи с id=1,2.
     * Ожидаемый результат: Запись с id=1 обновится, с id=2 вставится, в маппинге проставится дата и номер версии.
     */
    @Test
    public void testFirstTimeUpdate() {

        RefBook firstVersion = createFirstRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null, null, "test_table", "id", "is_deleted", null, null);
        List<FieldMapping> fieldMappings = createFieldMappings();
        FieldMapping primaryFieldMapping = fieldMappings.stream().filter(f -> f.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElse(null);
        Page<Map<String, Object>> data = createFirstRdmData();
        List<Map<String, Object>> dataMap = createFirstVerifyDataMap();

        final String refBookCode = versionMapping.getCode();
        when(dao.getVersionMapping(refBookCode)).thenReturn(versionMapping);
        when(dao.getFieldMappings(refBookCode)).thenReturn(fieldMappings);
        when(dao.getDataIds(versionMapping.getTable(), primaryFieldMapping)).thenReturn(singletonList(BigInteger.valueOf(1L)));

        when(syncSourceService.getRefBook(eq(refBookCode))).thenReturn(firstVersion);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() == 0))).thenReturn(data);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() > 0))).thenReturn(Page.empty());
        when(mappingService.map(AttributeTypeEnum.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(0).get("id"))).thenReturn(BigInteger.valueOf(1L));
        when(mappingService.map(AttributeTypeEnum.STRING, DataTypeEnum.VARCHAR, data.getContent().get(0).get("name"))).thenReturn("London");
        when(mappingService.map(AttributeTypeEnum.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(1).get("id"))).thenReturn(BigInteger.valueOf(2L));
        when(mappingService.map(AttributeTypeEnum.STRING, DataTypeEnum.VARCHAR, data.getContent().get(1).get("name"))).thenReturn("Moscow");

        rdmSyncService.update(refBookCode);

        verify(dao).updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), dataMap.get(0), true);
        verify(dao).insertRow(versionMapping.getTable(), dataMap.get(1), true);
        verify(dao).updateVersionMapping(versionMapping.getId(), firstVersion.getLastVersion(), firstVersion.getLastPublishDate());
    }

    /**
     * Кейс: Обновление справочника c уже указанной версией в маппинге. В таблице клиента уже есть запись с id=1,2. Из НСИ приходят записи с id=2,3.
     * Ожидаемый результат: Запись с id=1 пометится как удаленная, с id=3 добавится. В маппинге проставится дата и номер новой версии.
     */
    @Test
    public void testUpdate() {

        RefBook firstVersion = createFirstRdmVersion();
        RefBook secondVersion = createSecondRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", firstVersion.getLastVersion(), firstVersion.getLastPublishDate(), "test_table", "id", "is_deleted", null, null);
        List<FieldMapping> fieldMappings = createFieldMappings();
        Page<RefBookRowValue> data = createSecondRdmData();
        List<Map<String, Object>> dataMap = createSecondVerifyDataMap();
        VersionsDiff diff = prepareUpdateRefBookDataDiff();

        when(dao.getVersionMapping(versionMapping.getCode())).thenReturn(versionMapping);
        when(dao.getFieldMappings(versionMapping.getCode())).thenReturn(fieldMappings);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() == 0))).thenReturn(diff);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() > 0))).thenReturn(VersionsDiff.dataChangedInstance(Page.empty()));
        when(syncSourceService.getRefBook(anyString())).thenReturn(secondVersion);
        when(mappingService.map(AttributeTypeEnum.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(0).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(1L));
        when(mappingService.map(AttributeTypeEnum.STRING, DataTypeEnum.VARCHAR, data.getContent().get(0).getFieldValues().get(1).getValue())).thenReturn("London");
        when(mappingService.map(AttributeTypeEnum.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(2).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(3L));
        when(mappingService.map(AttributeTypeEnum.STRING, DataTypeEnum.VARCHAR, data.getContent().get(2).getFieldValues().get(1).getValue())).thenReturn("Guadalupe");

        rdmSyncService.update(versionMapping.getCode());
        verify(dao).markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), BigInteger.valueOf(1L), true, true);
        verify(dao).insertRow(versionMapping.getTable(), dataMap.get(1), true);
        verify(dao).updateVersionMapping(versionMapping.getId(), secondVersion.getLastVersion(), secondVersion.getLastPublishDate());
    }

    /**
     * Кейс: Обновление справочника c уже указанной версией в маппинге. В таблице клиента уже есть запись с id=1,2,3. 1 помечена как удаленная. Из НСИ приходят записи с id=1,2,3.
     * Ожидаемый результат: У записи id=1 должен сняться признак удаления. В маппинге проставится дата и номер новой версии.
     */
    @Test
    public void testInsert() {

        RefBook oldVersion = createSecondRdmVersion();
        RefBook newVersion = createThirdRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", oldVersion.getLastVersion(), oldVersion.getLastPublishDate(), "test_table", "id", "is_deleted", null, null);
        List<FieldMapping> fieldMappings = createFieldMappings();
        Page<RefBookRowValue> data = createThirdRdmData();
        List<Map<String, Object>> dataMap = createThirdVerifyDataMap();
        VersionsDiff diff = prepareInsertRefBookDataDiff();
        when(dao.getVersionMapping(versionMapping.getCode())).thenReturn(versionMapping);
        when(dao.getFieldMappings(versionMapping.getCode())).thenReturn(fieldMappings);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() == 0))).thenReturn(diff);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() > 0))).thenReturn(VersionsDiff.dataChangedInstance(Page.empty()));
        when(syncSourceService.getRefBook(anyString())).thenReturn(newVersion);
        when(mappingService.map(AttributeTypeEnum.INTEGER, DataTypeEnum.INTEGER, data.getContent().get(0).getFieldValues().get(0).getValue())).thenReturn(BigInteger.valueOf(1L));
        when(mappingService.map(AttributeTypeEnum.STRING, DataTypeEnum.VARCHAR, data.getContent().get(0).getFieldValues().get(1).getValue())).thenReturn("London");
        when(dao.isIdExists(versionMapping.getTable(), versionMapping.getPrimaryField(), BigInteger.ONE)).thenReturn(true);
        rdmSyncService.update(versionMapping.getCode());
        verify(dao).markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), BigInteger.valueOf(1L), false, true);
        verify(dao).updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), dataMap.get(2), true);
        verify(dao).updateVersionMapping(versionMapping.getId(), newVersion.getLastVersion(), newVersion.getLastPublishDate());
    }

    @Test
    public void testSyncAfterMappingChanged() {

        LocalDate date = LocalDate.of(1997, 6, 24);

        when(mappingService.map(any(), any(), any())).thenAnswer(invocation -> invocation.getArguments()[2]);

        LocalTime version1Publication = LocalTime.of(14, 46); // В рдм публикуется версия справочника со структурой S1
        LocalTime sync1 = LocalTime.of(15, 0); // В 15:00 мы синхронизируемся со справочником по маппингу M1, соответствующему S1
        LocalTime version2Publication = LocalTime.of(15, 6); // В 15:06 в рдм выходит новая версия со структурой S2. Мы еще не успели накатить наши скрипты по изменению маппинга.
        LocalTime sync2 = LocalTime.of(15, 15); // Синхронизируемся со второй версией со структурой S2 по старому маппингу M1
        LocalTime mappingChanged = LocalTime.of(15, 23); // Обновили маппинги, теперь они соответствуют S2.

        String code = "KOD";
        String table = "table";
        String primaryField = "id";
        String deletedField = "deletedField";
        RefBook lastPublished = new RefBook();
        lastPublished.setLastVersionId(1);
        lastPublished.setLastVersion("1.0");
        lastPublished.setLastPublishDate(version1Publication.atDate(date));
        lastPublished.setCode(code);
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of(primaryField, AttributeTypeEnum.INTEGER));
        refBookStructure.setPrimaries(List.of(primaryField));
        lastPublished.setStructure(refBookStructure);
        VersionMapping vm = new VersionMapping(1, code, null, null, table, primaryField, deletedField, LocalDateTime.MIN, LocalDateTime.MIN);

        List<FieldMapping> fm = new ArrayList<>(singletonList(new FieldMapping(primaryField, "varchar", primaryField)));
        Map<String, Object> row1version1 = new HashMap<>(Map.of(primaryField, "1"));
        Map<String, Object> row2version1 = new HashMap<>(Map.of(primaryField, "2"));
        PageImpl<Map<String, Object>> lastPublishedVersionPage = new PageImpl<>(List.of(row1version1, row2version1), createDataCriteria(), 2);
        when(dao.getVersionMapping(code)).thenReturn(vm);
        when(dao.getFieldMappings(code)).thenReturn(fm);
        when(syncSourceService.getRefBook(eq(code))).thenReturn(lastPublished);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() == 0 && dataCriteria.getCode().equals(lastPublished.getCode())))).thenReturn(lastPublishedVersionPage);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() > 0))).thenReturn(Page.empty());
        rdmSyncService.update(code);
        verify(dao, times(1)).insertRow(eq(table), eq(row1version1), eq(true));
        verify(dao, times(1)).insertRow(eq(table), eq(row2version1), eq(true));
        clearInvocations(dao);
//      sync1 прошел успешно, выходит новая версия с новой структурой, однако у нас старые маппинги
        lastPublished.setLastVersionId(2);
        vm.setLastSync(sync1.atDate(date));
        vm.setVersion(lastPublished.getLastVersion());
        vm.setPublicationDate(version1Publication.atDate(date));
        when(dao.getDataIds(table, fm.get(0))).thenReturn(List.of( // Добавленные данные
            "1", "2"
        ));
        String addedField = "addedField";
        Map<String, AttributeTypeEnum> newAttributes = new LinkedHashMap<>(lastPublished.getStructure().getAttributesAndTypes());
        newAttributes.put(addedField, AttributeTypeEnum.STRING);
        lastPublished.setStructure(new RefBookStructure(lastPublished.getStructure().getReferences(), lastPublished.getStructure().getPrimaries(), newAttributes));
        lastPublished.setLastPublishDate(version2Publication.atDate(date));
        String addedVal1 = "ABRA";
        String addedVal2 = "CADABRA";
        row1version1.put(addedField, addedVal1);
        row2version1.put(addedField, addedVal2);
        lastPublishedVersionPage = new PageImpl<>(List.of(row1version1, row2version1), createDataCriteria(), 2);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() == 0 && dataCriteria.getCode().equals(lastPublished.getCode())))).thenReturn(lastPublishedVersionPage);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() > 0 && dataCriteria.getCode().equals(lastPublished.getCode())))).thenReturn(Page.empty());
        rdmSyncService.update(code);

        verify(dao, never()).insertRow(eq(table), anyMap(), eq(true));
        verify(dao, never()).updateRow(eq(table), eq(primaryField), eq(row1version1), eq(true));
        verify(dao, never()).updateRow(eq(table), eq(primaryField), eq(row2version1), eq(true));
        clearInvocations(dao);
//      sync2 прошел успешно, однако мы пропустили добавленное поле, хотя разница по структуре и по данным была ненулевой
        vm.setLastSync(sync2.atDate(date));
        vm.setVersion(lastPublished.getLastVersion());
        vm.setPublicationDate(version2Publication.atDate(date));
        fm.add(new FieldMapping(addedField, "varchar", addedField)); // обновили маппинги
        vm.setMappingLastUpdated(mappingChanged.atDate(date));
        rdmSyncService.update(code);
        verify(dao, times(1)).updateRow(eq(table), eq(primaryField), eq(row1version1), eq(true));
        verify(dao, times(1)).updateRow(eq(table), eq(primaryField), eq(row2version1), eq(true));
    }

    private RefBook createFirstRdmVersion() {

        RefBook refBook = new RefBook();
        refBook.setLastVersionId(1);
        refBook.setCode("TEST");
        refBook.setLastVersion("1.0");
        refBook.setLastPublishDate(LocalDateTime.of(2019, Month.FEBRUARY, 26, 10, 0));
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        refBookStructure.setPrimaries(singletonList("id"));
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    private RefBook createSecondRdmVersion() {

        RefBook refBook = new RefBook();
        refBook.setLastVersionId(2);
        refBook.setCode("TEST");
        refBook.setLastVersion("1.1");
        refBook.setLastPublishDate(LocalDateTime.of(2019, Month.FEBRUARY, 27, 10, 0));
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        refBookStructure.setPrimaries(singletonList("id"));
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    private RefBook createThirdRdmVersion() {

        RefBook refBook = new RefBook();
        refBook.setLastVersionId(3);
        refBook.setCode("TEST");
        refBook.setLastVersion("1.2");
        refBook.setLastPublishDate(LocalDateTime.of(2019, Month.MARCH, 7, 10, 0));
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        refBookStructure.setPrimaries(singletonList("id"));
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    private VersionsDiff prepareUpdateRefBookDataDiff() {
       RowDiff row1 = new RowDiff(RowDiffStatus.DELETED, Map.of("id", BigInteger.ONE, "name", "London"));
        RowDiff row2 = new RowDiff( RowDiffStatus.INSERTED, Map.of("id", BigInteger.valueOf(3L), "name", "Guadalupe"));
        List<RowDiff> rowValues = asList(row1, row2);
        return VersionsDiff.dataChangedInstance(new PageImpl<>(rowValues, createDataCriteria(), 2));
    }

    private VersionsDiff prepareInsertRefBookDataDiff() {
        RowDiff row = new RowDiff(RowDiffStatus.INSERTED, Map.of("id", BigInteger.ONE, "name", "London"));
        return VersionsDiff.dataChangedInstance(new PageImpl<>(singletonList(row), createDataCriteria(), 1));
    }

    private DataCriteria createDataCriteria() {

        DataCriteria searchDataCriteriaCount = new DataCriteria();
        searchDataCriteriaCount.setPageSize(MAX_SIZE);
        return searchDataCriteriaCount;
    }

    private List<FieldMapping> createFieldMappings() {
        List<FieldMapping> list = new ArrayList<>();
        list.add(new FieldMapping("id", "bigint", "id"));
        list.add(new FieldMapping("full_name", "varchar", "name"));
        return list;
    }

    private Page<Map<String, Object>> createFirstRdmData() {

        List<Map<String, Object>> list = new ArrayList<>();
        list.add(Map.of("id", 1, "name", "London"));
        list.add(Map.of("id", 2, "name", "Moscow"));
        return new PageImpl<>(list, createDataCriteria(), 2);
    }

    private Page<RefBookRowValue> createSecondRdmData() {

        List<RefBookRowValue> list = new ArrayList<>();
        list.add(new RefBookRowValue(1L, asList(new IntegerFieldValue("id", 1), new StringFieldValue("name", "London")), null));
        list.add(new RefBookRowValue(2L, asList(new IntegerFieldValue("id", 2), new StringFieldValue("name", "Moscow")), null));
        list.add(new RefBookRowValue(3L, asList(new IntegerFieldValue("id", 3), new StringFieldValue("name", "Guadalupe")), null));
        return new PageImpl<>(list, createDataCriteria(), 3);
    }

    private Page<RefBookRowValue> createThirdRdmData() {

        List<RefBookRowValue> list = new ArrayList<>();
        list.add(new RefBookRowValue(1L, asList(new IntegerFieldValue("id", 1), new StringFieldValue("name", "London")), null));
        list.add(new RefBookRowValue(2L, asList(new IntegerFieldValue("id", 2), new StringFieldValue("name", "Moscow")), null));
        list.add(new RefBookRowValue(3L, asList(new IntegerFieldValue("id", 3), new StringFieldValue("name", "Guadalupe")), null));
        return new PageImpl<>(list, createDataCriteria(), 3);
    }

    private List<Map<String, Object>> createFirstVerifyDataMap() {

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", BigInteger.valueOf(1L));
        row1.put("full_name", "London");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", BigInteger.valueOf(2L));
        row2.put("full_name", "Moscow");
        return asList(row1, row2);
    }

    private List<Map<String, Object>> createSecondVerifyDataMap() {

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", BigInteger.valueOf(2L));
        row1.put("full_name", "Moscow");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", BigInteger.valueOf(3L));
        row2.put("full_name", "Guadalupe");
        return asList(row1, row2);
    }

    private List<Map<String, Object>> createThirdVerifyDataMap() {

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", BigInteger.valueOf(2L));
        row1.put("full_name", "Moscow");
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", BigInteger.valueOf(3L));
        row2.put("full_name", "Guadalupe");
        Map<String, Object> row3 = new HashMap<>();
        row3.put("id", BigInteger.valueOf(1L));
        row3.put("full_name", "London");
        return asList(row1, row2, row3);
    }
}
