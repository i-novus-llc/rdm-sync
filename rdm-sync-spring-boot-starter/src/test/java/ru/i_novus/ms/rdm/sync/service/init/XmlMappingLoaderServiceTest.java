package ru.i_novus.ms.rdm.sync.service.init;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Ignore
public class XmlMappingLoaderServiceTest {
//
//    @InjectMocks
////    private XmlMappingLoaderService xmlMappingLoaderService;
//
//    @Mock
//    private RdmSyncDao dao;
//
//    @Captor
//    private ArgumentCaptor<VersionMapping> versionMappingCaptor;
//
//    @Mock
//    private ClusterLockService lockService;
//
//    /**
//     * При первой загрузке маппинга справочника
//     * без указания конкретной версии справочника,
//     * добавится маппинг справочника с версией CURRENT
//     */
//    @Test
//    public void testFirstLoadXmlMappingWithoutRefBookVersion() {
//        xmlMappingLoaderService.setRdmMappingXmlPath("/rdm-mapping-EK002-CURRENT.xml");
//        VersionMapping expectedVersionMapping = generateVersionMapping();
//        expectedVersionMapping.setCode("EK002");
//        expectedVersionMapping.setRefBookVersion("CURRENT");
//        expectedVersionMapping.setMappingVersion(-1);
//
//        when(lockService.tryLock()).thenReturn(true);
//        xmlMappingLoaderService.load();
//        verify(dao).insertVersionMapping(versionMappingCaptor.capture());
//        VersionMapping actualVersionMapping = versionMappingCaptor.getValue();
//
//        assertVersionMapping(expectedVersionMapping, actualVersionMapping);
//    }
//
//    /**
//     * При повторной загрузке маппинга справочника
//     * без указания конкретной версии справочника,
//     * обновится маппинг справочника с версией CURRENT
//     */
//    @Test
//    public void testUpdateXmlMappingWithoutRefBookVersion() {
//        xmlMappingLoaderService.setRdmMappingXmlPath("/rdm-mapping-EK002-CURRENT.xml");
//
//        VersionMapping expectedFromDataBaseVersionMapping = generateVersionMapping();
//        expectedFromDataBaseVersionMapping.setCode("EK002");
//        expectedFromDataBaseVersionMapping.setRefBookVersion("CURRENT");
//        expectedFromDataBaseVersionMapping.setMappingVersion(-1);
//
//        VersionMapping expectedVersionMapping = generateVersionMapping();
//        expectedVersionMapping.setCode("EK002");
//        expectedVersionMapping.setRefBookVersion("CURRENT");
//        expectedVersionMapping.setMappingVersion(1);
//
//        when(dao.getVersionMapping("EK002", "CURRENT")).thenReturn(expectedFromDataBaseVersionMapping);
//        when(lockService.tryLock()).thenReturn(true);
//        xmlMappingLoaderService.load();
//        verify(dao).updateCurrentMapping(versionMappingCaptor.capture());
//        VersionMapping actualVersionMapping = versionMappingCaptor.getValue();
//
//        assertVersionMapping(expectedVersionMapping, actualVersionMapping);
//    }
//
//    /**
//     * При первой загрузке маппинга справочника
//     * с указанной версией справочника,
//     * добавится маппинг справочника с указанной версией
//     */
//    @Test
//    public void testFirstLoadXmlMappingWithRefBookVersion() {
//        xmlMappingLoaderService.setRdmMappingXmlPath("/rdm-mapping-EK003-1.0.xml");
//        VersionMapping expectedVersionMapping = generateVersionMapping();
//        expectedVersionMapping.setCode("EK003");
//        expectedVersionMapping.setRefBookVersion("1.0");
//        expectedVersionMapping.setMappingVersion(-1);
//
//        when(lockService.tryLock()).thenReturn(true);
//        xmlMappingLoaderService.load();
//        verify(dao).insertVersionMapping(versionMappingCaptor.capture());
//        VersionMapping actualVersionMapping = versionMappingCaptor.getValue();
//
//        assertVersionMapping(expectedVersionMapping, actualVersionMapping);
//    }
//
//    /**
//     * При повторной загрузке маппинга справочника
//     * c указанной версией справочника,
//     * обновится маппинг справочника с указанной версией справочника
//     */
//    @Test
//    public void testUpdateXmlMappingWithRefBookVersion() {
//        xmlMappingLoaderService.setRdmMappingXmlPath("/rdm-mapping-EK003-1.0.xml");
//        VersionMapping expectedFromDataBaseVersionMapping = generateVersionMapping();
//        expectedFromDataBaseVersionMapping.setCode("EK003");
//        expectedFromDataBaseVersionMapping.setRefBookVersion("1.0");
//        expectedFromDataBaseVersionMapping.setMappingVersion(-1);
//
//        VersionMapping expectedVersionMapping = generateVersionMapping();
//        expectedVersionMapping.setCode("EK003");
//        expectedVersionMapping.setRefBookVersion("1.0");
//        expectedVersionMapping.setMappingVersion(1);
//
//        when(dao.getVersionMapping("EK003", "1.0")).thenReturn(expectedFromDataBaseVersionMapping);
//        when(lockService.tryLock()).thenReturn(true);
//        xmlMappingLoaderService.load();
//        verify(dao).updateCurrentMapping(versionMappingCaptor.capture());
//        VersionMapping actualVersionMapping = versionMappingCaptor.getValue();
//
//        assertVersionMapping(expectedVersionMapping, actualVersionMapping);
//    }
//
//    private void assertVersionMapping(VersionMapping expectedVersionMapping, VersionMapping actualVersionMapping) {
//        assertEquals(expectedVersionMapping.getCode(), actualVersionMapping.getCode());
//        assertEquals(expectedVersionMapping.getRefBookVersion(), actualVersionMapping.getRefBookVersion());
//    }
//
//    private VersionMapping generateVersionMapping() {
//        return new VersionMapping(
//                1, null, "RDM", null, "rdm.ref_ek003", "id",
//                "RDM", "_sync_rec_id", "deleted_ts", null, 1,
//                1, SyncTypeEnum.NOT_VERSIONED, "");
//    }

}