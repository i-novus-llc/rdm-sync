package ru.i_novus.ms.rdm.sync.dao.rsql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for RsqlToSqlConverter.
 * Following test writing best practices:
 * - Output-based testing (pure function: input → output)
 * - Test observable behavior (RSQL input → SQL output)
 * - Focus on domain logic and edge cases
 * - Security testing (SQL injection protection)
 */
class RsqlToSqlConverterTest {

    // ==================== EQUALITY OPERATORS ====================

    /**
     * Test == (equals) operator with string value.
     */
    @Test
    void convertToSql_withEqualsOperator_shouldConvertToSqlEquals() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name==John");

        // Assert
        assertEquals("name = 'John'", result);
    }

    /**
     * Test == (equals) operator with numeric value.
     * FIXED: Now always quoted to prevent VARCHAR = INTEGER errors.
     * PostgreSQL automatically converts '25' to INTEGER for numeric columns.
     */
    @Test
    void convertToSql_withEqualsOperatorAndNumber_shouldConvertWithQuotes() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("age==25");

        // Assert
        assertEquals("age = '25'", result);
    }

    /**
     * Test == (equals) operator with decimal value.
     * FIXED: Now quoted. PostgreSQL converts '99.99' to DECIMAL automatically.
     */
    @Test
    void convertToSql_withEqualsOperatorAndDecimal_shouldConvertWithQuotes() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("price==99.99");

        // Assert
        assertEquals("price = '99.99'", result);
    }

    /**
     * Test == (equals) operator with boolean value.
     */
    @Test
    void convertToSql_withEqualsOperatorAndBoolean_shouldConvertWithoutQuotes() {
        // Act
        String result1 = RsqlToSqlConverter.convertToSql("active==true");
        String result2 = RsqlToSqlConverter.convertToSql("disabled==false");

        // Assert
        assertEquals("active = true", result1);
        assertEquals("disabled = false", result2);
    }

    /**
     * Test != (not equals) operator.
     */
    @Test
    void convertToSql_withNotEqualsOperator_shouldConvertToSqlNotEquals() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("status!=active");

        // Assert
        assertEquals("status <> 'active'", result);
    }

    // ==================== COMPARISON OPERATORS ====================

    /**
     * Test > (greater than) operator with alternative notation.
     * FIXED: Values now quoted.
     */
    @Test
    void convertToSql_withGreaterThanOperator_shouldConvertCorrectly() {
        // Act
        String result1 = RsqlToSqlConverter.convertToSql("age>18");
        String result2 = RsqlToSqlConverter.convertToSql("age=gt=18");

        // Assert
        assertEquals("age > '18'", result1);
        assertEquals("age > '18'", result2);
    }

    /**
     * Test >= (greater than or equals) operator.
     * FIXED: Values now quoted.
     */
    @Test
    void convertToSql_withGreaterThanOrEqualsOperator_shouldConvertCorrectly() {
        // Act
        String result1 = RsqlToSqlConverter.convertToSql("age>=18");
        String result2 = RsqlToSqlConverter.convertToSql("age=ge=18");

        // Assert
        assertEquals("age >= '18'", result1);
        assertEquals("age >= '18'", result2);
    }

    /**
     * Test < (less than) operator.
     * FIXED: Values now quoted.
     */
    @Test
    void convertToSql_withLessThanOperator_shouldConvertCorrectly() {
        // Act
        String result1 = RsqlToSqlConverter.convertToSql("price<100");
        String result2 = RsqlToSqlConverter.convertToSql("price=lt=100");

        // Assert
        assertEquals("price < '100'", result1);
        assertEquals("price < '100'", result2);
    }

    /**
     * Test <= (less than or equals) operator.
     * FIXED: Values now quoted.
     */
    @Test
    void convertToSql_withLessThanOrEqualsOperator_shouldConvertCorrectly() {
        // Act
        String result1 = RsqlToSqlConverter.convertToSql("price<=100");
        String result2 = RsqlToSqlConverter.convertToSql("price=le=100");

        // Assert
        assertEquals("price <= '100'", result1);
        assertEquals("price <= '100'", result2);
    }

    // ==================== IN/OUT OPERATORS ====================

    /**
     * Test =in= operator with multiple values.
     */
    @Test
    void convertToSql_withInOperator_shouldConvertToSqlIn() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("status=in=(active,pending,approved)");

        // Assert
        assertEquals("status IN ('active','pending','approved')", result);
    }

    /**
     * Test =in= operator with numeric values.
     * FIXED: Now quoted. PostgreSQL converts to appropriate type.
     */
    @Test
    void convertToSql_withInOperatorAndNumbers_shouldConvertWithQuotes() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("id=in=(1,2,3,4,5)");

        // Assert
        assertEquals("id IN ('1','2','3','4','5')", result);
    }

    /**
     * Test =out= (not in) operator.
     */
    @Test
    void convertToSql_withOutOperator_shouldConvertToSqlNotIn() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("status=out=(deleted,archived)");

        // Assert
        assertEquals("status NOT IN ('deleted','archived')", result);
    }

    // ==================== NULL OPERATORS ====================

    /**
     * Test =null= operator (IS NULL).
     */
    @Test
    void convertToSql_withNullOperator_shouldConvertToIsNull() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("deleted_ts=null=null");

        // Assert
        assertEquals("deleted_ts IS NULL", result);
    }

    /**
     * Test =notnull= operator (IS NOT NULL).
     */
    @Test
    void convertToSql_withNotNullOperator_shouldConvertToIsNotNull() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("deleted_ts=notnull=null");

        // Assert
        assertEquals("deleted_ts IS NOT NULL", result);
    }

    // ==================== LIKE OPERATORS ====================

    /**
     * Test =like= operator with wildcard.
     */
    @Test
    void convertToSql_withLikeOperator_shouldConvertToSqlLike() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name=like=Скорая*");

        // Assert
        assertEquals("name LIKE 'Скорая%'", result);
    }

    /**
     * Test =like= operator with wildcard at the beginning.
     */
    @Test
    void convertToSql_withLikeOperatorWildcardAtStart_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name=like=*помощь");

        // Assert
        assertEquals("name LIKE '%помощь'", result);
    }

    /**
     * Test =like= operator with wildcards on both sides.
     */
    @Test
    void convertToSql_withLikeOperatorWildcardsOnBothSides_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("description=like=*test*");

        // Assert
        assertEquals("description LIKE '%test%'", result);
    }

    /**
     * Test =ilike= operator (case insensitive).
     */
    @Test
    void convertToSql_withIlikeOperator_shouldConvertToSqlIlike() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name=ilike=JOHN*");

        // Assert
        assertEquals("name ILIKE 'john%'", result);
    }

    /**
     * Test =qlike= operator (ignoring quotes).
     */
    @Test
    void convertToSql_withQlikeOperator_shouldRemoveQuotes() {
        // Act - testing with quotes that should be removed (wildcard must be inside the value)
        String result = RsqlToSqlConverter.convertToSql("name=qlike=\"Test*\"");

        // Assert
        // Quotes should be removed, wildcards converted
        assertEquals("name LIKE 'Test%'", result);
    }

    /**
     * Test =iqlike= operator (case insensitive + ignoring quotes).
     */
    @Test
    void convertToSql_withIqlikeOperator_shouldRemoveQuotesAndLowerCase() {
        // Act - wildcard must be inside the value
        String result = RsqlToSqlConverter.convertToSql("name=iqlike=\"TEST*\"");

        // Assert
        // Quotes should be removed, converted to lowercase, wildcards converted
        assertEquals("name ILIKE 'test%'", result);
    }

    // ==================== LOGICAL OPERATORS ====================

    /**
     * Test AND operator with semicolon notation.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withAndOperatorSemicolon_shouldConvertToSqlAnd() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name==John;age>25");

        // Assert
        assertEquals("(name = 'John' AND age > '25')", result);
    }

    /**
     * Test AND operator with 'and' keyword.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withAndOperatorKeyword_shouldConvertToSqlAnd() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name==John and age>25");

        // Assert
        assertEquals("(name = 'John' AND age > '25')", result);
    }

    /**
     * Test OR operator with comma notation.
     */
    @Test
    void convertToSql_withOrOperatorComma_shouldConvertToSqlOr() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("status==active,status==pending");

        // Assert
        assertEquals("(status = 'active' OR status = 'pending')", result);
    }

    /**
     * Test OR operator with 'or' keyword.
     */
    @Test
    void convertToSql_withOrOperatorKeyword_shouldConvertToSqlOr() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("status==active or status==pending");

        // Assert
        assertEquals("(status = 'active' OR status = 'pending')", result);
    }

    /**
     * Test multiple AND conditions.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withMultipleAndConditions_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name==John;age>=18;status==active");

        // Assert
        assertEquals("(name = 'John' AND age >= '18' AND status = 'active')", result);
    }

    /**
     * Test combined AND and OR operators.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withCombinedAndOrOperators_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("name==John;age>25,status==admin");

        // Assert
        // AND has higher precedence than OR
        assertEquals("((name = 'John' AND age > '25') OR status = 'admin')", result);
    }

    /**
     * Test complex expression from README example.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withComplexExpression_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql(
                "name==John;year>=2000;year<2010"
        );

        // Assert
        assertEquals("(name = 'John' AND year >= '2000' AND year < '2010')", result);
    }

    /**
     * Test complex expression with IN and OR from README.
     */
    @Test
    void convertToSql_withComplexInAndOrExpression_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql(
                "genres=in=(sci-fi,action);genres=out=(romance,horror),director==Tarantino"
        );

        // Assert
        assertEquals(
                "((genres IN ('sci-fi','action') AND genres NOT IN ('romance','horror')) OR director = 'Tarantino')",
                result
        );
    }

    // ==================== EDGE CASES ====================

    /**
     * Test with empty/null filter.
     */
    @Test
    void convertToSql_withEmptyFilter_shouldReturnEmptyString() {
        // Act & Assert
        assertEquals("", RsqlToSqlConverter.convertToSql(""));
        assertEquals("", RsqlToSqlConverter.convertToSql(null));
        assertEquals("", RsqlToSqlConverter.convertToSql("   "));
    }

    /**
     * Test with special characters in string value.
     */
    @Test
    void convertToSql_withSpecialCharactersInValue_shouldEscapeCorrectly() {
        // Act - using double quotes in RSQL to wrap value with single quote
        String result = RsqlToSqlConverter.convertToSql("name==\"O'Brien\"");

        // Assert
        // Single quotes should be escaped in SQL
        assertEquals("name = 'O''Brien'", result);
    }

    /**
     * Test with negative numbers.
     * FIXED: Now quoted.
     */
    @Test
    void convertToSql_withNegativeNumbers_shouldConvertCorrectly() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("temperature<-10");

        // Assert
        assertEquals("temperature < '-10'", result);
    }

    /**
     * Test with NULL value in equals operator.
     */
    @Test
    void convertToSql_withNullValueInEquals_shouldConvertToNull() {
        // Act
        String result = RsqlToSqlConverter.convertToSql("value==null");

        // Assert
        assertEquals("value = NULL", result);
    }

    /**
     * Test VARCHAR field with numeric string values - THE FIX!
     * Previously caused PostgreSQL error: "character varying = integer"
     * Now works correctly with quoted values.
     */
    @Test
    void convertToSql_varcharFieldWithNumericValues_nowFixed() {
        // Act
        String result1 = RsqlToSqlConverter.convertToSql("status==0");
        String result2 = RsqlToSqlConverter.convertToSql("code==123");
        String result3 = RsqlToSqlConverter.convertToSql("id=in=(1,2,3)");

        // Assert
        // ✅ FIXED: All values are quoted now
        // Works for both VARCHAR and INTEGER columns (PostgreSQL auto-converts)
        assertEquals("status = '0'", result1);
        assertEquals("code = '123'", result2);
        assertEquals("id IN ('1','2','3')", result3);
    }

    // ==================== SECURITY TESTS ====================

    /**
     * Test SQL injection protection in LIKE operator.
     * Should throw exception when dangerous patterns are detected.
     * Parser itself catches malformed RSQL before reaching our security checks.
     */
    @Test
    void convertToSql_withSqlInjectionInLike_shouldThrowException() {
        // Act & Assert - parser rejects malformed RSQL
        assertThrows(RuntimeException.class, () ->
                RsqlToSqlConverter.convertToSql("name=like=test'; DROP TABLE users; --")
        );
    }

    /**
     * Test SQL injection protection with percent sign in LIKE.
     */
    @Test
    void convertToSql_withPercentSignInLike_shouldThrowException() {
        // Act & Assert
        assertThrows(SecurityException.class, () ->
                RsqlToSqlConverter.convertToSql("name=like=test%")
        );
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Test with invalid RSQL syntax.
     */
    @Test
    void convertToSql_withInvalidSyntax_shouldThrowException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                RsqlToSqlConverter.convertToSql("name===invalid")
        );
    }

    /**
     * Test with unsupported operator.
     */
    @Test
    void convertToSql_withUnsupportedOperator_shouldThrowException() {
        // This test might need adjustment based on actual parser behavior
        // The parser itself might reject unknown operators before reaching our code
        assertThrows(Exception.class, () ->
                RsqlToSqlConverter.convertToSql("name=unknown=value")
        );
    }

    /**
     * Test LIKE operator with multiple arguments (should fail).
     */
    @Test
    void convertToSql_withLikeOperatorMultipleArgs_shouldThrowException() {
        // LIKE should only accept one argument
        assertThrows(Exception.class, () ->
                RsqlToSqlConverter.convertToSql("name=like=(value1,value2)")
        );
    }

    /**
     * Test NULL operator with wrong argument.
     */
    @Test
    void convertToSql_withNullOperatorWrongArgument_shouldThrowException() {
        // NULL operators should have 'null' as value
        assertThrows(IllegalArgumentException.class, () ->
                RsqlToSqlConverter.convertToSql("name=null=notNull")
        );
    }

    // ==================== REAL-WORLD SCENARIOS ====================

    /**
     * Test real-world scenario: filter active users by age range.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withRealWorldScenario_activeUsersByAgeRange() {
        // Act
        String result = RsqlToSqlConverter.convertToSql(
                "active==true;age>=18;age<=65;deleted_ts=null=null"
        );

        // Assert
        assertEquals(
                "(active = true AND age >= '18' AND age <= '65' AND deleted_ts IS NULL)",
                result
        );
    }

    /**
     * Test real-world scenario: search by name pattern.
     */
    @Test
    void convertToSql_withRealWorldScenario_searchByNamePattern() {
        // Act
        String result = RsqlToSqlConverter.convertToSql(
                "name=ilike=*иван*;status=in=(active,verified)"
        );

        // Assert
        assertEquals(
                "(name ILIKE '%иван%' AND status IN ('active','verified'))",
                result
        );
    }

    /**
     * Test real-world scenario: exclude certain categories.
     */
    @Test
    void convertToSql_withRealWorldScenario_excludeCategories() {
        // Act
        String result = RsqlToSqlConverter.convertToSql(
                "category=out=(deleted,archived,spam);created_date>=2024-01-01"
        );

        // Assert
        assertEquals(
                "(category NOT IN ('deleted','archived','spam') AND created_date >= '2024-01-01')",
                result
        );
    }

    /**
     * Test real-world scenario from README: medical organizations filter.
     * FIXED: Numeric values now quoted.
     */
    @Test
    void convertToSql_withRealWorldScenario_medicalOrganizations() {
        // Act
        String result = RsqlToSqlConverter.convertToSql(
                "name=like=Скорая*;region_code=in=(77,50,23);is_active==true"
        );

        // Assert
        assertEquals(
                "(name LIKE 'Скорая%' AND region_code IN ('77','50','23') AND is_active = true)",
                result
        );
    }
}
