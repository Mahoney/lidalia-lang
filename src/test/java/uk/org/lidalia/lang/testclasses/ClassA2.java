package uk.org.lidalia.lang.testclasses;

import uk.org.lidalia.lang.Identity;

public class ClassA2 extends ClassA {

	@Identity
	private String newIdentityValue;

	public void setNewIdentityValue(String newIdentityValue) {
		this.newIdentityValue = newIdentityValue;
	}

	public String getNewIdentityValue() {
		return newIdentityValue;
	}
}
