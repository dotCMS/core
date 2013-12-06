package com.dotmarketing.servlets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.metadata.ClassMetadata;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ClickstreamRequest;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.oreilly.servlet.MultipartRequest;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Description of the Class
 * 
 * @author Will
 * @created September 18, 2002
 */

public class ImportExportXMLServlet extends HttpServlet {
	/**
	 * The path where backup files are stored
	 */
	String backupFilePath = "../backup";

	/**
	 * The path where tmp files are stored. This gets wiped alot
	 */
	String backupTempFilePath = "../backup/temp";

	public void init(ServletConfig config) throws ServletException {
		// Create backup and temp directory
		File f = new File(FileUtil.getRealPath(backupFilePath));
		f.mkdirs();
		f = new File(FileUtil.getRealPath(backupTempFilePath));
		f.mkdirs();
		deleteTempFiles();
	}

	/**
	 * The main Servlet method takes the URL parameter called "action"
	 */
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String action = request.getParameter("action");
		HttpSession session = request.getSession();
		if (!session.isNew()) {
			if ("upload".equals(action)) {

				doUpload(request, response);
				return;
			}

			/*
			 * Creates a zip in the backup folder
			 */
			if ("createZip".equals(action)) {
				response.getWriter().println("Creating XML Files");
				createXMLFiles();
				String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
				File zipFile = new File(FileUtil.getRealPath(backupFilePath + "/backup_" + x + "_.zip"));
				response.getWriter().println("Zipping up to file:" + zipFile.getAbsolutePath());
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(zipFile));

				zipTempDirectoryToStream(bout);
				response.getWriter().println("Done.");
				return;
			}

			/*
			 * Wipes out the dotCMS db
			 */
			if ("wipeOutDotCMSDatabase".equals(action)) {
				response.getWriter().println("Deleting everything");

			}

