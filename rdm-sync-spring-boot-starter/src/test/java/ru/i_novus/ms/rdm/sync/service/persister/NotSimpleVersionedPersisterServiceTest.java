package ru.i_novus.ms.rdm.sync.service.persister;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResultType;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotSimpleVersionedPersisterServiceTest {

    private NotVersionedPersisterService persisterService;

    @Mock
    private RdmSyncDao dao;

    @BeforeEach
    public void setUp() {
        persisterService = new NotVersionedPersisterService(dao);
    }

    /**
     * Кейс: Обновление справочника в первый раз, версия в маппинге не указана. В таблице клиента уже есть запись с id=1, из НСИ приходят записи с id=1,2.
     * Ожидаемый результат: Запись с id=1 обновится, с id=2 вставится, в маппинге проставится дата и номер версии.
     */
    @Test
    void testFirstTimeUpdate() {

        RefBookVersion firstVersion = createFirstRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null,null,  "test_table", "test_pk_field", "","id", "deleted_ts", null, -1, 1, SyncTypeEnum.NOT_VERSIONED, null, true, false);
        List<FieldMapping> fieldMappings = createFieldMappings();

        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(fieldMappings);

        persisterService.firstWrite(firstVersion, versionMapping, new DownloadResult("test_tbl", DownloadResultType.VERSION));

        verify(dao).migrateNotVersionedTempData("test_tbl", versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), List.of("id", "full_name"), firstVersion.getFrom());
    }


    /**
     * Кейс: Обновление следующей версией справочника, т.е уже есть загруженная версия и появилась новая
     */
    @Test
    void testUpdate() {

        RefBookVersion firstVersion = createFirstRdmVersion();
        RefBookVersion secondVersion = createSecondRdmVersion();
        VersionMapping versionMapping = new VersionMapping(1, "TEST", null, firstVersion.getVersion(),  "test_table", "test_pk_field", "","id", "deleted_ts", null, -1, 1, SyncTypeEnum.NOT_VERSIONED, null, true, false);
        List<FieldMapping> fieldMappings = createFieldMappings();

        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(fieldMappings);

        persisterService.merge(secondVersion, firstVersion.getVersion(), versionMapping, new DownloadResult("test_tbl", DownloadResultType.DIFF));
        verify(dao).migrateDiffTempData("test_tbl", versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), List.of("id", "full_name"), secondVersion.getFrom());
    }

    @Test
    void testRepeatVersion() {
        String testTable = "test_table";
        RefBookVersion firstRdmVersion = createFirstRdmVersion();
        List<FieldMapping> fieldMappings = createFieldMappings();
        VersionMapping versionMapping = new VersionMapping(1, firstRdmVersion.getCode(), null,  firstRdmVersion.getVersion(), testTable, "test_pk_field","","id", "deleted_ts", LocalDateTime.now(), 2, null, SyncTypeEnum.NOT_VERSIONED, null, true, false);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(fieldMappings);

        persisterService.repeatVersion(firstRdmVersion, versionMapping, new DownloadResult("temp_tbl", DownloadResultType.VERSION));

        verify(dao).migrateNotVersionedTempData("temp_tbl", versionMapping.getTable(), versionMapping.getPrimaryField(), versionMapping.getDeletedField(), List.of("id", "full_name"), firstRdmVersion.getFrom());
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

    private List<FieldMapping> createFieldMappings() {
        List<FieldMapping> list = new ArrayList<>();
        list.add(new FieldMapping("id", "bigint", "id"));
        list.add(new FieldMapping("full_name", "varchar", "name"));
        return list;
    }
}
