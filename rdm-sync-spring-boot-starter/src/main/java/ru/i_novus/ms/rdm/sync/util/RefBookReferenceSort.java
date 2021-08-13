package ru.i_novus.ms.rdm.sync.util;

import ru.i_novus.ms.rdm.api.model.refbook.RefBook;

import java.util.*;

public class RefBookReferenceSort {

    private RefBookReferenceSort() {}

    public static List<String> getSortedCodes(List<RefBook> refBooks) {

        Map<String, DictionaryNode> refCodes = new HashMap<>();
        for (RefBook refbook : refBooks) {
            refCodes.put(refbook.getCode(), new DictionaryNode());
        }

        for (RefBook version : refBooks) {

            DictionaryNode node = refCodes.get(version.getCode());
            version.getStructure().getReferences().forEach(reference -> {

                String referenceCode = reference.getReferenceCode();
                if (refCodes.containsKey(referenceCode)) {
                    node.child.add(referenceCode);
                }
            });
        }

        List<String> topologicalOrder = topologicalSort(refCodes);

        LinkedList<String> inverseOrder = new LinkedList<>();
        for (String s : topologicalOrder) {
            inverseOrder.push(s);
        }
        return inverseOrder;
    }

    private static List<String> topologicalSort(Map<String, DictionaryNode> refCodes) {

        Set<String> visited = new HashSet<>();
        LinkedList<String> stack = new LinkedList<>();

        for (Map.Entry<String, DictionaryNode> e : refCodes.entrySet()) {

            String refBookCode = e.getKey();
            if (!visited.contains(refBookCode)) {
                topologicalSort0(stack, visited, refCodes, refBookCode);
            }
        }

        return stack;
    }

    private static void topologicalSort0(LinkedList<String> stack, Set<String> visited,
                                         Map<String, DictionaryNode> refCodes, String refBookCode) {

        visited.add(refBookCode);

        for (String referenceCode : refCodes.get(refBookCode).child) {
            if (!visited.contains(referenceCode)) {
                topologicalSort0(stack, visited, refCodes, referenceCode);
            }
        }

        stack.push(refBookCode);
    }

    private static class DictionaryNode {

        private final Collection<String> child = new LinkedList<>();
    }

}
