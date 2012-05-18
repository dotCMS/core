package com.liferay.util.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public interface CLibrary extends Library {
	CLibrary INSTANCE = (CLibrary)
	Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), 
			CLibrary.class);
	int link(String fromFile, String toFile);
	int unlink(String file);
}