			/*
			 * Creates and downloads a zip folder
			 */
			if ("downloadZip".equals(action)) {

				String x = UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
				File zipFile = new File(FileUtil.getRealPath(backupFilePath + "/backup_" + x + "_.zip"));

				response.setHeader("Content-type", "");
				response.setHeader("Content-Disposition", "attachment; filename=" + zipFile.getName());

				createXMLFiles();

				zipTempDirectoryToStream(response.getOutputStream());
				return;
			}

		}

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head>");
		out.println("<title>");
		out.println("Import/Export dotCMS Content");
		out.println("</title>");
		out.println("<style type=\"text/css\">");
		out.println("@import \"/css/global.css\";");
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.println("<h1>Import/Export dotCMS Content</h1><hr size='1'>");
		out.println("<ul>");
		out.println("<li>");
		out.println("<a href='?action=createZip'>Backup to Zip File</a>");
		out.println("</li>");
		out.println("<li>");
		out.println("<a href='?action=wipeOutDotCMSDatabase'>Delete the dotCMS database</a>");
		out.println("</li>");
		out.println("<li>");
		out.println("<a href='?action=downloadZip'>Download Zip File</a>");
		out.println("</li>");
		out.println("</ul>");
		out.println("<form method='post' action='?action=upload' enctype='multipart/form-data'>");
		out.println("<table cellpadding='4'><tr><td style='font-size:12px;'>file to import:</td><td><input type='file' name='fileUpload'></td></tr>");
		out.println("<tr><td></td><td>");
		out.println("<input type='submit' class=\"core-button\" value='upload xml or zip file'></td></tr></table></form>");
		out.println("</body>");
		out.println("</html>");
	}

	/**
	 * Handles the file upload for the Servlet. It will send files to be
	 * unzipped if nessary
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void doUpload(HttpServletRequest request, HttpServletResponse response) throws IOException {
		deleteTempFiles();
		String tempdir = FileUtil.getRealPath(backupTempFilePath);

		MultipartRequest mpr;
		try {
			mpr = new MultipartRequest(request, tempdir, 1000000000);
			File importFile = mpr.getFile("fileUpload");

			/*
			 * Unzip zipped backups
			 */
			if (importFile != null && importFile.getName().toLowerCase().endsWith(".zip")) {

				InputStream in = new BufferedInputStream(new FileInputStream(importFile));
				ZipInputStream zin = new ZipInputStream(in);
				ZipEntry e;

				while ((e = zin.getNextEntry()) != null) {
					unzip(zin, e.getName());
				}
				zin.close();
				importFile.delete();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		}
		File f = new File(FileUtil.getRealPath(backupTempFilePath));
		String[] _tempFiles = f.list(new XMLFileNameFilter());
		PrintWriter out = response.getWriter();
		out.println("<pre>Found " + _tempFiles.length + " files to import");
		for (int i = 0; i < _tempFiles.length; i++) {
			File _importFile = new File(FileUtil.getRealPath(backupTempFilePath + "/" + _tempFiles[i]));
			System.gc();
			doXMLFileImport(_importFile, out);
			out.flush();
		}
		out.println("Done Importing");
		
	}

	/**
	 * This method takes an xml file and will try to import it via XStream and
	 * Hibernate
	 * 
	 * @param f
	 *            File to be parsed and imported
	 * @param out
	 *            Printwriter to write responses to Reponse Printwriter so this
	 *            method can write to screen.
	 */

	private void doXMLFileImport(File f, PrintWriter out) {
		BufferedInputStream _bin = null;
			try {
			    XStream _xstream = null;
				String _className = null;
				Class _importClass = null;
				HibernateUtil _dh = null;

				_className = f.getName().substring(0, f.getName().lastIndexOf("."));
				_xstream = new XStream(new DomDriver());
				_importClass = Class.forName(_className);
				out.println("Importing:\t" + _className);
				if (_importClass.equals(User.class)) {

				} else if (_importClass.equals(Company.class)) {

				} else {

				   _dh = new HibernateUtil(_importClass);
				   _bin = new BufferedInputStream(new FileInputStream(f));
				   List l = (List) _xstream.fromXML(_bin);
				   out.println("Found :\t" + l.size() + " " + _className + "(s)");
				   String id = _dh.getSession().getSessionFactory().getClassMetadata(_importClass).getIdentifierPropertyName();
				   for (int j = 0; j < l.size(); j++) {
						Object obj = l.get(j);
						if (UtilMethods.isSet(id)) {
							String prop = BeanUtils.getProperty(obj, id);

							try {
								Long myId = new Long(Long.parseLong(prop));
								_dh.saveWithPrimaryKey(obj, myId);
							} catch (Exception e) {
								_dh.saveWithPrimaryKey(obj, prop);
							}

						} else {
							_dh.save(obj);
						}
					}

				}	
		} catch (DotHibernateException e) {
		   // TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (ClassNotFoundException e1) {
			Logger.error(this,e1.getMessage(),e1);
		} catch (HibernateException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
		} finally {
			try {
				if (_bin != null) {
					_bin.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Logger.error(this,e.getMessage(),e);
			}
		}

	}

	/**
	 * This method will pull a list of all tables /classed being managed by
	 * hibernate and export them, one class per file to the backupTempFilePath
	 * as valid XML. It uses XStream to write the xml out to the files.
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	private void createXMLFiles() throws ServletException, IOException {

		deleteTempFiles();

		Set<Class> _tablesToDump = new HashSet<Class>();
		try {

			/* get a list of all our tables */
			Map map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				Class x = (Class) pairs.getKey();
				if (!x.equals(Inode.class) && !x.equals(Clickstream.class) && !x.equals(ClickstreamRequest.class))
					_tablesToDump.add(x);

			}
			XStream _xstream = null;
			HibernateUtil _dh = null;
			List _list = null;
			File _writing = null;
			BufferedOutputStream _bout = null;

			for (Class clazz : _tablesToDump) {
				_xstream = new XStream(new DomDriver());

				/*
				 * String _shortClassName =
				 * clazz.getName().substring(clazz.getName().lastIndexOf("."),clazz.getName().length());
				 * xstream.alias(_shortClassName, clazz);
				 */

				_writing = new File(FileUtil.getRealPath(backupTempFilePath + "/" + clazz.getName() + ".xml"));
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
				_dh = new HibernateUtil(clazz);
				_dh.setQuery("from " + clazz.getName());

				_list = _dh.list();
				Logger.info(this, "writing : " + _list.size() + " records to " + clazz.getName());
				_xstream.toXML(_list, _bout);

				_bout.close();
				_list = null;
				_dh = null;
				_bout = null;
				System.gc();
			}

			/* Run Liferay's Tables */
			/* Companies */
			_list = PublicCompanyFactory.getCompanies();
			_xstream = new XStream(new DomDriver());
			_writing = new File(FileUtil.getRealPath(backupTempFilePath + "/" + Company.class.getName() + ".xml"));
			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			_xstream.toXML(_list, _bout);
			_bout.close();
			_list = null;
			_bout = null;

			/* Users */
			_list = APILocator.getUserAPI().findAllUsers();
			_xstream = new XStream(new DomDriver());
			_writing = new File(FileUtil.getRealPath(backupTempFilePath + "/" + User.class.getName() + ".xml"));
			_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			_xstream.toXML(_list, _bout);
			_bout.close();
			_list = null;
			_bout = null;


		} catch (Exception e) {

			Logger.error(this,e.getMessage(),e);
		}

	}

	/**
	 * Will zip up all files in the tmp directory and send the result to the
	 * given OutputStream
	 * 
	 * @param out
	 *            OutputStream to write the zip files to
	 * @throws IOException
	 */
	private void zipTempDirectoryToStream(OutputStream out) throws IOException {

		byte b[] = new byte[512];
		ZipOutputStream zout = new ZipOutputStream(out);
		File f = new File(FileUtil.getRealPath(backupTempFilePath));
		String[] s = f.list();
		for (int i = 0; i < s.length; i++) {
			InputStream in = new BufferedInputStream(new FileInputStream(f = new File(FileUtil.getRealPath(backupTempFilePath + "/" + s[i]))));
			ZipEntry e = new ZipEntry(s[i].replace(File.separatorChar, '/'));
			zout.putNextEntry(e);
			int len = 0;
			while ((len = in.read(b)) != -1) {
				zout.write(b, 0, len);
			}
			zout.closeEntry();
			in.close();
		}
		zout.close();
		out.close();
	}

	/**
	 * Does what it says - deletes all files from the backupTempFilePath
	 * 
	 */
	private void deleteTempFiles() {
		File f = new File(FileUtil.getRealPath(backupTempFilePath));
		String[] _tempFiles = f.list();
		for (int i = 0; i < _tempFiles.length; i++) {
			f = new File(FileUtil.getRealPath(backupTempFilePath + "/" + _tempFiles[i]));
			f.delete();
		}

	}

	/**
	 * Takes a ZipInputStream and filename and will extract them to the
	 * backupTempFilePath
	 * 
	 * @param zin
	 *            ZipInputStream
	 * @param s
	 *            FileName to be extracted
	 * @throws IOException
	 */
	private void unzip(ZipInputStream zin, String s) throws IOException {
		Logger.info(this, "unzipping " + s);
		File f = new File(FileUtil.getRealPath(backupTempFilePath + "/" + s));
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
		byte[] b = new byte[512];
		int len = 0;
		while ((len = zin.read(b)) != -1) {
			out.write(b, 0, len);
		}
		out.close();
	}

	/**
	 * Simple FileNameFilter for XML files
	 * 
	 * @author will
	 * 
	 */
	private class XMLFileNameFilter implements FilenameFilter {

		public boolean accept(File f, String s) {
			if (s.toLowerCase().endsWith(".xml")) {
				return true;
			} else {
				return false;
			}
		}

	}

	/**
	 * This is not completed should delete all the dotcms data from an install
	 * 
	 */
	private void deleteDotCMS() {
	
			/* get a list of all our tables */
			try {
				Set<Class> _tablesToDump = new HashSet<Class>();
				Map map;

				map = HibernateUtil.getSession().getSessionFactory().getAllClassMetadata();

				Iterator it = map.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry pairs = (Map.Entry) it.next();
					ClassMetadata cmd = (ClassMetadata) pairs.getValue();

				}
			
		} catch (HibernateException e) {
			// TODO Auto-generated catch block
			Logger.error(this,e.getMessage(),e);
	    } catch (DotHibernateException e) {
		   // TODO Auto-generated catch block
	    	Logger.error(this,e.getMessage(),e);
	    }

	}
}
