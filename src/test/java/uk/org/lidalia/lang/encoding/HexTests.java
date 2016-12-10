package uk.org.lidalia.lang.encoding;

import org.junit.Test;
import uk.org.lidalia.lang.Bytes;
import uk.org.lidalia.lang.encoding.hex.Hex;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.lang.encoding.hex.HexEncoder.hex;

public class HexTests {

    @Test
    public void encodesAsHex() {

        // given:
        Hex encoded = hex.encode("Any old string");

        // expect:
        assertThat(encoded.toString(), is("416e79206f6c6420737472696e67"));
        assertThat(encoded.decode(), is(Bytes.of("Any old string")));
        assertThat(encoded.decode().string(), is("Any old string"));
    }

    @Test
    public void decodedHex() {

        // given:
        Hex encoded = hex.of("416e79206f6c6420737472696e67");

        // expect:
        assertThat(encoded.toString(), is("416e79206f6c6420737472696e67"));
        assertThat(encoded.decode(), is(Bytes.of("Any old string")));
        assertThat(encoded.decode().string(), is("Any old string"));
    }

    @Test
    public void decodedHexCaseInsensitive() {

        // given:
        Hex encoded = hex.of("416E79206F6C6420737472696E67");

        // expect:
        assertThat(encoded.toString(), is("416E79206F6C6420737472696E67"));
        assertThat(encoded.decode(), is(Bytes.of("Any old string")));
        assertThat(encoded.decode().string(), is("Any old string"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHexString() {
        hex.of("416E79206F6C6420737472696E67Z");
    }
}
