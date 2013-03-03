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

import static org.junit.Assert.assertThat;
import static uk.org.lidalia.lang.Exceptions.throwUnchecked;
import static uk.org.lidalia.test.Assert.isNotInstantiable;
import static uk.org.lidalia.test.ShouldThrow.shouldThrow;

import org.junit.Test;

import java.util.concurrent.Callable;

public class TestExceptions {

    @Test
    public void throwUncheckedWithCheckedException() {
        final RichException checkedException = new RichException();
        shouldThrow(checkedException, new Runnable() {
            @Override
            public void run() {
                throwUnchecked(checkedException);
            }
        });
    }

    @Test
     public void throwUncheckedWithCheckedExceptionAndReturnStatementToTrickCompiler() {
        final RichException checkedException = new RichException();
        shouldThrow(checkedException, new Callable<Void>() {
            @Override
            public Void call() {
                return throwUnchecked(checkedException, null);
            }
        });
    }

    @Test
    public void notInstantiable() {
        assertThat(Exceptions.class, isNotInstantiable());
    }
}
