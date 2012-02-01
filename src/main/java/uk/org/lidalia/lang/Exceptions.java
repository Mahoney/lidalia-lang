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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Exceptions {

	public static String throwableToString(String baseToString, List<Throwable> causes) {
		if (causes.isEmpty()) {
			return baseToString;
		} else {
			StringBuilder stringValue = new StringBuilder(baseToString);
			stringValue.append("; caused by: ").append(causes);
			return stringValue.toString();
		}
	}
	
	public static void throwUnchecked(final Throwable ex){
        Exceptions.<RuntimeException>throwsUnchecked(ex);
        throw new AssertionError("This code should be unreachable. Something went terrible wrong here!");
    }

    @SuppressWarnings("unchecked")
	private static <T extends Throwable> void throwsUnchecked(Throwable toThrow) throws T {
        throw (T) toThrow;
    }

    static List<Throwable> buildUnmodifiableCauseList(Throwable cause, Throwable[] otherCauses) {
		ArrayList<Throwable> causes = new ArrayList<Throwable>(otherCauses.length + 1);
		causes.add(cause);
		causes.addAll(Arrays.asList(otherCauses));
		return Collections.unmodifiableList(causes);
	}

	private Exceptions() {
		throw new UnsupportedOperationException("Not instantiable");
	}
}
