package mt.fireworks.pauseless;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.*;

public class InternTrieTest {

    @Test @Ignore
    public void testStringTrie() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        String[] randomStrings = new String[1000];
        byte[][] stringBytes = new byte[randomStrings.length][];

        generate_random_strings: {
            for (int idx = 0; idx < randomStrings.length; idx++) {
                int len = rng.nextInt(1,7);
                String rs = randomAlphanumeric(len);
                randomStrings[idx] = rs;
                stringBytes[idx] = rs.getBytes(US_ASCII);
            }
        }

        InternTrie<String> intern = new InternTrie<>();
        String[] internedStrings = new String[1000];

        intern_random_strings: {
            for (int idx = 0; idx < randomStrings.length; idx++) {
                byte[] bytes = stringBytes[idx];
                String val = intern.intern(bytes, (objData, off, len) -> new String(objData, off, len, US_ASCII));
                String expected = randomStrings[idx];
                Assert.assertEquals(expected, val);
                internedStrings[idx] = val;
            }
        }


        for (int i = 0; i < 10; i++) benchmark: {
            long intDur = 0;
            long newDur = 0;

            for (int idx = 0; idx < 10_000_000; idx++) {
                int strIdx = rng.nextInt(stringBytes.length);
                byte[] bytes = stringBytes[strIdx];

                long t1 = System.nanoTime();
                String expected = new String(bytes, US_ASCII).intern();
                long t2 = System.nanoTime();
                String interned = intern.intern(bytes, (objData, off, len) -> new String(objData, off, len, US_ASCII));
                long t3 = System.nanoTime();

                assertEquals(expected, interned);
                intDur += t2 - t1;
                newDur += t3 - t2;
            }

            System.out.println(intDur);
            System.out.println(newDur);
        }


    }





    static String randomAlphanumeric(int len) {
        // num ASCII range: 48 - 57
        // A-Z ASCII range: 65 - 90
        // a-z ASCII range: 97 - 122

        char[] cs = new char[len];
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < len; i++) {
            int rn = rng.nextInt(62);
            char ch = 0;
            if (rn < 10) {
                ch = (char) ((int) '0' + rn);
            }
            else if (10 <= rn && rn < 36) {
                ch = (char) ((int) 'A' + rn - 10);
            }
            else {
                ch = (char) ((int) 'a' + rn - 36);
            }

            cs[i] = ch;
        }

        String res = new String(cs);
        return res;
    }

}
