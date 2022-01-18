package ru.i_novus.ms.rdm.sync.service.persister;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import ru.i_novus.ms.rdm.sync.service.RdmMappingServiceImpl;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotSimpleVersionedPersisterServiceTest {

    private NotVersionedPersisterService persisterService;

    @Mock
    private RdmSyncDao dao;

    @Before
    public void setUp() {
        persisterService = new NotVersionedPersisterService(dao, 100, new RdmMappingServiceImpl());
    }

    /**
     * Кейс: Обновление справочника в первый раз, версия в маппинге не указана. В таблице клиента уже есть запись с id=1, из НСИ приходят записи с id=1,2.
     * Ожидаемый результат: Запись с id=1 обновится, с id=2 вставится, в маппинге проставится дата и номер версии.
     */
    @Test
    public void testFirstTimeUpdate() {

        RefBookVersion firstVersion = createFirstRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null,null,  "test_table", "test_pk_field", "","id", "deleted_ts", null, -1, 1, SyncTypeEnum.NOT_VERSIONED);
        List<FieldMapping> fieldMappings = createFieldMappings();
        FieldMapping primaryFieldMapping = fieldMappings.stream().filter(f -> f.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElse(null);
        Page<Map<String, Object>> data = createFirstRdmData();
        List<Map<String, Object>> dataMap = createFirstVerifyDataMap();

        final String refBookCode = versionMapping.getCode();
        when(dao.getFieldMappings(refBookCode)).thenReturn(fieldMappings);
        when(dao.getDataIds(versionMapping.getTable(), primaryFieldMapping)).thenReturn(singletonList(BigInteger.valueOf(1L)));

        SyncSourceService syncSourceService = mock(SyncSourceService.class);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() == 0))).thenReturn(data);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() > 0))).thenReturn(Page.empty());

        persisterService.firstWrite(firstVersion, versionMapping, syncSourceService);

        dataMap.get(0).put(versionMapping.getDeletedField(), null);
        verify(dao).updateRows(versionMapping.getTable(), versionMapping.getPrimaryField(), singletonList(dataMap.get(0)), true);
        verify(dao).insertRows(versionMapping.getTable(), singletonList(dataMap.get(1)), true);
    }

    /**
     * Кейс: Обновление справочника c уже указанной версией в маппинге. В таблице клиента уже есть запись с id=1,2. Из НСИ приходят записи с id=2,3.
     * Ожидаемый результат: Запись с id=1 пометится как удаленная, с id=3 добавится. В маппинге проставится дата и номер новой версии.
     */
    @Test
    public void testUpdate() {

        RefBookVersion firstVersion = createFirstRdmVersion();
        RefBookVersion secondVersion = createSecondRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null, firstVersion.getVersion(),  "test_table", "test_pk_field", "","id", "deleted_ts", null, -1, 1, SyncTypeEnum.NOT_VERSIONED);
        List<FieldMapping> fieldMappings = createFieldMappings();
        List<Map<String, Object>> dataMap = createSecondVerifyDataMap();
        VersionsDiff diff = prepareUpdateRefBookDataDiff();

        when(dao.getFieldMappings(versionMapping.getCode())).thenReturn(fieldMappings);
        SyncSourceService syncSourceService = mock(SyncSourceService.class);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() == 0))).thenReturn(diff);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() > 0))).thenReturn(VersionsDiff.dataChangedInstance(Page.empty()));

        persisterService.merge(secondVersion, firstVersion.getVersion(), versionMapping, syncSourceService);
        verify(dao).markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), BigInteger.valueOf(1L), secondVersion.getFrom(), true);
        verify(dao).insertRow(versionMapping.getTable(), dataMap.get(1), true);
    }


    /**
     * Кейс: Обновление справочника c уже указанной версией в маппинге. В таблице клиента уже есть запись с id=1,2,3. 1 помечена как удаленная. Из НСИ приходят записи с id=1,2,3.
     * Ожидаемый результат: У записи id=1 должен сняться признак удаления. В маппинге проставится дата и номер новой версии.
     */
    @Test
    public void testInsert() {

        RefBookVersion oldVersion = createSecondRdmVersion();
        RefBookVersion newVersion = createThirdRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null, oldVersion.getVersion(),  "test_table", "test_pk_field","","id", "deleted_ts", null, -1, 1, SyncTypeEnum.NOT_VERSIONED);
        List<FieldMapping> fieldMappings = createFieldMappings();
        Page<RefBookRowValue> data = createThirdRdmData();
        List<Map<String, Object>> dataMap = createThirdVerifyDataMap();
        VersionsDiff diff = prepareInsertRefBookDataDiff();
        when(dao.getFieldMappings(versionMapping.getCode())).thenReturn(fieldMappings);
        SyncSourceService syncSourceService = mock(SyncSourceService.class);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() == 0))).thenReturn(diff);
        when(syncSourceService.getDiff(argThat(versionsDiffCriteria -> versionsDiffCriteria != null && versionsDiffCriteria.getPageNumber() > 0))).thenReturn(VersionsDiff.dataChangedInstance(Page.empty()));
        when(dao.isIdExists(versionMapping.getTable(), versionMapping.getPrimaryField(), BigInteger.ONE)).thenReturn(true);
        persisterService.merge(newVersion, oldVersion.getVersion(), versionMapping, syncSourceService);
        verify(dao).markDeleted(versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), BigInteger.valueOf(1L), null, true);
        verify(dao).updateRow(versionMapping.getTable(), versionMapping.getPrimaryField(), dataMap.get(2), true);
    }

    @Test
    public void testRepeatVersion() {
        String testTable = "test_table";
        FieldMapping fieldMapping = new FieldMapping("id", "bigint", "id");
        when(dao.getDataIds(testTable, fieldMapping)).thenReturn(List.of(BigInteger.valueOf(1)));
        Page<Map<String, Object>> data = createFirstRdmData();
        RefBookVersion firstRdmVersion = createFirstRdmVersion();
        List<FieldMapping> fieldMappings = createFieldMappings();
        when(dao.getFieldMappings(firstRdmVersion.getCode())).thenReturn(fieldMappings);
        VersionMapping versionMapping = new VersionMapping(null, firstRdmVersion.getCode(), null,  firstRdmVersion.getVersion(), testTable, "test_pk_field","","id", "deleted_ts", LocalDateTime.now(), 2, null, SyncTypeEnum.NOT_VERSIONED);
        SyncSourceService syncSourceService = mock(SyncSourceService.class);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() == 0 && dataCriteria.getCode().equals(firstRdmVersion.getCode())))).thenReturn(data);
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria!=null && dataCriteria.getPageNumber() > 0))).thenReturn(Page.empty());

        persisterService.repeatVersion(firstRdmVersion, versionMapping, syncSourceService);

        List<Map<String, Object>> verifyData = createFirstVerifyDataMap();
        verifyData.get(0).put(versionMapping.getDeletedField(), null);
        verify(dao).insertRows(testTable, Arrays.asList(verifyData.get(1)), true);
        verify(dao).updateRows(testTable, "id", Arrays.asList(verifyData.get(0)), true);

    }




    private RefBookVersion createFirstRdmVersion() {

        RefBookVersion refBook = new RefBookVersion();
        refBook.setVersionId(1);
        refBook.setCode("TEST");
        refBook.setVersion("1.0");
        refBook.setFrom(LocalDateTime.of(2019, Month.FEBRUARY, 26, 10, 0));
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        refBookStructure.setPrimaries(singletonList("id"));
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    private RefBookVersion createSecondRdmVersion() {

        RefBookVersion refBook = new RefBookVersion();
        refBook.setVersionId(2);
        refBook.setCode("TEST");
        refBook.setVersion("1.1");
        refBook.setFrom(LocalDateTime.of(2019, Month.FEBRUARY, 27, 10, 0));
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        refBookStructure.setPrimaries(singletonList("id"));
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    private RefBookVersion createThirdRdmVersion() {

        RefBookVersion refBook = new RefBookVersion();
        refBook.setVersionId(3);
        refBook.setCode("TEST");
        refBook.setVersion("1.2");
        refBook.setFrom(LocalDateTime.of(2019, Month.MARCH, 7, 10, 0));
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        refBookStructure.setPrimaries(singletonList("id"));
        refBook.setStructure(refBookStructure);
        return refBook;
    }

    private VersionsDiff prepareUpdateRefBookDataDiff() {
        RowDiff row1 = new RowDiff(RowDiffStatusEnum.DELETED, Map.of("id", BigInteger.ONE, "name", "London"));
        RowDiff row2 = new RowDiff( RowDiffStatusEnum.INSERTED, Map.of("id", BigInteger.valueOf(3L), "name", "Guadalupe"));
        List<RowDiff> rowValues = asList(row1, row2);
        return VersionsDiff.dataChangedInstance(new PageImpl<>(rowValues, createDataCriteria(), 2));
    }

    private VersionsDiff prepareInsertRefBookDataDiff() {
        RowDiff row = new RowDiff(RowDiffStatusEnum.INSERTED, Map.of("id", BigInteger.ONE, "name", "London"));
        return VersionsDiff.dataChangedInstance(new PageImpl<>(singletonList(row), createDataCriteria(), 1));
    }

    private DataCriteria createDataCriteria() {

        DataCriteria searchDataCriteriaCount = new DataCriteria();
        searchDataCriteriaCount.setPageSize(100);
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
