package com.dotmarketing.quartz.job;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.csvreader.CsvReader;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.factories.TagFactory;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class implement the import contentlet thread to be use by the quartz job schedule task
 * @author Oswaldo Gallango
 * @version 1.0
 */
public class ContentImportThread implements Job{

	private final String fileExtension ="csv";
	private PrintStream myOutput = null;

	//Temp maps used to parse the file
	private HashMap<Integer, Field> headers;
	private HashMap<Integer, Field> keyFields;

	//Counters for the preview page
	private int newContentCounter;
	private int contentToUpdateCounter;

	//Counters for the results page
	private int contentUpdatedDuplicated;
	private int contentUpdated;
	private int contentCreated;
	@SuppressWarnings("unchecked")
	private HashSet keyContentUpdated = new HashSet ();
	private StringBuffer choosenKeyField;

	private int commitGranularity = 10;
	private int sleepTime = 200;
	@SuppressWarnings("unused")
	private Role CMSAdmin;

	@SuppressWarnings("unchecked")
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();


	public ContentImportThread() {
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Thread#destroy()
	 */
	public void destroy() {
		try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(ContentImportThread.class,e.getMessage(),e);
		}
	}

	/**
	 * This method run the import content thread
	 */
	@SuppressWarnings("deprecation")
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Logger.debug(this, "Running ContentImportThread - " + new Date());

