package uk.org.lidalia.lang;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.org.lidalia.lang.testclasses.ClassA;
import uk.org.lidalia.lang.testclasses.ClassA1;
import uk.org.lidalia.lang.testclasses.ClassA2;
import uk.org.lidalia.lang.testclasses.ClassB;

public class TestIdentity {
	
	@Test
	public void testEqualsOnInstanceOfSameClass() {
		ClassA o1 = new ClassA();
		o1.setValue1("hello");
		assertTrue(o1.equals(o1));
		assertFalse(o1.equals(null));
		
		ClassA o2 = new ClassA();
		assertTrue(o2.equals(o2));
		assertFalse(o1.equals(o2));
		assertFalse(o2.equals(o1));
		
		o2.setValue1("hello");
		assertTrue(o1.equals(o2));
		assertTrue(o2.equals(o1));
	}
	
	@Test
	public void testEqualsOnInstanceOfSameClassMultipleIdentityMembers() {
		ClassA2 o1 = new ClassA2();
		o1.setValue1("hello");
		
		ClassA2 o2 = new ClassA2();
		o2.setValue1("hello");
		assertTrue(o1.equals(o2));
		assertTrue(o2.equals(o1));
		
		o1.setNewIdentityValue("world");
		assertFalse(o1.equals(o2));
		assertFalse(o2.equals(o1));
		
		o2.setNewIdentityValue("world");
		assertTrue(o1.equals(o2));
		assertTrue(o2.equals(o1));
	}
	
	@Test
	public void testEqualsOnInstanceOfClassesNotAssignableFromEachOther() {
		ClassA o1 = new ClassA();
		o1.setValue1("hello");
		ClassB o2 = new ClassB();
		o2.setValue1("hello");
		assertTrue(o2.equals(o2));
		assertFalse(o1.equals(o2));
		assertFalse(o2.equals(o1));
	}
	
	@Test
	public void testEqualsOnInstancesOfClassesAssignableFromEachOtherWithSameIdentityMembers() {
		ClassA o1 = new ClassA();
		o1.setValue1("hello");
		ClassA1 o2 = new ClassA1();
		o2.setValue1("hello");
		o2.setNonIdentityValue("world");		
		assertTrue(o2.equals(o2));
		assertTrue(o1.equals(o2));
		assertTrue(o2.equals(o1));
	}
	
	@Test
	public void testEqualsOnInstancesOfClassesAssignableFromEachOtherWithDifferentIdentityMembers() {
		ClassA o1 = new ClassA();
		o1.setValue1("hello");
		ClassA2 o2 = new ClassA2();
		o2.setValue1("hello");
		o2.setNewIdentityValue("world");		
		assertTrue(o2.equals(o2));
		assertFalse(o1.equals(o2));
		assertFalse(o2.equals(o1));
	}

    @Test public void toStringWorks() {
        ClassA o = new ClassA();
        o.setValue1("hello");
        assertEquals("ClassA[value1=hello]", o.toString());
    }
}
