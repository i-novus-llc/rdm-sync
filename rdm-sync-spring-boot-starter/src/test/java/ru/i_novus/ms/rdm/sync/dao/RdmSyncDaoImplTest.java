package ru.i_novus.ms.rdm.sync.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.mapping.VersionMapping;
import ru.i_novus.ms.rdm.sync.service.RdmMappingService;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"rawtypes","unchecked"})
public class RdmSyncDaoImplTest {

    private static final String REFBOOK_CODE = "test";
    private static final String REFBOOK_VERSION = "1.2";
    private static final LocalDateTime PUBLICATION_DATE = LocalDateTime.of(2021, 1, 1, 1, 1);

    private static final int LOCAL_ID = 10;
    private static final String LOCAL_SCHEMA = "schm";
    private static final String LOCAL_TABLE = "ltab";
    private static final String SCHEMA_TABLE = LOCAL_SCHEMA + "." + LOCAL_TABLE;
    private static final String CODE_FIELD = "code";
    private static final String TEXT_FIELD = "text";

    @InjectMocks
    private RdmSyncDaoImpl dao;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private RdmMappingService rdmMappingService;

    @Before
    public void setUp() {

        //when(namedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(jdbcTemplate);
    }

    @Test
    public void testGetVersionMappings() {

        VersionMapping versionMapping = newVersionMapping();

        List<VersionMapping> versionMappings = singletonList(versionMapping);
        when(namedParameterJdbcTemplate.query(any(String.class), any(RowMapper.class)))
                .thenReturn(versionMappings);

        List<VersionMapping> list = dao.getVersionMappings();
        assertSame(versionMappings, list);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(namedParameterJdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertNotNull(sql);
        assertFalse(sql.contains(":code"));

        verifyNoMore();
    }

    @Test
    public void testGetVersionMapping() {

        VersionMapping versionMapping = newVersionMapping();
        when(namedParameterJdbcTemplate.query(any(String.class), any(Map.class), any(RowMapper.class)))
                .thenReturn(singletonList(versionMapping));

        VersionMapping item = dao.getVersionMapping(REFBOOK_CODE);
        assertSame(versionMapping, item);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(namedParameterJdbcTemplate).query(sqlCaptor.capture(), mapCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertNotNull(sql);
        assertTrue(sql.contains(":code"));

        Map<String, Object> map = (Map<String, Object>) mapCaptor.getValue();
        assertNotNull(map);
        assertEquals(REFBOOK_CODE, map.get("code"));

        verifyNoMore();
    }

    @Test
    public void testGetLastVersion() {

        when(namedParameterJdbcTemplate.query(any(String.class), any(Map.class), any(RowMapper.class)))
                .thenReturn(singletonList(LOCAL_ID));

        int result = dao.getLastVersion(REFBOOK_CODE);
        assertEquals(LOCAL_ID, result);

        verify(namedParameterJdbcTemplate).query(any(String.class), any(Map.class), any(RowMapper.class));

        verifyNoMore();
    }

    @Test
    public void testGetFieldMappings() {

        List<FieldMapping> fieldMappings = newFieldMappings();
        when(namedParameterJdbcTemplate.query(any(String.class), any(Map.class), any(RowMapper.class)))
                .thenReturn(fieldMappings);

        List<FieldMapping> list = dao.getFieldMappings(REFBOOK_CODE);
        assertSame(fieldMappings, list);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(namedParameterJdbcTemplate).query(sqlCaptor.capture(), mapCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertNotNull(sql);
        assertTrue(sql.contains(":code"));

        Map<String, Object> map = (Map<String, Object>) mapCaptor.getValue();
        assertNotNull(map);
        assertEquals(REFBOOK_CODE, map.get("code"));

        verifyNoMore();
    }

    @Test
    public void testGetLocalColumnTypes() {

        List<FieldMapping> fieldMappings = newFieldMappings();
        List<Pair<String, String>> columnTypes = fieldMappings.stream()
                .map(mapping -> Pair.of(mapping.getSysField(), mapping.getSysDataType()))
                .collect(toList());

        when(namedParameterJdbcTemplate.query(any(String.class), any(Map.class), any(RowMapper.class)))
                .thenReturn(columnTypes);

        List<Pair<String, String>> result = dao.getLocalColumnTypes(SCHEMA_TABLE);
        assertSame(columnTypes, result);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(namedParameterJdbcTemplate).query(sqlCaptor.capture(), mapCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertNotNull(sql);

        Map<String, Object> map = (Map<String, Object>) mapCaptor.getValue();
        assertNotNull(map);

        map.forEach((k, v) -> assertTrue(sql.contains(":" + k)));

        assertEquals(LOCAL_SCHEMA, map.get("schemaName"));
        assertEquals(LOCAL_TABLE, map.get("tableName"));

        verifyNoMore();
    }

    @Test
    public void testGetDataIds() {

        List<FieldMapping> fieldMappings = newFieldMappings();
        FieldMapping primaryFieldMapping = fieldMappings.stream()
                .filter(mapping -> CODE_FIELD.equals(mapping.getSysField()))
                .findFirst().orElse(null);
        assertNotNull(primaryFieldMapping);

        List<Object> dataIds = List.of(1, 2, 3);
        when(namedParameterJdbcTemplate.query(any(String.class), any(RowMapper.class)))
                .thenReturn(dataIds);

        List<Object> result = dao.getDataIds(SCHEMA_TABLE, primaryFieldMapping);
        assertSame(dataIds, result);

        verify(namedParameterJdbcTemplate).query(any(String.class), any(RowMapper.class));

        verifyNoMore();
    }

    private VersionMapping newVersionMapping() {

        return new VersionMapping(LOCAL_ID, REFBOOK_CODE, REFBOOK_VERSION, PUBLICATION_DATE,
                SCHEMA_TABLE, "primary", "deleted", LocalDateTime.now(), LocalDateTime.now());
    }

    private List<FieldMapping> newFieldMappings() {

        FieldMapping idMapping = new FieldMapping(CODE_FIELD, "integer", "id");
        FieldMapping nameMapping = new FieldMapping(TEXT_FIELD, "varchar", "name");

        return List.of(idMapping, nameMapping);
    }

    private void verifyNoMore() {

        verifyNoMoreInteractions(jdbcTemplate, namedParameterJdbcTemplate, rdmMappingService);
    }
}