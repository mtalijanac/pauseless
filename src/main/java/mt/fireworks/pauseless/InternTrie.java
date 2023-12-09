package mt.fireworks.pauseless;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * InternTrie je mapa keširanih stringova koja služi kao
 * alternativa {@code String#intern()} operaciji.
 *
 * Osnovna optimizacija SerdesTriea je da ne zahtjeva instanciran
 * string da bi napravio intern, već prima sirovo polje bajtova.
 *
 * Klasa je thread safe.
 *
 */
public class InternTrie<T> {

    interface Unmarshaller<T> {
        T unmarshall(byte[] objData, int off, int len);
    }

    final TrieNode<T> root = new TrieNode<>(0);

    public T intern(byte[] objData, Unmarshaller<T> unmarshaller) {
        return computeIfAbsent(objData, 0, objData.length, unmarshaller);
    }


    public T computeIfAbsent(byte[] objData, int off, int len, Unmarshaller<T> unmarshaller) {

        TrieNode<T> current = root;
        for (int idx = off; idx < len; idx += 8) {

            // kodiraj byte[] u longove
            long nodeKey = 0;
            int endIdx = Math.min(len, idx + 8);
            for (int jdx = idx; jdx < endIdx; jdx++) {
                byte b = objData[jdx];
                nodeKey = (nodeKey << 8) | (0xff & b);
            }

            // sve dok je ključ dugačak 8 bajtova spusti se na idući node stabla
            // za kraće vrijednosti pročitaj gotovu vrijednost iz mape.
            int keyLen = endIdx - idx;
            if (keyLen >= 8) {
                current = current.childNode(nodeKey);
            }
            else {
                return current.childValue(nodeKey, unmarshaller, objData, off, len);
            }
        }

        T value = current.getValue(unmarshaller, objData, off, len);
        return value;
    }


    @RequiredArgsConstructor
    static class TrieNode<T> {

        @NonNull long nodeKey;
        T value;
        MutableLongObjectMap<Object> children;


        public synchronized MutableLongObjectMap<Object> getChildren() {
            if (children == null) {
                children = new LongObjectHashMap<>();
            }

            return children;
        }

        public synchronized TrieNode<T> childNode(long nodeKey) {
            TrieNode<T> child = (TrieNode<T>) getChildren().getIfAbsentPutWithKey(nodeKey, k -> new TrieNode<>(k));
            return child;
        }

        public synchronized T childValue(long nodeKey, Unmarshaller<T> supplier, byte[] key, int off, int len) {
            T val = (T) getChildren().getIfAbsentPutWithKey(nodeKey,
                    k -> supplier.unmarshall(key, off, len)
            );
            return val;
        }

        public synchronized T getValue(Unmarshaller<T> supplier, byte[] key, int off, int len) {
            if (this.value != null) {
                return this.value;
            }

            if (len == 0) {
                return null;
            }

            this.value = supplier.unmarshall(key, off, len);
            return this.value;
        }
    }



    @Override
    public String toString() {
        AtomicInteger nodeCount = new AtomicInteger();
        AtomicInteger mapCount = new AtomicInteger();
        AtomicInteger valueCount = new AtomicInteger();
        about(root, nodeCount, mapCount, valueCount);

        String s = "nodes: " + nodeCount.get() + ", maps: " + mapCount.get() + ", values: " + valueCount.get();
        return s;
    }

    void about(TrieNode node, AtomicInteger nodeCount, AtomicInteger mapCount, AtomicInteger valueCount) {
        nodeCount.incrementAndGet();
        if (node.value != null) valueCount.incrementAndGet();
        if (node.children == null) return;

        mapCount.incrementAndGet();

        node.children.keySet().forEach(each -> {
            Object obj = node.children.get(each);
            if (obj instanceof TrieNode) {
                TrieNode<T> t = (TrieNode<T>) obj;
                about(t, nodeCount, mapCount, valueCount);
            }
            else {
                valueCount.incrementAndGet();
            }
        });
    }

}
