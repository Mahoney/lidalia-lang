/*
 * Copyright (c) 2009-2010 Robert Elliot
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.org.lidalia.lang;

import java.util.concurrent.Callable;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.org.lidalia.lang.Exceptions.doUnchecked;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;
import static uk.org.lidalia.lang.Assert.isNotInstantiable;
import static uk.org.lidalia.lang.ShouldThrow.shouldThrow;

import org.junit.Test;

public class ExceptionsTests {

    @Test
    public void throwUncheckedWithCheckedException() {
        final Exception checkedException = new Exception();
        Exception actual = shouldThrow(Exception.class, () -> throwUnchecked(checkedException));
        assertThat(actual, is(checkedException));
    }

    @Test
     public void throwUncheckedWithCheckedExceptionAndReturnStatementToTrickCompiler() {
        final Exception checkedException = new Exception();
        Exception actual = shouldThrow(Exception.class, new Runnable() {
            @Override
            public void run() {
                compilerThinksReturnsString();
            }

            private String compilerThinksReturnsString() {
                return throwUnchecked(checkedException, null);
            }
        });
        assertThat(actual, is(checkedException));
    }

    @Test
    public void throwUncheckedWithNull() {
        shouldThrow(NullPointerException.class, () -> throwUnchecked(null));
    }

    @Test
    public void throwUncheckedWithNullAndReturnStatementToTrickCompiler() {
        shouldThrow(NullPointerException.class, new Runnable() {
            @Override
            public void run() {
                compilerThinksReturnsString();
            }

            private String compilerThinksReturnsString() {
                return throwUnchecked(null, null);
            }
        });
    }

    @Test
    public void doUncheckedReturnsValueWhenSuccess() {
        String result = doUnchecked(() -> "Success");
        assertThat(result, is("Success"));
    }

    @Test
    public void doUncheckedThrowsUncheckedThrowable() {
        final Exception checkedException = new Exception();
        Exception actual = shouldThrow(Exception.class, () -> doUnchecked((Callable<String>) () -> {
            throw checkedException;
        }));
        assertThat(actual, is(checkedException));
    }

    @Test
    public void notInstantiable() {
        assertThat(Exceptions.class, isNotInstantiable());
    }
}
