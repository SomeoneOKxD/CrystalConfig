package dev.someoneok.crystalconfig.utils;

import dev.someoneok.crystalconfig.render.RenderBackend;

import java.lang.reflect.Method;

final class RenderBackendHooks {
    private RenderBackendHooks() {
    }

    static boolean invoke(RenderBackend backend, String name, Class<?>[] parameterTypes, Object... args) {
        return invokeNullable(backend, name, parameterTypes, args).invoked();
    }

    static Result invokeNullable(RenderBackend backend, String name, Class<?>[] parameterTypes, Object... args) {
        if (backend == null) return Result.notInvoked();

        try {
            Method method = findMethod(backend.getClass(), name, parameterTypes);
            method.setAccessible(true);
            return Result.invoked(method.invoke(backend, args));
        } catch (Throwable ignored) {
            return Result.notInvoked();
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }

        Method method = findInterfaceMethod(type, name, parameterTypes);
        if (method != null) return method;
        throw new NoSuchMethodException(name);
    }

    private static Method findInterfaceMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        for (Class<?> iface : type.getInterfaces()) {
            try {
                return iface.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }

            Method nested = findInterfaceMethod(iface, name, parameterTypes);
            if (nested != null) return nested;
        }
        return null;
    }

    record Result(boolean invoked, Object value) {
        static Result invoked(Object value) {
            return new Result(true, value);
        }

        static Result notInvoked() {
            return new Result(false, null);
        }
    }
}
