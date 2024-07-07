package ru.i_novus.ms.rdm.sync.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.i_novus.ms.rdm.sync.model.DataTypeEnum.*;

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
        Object result = rdmMappingService.map(AttributeTypeEnum.INTEGER, INTEGER, BigInteger.ONE);
        assertEquals(BigInteger.ONE, result);

        result = rdmMappingService.map(AttributeTypeEnum.INTEGER, VARCHAR, 1);
        assertEquals("1", result);

        result = rdmMappingService.map(AttributeTypeEnum.INTEGER, FLOAT, 1);
        assertEquals(Float.parseFloat("1"), result);

        try {
            rdmMappingService.map(AttributeTypeEnum.INTEGER, BOOLEAN, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to BOOLEAN. Value: 1", e.getMessage());
        }

        try {
            rdmMappingService.map(AttributeTypeEnum.INTEGER, DATE, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to DATE. Value: 1", e.getMessage());
        }

        try {
            rdmMappingService.map(AttributeTypeEnum.INTEGER, JSONB, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to JSONB. Value: 1", e.getMessage());
        }
    }

    @Test
    void testString() {
        Object result = rdmMappingService.map(AttributeTypeEnum.STRING, VARCHAR, "10");
        assertEquals("10", result);

        result = rdmMappingService.map(AttributeTypeEnum.STRING, INTEGER, "10");
        assertEquals(BigInteger.TEN, result);

        result = rdmMappingService.map(AttributeTypeEnum.STRING, FLOAT, "10.5");
        assertEquals(Float.parseFloat("10.5"), result);

        result = rdmMappingService.map(AttributeTypeEnum.STRING, BOOLEAN, "true");
        assertTrue((result instanceof Boolean) && (Boolean) result);

        result = rdmMappingService.map(AttributeTypeEnum.STRING, DATE, "2007-10-15");
        assertEquals(LocalDate.of(2007, Month.OCTOBER, 15), result);

        try {
            rdmMappingService.map(AttributeTypeEnum.STRING, JSONB, "1");
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting STRING to JSONB. Value: 1", e.getMessage());
        }
    }

    @Test
    void testBoolean() {
        Object result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, BOOLEAN, true);
        assertEquals(true, result);

        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, VARCHAR, true);
        assertEquals("true", result);

        try {
            rdmMappingService.map(AttributeTypeEnum.BOOLEAN, DATE, true);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting BOOLEAN to DATE. Value: true", e.getMessage());
        }
//      В рдм поле типа boolean. Значение не присутстствует. Необходимо, чтобы они смаппились на
//      дефолтный false.
        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, VARCHAR, null);
        assertEquals("false", result);
        result = rdmMappingService.map(AttributeTypeEnum.BOOLEAN, BOOLEAN, null);
        assertEquals(false, result);
    }

    @Test
    void testDate() {
        LocalDate date = LocalDate.of(2007, Month.OCTOBER, 15);

        Object result = rdmMappingService.map(AttributeTypeEnum.DATE, DATE, date);
        assertEquals(date, result);

        result = rdmMappingService.map(AttributeTypeEnum.DATE, VARCHAR, date);
        assertEquals("2007-10-15", result);

        try {
            rdmMappingService.map(AttributeTypeEnum.DATE, INTEGER, date);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting DATE to INTEGER. Value: 2007-10-15", e.getMessage());
        }
    }

    @Test
    void testReference() {
        Reference reference = new Reference("1", "Moscow");
        Object result = rdmMappingService.map(AttributeTypeEnum.REFERENCE, JSONB, new Reference("1", "Moscow"));
        assertEquals(reference, result);

        try {
            rdmMappingService.map(AttributeTypeEnum.INTEGER, JSONB, 1);
            fail("Ожидается ClassCastException");
        } catch (ClassCastException e) {
            assertEquals("Error while casting INTEGER to JSONB. Value: 1", e.getMessage());
        }
    }
}
