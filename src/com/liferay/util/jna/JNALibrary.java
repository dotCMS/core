package com.liferay.util.jna;

import java.io.IOException;

import com.sun.jna.Platform;

public class JNALibrary {

	public static void link(String sourceFile, String destinationFile) throws IOException {
		if(Platform.isWindows()) {
			try {
				Kernel32Library.INSTANCE.CreateHardLinkA(destinationFile, sourceFile, null);
			} catch(UnsatisfiedLinkError e) {
				createHardLinkWithExec(sourceFile, destinationFile);
			}
		} else  {
			CLibrary.INSTANCE.link(sourceFile, destinationFile);
		}
	}

	public static void unlink(String file) {
		if(Platform.isWindows()) {
			Kernel32Library.INSTANCE.DeleteFileA(file);
		} else  {
			CLibrary.INSTANCE.unlink(file);
		}
	}

	private static void createHardLinkWithExec(String sourceFile, String destinationFile) throws IOException
    {
        String osname = System.getProperty("os.name");
        ProcessBuilder pb;
        if (osname.startsWith("Windows"))
        {
            float osversion = Float.parseFloat(System.getProperty("os.version"));
            if (osversion >= 6.0f)
            {
                pb = new ProcessBuilder("cmd", "/c", "mklink", "/H", destinationFile, sourceFile);
            }
            else
            {
                pb = new ProcessBuilder("fsutil", "hardlink", "create", destinationFile, sourceFile);
            }
        }
        else
        {
            pb = new ProcessBuilder("ln", sourceFile, destinationFile);
            pb.redirectErrorStream(true);
        }
        Process p = pb.start();
        try
        {
            p.waitFor();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

}
