package uk.org.lidalia.lang.reflect;

import java.lang.reflect.Member;

public enum Modifier {

    PUBLIC {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isPublic(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isPublic(member.getModifiers());
        }
    },

    PRIVATE {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isPrivate(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isPrivate(member.getModifiers());
        }
    },

    PROTECTED {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isProtected(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isProtected(member.getModifiers());
        }
    },

    STATIC {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isStatic(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isStatic(member.getModifiers());
        }
    },

    FINAL {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isFinal(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isFinal(member.getModifiers());
        }
    },

    SYNCHRONIZED {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isSynchronized(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isSynchronized(member.getModifiers());
        }
    },

    VOLATILE {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isVolatile(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isVolatile(member.getModifiers());
        }
    },

    TRANSIENT {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isTransient(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isTransient(member.getModifiers());
        }
    },

    NATIVE {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isNative(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isNative(member.getModifiers());
        }
    },

    INTERFACE {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isInterface(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isInterface(member.getModifiers());
        }
    },

    ABSTRACT {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isAbstract(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isAbstract(member.getModifiers());
        }
    },

    STRICT {
        @Override
        public boolean isTrueOf(Member member) {
            return java.lang.reflect.Modifier.isStrict(member.getModifiers());
        }

        @Override
        public boolean isTrueOf(Class<?> member) {
            return java.lang.reflect.Modifier.isStrict(member.getModifiers());
        }
    };

    public abstract boolean isTrueOf(Member member);
    public abstract boolean isTrueOf(Class<?> member);
}
