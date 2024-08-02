package mt.fireworks.pauseless;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.*;

public class InternTrieTest {


    @Test
    public void offsetTest() {
        InternTrie<String> internTrie = new InternTrie<String>();

        String a = "example";
        byte[] abytes = a.getBytes(US_ASCII);
        String a2 = internTrie.intern(abytes, (data) -> new String(data, US_ASCII));
        assertEquals(a, a2);


        String b = "usage example";
        byte[] bbytes = b.getBytes(US_ASCII);
        String b2 = internTrie.intern(bbytes, b.indexOf(a), a.length(), (data, off, len) -> new String(data, off, len, US_ASCII));
        assertEquals(a, b2);

        String c = "uninspiring example of usage";
        byte[] cbytes = c.getBytes(US_ASCII);
        String c2 = internTrie.intern(cbytes, c.indexOf(a), a.length(), (data, off, len) -> new String(data, off, len, US_ASCII));
        assertEquals(a, c2);


        assertSame(a2, b2);
        assertSame(a2, c2);
    }


    @Test
    public void testStringTrie() {
        InternTrie<String> internTrie = new InternTrie<String>();

        for (int idx = 0; idx < 1_000_000; idx++) {
            internRandomString(internTrie);
        }
    }

    void internRandomString(InternTrie<String> internTrie) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int strLen = rng.nextInt(1,23);
        String randomString = RandomStringUtils.randomAlphanumeric(strLen);
        byte[] bytes = randomString.getBytes(US_ASCII);

        String val1 = internTrie.intern(bytes, (objData, off, len) -> new String(objData, off, len, US_ASCII));
        assertEquals(randomString, val1);

        String val2 = internTrie.intern(bytes, (objData, off, len) -> new String(objData, off, len, US_ASCII));
        assertSame(val1, val2);

        String val3 = internTrie.intern(bytes, (objData) -> new String(objData, US_ASCII));
        assertSame(val1, val3);
    }



    @Test
    public void randomBilionOfStrings() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int countOfUniqueStrings = 100_000;
        String[] randomStrings = new String[countOfUniqueStrings];

        createRandomStrings: {
            for (int idx = 0; idx < randomStrings.length; idx++) {
                int strLen = rng.nextInt(1,12);
                String word = RandomStringUtils.randomAlphanumeric(strLen);
                randomStrings[idx] = word;
            }
        }

        InternTrie<String> trie = new InternTrie<String>();

        int aLargeNumber = 100_000_000;
        for (int i = 0; i < aLargeNumber; i++) {
            int idx = rng.nextInt(randomStrings.length);
            String expected = randomStrings[idx];
            byte[] bytes = expected.getBytes(UTF_8);
            String actual = trie.intern(bytes, 0 , bytes.length, (d, o, l) -> new String(d, o, l, UTF_8));

            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void offsetStrings() throws IOException {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int countOfUniqueStrings = 100_000;
        String[] randomStrings = new String[countOfUniqueStrings];

        createRandomStrings: {
            for (int idx = 0; idx < randomStrings.length; idx++) {
                int strLen = rng.nextInt(1,12);
                String word = RandomStringUtils.randomAlphanumeric(strLen);
                randomStrings[idx] = word;
            }
        }


        byte[] allStringsArray = null;
        int[] offsets = new int[randomStrings.length];
        int[] lens = new int[randomStrings.length];


        createData: {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int offset = 0;
            for (int idx = 0; idx < randomStrings.length; idx++) {
                String str = randomStrings[idx];
                byte[] data = str.getBytes(UTF_8);

                baos.write(data);
                offsets[idx] = offset;
                lens[idx] = data.length;
                offset += data.length;
            }
            allStringsArray = baos.toByteArray();
        }


        InternTrie<String> trie = new InternTrie<String>();

        int billion = 100_000_000;
        for (int i = 0; i < billion; i++) {
            int idx = rng.nextInt(randomStrings.length);
            String expected = randomStrings[idx];
            int offset = offsets[idx];
            int length = lens[idx];

            String actual = trie.intern(allStringsArray, offset, length, (data, off, len) -> new String(data, off, len, UTF_8));
            Assert.assertEquals(expected, actual);
        }
    }


    @Test
    public void offsetTest2() {
        final InternTrie<String> it = new InternTrie<String>();
        final String adaText = "ada".intern();

        {
            byte[] adaBytes = "ada".getBytes(UTF_8);
            String adaIntern = it.intern(adaBytes, (data) -> new String(data, UTF_8).intern());
            assertSame(adaText, adaIntern);
        }

        {
            byte[] adaBytes = "ada".getBytes(UTF_8);
            String adaIntern = it.intern(adaBytes, (data, off, len) -> new String(data, off, len, UTF_8).intern());
            assertSame(adaText, adaIntern);
        }

        {
            byte[] adaBytes = "adadada".getBytes(UTF_8);

            String adaIntern_1 = it.intern(adaBytes, 0, 3, (data, off, len) -> new String(data, off, len, UTF_8).intern());
            assertSame(adaText, adaIntern_1);

            String adaIntern_2 = it.intern(adaBytes, 2, 3, (data, off, len) -> new String(data, off, len, UTF_8).intern());
            assertSame(adaText, adaIntern_2);

            String adaIntern_3 = it.intern(adaBytes, 4, 3, (data, off, len) -> new String(data, off, len, UTF_8).intern());
            assertSame(adaText, adaIntern_3);
        }

        {
            String alphabet = "abcdefghijklmnopqrstuvwxyz";
            byte[] bytes = alphabet.getBytes(US_ASCII);

            for (int i = 0; i < alphabet.length(); i++) {
                for (int j = i + 1; j <= alphabet.length(); j++) {
                    String subString = alphabet.substring(i, j).intern();
                    String internVal = it.intern(bytes, i, j-i, (data, off, len) -> new String(data, off, len, UTF_8).intern());

                    assertEquals(subString, internVal);
                    assertSame(subString, internVal);
                }
            }
        }
    }

}
