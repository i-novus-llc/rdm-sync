package ru.i_novus.ms.rdm.sync.service.persister;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.LoadedVersion;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.*;
import ru.i_novus.ms.rdm.sync.api.service.SyncSourceService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.model.RefBookPassport;
import ru.i_novus.ms.rdm.sync.service.RdmMappingServiceImpl;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleVersionedPersisterServiceTest {

    private SimpleVersionedPersisterService persisterService;

    @Mock
    private RdmSyncDao dao;

    @Mock
    private SyncSourceService syncSourceService;

    @Before
    public void setUp() {
        persisterService = new SimpleVersionedPersisterService(dao, 1000, new RdmMappingServiceImpl(), 1, 1000);
    }

    @Test
    public void testFirstWrite() {
        RefBookVersion refBook = generateRefBookVersion();
        VersionMapping versionMapping = generateVersionMapping(refBook);
        List<Map<String, Object>> data = generateData();
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 0))).thenReturn(new PageImpl<>(data));
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() != 0))).thenReturn(Page.empty());
        when(dao.getFieldMappings(eq(versionMapping.getId()))).thenReturn(generateFieldMappings());
        LoadedVersion loadedVersion = new LoadedVersion(1, refBook.getCode(), refBook.getVersion(), refBook.getFrom(), null, LocalDateTime.now(), true);
        when(dao.getLoadedVersion(refBook.getCode(), refBook.getVersion())).thenReturn(loadedVersion);

        persisterService.firstWrite(refBook, versionMapping, syncSourceService);
        verify(dao, times(1)).insertSimpleVersionedRows(versionMapping.getTable(), data, loadedVersion.getId());
    }

    @Test
    public void testMerge() {
        RefBookVersion newVersion = generateRefBookVersion();
        newVersion.setVersion("2");
        RefBookVersion oldVersion = generateRefBookVersion();
        oldVersion.setVersion("1");
        oldVersion.setTo(newVersion.getFrom());
        VersionMapping versionMapping = generateVersionMapping(oldVersion);
        List<Map<String, Object>> data = generateData();
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 0 && "2".equals(dataCriteria.getVersion())))).thenReturn(new PageImpl<>(data));
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() != 0))).thenReturn(Page.empty());
        when(dao.getFieldMappings(eq(versionMapping.getId()))).thenReturn(generateFieldMappings());
        LoadedVersion loadedVersion = new LoadedVersion(1, newVersion.getCode(), newVersion.getVersion(), newVersion.getFrom(), null, LocalDateTime.now(), true);
        when(dao.getLoadedVersion(newVersion.getCode(), newVersion.getVersion())).thenReturn(loadedVersion);

        persisterService.merge(newVersion, oldVersion.getVersion(), versionMapping, syncSourceService);

        verify(dao, times(1)).insertSimpleVersionedRows(versionMapping.getTable(), data, loadedVersion.getId());
    }

    @Test
    public void testRepeatVersion() {
        RefBookVersion refBookVersion = generateRefBookVersion();
        refBookVersion.setTo(LocalDateTime.now());
        VersionMapping versionMapping = generateVersionMapping(refBookVersion);
        List<Map<String, Object>> data = generateData();
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() == 0 && refBookVersion.getVersion().equals(dataCriteria.getVersion())))).thenReturn(new PageImpl<>(data));
        when(syncSourceService.getData(argThat(dataCriteria -> dataCriteria != null && dataCriteria.getPageNumber() != 0))).thenReturn(Page.empty());
        when(dao.getFieldMappings(versionMapping.getId())).thenReturn(generateFieldMappings());
        LoadedVersion loadedVersion = new LoadedVersion(1, refBookVersion.getCode(), refBookVersion.getVersion(), refBookVersion.getFrom(), null, LocalDateTime.now(), true);
        when(dao.getLoadedVersion(refBookVersion.getCode(), refBookVersion.getVersion())).thenReturn(loadedVersion);

        persisterService.repeatVersion(refBookVersion, versionMapping, syncSourceService);

        verify(dao, times(1)).upsertVersionedRows(versionMapping.getTable(), data, loadedVersion.getId(), "id");
    }

    private List<FieldMapping> generateFieldMappings() {
        return Arrays.asList(new FieldMapping("id", "integer", "id"), new FieldMapping("name", "varchar", "name"));
    }


    private List<Map<String, Object>> generateData() {
        return Arrays.asList(
                Map.of("id", BigInteger.valueOf(1), "name", "name1"),
                Map.of("id", BigInteger.valueOf(2), "name", "name2")

        );
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
                refBookVersion.getVersion(),
                schemaTable,
                "sys_pk",
                null,
                "id",
                "deleted_ts",
                null,
                -1,
                null,
                SyncTypeEnum.SIMPLE_VERSIONED,
                null);
    }
}
