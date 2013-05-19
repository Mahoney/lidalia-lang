package uk.org.lidalia.lang;

import java.lang.reflect.Member;

/**
 * An enumeration of the various modifiers that can be applied to classes or members.
 */
public enum Modifier {

    PUBLIC {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isPublic(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isPublic(modifiable.getModifiers());
        }
    },

    PRIVATE {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isPrivate(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isPrivate(modifiable.getModifiers());
        }
    },

    PROTECTED {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isProtected(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isProtected(modifiable.getModifiers());
        }
    },

    STATIC {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isStatic(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isStatic(modifiable.getModifiers());
        }
    },

    FINAL {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isFinal(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isFinal(modifiable.getModifiers());
        }
    },

    SYNCHRONIZED {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isSynchronized(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isSynchronized(modifiable.getModifiers());
        }
    },

    VOLATILE {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isVolatile(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isVolatile(modifiable.getModifiers());
        }
    },

    TRANSIENT {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isTransient(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isTransient(modifiable.getModifiers());
        }
    },

    NATIVE {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isNative(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isNative(modifiable.getModifiers());
        }
    },

    INTERFACE {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isInterface(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isInterface(modifiable.getModifiers());
        }
    },

    ABSTRACT {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isAbstract(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isAbstract(modifiable.getModifiers());
        }
    },

    STRICT {
        @Override
        public boolean existsOn(final Member modifiable) {
            return java.lang.reflect.Modifier.isStrict(modifiable.getModifiers());
        }

        @Override
        public boolean existsOn(final Class<?> modifiable) {
            return java.lang.reflect.Modifier.isStrict(modifiable.getModifiers());
        }
    };

    /**
     * @param modifiable - the member that may have this modifier
     * @return true if the member has this modifier, false otherwise
     */
    public abstract boolean existsOn(Member modifiable);

    /**
     * @param modifiable - the class that may have this modifier
     * @return true if the class has this modifier, false otherwise
     */
    public abstract boolean existsOn(Class<?> modifiable);
}
