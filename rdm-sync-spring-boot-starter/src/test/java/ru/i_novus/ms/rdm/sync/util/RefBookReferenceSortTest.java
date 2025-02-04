package ru.i_novus.ms.rdm.sync.util;


import org.junit.jupiter.api.Test;
import ru.i_novus.ms.rdm.sync.api.model.RefBookStructure;
import ru.i_novus.ms.rdm.sync.api.model.RefBookVersion;

import java.util.*;

import static java.util.Collections.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.CollectionUtils.isEmpty;

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

        RefBookVersion refBook0 = new RefBookVersion(),
                refBook1 = new RefBookVersion(),
                refBook2 = new RefBookVersion(),
                refBook3 = new RefBookVersion(),
                refBook4 = new RefBookVersion(),
                refBook5 = new RefBookVersion(),
                refBook6 = new RefBookVersion(),
                refBook7 = new RefBookVersion();

        refBook0.setCode("0");
        refBook1.setCode("1");
        refBook2.setCode("2");
        refBook3.setCode("3");
        refBook4.setCode("4");
        refBook5.setCode("5");
        refBook6.setCode("6");
        refBook7.setCode("7");

        refBook0.setStructure(newStructure(emptySet(), emptyList()));
        refBook1.setStructure(newStructure(emptySet(), singletonList(refBook0.getCode())));
        refBook2.setStructure(newStructure(emptySet(), singletonList(refBook1.getCode())));
        refBook3.setStructure(newStructure(emptySet(), singletonList(refBook1.getCode())));
        refBook4.setStructure(newStructure(emptySet(), emptyList()));
        refBook5.setStructure(newStructure(emptySet(), List.of(refBook2.getCode(), refBook4.getCode())));
        refBook6.setStructure(newStructure(emptySet(), List.of(refBook3.getCode(), refBook4.getCode())));
        refBook7.setStructure(newStructure(emptySet(), List.of(refBook5.getCode(), refBook6.getCode())));

        List<RefBookVersion> refBooks = List.of(
                refBook0, refBook1, refBook2, refBook3, refBook4, refBook5, refBook6, refBook7
        );

        List<RefBookVersion> sortingRefBooks = new ArrayList<>(refBooks);
        for (int i = 0; i < 1000; i++) {

            shuffle(sortingRefBooks); // Перемешиваем, чтобы рандомизировать порядок
            List<String> inverseOrder = RefBookReferenceSort.getSortedCodes(sortingRefBooks);
            testOrder(inverseOrder, sortingRefBooks);
        }
    }

    private RefBookStructure newStructure(Set<RefBookStructure.Attribute> attributes, List<String> references) {
        RefBookStructure refBookStructure = new RefBookStructure();
        refBookStructure.setAttributes(attributes);
        refBookStructure.setReferences(references);
        return refBookStructure;
    }

    private void testOrder(List<String> inverseOrder, List<RefBookVersion> refBooks) {

        assertFalse(isEmpty(refBooks));

        Set<String> visited = new HashSet<>();
        Map<String, RefBookVersion> refCodeBooks = refBooks.stream()
                .collect(toMap(RefBookVersion::getCode, identity()));

        for (String s : inverseOrder) {

            RefBookVersion refBook = refCodeBooks.get(s);
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
