package com.dotmarketing.db;

public final class SneakyThrow {
    public static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) throw new NullPointerException("t");
        sneakyThrow0(t);
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
        throw (T)t;
    }
}
