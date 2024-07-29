package mt.fireworks.pauseless;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.*;

import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.list.primitive.MutableLongList;

import lombok.*;

/**
 * Benchmark InternTrie speedup with increasing number of threads.
 */
public class InternTrieScalabilityTest {

    /** set to true to print debug information */
    final static boolean debugEnabled = false;


    public static void main(String[] args) {
        final int testUpToThreadCount = Runtime.getRuntime().availableProcessors();

        System.out.println("InternTrie scalability test started.");
        System.out.println("Core count: " + testUpToThreadCount);



        // create base dictionary to be used for testing
        // start worker threads which translate bytearray to string
        // run test for 10 seconds
        // measure how much job did worker do


        final ArrayList<String> dictionaryText = new ArrayList<>();
        final ArrayList<byte[]> dictionaryBytes = new ArrayList<>();

        createTestDictionary: {
            int wordCount = 100_000;

            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int idx = 0; idx < wordCount; idx++) {
                int strLen = rng.nextInt(1,12);
                String word = RandomStringUtils.randomAlphanumeric(strLen);
                dictionaryText.add(word);
                dictionaryBytes.add(word.getBytes(StandardCharsets.UTF_8));
            }
        }

        // warmup
        System.out.println("Warming up for next 10 sec.");
        runTest(1, 10_000, dictionaryText, dictionaryBytes);
        if (debugEnabled) System.out.println();
        System.out.println("Test results: ");



        long baseCount = 0;
        long runForMs = 2000;

        for (int numOfThreads = 1; numOfThreads <= testUpToThreadCount; numOfThreads++) {
            if (debugEnabled) System.out.println("  -----------------------------------------------------------");

            // run tests 5 times
            // discard fastest and slowest result

            MutableLongList results = LongLists.mutable.withInitialCapacity(5);
            for (int j = 0; j < 5; j++) {
                long count = runTest(numOfThreads, runForMs, dictionaryText, dictionaryBytes);
                results.add(count);
            }

            results.remove(results.min());
            results.remove(results.max());

            long sum = results.sum();
            long runtime = runForMs * results.size();
            long speed = sum * 1000 / runtime;
            long speed1th = speed / numOfThreads;

            if (baseCount == 0) baseCount = sum;
            float speedup = (float) sum / baseCount;

            String res =
                    String.format("  numOfThreads: %2d", numOfThreads)
                    + ", " +
                    String.format("speed: %10d inters/sec", speed)
                    + ", " +
                    String.format("one thread speed avg: %10d inters/sec", speed1th)
                    + ", " +
                    String.format("speedup: %5.02f", speedup);

            System.out.println(res);
        }

        System.out.println("Done");
    }

    @SneakyThrows
    static long runTest(
        int numOfThreads, long runForMs,
        ArrayList<String> dictionaryText,
        ArrayList<byte[]> dictionaryBytes
    ) {

        final InternTrie<String> trie = new InternTrie<>();

        final ExecutorService executor = Executors.newCachedThreadPool();
        final ArrayList<Future<Long>> futures = new ArrayList<>();

        for (int idx = 0; idx < numOfThreads; idx++) {
            BackgroundWorker worker = new BackgroundWorker(
                    dictionaryText, dictionaryBytes,
                    trie, runForMs
            );
            Future<Long> future = executor.submit(worker);
            futures.add(future);
        }


        long count = 0;
        for (Future<Long> future : futures)
            count += future.get();

        executor.shutdown();

        String res =
            String.format("  numOfThreads: %2d", numOfThreads)
            + ", " +
            String.format("count: %10d", count);

        if (debugEnabled) System.out.println(res);

        return count;
    }


    @AllArgsConstructor
    @Data
    static class BackgroundWorker implements Callable<Long> {
        final ArrayList<String> dictionaryText;
        final ArrayList<byte[]> dictionaryBytes;
        final InternTrie<String> trie;
        final long runForMs;

        public Long call() throws Exception {
            long start = System.currentTimeMillis();
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int size = dictionaryBytes.size();

            long counter = 0;

            while (true) {
                int rndIdx = rng.nextInt(size);
                byte[] data = dictionaryBytes.get(rndIdx);
                String strOrg = dictionaryText.get(rndIdx);
                String strTrie = trie.intern(data, (objData, off, len) -> new String(objData, off, len, UTF_8));
                if (!strOrg.equals(strTrie)) {
                    throw new RuntimeException("'" + strOrg + "' != '" + strTrie + "'");
                }

                counter ++;

                long now = System.currentTimeMillis();
                long dur = now - start;
                if (dur > runForMs) break;
            }

            return counter;
        }

    }

}
