package uk.org.lidalia.lang.encoding;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import uk.org.lidalia.lang.Bytes;
import uk.org.lidalia.lang.encoding.base64.Base64;

import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.lang.encoding.base64.Base64Encoder.base64;

public class Base64Tests {

    @Test
    public void encodesAsBase64() {

        // given:
        Base64 encoded = base64.encode("Any old string");

        // expect:
        assertThat(encoded.toString(), is("QW55IG9sZCBzdHJpbmc="));
        assertThat(encoded.decode(), is(Bytes.of("Any old string")));
        assertThat(encoded.decode().string(), is("Any old string"));
    }

    @Test
    public void encodesEmptyAsBase64() {

        // given:
        Base64 encoded = base64.encode("");

        // expect:
        assertThat(encoded.toString(), is(""));
        assertThat(encoded.decode(), is(Bytes.of(new byte[0])));
        assertThat(encoded.decode().string(), is(""));
    }

    @Test
    public void encodesAnythingAsBase64() {

        //given:
        String toEncode = RandomStringUtils.randomAscii(new Random().nextInt(25));

        // when:
        Base64 encoded = base64.encode(toEncode);

        // expect:
        assertThat(encoded.decode().string(), is(toEncode));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotAcceptNonBase64() {

        // when:
        Base64 encoded = base64.of("===");
    }
}
