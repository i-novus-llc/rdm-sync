package ru.i_novus.ms.rdm.sync.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.sync.api.model.AttributeTypeEnum;
import ru.i_novus.ms.rdm.sync.api.model.RefBook;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;

import java.util.*;

import static java.util.Collections.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.util.CollectionUtils.isEmpty;

@RunWith(JUnit4.class)
public class RefBookReferenceSortTest {

    @Test
    public void testGetOrder() {

//      Проверяемая структура ссылок в справочниках:
//
//          5 -----> 2
//        /   \       \
//      7      4       1 -----> 0
//        \   /       /
//          6 -----> 3
//
//      Все ссылки идут слева направо.
//
//      Более формально:
//      7 -> 5, 7 -> 6;
//      5 -> 4, 5 -> 2
//      6 -> 4, 6 -> 3
//      2 -> 1
//      3 -> 1
//      1 -> 0

        RefBook refBook0 = new RefBook(),
                refBook1 = new RefBook(),
                refBook2 = new RefBook(),
                refBook3 = new RefBook(),
                refBook4 = new RefBook(),
                refBook5 = new RefBook(),
                refBook6 = new RefBook(),
                refBook7 = new RefBook();

        refBook0.setCode("0");
        refBook1.setCode("1");
        refBook2.setCode("2");
        refBook3.setCode("3");
        refBook4.setCode("4");
        refBook5.setCode("5");
        refBook6.setCode("6");
        refBook7.setCode("7");

        refBook0.setStructure(newStructure(emptyMap(), emptyList()));
        refBook1.setStructure(newStructure(emptyMap(), singletonList(refBook0.getCode())));
        refBook2.setStructure(newStructure(emptyMap(), singletonList(refBook1.getCode())));
        refBook3.setStructure(newStructure(emptyMap(), singletonList(refBook1.getCode())));
        refBook4.setStructure(newStructure(emptyMap(), emptyList()));
        refBook5.setStructure(newStructure(emptyMap(), List.of(refBook2.getCode(), refBook4.getCode())));
        refBook6.setStructure(newStructure(emptyMap(), List.of(refBook3.getCode(), refBook4.getCode())));
        refBook7.setStructure(newStructure(emptyMap(), List.of(refBook5.getCode(), refBook6.getCode())));

        List<RefBook> refBooks = List.of(
                refBook0, refBook1, refBook2, refBook3, refBook4, refBook5, refBook6, refBook7
        );

        List<RefBook> sortingRefBooks = new ArrayList<>(refBooks);
        for (int i = 0; i < 1000; i++) {

            shuffle(sortingRefBooks); // Перемешиваем, чтобы рандомизировать порядок
            List<String> inverseOrder = RefBookReferenceSort.getSortedCodes(sortingRefBooks);
            testOrder(inverseOrder, sortingRefBooks);
        }
    }

    private RefBookStructure newStructure(Map<String, AttributeTypeEnum> attributesAndTypes, List<String> references) {
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributesAndTypes(attributesAndTypes);
        refBookStructure.setReferences(references);
        return refBookStructure;
    }

    private void testOrder(List<String> inverseOrder, List<RefBook> refBooks) {

        assertFalse(isEmpty(refBooks));

        Set<String> visited = new HashSet<>();
        Map<String, RefBook> refCodeBooks = refBooks.stream()
                .collect(toMap(RefBook::getCode, identity()));

        for (String s : inverseOrder) {

            RefBook refBook = refCodeBooks.get(s);
            // Если здесь true, то либо неправильная топологическая сортировка,
            // либо справочники содержат циклические ссылки.
            assertFalse(visited.contains(refBook.getCode()));
            
            visited.add(refBook.getCode());
            
            for (String reference : refBook.getStructure().getReferences()) {
                // Если здесь false, то сортировка неправильная,
                // потому что не посещена вершина, на которую есть ссылка.
                assertTrue(visited.contains(reference));
            }
        }
    }

}
