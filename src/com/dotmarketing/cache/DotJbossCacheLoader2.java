package com.dotmarketing.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.collections.map.LRUMap;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Modification;
import org.jboss.cache.RegionManager;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.CacheLoader;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.TrashUtils;

public class DotJbossCacheLoader2 implements CacheLoader {

	File root = null;
	String rootPath = null;

	/**
	 * For full path, check '*' '<' '>' '|' '"' '?' Regex: [\*<>|"?]
	 */
	public static String PATH_PATTERN = "[\\*<>|\"?]";

	/**
	 * For fqn, check '*' '<' '>' '|' '"' '?' and also '\' '/' and ':'
	 */
	public static final String FQN_PATTERN = "[\\\\\\/*<>|\"?]";
	private static LRUMap cannotCacheCache = new LRUMap(1000);
	private FileCacheLoaderConfig config;


	private boolean paused=false;

	private boolean enabled=true;

	/**
	 * CacheImpl data file.
	 */
	public static final String DATA = "data.dat";

	/**
	 * CacheImpl directory suffix.
	 */
	public static final String DIR_SUFFIX = "fdb";
	private static boolean isOldWindows;

	static {
		float osVersion;
		try {
			osVersion = Float.parseFloat(System.getProperty("os.version")
					.trim());
		} catch (Exception e) {
			osVersion = -1;
		}
		// 4.x is windows NT/2000 and 5.x is XP.
		isOldWindows = System.getProperty("os.name").toLowerCase().startsWith(
				"windows")
				&& osVersion < 4;
	}

	public DotJbossCacheLoader2() {
		Logger.debug(DotJbossCacheLoader2.class, "Creating Cache Loader");
	}

