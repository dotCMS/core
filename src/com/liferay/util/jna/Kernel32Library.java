package com.liferay.util.jna;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32Library extends StdCallLibrary {
    Kernel32Library INSTANCE = (Kernel32Library) (Platform.isWindows()
            ? Native.loadLibrary("kernel32", Kernel32Library.class,W32APIOptions.UNICODE_OPTIONS)
            : null);

    public static class LPSECURITY_ATTRIBUTES extends Structure {
        public int nLength;
        public Pointer lpSecurityDescriptor;
        public boolean bInheritHandle;
    }

    boolean CreateHardLinkA(String newPath, String existingPath, LPSECURITY_ATTRIBUTES lpSecurityAttributes);

    boolean DeleteFileA(String existingPath);
}
