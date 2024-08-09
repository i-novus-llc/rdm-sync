package ru.i_novus.ms.rdm.sync.service.persister;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.Range;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResult;
import ru.i_novus.ms.rdm.sync.service.downloader.DownloadResultType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleVersionedPersisterServiceTest {

    private SimpleVersionedPersisterService persisterService;

    @Mock
    private RdmSyncDao dao;

    @BeforeEach
    public void setUp() {
        persisterService = new SimpleVersionedPersisterService(dao);
    }

    @Test
    void testFirstWrite() {
        RefBookVersion refBook = generateRefBookVersion();
        VersionMapping versionMapping = generateVersionMapping(refBook);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(generateFieldMappings());
        LoadedVersion loadedVersion = new LoadedVersion(1, refBook.getCode(), refBook.getVersion(), refBook.getFrom(), null, LocalDateTime.now(), true);
        when(dao.getLoadedVersion(refBook.getCode(), refBook.getVersion())).thenReturn(loadedVersion);

        persisterService.firstWrite(refBook, versionMapping, new DownloadResult("temp_table", DownloadResultType.VERSION));
        verify(dao, times(1)).migrateVersionedTempData("temp_table", versionMapping.getTable(), versionMapping.getPrimaryField(), loadedVersion.getId(), List.of("id", "name"));
    }

    @Test
    void testMerge() {
        RefBookVersion newVersion = generateRefBookVersion();
        newVersion.setVersion("2");
        RefBookVersion oldVersion = generateRefBookVersion();
        oldVersion.setVersion("1");
        oldVersion.setTo(newVersion.getFrom());
        VersionMapping versionMapping = generateVersionMapping(oldVersion);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(generateFieldMappings());
        LoadedVersion loadedVersion = new LoadedVersion(1, newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), null, LocalDateTime.now(), true);
        when(dao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion())).thenReturn(loadedVersion);

        persisterService.merge(newVersion, oldVersion.getVersion(), versionMapping, new DownloadResult("temp_tbl", DownloadResultType.VERSION));

        verify(dao, times(1)).migrateVersionedTempData("temp_tbl", versionMapping.getTable(), versionMapping.getPrimaryField(), loadedVersion.getId(), List.of("id", "name"));
    }

    @Test
    void testRepeatVersion() {
        RefBookVersion refBookVersion = generateRefBookVersion();
        refBookVersion.setTo(LocalDateTime.now());
        VersionMapping versionMapping = generateVersionMapping(refBookVersion);
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(generateFieldMappings());
        LoadedVersion loadedVersion = new LoadedVersion(1, refBookVersion.getCode(), refBookVersion.getVersion(), refBookVersion.getFrom(), null, LocalDateTime.now(), true);
        when(dao.getLoadedVersion(refBookVersion.getCode(), refBookVersion.getVersion())).thenReturn(loadedVersion);

        persisterService.repeatVersion(refBookVersion, versionMapping, new DownloadResult("temp_tbl", DownloadResultType.VERSION));

        verify(dao, times(1)).reMigrateVersionedTempData("temp_tbl", versionMapping.getTable(),  versionMapping.getPrimaryField(),loadedVersion.getId(), List.of("id", "name"));
    }

    private List<FieldMapping> generateFieldMappings() {
        return Arrays.asList(new FieldMapping("id", "integer", "id"), new FieldMapping("name", "varchar", "name"));
    }

    private RefBookVersion generateRefBookVersion() {
        String versionNumber = "1";
        String refCode = "test";
        LocalDateTime publishDate = LocalDateTime.of(2021, 1, 14, 9, 21);
        RefBookStructure structure = new RefBookStructure(null, Collections.singletonList("id"), Map.of("id", AttributeTypeEnum.INTEGER, "name", AttributeTypeEnum.STRING));
        return new RefBookVersion(refCode, versionNumber, publishDate, null, 1, structure);
    }

    private VersionMapping generateVersionMapping(RefBookVersion refBookVersion) {
        String schemaTable = "rdm.test";
        return new VersionMapping(
                1,
                refBookVersion.getCode(),
                "test",
                schemaTable,
                "sys_pk",
                null,
                "id",
                "deleted_ts",
                null,
                -1,
                null,
                SyncTypeEnum.SIMPLE_VERSIONED,
                new Range(refBookVersion.getVersion()),
                true,
                false);
    }
}
