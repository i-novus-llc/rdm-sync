package ru.i_novus.ms.rdm.sync.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.sync.api.exception.RdmSyncException;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.api.model.SyncRefBook;
import ru.i_novus.ms.rdm.sync.api.model.SyncTypeEnum;
import ru.i_novus.ms.rdm.sync.api.service.VersionMappingService;
import ru.i_novus.ms.rdm.sync.dao.RdmSyncDao;
import ru.i_novus.ms.rdm.sync.dao.VersionedDataDao;
import ru.i_novus.ms.rdm.sync.dao.criteria.LocalDataCriteria;
import ru.i_novus.ms.rdm.sync.dao.criteria.VersionedLocalDataCriteria;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocalRdmDataServiceImplV2.
 * Following test writing best practices:
 * - Test observable behavior, not implementation details
 * - Focus on business logic and edge cases
 * - Use mocks sparingly and strategically
 */
@ExtendWith(MockitoExtension.class)
class LocalRdmDataServiceImplV2Test {

    @Mock
    private RdmSyncDao dao;

    @Mock
    private VersionedDataDao versionedDataDao;

    @Mock
    private VersionMappingService versionMappingService;

    @InjectMocks
    private LocalRdmDataServiceImplV2 service;

    // Test data constants
    private static final String TEST_REF_BOOK_CODE = "TEST_REF_BOOK";
    private static final String TEST_VERSION = "1.0";
    private static final String TEST_TABLE = "test_table";
    private static final String TEST_PRIMARY_FIELD = "id";
    private static final String TEST_DELETED_FIELD = "deleted_ts";
    private static final String TEST_SYS_PK_COLUMN = "sys_recordId";

