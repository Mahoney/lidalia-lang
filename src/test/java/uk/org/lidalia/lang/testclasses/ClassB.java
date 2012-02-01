package uk.org.lidalia.lang.testclasses;

import uk.org.lidalia.lang.Identity;
import uk.org.lidalia.lang.RichObject;

public class ClassB extends RichObject {

	@Identity
	private String value1;

	public void setValue1(String value1) {
		this.value1 = value1;
	}

	public String getValue1() {
		return value1;
	}
}
