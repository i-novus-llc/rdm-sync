package ru.i_novus.ms.rdm.sync.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

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

        refBook0.setStructure(new Structure(emptyList(), emptyList()));
        refBook1.setStructure(new Structure(emptyList(), singletonList(newReference(refBook0))));
        refBook2.setStructure(new Structure(emptyList(), singletonList(newReference(refBook1))));
        refBook3.setStructure(new Structure(emptyList(), singletonList(newReference(refBook1))));
        refBook4.setStructure(new Structure(emptyList(), emptyList()));
        refBook5.setStructure(new Structure(emptyList(), List.of(newReference(refBook2), newReference(refBook4))));
        refBook6.setStructure(new Structure(emptyList(), List.of(newReference(refBook3), newReference(refBook4))));
        refBook7.setStructure(new Structure(emptyList(), List.of(newReference(refBook5), newReference(refBook6))));

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

    private Structure.Reference newReference(RefBook refBook) {

        return new Structure.Reference("", refBook.getCode(), "");
    }

    private void testOrder(List<String> inverseOrder, List<RefBook> refBooks) {

        assertFalse(isEmpty(refBooks));

        Set<String> visited = new HashSet<>();
        Map<String, RefBook> refCodeBooks = refBooks.stream()
                .map(RefBook::new)
                .collect(toMap(RefBookVersion::getCode, identity()));

        for (String s : inverseOrder) {

            RefBook refBook = refCodeBooks.get(s);
            // Если здесь true, то либо неправильная топологическая сортировка,
            // либо справочники содержат циклические ссылки.
            assertFalse(visited.contains(refBook.getCode()));
            
            visited.add(refBook.getCode());
            
            for (Structure.Reference reference : refBook.getStructure().getReferences()) {
                // Если здесь false, то сортировка неправильная,
                // потому что не посещена вершина, на которую есть ссылка.
                assertTrue(visited.contains(reference.getReferenceCode()));
            }
        }
    }

}
