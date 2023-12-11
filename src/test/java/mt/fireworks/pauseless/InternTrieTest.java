package mt.fireworks.pauseless;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.*;

public class InternTrieTest {


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
    }

}