		try {
			String structureName = (String)context.getMergedJobDataMap().get("structure");
			String path = (String)context.getMergedJobDataMap().get("path");
			String logPath = (String)context.getMergedJobDataMap().get("logpath");
			String updateByKeyField = (String)context.getMergedJobDataMap().get("updatebykeyfield");

			if(!logPath.endsWith(java.io.File.separator)){
				logPath = logPath+java.io.File.separator;
			}
			Logger.debug(this, "Structure:"+structureName+" - PATH:"+path+" - LOGPATH:"+logPath);

			File file = new File(path);

			if(file.exists()){

				/**
				 * Check if is a multiple or single file(s) import job
				 */
				if(file.exists() && file.isDirectory()){

					String[] fileList = file.list();
					for(String filename : fileList){
						/**
						 * Get only the csv files in the directory
						 */
						Logger.debug(this, "Reading File:"+filename);
						if(filename.endsWith(fileExtension)){

							if(!path.endsWith(java.io.File.separator)){
								path = path+java.io.File.separator;
							}
							File tempFile = new File(path+filename);
							Logger.debug(this, "Reading File in:"+path+filename);
							if(tempFile.exists()){
								try {

									/**
									 * Create log File
									 */
									openLogFile(logPath, tempFile);

									/**
									 * Get File data to import
									 */

									String[] keyfields = null;
									if (UtilMethods.isSet(updateByKeyField)) {
										keyfields = new String[] {updateByKeyField};
									} else {
										keyfields = new String[0];
									}

									Logger.debug(this, "Beginning File Import");
									byte[] bytes = getBytesFromFile(tempFile);
									importFile(bytes, structureName, keyfields, false, APILocator.getUserAPI().getSystemUser());
									Logger.debug(this, "File imported");
									/**
									 * move the file to the complete location folder
									 */
									moveFile(tempFile, path+"completed"+java.io.File.separator);

								}catch(Exception e){
									Logger.error(this, "File could not be processed. "+e.getMessage());
									myOutput.println("File could not be processed. "+e.getMessage());
									/**
									 * move the file to the complete location folder
									 */
									moveFile(tempFile, path+"notcompleted"+java.io.File.separator);
								}finally{
									myOutput.close();
								}
							}
						}
					}
				}else{
					try {
						/**
						 * Create log File
						 */
						openLogFile(logPath, file);

						/**
						 * Get File data to import
						 */

						String[] keyfields = null;
						if (UtilMethods.isSet(updateByKeyField)) {
							keyfields = new String[] {updateByKeyField};
						} else {
							keyfields = new String[0];
						}

						Logger.debug(this, "Beginning File Import");
						byte[] bytes = getBytesFromFile(file);
						importFile(bytes, structureName, keyfields, false, APILocator.getUserAPI().getSystemUser());
						Logger.debug(this, "File imported");
						/**
						 * move the file to the complete location folder
						 */
						moveFile(file, file.getPath().replaceAll(file.getName(), "")+"completed"+java.io.File.separator);

					}catch(Exception e){
						Logger.error(this, "File could not be processed. "+e.getMessage());
						myOutput.println("File could not be processed. "+e.getMessage());
						/**
						 * move the file to the complete location folder
						 */
						moveFile(file, file.getPath().replaceAll(file.getName(), "")+"notcompleted"+java.io.File.separator);
					}finally{
						myOutput.close();
					}
				}


			}else{
				Logger.error(this, "File "+path+" doesn't exist");
			}

			Logger.info(this,"The ContentImportThread Job End successfully"+(String)context.getJobDetail().getName());
		} catch (Exception e) {
			Logger.error(this, e.toString());
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this, e.getMessage(), e);
			}
		}
	}

	/**
	 *
	 * @param logPath Path where the log file is created
	 * @param file File to import
	 */
	private void openLogFile(String logPath, File file){
		try {

			File forlderPath = new File(logPath);
			if(!forlderPath.exists()){
				forlderPath.mkdirs();
			}

			File outputFile = new File(logPath+file.getName()+"_result_"+UtilMethods.dateToHTMLDate(new Date(), "yyyyMMddHHmmss")+".txt");
			if (!outputFile.exists())
				outputFile.createNewFile();

			myOutput = new PrintStream(new FileOutputStream(outputFile));

		} catch (IOException e) {
			Logger.error(this, e.getMessage());
		}
	}

	/**
	 * Move original file to destination path
	 * @param orig File to move
	 * @param newpath destination path
	 */
	private void moveFile(File orig, String newpath){

		try {

			File forlderPath = new File(newpath);
			if(!forlderPath.exists()){
				forlderPath.mkdirs();
			}

			File dest = new File(newpath+orig.getName()+"."+UtilMethods.dateToHTMLDate(new Date(), "yyyyMMddHHmmss"));
			if (!dest.exists())
				dest.createNewFile();

			FileInputStream is = new FileInputStream(orig);
			FileChannel channelFrom = is.getChannel();
			FileChannel channelTo = new FileOutputStream(dest).getChannel();
			channelFrom.transferTo(0, channelFrom.size(), channelTo);
			channelTo.force(false);
			channelTo.close();
			channelFrom.close();
			is.close();

			myOutput.println("File:"+orig.getAbsolutePath()+" move to: "+dest.getAbsolutePath());
			orig.delete();

		} catch (IOException e) {
			Logger.error(this, e.getMessage());
			myOutput.println("ERROR: "+e.getMessage());
		}
	}

	/**
	 * Get the file byte array
	 * @param file
	 * @return byte[]
	 * @throws IOException
	 */
	private byte[] getBytesFromFile(File file) throws IOException {
		byte[] currentData = new byte[0];
		FileInputStream is = new FileInputStream(file);
		int size = is.available();
		currentData = new byte[size];
		is.read(currentData);
		return currentData;
	}


	@SuppressWarnings({ "unchecked", "deprecation" })
	private void importFile(byte[] bytes, String structureName, String[] keyfields, boolean preview, User user)
	throws DotRuntimeException, DotDataException {


		try {
			CMSAdmin = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
		} catch (Exception e1) {
			Logger.error (this, "importFile: failed retrieving the CMSAdmin role.", e1);
			throw new DotRuntimeException (e1.getMessage());
		}


		Structure st = StructureFactory.getStructureByType(structureName.trim());
		if(!InodeUtils.isSet(st.getInode())){
			throw new DotRuntimeException ("Structure "+structureName+" doesn't exists");
		}

		//Initializing variables
		int lines = 0;
		int errors = 0;
		int lineNumber = 0;
		newContentCounter = 0;
		contentToUpdateCounter = 0;
		contentCreated = 0;
		contentUpdated = 0;
		contentUpdatedDuplicated = 0;
		keyContentUpdated = new HashSet ();
		choosenKeyField = new StringBuffer();

		headers = new HashMap<Integer, Field> ();
		keyFields = new HashMap<Integer, Field> ();


		//Parsing the file line per line
		Reader reader = null;
		try {
			reader = new InputStreamReader(new ByteArrayInputStream(bytes));
			CsvReader csvreader = new CsvReader(reader);
			csvreader.setSafetySwitch(false);

			if (csvreader.readHeaders()) {

				//Importing headers from the first file line
				importHeaders(csvreader.getHeaders(), st, keyfields, preview, user);
				lineNumber++;

				//Reading the whole file
				if (headers.size() > 0) {

					if (!preview)
						HibernateUtil.startTransaction();

					while (csvreader.readRecord()) {
						lineNumber++;
						try {
							lines++;
							Logger.debug(this, "Line " + lines + ": (" + csvreader.getRawRecord() + ").");
							//Importing a line
							importLine(csvreader.getValues(), st, preview, user, lineNumber);

							if (!preview && (lineNumber % commitGranularity == 0)) {
								HibernateUtil.commitTransaction();
								HibernateUtil.startTransaction();
							}

							if (!preview)
								Thread.sleep(sleepTime);
						} catch (DotRuntimeException ex) {
							String errorMessage = ex.getMessage();
							if(errorMessage.indexOf("Line #") == -1){
								errorMessage = "Line #"+lineNumber+" "+errorMessage;
							}
							myOutput.println(errorMessage);
							errors++;
							myOutput.println("Error line: " + lines + " (" + csvreader.getRawRecord()+ "). Line Ignored.");
						}
					}

					HibernateUtil.commitTransaction();

					myOutput.println(lines + " lines of data were read.");
					if (errors > 0)
						myOutput.println(errors + " input lines had errors.");

					if (newContentCounter > 0)
						myOutput.println("Approximately " + (newContentCounter) + " new content will be created.");
					if (contentToUpdateCounter > 0)
						myOutput.println("Approximately " + (contentToUpdateCounter) + " old content will be updated.");

					myOutput.println(contentCreated + " new \"" + st.getName() + "\" were created.");
					myOutput.println(contentUpdatedDuplicated + " \"" + st.getName() + "\" contentlets updated corresponding to "+contentUpdated+" repeated contents based on the key provided");

					if (errors > 0)
						myOutput.println(errors + " contentlets were ignored due to invalid information");

					if(lines == 0 || lines == errors){
						throw new DotRuntimeException (lines + " lines read correctly. " + errors + " errors found. Nothing was imported.");
					}

				} else {
					myOutput.println("No headers found on the file, nothing will be imported.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
			throw new DotRuntimeException (e.getMessage());
		} finally {

			Logger.info(this, lines + " lines read correctly. " + errors + " errors found.");
			myOutput.println(lines + " lines read correctly. " + errors + " errors found.");

			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {

				}
		}


	}

	private void importHeaders(String[] headerLine, Structure structure, String[] keyFieldsInodes, boolean preview,
			User user) throws DotRuntimeException {

		int importableFields = 0;

		//Importing headers and storing them in a hashmap to be reused later in the whole import process
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		for (int i = 0; i < headerLine.length; i++) {
			boolean found = false;
			String header = headerLine[i].replaceAll("'", "");
			for (Field field : fields) {
				if (field.getFieldName().equalsIgnoreCase(header)) {
					if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())){
						found = true;
						myOutput.println("Header: \"" + header+ "\" matches a field of type button, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.FILE.toString())){
						found = true;
						myOutput.println("Header: \"" + header+ "\" matches a field of type file, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())){
						found = true;
						myOutput.println("Header: \"" + header+ "\" matches a field of type image, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())){
						found = true;
						myOutput.println("Header: \"" + header+ "\" matches a field of type line divider, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
						found = true;
						myOutput.println("Header: \"" + header+ "\" matches a field of type tab divider, this column of data will be ignored.");
					}
					else {
						found = true;
						headers.put(i, field);
						for (String fieldInode : keyFieldsInodes) {
							if (fieldInode.equalsIgnoreCase(field.getInode()))
								keyFields.put(i, field);
						}
						break;
					}
				}
			}
			if (!found) {
				myOutput.println("Header: \"" + header+ "\" doesn't match any structure field, this column of data will be ignored.");
			}
		}

		for (Field field : fields) {
			if (isImportableField(field)){
				importableFields++;
			}
		}

		//Checking keyField selected by the user against the headers
		for (String keyField : keyFieldsInodes) {
			boolean found = false;
			for (Field headerField : headers.values()) {
				if (headerField.getInode().equalsIgnoreCase(keyField)) {
					found = true;
					break;
				}
			}
			if (!found) {
				myOutput.println("Key field: \"" + FieldFactory.getFieldByInode(keyField).getFieldName()
						+ "\" choosen doesn't match any of the headers found in the file.");
			}
		}

		if (keyFieldsInodes.length == 0)
			myOutput.println("No key fields were choosen, it could give to you duplicated content.");

		//Adding some messages to the results
		if (importableFields == headers.size()) {
			myOutput.println("All the " + headers.size() + " headers found on the file matches all the structure fields.");
		} else {
			myOutput.println("Not all the structure fields were matched against the file headers. Some content fields could be left empty.");
			if (headers.size() > 0){
				myOutput.println(headers.size() + " headers found on the file matches the structure fields.");
			} else {
				myOutput.println("No headers found on the file that match any of the structure fields. The process will not import anything.");
				throw new DotRuntimeException("No headers found on the file that match any of the structure fields. The process will not import anything.");
			}

		}
	}

	@SuppressWarnings("unchecked")
	private void importLine(String[] line, Structure structure, boolean preview, User user, int lineNumber) throws DotRuntimeException {

		try {
			//Building a values HashMap based on the headers/columns position
			HashMap<Integer, Object> values = new HashMap<Integer, Object>();
			Set<Category> categories = new HashSet<Category> ();
			for (Integer column : headers.keySet()) {
				Field field = headers.get(column);
				if (line.length < column) {
					throw new DotRuntimeException("Incomplete line found, the line #" + lineNumber +
					" doesn't contain all the required columns.");
				}
				String value = line[column];
				Object valueObj = value;
				if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
					if (field.getFieldContentlet().startsWith("date")) {
						if(UtilMethods.isSet(value)) {
							try { valueObj = parseExcelDate(value) ;} catch (ParseException e) {
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() +
										", value: " + value + ", couldn't be parsed as any of the following supported formats: " +
										printSupportedDateFormats());
							}
						} else {
							valueObj = null;
						}
					}
				} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
					if (field.getFieldContentlet().startsWith("date")) {
						if(UtilMethods.isSet(value)) {
							try { valueObj = parseExcelDate(value) ;} catch (ParseException e) {
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() +
										", value: " + value + ", couldn't be parsed as any of the following supported formats: " +
										printSupportedDateFormats());
							}
						} else {
							valueObj = null;
						}
					}
				} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
					if (field.getFieldContentlet().startsWith("date")) {
						if(UtilMethods.isSet(value)) {
							try { valueObj = parseExcelDate(value) ;} catch (ParseException e) {
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() +
										", value: " + value + ", couldn't be parsed as any of the following supported formats: " +
										printSupportedDateFormats());
							}
						} else {
							valueObj = null;
						}
					}
				} else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())) {
					valueObj = value;
					if(UtilMethods.isSet(value)) {
						String[] categoryKeys = value.split(",");
						for(String catKey : categoryKeys) {
							Category cat = catAPI.findByKey(catKey.trim(), user, false);
							if(cat == null)
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() +
										", value: " + value + ", invalid category key found, line will be ignored.");
							categories.add(cat);
						}
					}
				}
				else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
						field.getFieldType().equals(Field.FieldType.SELECT.toString()) ||
						field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ||
						field.getFieldType().equals(Field.FieldType.RADIO.toString())
				) {
					valueObj = value;
					if(UtilMethods.isSet(value))
					{


						String fieldEntriesString = field.getValues();
						String[] fieldEntries = fieldEntriesString.split("\n");
						boolean found = false;
						for(String fieldEntry : fieldEntries)
						{
							String entryValue = fieldEntry.split("\\|")[1].trim();

							if(entryValue.equals(value) || value.contains(entryValue))
							{
								found = true;
								break;
							}
						}
						if(!found)
						{
							throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() +
									", value: " + value + ", invalid value found, line will be ignored.");
						}
					}
					else {
						valueObj = null;
					}
				}
				else if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
					if (value.length() > 255)
						value = value.substring(0, 255);
					valueObj = value;
				} else {
					valueObj = value;
				}
				values.put(column, valueObj);
			}

			//Searching contentlets to be updated by key fields
			List<Contentlet> contentlets = new ArrayList<Contentlet>();
			String conditionValues = "";
			StringBuffer buffy = new StringBuffer();
			buffy.append("+structureInode:" + structure.getInode() + " +working:true +deleted:false");


			if (keyFields.size() > 0) {

				for (Integer column : keyFields.keySet()) {
					Field field = keyFields.get(column);
					Object value = values.get(column);
					String text = null;
					if (value instanceof Date || value instanceof Timestamp) {
						SimpleDateFormat formatter = null;
						if(field.getFieldType().equals(Field.FieldType.DATE.toString())
								|| field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
						{
						    DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
							text = df.format((Date)value);
						} else if(field.getFieldType().equals(Field.FieldType.TIME.toString())) {
						    DateFormat df = new SimpleDateFormat("HHmmss");
                            text = df.format((Date)value);
						} else {
							formatter = new SimpleDateFormat();
							text = formatter.format(value);
							Logger.warn(getClass(),"importLine: field's date format is undetermined.");
						}
					} else {
						text = value.toString();
					}
					if(!UtilMethods.isSet(text)){
						throw new DotRuntimeException("Line #" + lineNumber + " key field "+field.getFieldName()+" is required since it was defined as a key\n");
					}else{
						buffy.append(" +" +field.getFieldContentlet()+ ":"+ escapeLuceneSpecialCharacter(text));
						conditionValues += conditionValues + value + "-";
					}

					if(choosenKeyField.indexOf(field.getFieldName()) == -1){
						choosenKeyField.append(", "+field.getFieldName());
					}
				}
				contentlets = conAPI.checkoutWithQuery(buffy.toString(), user, true);
			}


			//Creating/updating content
			boolean isNew = false;
			if (contentlets.size() == 0) {
				newContentCounter++;
				isNew = true;
				//if (!preview) {
				Contentlet newCont = new Contentlet();
				newCont.setStructureInode(structure.getInode());
				newCont.setLanguageId(langAPI.getDefaultLanguage().getId());
				contentlets.add(newCont);
				//}
			} else {
				if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues)) {
					contentToUpdateCounter += contentlets.size();
					if (preview)
						keyContentUpdated.add(conditionValues);
				}
				if (contentlets.size() > 0) {
					myOutput.println("Line #" + lineNumber + ". The key fields choosen match more than one content, in this case: "
							+ " matches: " + contentlets.size() + " different content(s), looks like the key fields choosen " +
					"aren't a real key.");
				}

			}


			for (Contentlet cont : contentlets)
			{
				//Fill the new contentlet with the data
				for (Integer column : headers.keySet()) {
					Field field = headers.get(column);
					Object value = values.get(column);
					conAPI.setContentletProperty(cont, field, value);
					if (field.getFieldType().equals(Field.FieldType.TAG.toString()) &&
							value instanceof String) {
						String[] tags = ((String)value).split(",");
						for (String tag : tags) {
							TagFactory.addTagInode((String)tag.trim(), cont.getInode(), "");
						}
					}
				}

				//Check the new contentlet with the validator
				try
				{
					conAPI.validateContentlet(cont,new ArrayList<Category>(categories));
				}
				catch(DotContentletValidationException ex)
				{
					StringBuffer sb = new StringBuffer("Line #" + lineNumber + " contains errors\n");
					HashMap<String,List<Field>> errors = (HashMap<String,List<Field>>) ex.getNotValidFields();
					Set<String> keys = errors.keySet();
					for(String key : keys)
					{
						sb.append(key + ": ");
						List<Field> fields = errors.get(key);
						for(Field field : fields)
						{
							sb.append(field.getFieldName() + ",");
						}
						sb.append("\n");
					}
					throw new DotRuntimeException(sb.toString());
				}

				//If not preview save the contentlet
				if (!preview)
				{
					conAPI.checkin(cont, new ArrayList<Category>(categories), new ArrayList<Permission>(), user, false);
					APILocator.getVersionableAPI().setWorking(cont);
					APILocator.getVersionableAPI().setLive(cont);
				}

				if (isNew)
					contentCreated++;
				else
					if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues)) {
						contentUpdated++;
						contentUpdatedDuplicated++;
						keyContentUpdated.add(conditionValues);
					}else{
						contentUpdatedDuplicated++;
					}
			}

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
			throw new DotRuntimeException(e.getMessage());
		}

	}

	public static final String[] IMP_DATE_FORMATS = new String[] { "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy", "MM/dd/yyyy hh:mm aa", "MM/dd/yyyy HH:mm",
		"MM/dd/yy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy", "MM/dd/yyyy",
		"hh:mm:ss aa", "HH:mm:ss", "hh:mm aa"};

	private static String printSupportedDateFormats () {
		StringBuffer ret = new StringBuffer("[ ");
		for (String pattern : IMP_DATE_FORMATS) {
			ret.append(pattern + ", ");
		}
		ret.append(" ] ");
		return ret.toString();
	}

	private Date parseExcelDate (String date) throws ParseException
	{
		return DateUtil.convertDate(date, IMP_DATE_FORMATS);
	}

	private boolean isImportableField(Field field) {
		return !(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
				field.getFieldType().equals(Field.FieldType.FILE.toString()) ||
				field.getFieldType().equals(Field.FieldType.BUTTON.toString()) ||
				field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
				field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()));
	}

	/**
	 * Escape lucene reserved characters
	 * @param text
	 * @return String
	 */
	private String escapeLuceneSpecialCharacter(String text){
		text = text.replaceAll("\\[","\\\\[").replaceAll("\\]","\\\\]");
		text = text.replaceAll("\\{","\\\\{").replaceAll("\\}","\\\\}");
		text = text.replaceAll("\\+","\\\\+").replaceAll(":","\\\\:");
		text = text.replaceAll("\\*","\\\\*").replaceAll("\\?","\\\\?");
		text = text.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)");
		text = text.replaceAll("&&","\\\\&&").replaceAll("\\|\\|","\\\\||");
		text = text.replaceAll("!","\\\\!").replaceAll("\\^","\\\\^");
		text = text.replaceAll("-","\\\\-").replaceAll("~","\\\\~");
		text = text.replaceAll("\"","\\\"");

		return text;
	}


}
