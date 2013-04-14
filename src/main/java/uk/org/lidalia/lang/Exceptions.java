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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility functions around Exception management.
 */
public final class Exceptions {

    /**
     * Because this method throws an unchecked exception, when it is called in a method with a return type the compiler
     * does not know the method is exiting, requiring a further line to return null or throw an unchecked exception
     * directly. This generified method allows this to be avoided by tricking the compiler by adding a return statement
     * as so:
     * <pre>
     *     String someMethod() {
     *         try {
     *             somethingThatThrowsException();
     *         } catch (Exception e) {
     *             return throwUnchecked(e, null); // does not actually return, throws the exception
     *         }
     *     }
     * </pre>
     * @param toThrow The throwable that will be thrown, unwrapped and unchecked; must not be null
     * @param returnType trick to persuade the compiler that a method returns appropriately - always pass null here
     * @return Never returns, always throws the passed in exception
     * @throws NullPointerException if toThrow is null
     */
    public static <T> T throwUnchecked(final Throwable toThrow, final T returnType) {
        Exceptions.<RuntimeException>doThrowUnchecked(checkNotNull(toThrow));
        throw new AssertionError("This code should be unreachable. Something went terribly wrong here!");
    }

    /**
     * @param toThrow The throwable that will be thrown, unwrapped and unchecked; must not be null
     * @throws NullPointerException if toThrow is null
     */
    public static void throwUnchecked(final Throwable toThrow) {
        throwUnchecked(toThrow, null);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doThrowUnchecked(final Throwable toThrow) throws T {
        throw (T) toThrow;
    }

    private Exceptions() {
        throw new UnsupportedOperationException("Not instantiable");
    }
}
