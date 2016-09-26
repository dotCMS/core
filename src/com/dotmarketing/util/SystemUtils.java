package com.dotmarketing.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;

import com.dotcms.repackage.org.apache.commons.collections.Transformer;
import com.dotcms.repackage.org.apache.commons.collections.functors.ChainedTransformer;
import com.dotcms.repackage.org.apache.commons.collections.functors.ConstantTransformer;
import com.dotcms.repackage.org.apache.commons.collections.functors.InvokerTransformer;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.sun.management.OperatingSystemMXBean;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;


public class SystemUtils {

	private Runtime runtime = Runtime.getRuntime();

	public String Info() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.OsInfo());
		sb.append(this.MemInfo());
		sb.append(this.DiskInfo());
		return sb.toString();
	}

	public String OSname() {
		return System.getProperty("os.name");
	}

	public String OSversion() {
		return System.getProperty("os.version");
	}

	public String OsArch() {
		return System.getProperty("os.arch");
	}

	public long totalMem() {
		return Runtime.getRuntime().totalMemory();
	}

	public long usedMem() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	public String MemInfo() {
		NumberFormat format = NumberFormat.getInstance();
		StringBuilder sb = new StringBuilder();
		long maxMemory = runtime.maxMemory() / 1000;
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		sb.append("Free memory: ");
		sb.append(format.format(freeMemory / 1024 / 1000));
		sb.append("<br/>");
		sb.append("Allocated memory: ");
		sb.append(format.format(allocatedMemory / 1024));
		sb.append("<br/>");
		sb.append("Max memory: ");
		sb.append(format.format(maxMemory / 1024));
		sb.append("<br/>");
		sb.append("Total free memory: ");
		sb.append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
		sb.append("<br/>");
		return sb.toString();

	}

	private int availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	private static long lastSystemTime = 0;
	private static long lastProcessCpuTime = 0;

	private void baselineCounters() {
		lastSystemTime = System.nanoTime();

		if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
			lastProcessCpuTime = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
		}
	}

	public synchronized double getCpuUsage() {
		if (lastSystemTime == 0) {
			baselineCounters();
		}

		long systemTime = System.nanoTime();
		long processCpuTime = 0;

		if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean) {
			processCpuTime = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();
		}

		double cpuUsage = (double) (processCpuTime - lastProcessCpuTime) / (systemTime - lastSystemTime);

		lastSystemTime = systemTime;
		lastProcessCpuTime = processCpuTime;

		return cpuUsage / availableProcessors;
	}

	public String OsInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("OS: ");
		sb.append(this.OSname());
		sb.append("<br/>");
		sb.append("Version: ");
		sb.append(this.OSversion());
		sb.append("<br/>");
		sb.append(": ");
		sb.append(this.OsArch());
		sb.append("<br/>");
		sb.append("Available processors (cores): ");
		sb.append(runtime.availableProcessors());
		sb.append("<br/>");

		sb.append("cpu:");
		sb.append(getCpuUsage());
		sb.append("<br/>");
		return sb.toString();
	}

	public String DiskInfo() {
		/* Get a list of all filesystem roots on this system */
		File[] roots = File.listRoots();
		StringBuilder sb = new StringBuilder();

		/* For each filesystem root, print some info */
		for (File root : roots) {
			sb.append("File system root: ");
			sb.append(root.getAbsolutePath());
			sb.append("<br/>");
			sb.append("Total space (bytes): ");
			sb.append(root.getTotalSpace());
			sb.append("<br/>");
			sb.append("Free space (bytes): ");
			sb.append(root.getFreeSpace());
			sb.append("<br/>");
			sb.append("Usable space (bytes): ");
			sb.append(root.getUsableSpace());
			sb.append("<br/>");
		}
		return sb.toString();
	}

	public void testSerialzationBug() throws Exception {
		try{
			MyObject myObj = new MyObject();
			myObj.name = "bob";
	
			

			
			
			File file = new File(Config.CONTEXT.getRealPath("/object.ser"));
		    byte[] data=SerializationUtils.serialize(SerializationUtils.serialize(myObj));
		    FileUtils.writeByteArrayToFile(file,data);

	
			// Read the serialized data back in from the file "object.ser"

			data = FileUtils.readFileToByteArray(file);
	
			// Read the object from the data stream, and convert it back to a String
			MyObject objectFromDisk = (MyObject) SerializationUtils.deserialize(data);
	
			// Print the result.
			System.out.println(objectFromDisk.name);

		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage(), e);
		}

	}


}