    /**
     * Test getData() with valid parameters.
     * Verifies that the service returns data from DAO correctly.
     */
    @Test
    void getData_withValidParameters_shouldReturnData() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        Page<Map<String, Object>> expectedPage = createMockPage();

        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, null))
                .thenReturn(versionMapping);
        when(dao.getData(any(LocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Map<String, Object>> result = service.getData(
                TEST_REF_BOOK_CODE, false, 1, 20, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedPage, result);
    }

    /**
     * Test getData() with null page and size parameters.
     * Verifies that default values (page=0, size=10) are applied.
     */
    @Test
    void getData_withNullPageAndSize_shouldApplyDefaults() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        Page<Map<String, Object>> expectedPage = createMockPage();
        ArgumentCaptor<LocalDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LocalDataCriteria.class);

        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, null))
                .thenReturn(versionMapping);
        when(dao.getData(any(LocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        service.getData(TEST_REF_BOOK_CODE, false, null, null, null, null);

        // Assert: Verify that defaults were applied in the criteria
        verify(dao).getData(criteriaCaptor.capture());
        LocalDataCriteria criteria = criteriaCaptor.getValue();

        assertEquals(10, criteria.getLimit());
        assertEquals(0, criteria.getOffset()); // page=0, so offset should be 0
    }

    /**
     * Test getData() with getDeleted parameter set to true.
     * Verifies that deleted records criteria is properly configured.
     */
    @Test
    void getData_withGetDeletedTrue_shouldIncludeDeletedRecords() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        Page<Map<String, Object>> expectedPage = createMockPage();
        ArgumentCaptor<LocalDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LocalDataCriteria.class);

        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, null))
                .thenReturn(versionMapping);
        when(dao.getData(any(LocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        service.getData(TEST_REF_BOOK_CODE, true, 0, 10, null, null);

        // Assert: Verify deleted criteria is properly set
        verify(dao).getData(criteriaCaptor.capture());
        LocalDataCriteria criteria = criteriaCaptor.getValue();

        assertNotNull(criteria.getDeleted());
        assertTrue(criteria.getDeleted().isDeleted());
    }

    /**
     * Test getData() with RSQL filter.
     * Verifies that filter is converted and passed to criteria.
     */
    @Test
    void getData_withRsqlFilter_shouldConvertAndApplyFilter() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        Page<Map<String, Object>> expectedPage = createMockPage();
        String rsqlFilter = "name==test";
        ArgumentCaptor<LocalDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LocalDataCriteria.class);

        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, null))
                .thenReturn(versionMapping);
        when(dao.getData(any(LocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        service.getData(TEST_REF_BOOK_CODE, false, 0, 10, rsqlFilter, null);

        // Assert: Verify filter was applied to criteria
        verify(dao).getData(criteriaCaptor.capture());
        LocalDataCriteria criteria = criteriaCaptor.getValue();

        assertNotNull(criteria.getFilterSql());
    }

    /**
     * Test getData() when refBook is not found.
     * Verifies that RdmSyncException is thrown with appropriate message.
     */
    @Test
    void getData_whenRefBookNotFound_shouldThrowException() {
        // Arrange
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, null))
                .thenReturn(null);

        // Act & Assert
        RdmSyncException exception = assertThrows(RdmSyncException.class, () ->
                service.getData(TEST_REF_BOOK_CODE, false, 0, 10, null, null)
        );

        assertTrue(exception.getMessage().contains("RefBook with code '" + TEST_REF_BOOK_CODE + "' is not maintained in system."));
    }

    /**
     * Test getData() with custom page and size.
     * Verifies that pagination parameters are correctly calculated.
     */
    @Test
    void getData_withCustomPagination_shouldCalculateOffsetCorrectly() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        Page<Map<String, Object>> expectedPage = createMockPage();
        ArgumentCaptor<LocalDataCriteria> criteriaCaptor = ArgumentCaptor.forClass(LocalDataCriteria.class);

        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, null))
                .thenReturn(versionMapping);
        when(dao.getData(any(LocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act: page=2, size=20 should result in offset=40
        service.getData(TEST_REF_BOOK_CODE, false, 2, 20, null, null);

        // Assert
        verify(dao).getData(criteriaCaptor.capture());
        LocalDataCriteria criteria = criteriaCaptor.getValue();

        assertEquals(20, criteria.getLimit());
        assertEquals(40, criteria.getOffset()); // page * size = 2 * 20 = 40
    }

    /**
     * Test getVersionedData() with VERSIONED type.
     * Verifies that versionedDataDao is used for VERSIONED refBooks.
     */
    @Test
    void getVersionedData_withVersionedType_shouldUseVersionedDataDao() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        SyncRefBook syncRefBook = createSyncRefBook(SyncTypeEnum.VERSIONED);
        Page<Map<String, Object>> expectedPage = createMockPage();

        when(dao.getSyncRefBook(TEST_REF_BOOK_CODE))
                .thenReturn(syncRefBook);
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, TEST_VERSION))
                .thenReturn(versionMapping);
        when(versionedDataDao.getData(any(VersionedLocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Map<String, Object>> result = service.getVersionedData(
                TEST_REF_BOOK_CODE, TEST_VERSION, 0, 10, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(versionedDataDao).getData(any(VersionedLocalDataCriteria.class));
        verify(dao, never()).getSimpleVersionedData(any());
    }

    /**
     * Test getVersionedData() with NOT_VERSIONED type.
     * Verifies that dao.getSimpleVersionedData is used for non-versioned refBooks.
     */
    @Test
    void getVersionedData_withNotVersionedType_shouldUseSimpleVersionedData() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        SyncRefBook syncRefBook = createSyncRefBook(SyncTypeEnum.NOT_VERSIONED);
        Page<Map<String, Object>> expectedPage = createMockPage();

        when(dao.getSyncRefBook(TEST_REF_BOOK_CODE))
                .thenReturn(syncRefBook);
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, TEST_VERSION))
                .thenReturn(versionMapping);
        when(dao.getSimpleVersionedData(any(VersionedLocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Map<String, Object>> result = service.getVersionedData(
                TEST_REF_BOOK_CODE, TEST_VERSION, 0, 10, null, null
        );

        // Assert
        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(dao).getSimpleVersionedData(any(VersionedLocalDataCriteria.class));
        verify(versionedDataDao, never()).getData(any());
    }

    /**
     * Test getVersionedData() with null page and size.
     * Verifies that default values are applied.
     */
    @Test
    void getVersionedData_withNullPageAndSize_shouldApplyDefaults() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        SyncRefBook syncRefBook = createSyncRefBook(SyncTypeEnum.VERSIONED);
        Page<Map<String, Object>> expectedPage = createMockPage();
        ArgumentCaptor<VersionedLocalDataCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(VersionedLocalDataCriteria.class);

        when(dao.getSyncRefBook(TEST_REF_BOOK_CODE))
                .thenReturn(syncRefBook);
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, TEST_VERSION))
                .thenReturn(versionMapping);
        when(versionedDataDao.getData(any(VersionedLocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        service.getVersionedData(TEST_REF_BOOK_CODE, TEST_VERSION, null, null, null, null);

        // Assert
        verify(versionedDataDao).getData(criteriaCaptor.capture());
        VersionedLocalDataCriteria criteria = criteriaCaptor.getValue();

        assertEquals(10, criteria.getLimit());
        assertEquals(0, criteria.getOffset());
    }

    /**
     * Test getVersionedData() with RSQL filter.
     * Verifies that filter is converted and applied.
     */
    @Test
    void getVersionedData_withRsqlFilter_shouldConvertAndApplyFilter() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        SyncRefBook syncRefBook = createSyncRefBook(SyncTypeEnum.VERSIONED);
        Page<Map<String, Object>> expectedPage = createMockPage();
        String rsqlFilter = "code==ABC";
        ArgumentCaptor<VersionedLocalDataCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(VersionedLocalDataCriteria.class);

        when(dao.getSyncRefBook(TEST_REF_BOOK_CODE))
                .thenReturn(syncRefBook);
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, TEST_VERSION))
                .thenReturn(versionMapping);
        when(versionedDataDao.getData(any(VersionedLocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act
        service.getVersionedData(TEST_REF_BOOK_CODE, TEST_VERSION, 0, 10, rsqlFilter, null);

        // Assert
        verify(versionedDataDao).getData(criteriaCaptor.capture());
        VersionedLocalDataCriteria criteria = criteriaCaptor.getValue();

        assertNotNull(criteria.getFilterSql());
    }

    /**
     * Test getVersionedData() when refBook is not found.
     * Verifies that RdmSyncException is thrown.
     */
    @Test
    void getVersionedData_whenRefBookNotFound_shouldThrowException() {
        // Arrange
        SyncRefBook syncRefBook = createSyncRefBook(SyncTypeEnum.VERSIONED);

        when(dao.getSyncRefBook(TEST_REF_BOOK_CODE))
                .thenReturn(syncRefBook);
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, TEST_VERSION))
                .thenReturn(null);

        // Act & Assert
        RdmSyncException exception = assertThrows(RdmSyncException.class, () ->
                service.getVersionedData(TEST_REF_BOOK_CODE, TEST_VERSION, 0, 10, null, null)
        );

        assertTrue(exception.getMessage().contains("RefBook with code '" + TEST_REF_BOOK_CODE + "' is not maintained in system."));
    }

    /**
     * Test getVersionedData() with custom pagination.
     * Verifies correct offset calculation.
     */
    @Test
    void getVersionedData_withCustomPagination_shouldCalculateOffsetCorrectly() {
        // Arrange
        VersionMapping versionMapping = createVersionMapping();
        SyncRefBook syncRefBook = createSyncRefBook(SyncTypeEnum.VERSIONED);
        Page<Map<String, Object>> expectedPage = createMockPage();
        ArgumentCaptor<VersionedLocalDataCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(VersionedLocalDataCriteria.class);

        when(dao.getSyncRefBook(TEST_REF_BOOK_CODE))
                .thenReturn(syncRefBook);
        when(versionMappingService.getVersionMapping(TEST_REF_BOOK_CODE, TEST_VERSION))
                .thenReturn(versionMapping);
        when(versionedDataDao.getData(any(VersionedLocalDataCriteria.class)))
                .thenReturn(expectedPage);

        // Act: page=3, size=15 should result in offset=45
        service.getVersionedData(TEST_REF_BOOK_CODE, TEST_VERSION, 3, 15, null, null);

        // Assert
        verify(versionedDataDao).getData(criteriaCaptor.capture());
        VersionedLocalDataCriteria criteria = criteriaCaptor.getValue();

        assertEquals(15, criteria.getLimit());
        assertEquals(45, criteria.getOffset()); // page * size = 3 * 15 = 45
    }

    // Helper methods to create test data

    private VersionMapping createVersionMapping() {
        return VersionMapping.builder()
                .table(TEST_TABLE)
                .primaryField(TEST_PRIMARY_FIELD)
                .deletedField(TEST_DELETED_FIELD)
                .sysPkColumn(TEST_SYS_PK_COLUMN)
                .code(TEST_REF_BOOK_CODE)
                .refBookName("Test RefBook")
                .source("TEST")
                .mappingVersion(1)
                .type(SyncTypeEnum.NOT_VERSIONED)
                .matchCase(true)
                .refreshableRange(false)
                .build();
    }

    private SyncRefBook createSyncRefBook(SyncTypeEnum type) {
        return new SyncRefBook(1, TEST_REF_BOOK_CODE, type, "Test RefBook", Collections.emptySet());
    }

    private Page<Map<String, Object>> createMockPage() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "Test");
        return new PageImpl<>(Collections.singletonList(record));
    }
}
