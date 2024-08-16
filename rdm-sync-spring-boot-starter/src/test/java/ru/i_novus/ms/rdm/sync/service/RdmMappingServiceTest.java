package ru.i_novus.ms.rdm.sync.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.mapping.FieldMapping;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author lgalimova
 * @since 22.02.2019
 */
@ExtendWith(MockitoExtension.class)
class RdmMappingServiceTest {

    @InjectMocks
    private RdmMappingServiceImpl rdmMappingService;

    @Test
    void testInteger() {
        FieldMapping fm = new FieldMapping("name", "integer", "name");
        Object result = rdmMappingService.map(AttributeTypeEnum.INTEGER,fm, 1);
        assertEquals(BigInteger.ONE, result);

        fm.setSysDataType("varchar");
        result = rdmMappingService.map(AttributeTypeEnum.INTEGER, fm,1);
        assertEquals("1", result);

        fm.setSysDataType("decimal");
        result = rdmMappingService.map(AttributeTypeEnum.INTEGER, fm, 1);
        assertEquals(Float.parseFloat("1"), result);

        try {
            fm.setSysDataType("boolean");
            rdmMappingService.map(AttributeTypeEnum.INTEGER, fm, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to BOOLEAN. Value: 1", e.getMessage());
        }

        try {
            fm.setSysDataType("date");
            rdmMappingService.map(AttributeTypeEnum.INTEGER, fm, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to DATE. Value: 1", e.getMessage());
        }

        try {
            fm.setSysDataType("jsonb");
            rdmMappingService.map(AttributeTypeEnum.INTEGER, fm, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to JSONB. Value: 1", e.getMessage());
        }
    }

    @Test
    void testString() {
        FieldMapping fm = new FieldMapping("name", "varchar", "name");

        Object result = rdmMappingService.map(AttributeTypeEnum.STRING, fm, "10");
        assertEquals("10", result);

        fm.setSysDataType("integer");
        result = rdmMappingService.map(AttributeTypeEnum.STRING, fm, "10");
        assertEquals(BigInteger.TEN, result);

        fm.setSysDataType("decimal");
        result = rdmMappingService.map(AttributeTypeEnum.STRING, fm, "10.5");
        assertEquals(Float.parseFloat("10.5"), result);

        fm.setSysDataType("boolean");
        result = rdmMappingService.map(AttributeTypeEnum.STRING, fm, "true");
        assertTrue((result instanceof Boolean) && (Boolean) result);

        fm.setSysDataType("date");
        result = rdmMappingService.map(AttributeTypeEnum.STRING, fm,"2007-10-15");
        assertEquals(LocalDate.of(2007, Month.OCTOBER, 15), result);

        try {
            fm.setSysDataType("jsonb");
            rdmMappingService.map(AttributeTypeEnum.STRING, fm, "1");
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting STRING to JSONB. Value: 1", e.getMessage());
        }
    }

    @Test
    void testBoolean() {
        FieldMapping fm = new FieldMapping("name", "boolean", "name");
        Object result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, true);
        assertEquals(true, result);

        fm.setSysDataType("varchar");
        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, true);
        assertEquals("true", result);

        try {
            fm.setSysDataType("date");
            rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, true);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting BOOLEAN to DATE. Value: true", e.getMessage());
        }
//      В рдм поле типа boolean. Значение не присутстствует. Необходимо, чтобы они смаппились на
//      дефолтный false.
        fm.setSysDataType("varchar");
        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, null);
        assertEquals("false", result);
        fm.setSysDataType("boolean");
        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, null);
        assertEquals(false, result);
    }

    @Test
    void testDate() {
        FieldMapping fm = new FieldMapping("name", "date", "name");

        LocalDate date = LocalDate.of(2007, Month.OCTOBER, 15);

        Object result = rdmMappingService.map(AttributeTypeEnum.DATE, fm, date);
        assertEquals(date, result);

        fm.setSysDataType("varchar");
        result = rdmMappingService.map(AttributeTypeEnum.DATE, fm, date);
        assertEquals("2007-10-15", result);

        try {
            fm.setSysDataType("integer");
            rdmMappingService.map(AttributeTypeEnum.DATE, fm, date);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting DATE to INTEGER. Value: 2007-10-15", e.getMessage());
        }
    }

    @Test
    void testReference() {
        FieldMapping fm = new FieldMapping("name", "jsonb", "name");

        Reference reference = new Reference("1", "Moscow");
        Object result = rdmMappingService.map(AttributeTypeEnum.REFERENCE, fm, new Reference("1", "Moscow"));
        assertEquals(reference, result);

        try {
            fm.setSysDataType("jsonb");
            rdmMappingService.map(AttributeTypeEnum.INTEGER, fm, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to JSONB. Value: 1", e.getMessage());
        }
    }

    @Test
    void testSpELTransformation() {
        FieldMapping fm = new FieldMapping("name", "jsonb", "name");
        fm.setTransformExpression("#root == null ? null : " +
        "(#root == 1 || #root == 'истина') ? true : " +
        "(#root == 0 || #root == 'ложь') ? false : " +
        "false");


        Object result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, 1);
        assertTrue((result instanceof Boolean) && (Boolean) result);

        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, 0);
        assertFalse((result instanceof Boolean) && (Boolean) result);

        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, "истина");
        assertTrue((result instanceof Boolean) && (Boolean) result);

        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, fm, "ложь");
        assertFalse((result instanceof Boolean) && (Boolean) result);
    }
}
