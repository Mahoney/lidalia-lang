## Lidalia Lang

A set of utility classes that act as extensions of java.lang for common use cases.

See the [JavaDocs](./apidocs/index.html) for full documentation and the [Test Source](./xref-test/index.html) for complete
examples of usage. Below are some examples from some of the more common classes.

### Examples

#### Making Checked Exceptions Unchecked

    import static uk.org.lidalia.lang.Exceptions.throwUnchecked;

    private void throwsCheckedException() throws Exception {
        throw new Exception();
    }

    public void doSomething() {
        try {
            throwsCheckedException();
        } catch (Exception e) {
            throwUnchecked(e);
        }
    }

    public String doSomethingAndReturn() {
        try {
            throwsCheckedException();
            return "can't get here";
        } catch (Exception e) {
            return throwUnchecked(e, null);
        }
    }

#### Avoiding Object Boilerplate

    import uk.org.lidalia.lang.Identity;
    import uk.org.lidalia.lang.RichObject;

    public class Person extends RichObject {

        @Identity private final String firstName;
        @Identity private final String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }

is the equivalent of:

    public class Person {

        private final String firstName;
        private final String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;

            Person person = (Person) o;

            if (!firstName.equals(person.firstName)) return false;
            if (!lastName.equals(person.lastName)) return false;

            return true;
        }

        @Override
        public final int hashCode() {
            int result = 17;
            result = 37 * result + lastName.hashCode();
            result = 37 * result + lastName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Person[" +
                    "firstName=" + firstName +
                    ",lastName=" + lastName +
                    ']';
        }
    }