	public void commit(Object arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void create() throws Exception {
		root = new File(ConfigUtils.getDynamicContentPath() + File.separator
				+ "dotcache");
		rootPath = root.getAbsolutePath() + File.separator;

		if (root == null) {
			String tmpLocation = System.getProperty("java.io.tmpdir", "./temp");
			root = new File(tmpLocation);

			rootPath = root.getAbsolutePath() + File.separator;
		}
		if (!root.exists()) {

			if (config.isCheckCharacterPortability()) {
				/*
				 * Before creating the root, check whether the path is character
				 * portable. Anything that comes after is part of the fqn which
				 * is inspected later.
				 */
				isCharacterPortableLocation(root.getAbsolutePath());
			}

			boolean created = root.mkdirs();
			if (!created) {
				throw new IOException("Unable to create cache loader location "
						+ root);
			}
		}

		if (!root.isDirectory()) {
			throw new IOException("Cache loader location [" + root
					+ "] is not a directory!");
		}

	}

	public void destroy() {
		// TODO Auto-generated method stub

	}

	public boolean exists(Fqn arg0) throws Exception {
		File f = getDirectory(arg0, false);
		return f != null;
	}

	public Map<Object, Object> get(Fqn fqn) throws Exception {
		if (isPaused() || !isEnabled()) {
			return null;
		}
		return loadAttributes(fqn);
	}

	public Set<?> getChildrenNames(Fqn arg0) throws Exception {
		File parent = getDirectory(arg0, false);
		if (parent == null) {
			return null;
		}
		File[] children = parent.listFiles();
		Set<String> s = new HashSet<String>();
		for (File child : children) {
			if (child.isDirectory() && child.getName().endsWith(DIR_SUFFIX)) {
				String child_name = child.getName();
				child_name = child_name.substring(0, child_name
						.lastIndexOf(DIR_SUFFIX) - 1);
				s.add(child_name);
			}
		}
		return s.size() == 0 ? null : s;

	}

	public IndividualCacheLoaderConfig getConfig() {
		return config;
	}

	public void loadEntireState(ObjectOutputStream arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void loadState(Fqn arg0, ObjectOutputStream arg1) throws Exception {
		// TODO Auto-generated method stub

	}

	public void prepare(Object arg0, List<Modification> arg1, boolean arg2)
			throws Exception {
		put(arg1);

	}

	public void put(List<Modification> arg0) throws Exception {
		if (!isEnabled()) {
			return ;
		}
			ensureUnpaused();
			for (Modification mod : arg0) {
				put(mod.getFqn(), mod.getData());

			}

	}

	public void put(Fqn arg0, Map<Object, Object> arg1) throws Exception {
		if (!isEnabled()) {
			return ;
		}
			ensureUnpaused();
			doMarshall(arg0, arg1);


	}

	public Object put(Fqn arg0, Object key, Object value) throws Exception {
		if (!isEnabled()) {
			return null;
		}
		ensureUnpaused();
		if (!cacheToDisk(key.toString())) {
			return null;
		}
		Object retval;
		Map m = new HashMap();
		retval = m.put(key, value);
		put(arg0, m);
		return retval;
	}

	public void remove(Fqn fqn) throws Exception {
		if (!isEnabled()) {
			return ;
		}
		ensureUnpaused();
		if (!cacheToDisk(fqn.toString())) {
			return;
		}
		cannotCacheCache.remove(fqn.toString());
		File dir = getDirectory(fqn, false);
		if (dir != null) {
			boolean flag = moveDirectoryToTrash(dir);
			if (!flag) {
				Logger.warn(this, "failed removing " + fqn);
			}
		}


	}

	public Object remove(Fqn fqn, Object arg1) throws Exception {
		if (!isEnabled()) {
			return null;
		}
		ensureUnpaused();
		if (!cacheToDisk(fqn.toString())) {
			return null;
		}
		cannotCacheCache.remove(fqn.toString());

		File dir = getDirectory(fqn, false);
		if (dir != null) {
			boolean flag = moveDirectoryToTrash(dir);
			if (!flag) {
				Logger.warn(this, "failed removing " + fqn);
			}
		}
		return null;
	}

	public void removeData(Fqn fqn) throws Exception {
		if (!isEnabled()) {
			return ;
		}
		ensureUnpaused();
		if (!cacheToDisk(fqn.toString())) {
			return;
		}
		cannotCacheCache.remove(fqn.toString());

		File dir = getDirectory(fqn, false);
		if (dir != null) {
			boolean flag = moveDirectoryToTrash(dir);
			if (!flag) {
				Logger.warn(this, "failed removing " + fqn);
			}
		}

	}

	public void rollback(Object arg0) {
		// TODO Auto-generated method stub

	}

	public void setCache(CacheSPI arg0) {
		// TODO Auto-generated method stub

	}

	public void setConfig(IndividualCacheLoaderConfig base) {
		if (base instanceof FileCacheLoaderConfig) {
			this.config = (FileCacheLoaderConfig) base;
		} else if (base != null) {
			this.config = new FileCacheLoaderConfig(base);
		}
	}

	public void setRegionManager(RegionManager arg0) {
		// TODO Auto-generated method stub

	}

	public void start() throws Exception {
		// TODO Auto-generated method stub

	}

	public void stop() {
		// TODO Auto-generated method stub

	}

	public void storeEntireState(ObjectInputStream arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void storeState(Fqn arg0, ObjectInputStream arg1) throws Exception {
		// TODO Auto-generated method stub

	}

	private File getDirectory(Fqn fqn, boolean create) throws IOException {
		File f = new File(getFullPath(fqn));
		if (!f.exists()) {
			if (create) {
				boolean make = f.mkdirs();
				if (!make)
					throw new IOException("Unable to mkdirs " + f);
			} else {
				return null;
			}
		}
		return f;
	}

	private void doMarshall(Fqn fqn, Map attrs) throws Exception {
		if (fqn.toString().length() > 255) {
			return;
		}
		if (!cacheToDisk(fqn.toString())) {
			return;
		}
		if (cannotCacheCache.get(fqn.toString()) != null) {
			Logger.debug(this,
					"returning because object is in cannot cache cache");
			return;
		}

		File f = getDirectory(fqn, true);
		Set keys = attrs.keySet();
		File child = new File(f, DATA);
		if (!child.exists()) {
			if (config.isCheckCharacterPortability()) {
				/*
				 * Check whether the entire file path (root + fqn + data file
				 * name), is length portable
				 */
				isLengthPortablePath(child.getAbsolutePath());
				/*
				 * Check whether the fqn tree we're trying to store could
				 * contain non portable characters
				 */
				isCharacterPortableTree(child.getAbsolutePath());
			}

			if (!child.createNewFile()) {
				throw new IOException("Unable to create file: " + child);
			}
		}
		ObjectOutputStream output = null;
		OutputStream bout =null ;
		FileOutputStream os=null;
		try {
			if (Config.getBooleanProperty("USE_CACHE_COMPRESSION", true)) {
				os=new FileOutputStream(child);
				bout = new DeflaterOutputStream(os);
			} else {
				os=new FileOutputStream(child);
				bout = new BufferedOutputStream(os,
						8192);
			}
			// GZIPOutputStream bout = new GZIPOutputStream(new
			// FileOutputStream(child), 8192);
			// ZipOutputStream bout = new ZipOutputStream(new
			// FileOutputStream(child));
			output = new ObjectOutputStream(bout);
			output.writeObject(attrs);
		} catch (StackOverflowError e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			closeOutputStreams(bout,output,os);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (CacheException e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			closeOutputStreams(bout,output,os);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (RuntimeException e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			closeOutputStreams(bout,output,os);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (StreamCorruptedException e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			closeOutputStreams(bout,output,os);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (Exception e) {
			closeOutputStreams(bout,output,os);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
		} finally {
			closeOutputStreams(bout,output,os);
		}
	}
	
	private void closeOutputStreams(OutputStream... outputStreams ) {
		for (OutputStream os:outputStreams) {
			if (os!=null) {
				try {
					os.close();
				} catch (Exception e) {
					Logger.error(this, "Error closing stream: " + e.getMessage(),e);
				}
			}
		}
	}

	
	
	private void closeInputStreams(InputStream... inputStreams ) {
		for (InputStream is:inputStreams) {
			if (is!=null) {
				try {
					is.close();
				} catch (Exception e) {
					Logger.error(this, "Error closing stream: " + e.getMessage(),e);
				}
			}
		}
	}

	private String getFullPath(Fqn fqn) {
		StringBuilder sb = new StringBuilder(rootPath);
		if (fqn.size() == 1) {
			sb.append(fqn.get(0)).append(".").append(DIR_SUFFIX).append(
					File.separator);
		} else {
			for (int i = 0; i < fqn.size(); i++) {
				Object tmp = fqn.get(i);
				// This is where we convert from Object to String!
				String tmp_dir = tmp.toString().replace(':', '-'); // returns
				// tmp.this
				// if it's a
				if (fqn.size() == i + 1) {
					sb.append(buildBTreePath(tmp.hashCode() + "")
							+ File.separator);
				}
				// String
				sb.append(tmp_dir).append(".").append(DIR_SUFFIX).append(
						File.separator);
			}
		}
		return sb.toString();
	}

	private String buildBTreePath(String hashcode) {
		String result = "";
		try {
			result = hashcode.substring(1, 2);
			result += File.separator + hashcode.substring(2, 3);
			result += File.separator + hashcode.substring(3, 4);
			result += File.separator + hashcode.substring(4, 5);
		} catch (IndexOutOfBoundsException e) {
			Logger.debug(this, "hashcode too short returning path");
		}
		return result;
	}

	private boolean isLengthPortablePath(String absoluteFqnPath) {

		if (isOldWindows && absoluteFqnPath.length() > 255) {
			Logger
					.warn(
							this,
							"The full absolute path to the fqn that you are trying to store is bigger than 255 characters, this could lead to problems on certain Windows systems: "
									+ absoluteFqnPath);
			return false;
		}

		return true;
	}

	private boolean isCharacterPortableTree(String fqn) {
		StringTokenizer st = new StringTokenizer(fqn, File.separator);
		// Don't assume the Fqn is composed of Strings!!
		while (st.hasMoreTokens()) {
			String element = st.nextToken();
			// getFullPath converts Object to String via toString(), so we do
			// too
			boolean contains = RegEX.contains(element, FQN_PATTERN);

			if (contains) {
				Logger
						.warn(
								this,
								"One of the Fqn ( "
										+ fqn
										+ " ) elements contains one of these characters: '*' '<' '>' '|' '\"' '?' '\\' '/' ':' ");
				Logger
						.warn(
								this,
								"Directories containing these characters are illegal in some operating systems and could lead to portability issues");
				return false;
			}
		}

		return true;
	}

	private Map loadAttributes(Fqn fqn) throws Exception {
		File f = getDirectory(fqn, false);
		if (f == null)
			return null; // i.e., this node does not exist.
		// this node exists so we should never return a null after this... at
		// worst case, an empty HashMap.
		File child = new File(f, DATA);
		if (!child.exists()) {

			return null; // no node attribs exist hence the empty
		}
		// HashMap.
		// if(!child.exists()) return null;

		Map m;
		try {
			m = (Map) doUnmarshall(fqn, child);
			// m = (Map) regionAwareUnmarshall(fqn, child);
		} catch (FileNotFoundException fnfe) {
			// child no longer exists!
			m = null;
		} catch (Exception e) {

			m = null;
		}
		return m;
	}

	private Object doUnmarshall(Fqn fqn, Object fromFile) throws Exception {
		ObjectInputStream input = null;
		InputStream bin = null;
		FileInputStream is = null;
		try {
			if (Config.getBooleanProperty("USE_CACHE_COMPRESSION", true)) {
				is=new FileInputStream((File) fromFile);
				bin = new InflaterInputStream(is);
			} else {
				is=new FileInputStream((File) fromFile);
				bin = new BufferedInputStream(is, 8192);
			}
			// GZIPInputStream bin = new GZIPInputStream(new
			// FileInputStream((File) fromFile), 8192);
			// ZipInputStream bin = new ZipInputStream(new
			// FileInputStream((File) fromFile));
			input = new ObjectInputStream(bin);
			Object unmarshalledObj = input.readObject();
			return unmarshalledObj;
		} catch (StackOverflowError e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			closeInputStreams(input,bin,is);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (CacheException e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			closeInputStreams(input,bin,is);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (RuntimeException e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			closeInputStreams(input,bin,is);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (StreamCorruptedException e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			closeInputStreams(input,bin,is);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (IOException e) {
			return null;
		} catch (Exception e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			closeInputStreams(input,bin,is);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} finally {
			closeInputStreams(input,bin,is);
			
		}
	}

	private synchronized void disable() {
		if (!enabled) {
			return;
		}
		Logger.info(this, "Temporarily disabling on disk cache");
		enabled=false;
		Thread t=new Thread() {
			public void run() {
				boolean done=false;
				Fqn fqn=Fqn.fromString("");
				while (!done) {
					try {
						Thread.sleep(120000);
					} catch (InterruptedException e) {
					}
						try {
							File dir=getDirectory(fqn, false);
							moveDirectoryToTrash(dir, true);
							enable();
							done=true;
						} catch (IOException e) {
							Logger.error(DotJbossCacheLoader2.class,"IOException: " +e.getMessage(),e);
						}

				}

			}
		};
		t.setName("OnDiskCPRThread");
		t.start();
	}

	private synchronized void enable() {
		enabled=true;
		paused=false;
		Logger.info(this,"On disk cache enabled");
	}

	private boolean isEnabled() {
		return enabled;
	}

	private boolean isPaused() {
		return paused;
	}


	private  void ensureUnpaused () throws IOException {
		int count=0;

		while (isPaused()) {
			count ++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			if (count > 200) {
				throw new IOException("Timeout while waiting for cache to unlock");
			}
		}
		if (!enabled) {
			throw new IOException("Cache is disabled");
		}
		return ;
	}

	private void moveDirectoryToTrash(File dir, boolean isRoot) throws IOException {
	  if (isRoot) {
	   File[] subfiles = dir.listFiles();
	   if (!TrashUtils.moveFileToThrash(subfiles, "dotcache")) {
	    throw new IOException("Could not move directory to trash");
	   }
	  } else {
	   if (!TrashUtils.moveFileToThrash(dir, "dotcache")) {
	    throw new IOException("Could not move directory to trash");
	   }
	  }
	 }

	/**
	 * Recursively removes this and all subdirectories, plus all DATA files in
	 * them. To prevent damage, we only remove files under the cache root.
	 *
	 * @return <code>true</code> if directory was removed, <code>false</code> if
	 *         not.
	 * @throws Exception
	 */
	private boolean moveDirectoryToTrash(File dir) throws Exception {
		if (!dir.getCanonicalPath().toLowerCase().contains(
				root.getCanonicalPath().toLowerCase())) {
			Logger.error(this.getClass(),
					"Cache trying to delete a non-cache dir : "
							+ dir.getAbsolutePath());
			return false;
		}
		boolean isRoot = dir.getCanonicalPath().toLowerCase().equals(
				root.getCanonicalPath().toLowerCase());
		try {
			moveDirectoryToTrash(dir, isRoot);
		} catch (IOException e) {
			synchronized (this) {
				paused = true;
				int count = 0;
				while (paused) {

					try {
						moveDirectoryToTrash(dir, isRoot);
						paused = false;
					} catch (IOException e2) {
						count++;
						Thread.sleep(100);
						if (count > 100) {
							disable();
						}
					}
				}

			}
		} finally {
			paused=false;
		}
		return true;
	}

	private boolean isCharacterPortableLocation(String fileAbsolutePath) {
		boolean contains = RegEX.contains(fileAbsolutePath, PATH_PATTERN);
		if (contains) {
			Logger
					.warn(
							this,
							"Cache loader location ( "
									+ fileAbsolutePath
									+ " ) contains one of these characters: '*' '<' '>' '|' '\"' '?'");
			Logger
					.warn(
							this,
							"Directories containing these characters are illegal in some operative systems and could lead to portability issues");
			return false;
		}

		return true;
	}

	private boolean cacheToDisk(String key) {
		if (key.startsWith("VelocityMenuCache")) {
			return false;
		}
		if (key.startsWith("VelocityCache")) {
			if (!(key.contains("live") || key.contains("working"))) {
				return false;
			}
		}
		return true;
	}

}
