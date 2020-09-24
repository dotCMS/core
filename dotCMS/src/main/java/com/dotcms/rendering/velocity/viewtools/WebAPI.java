package com.dotcms.rendering.velocity.viewtools;
import com.dotcms.rendering.velocity.services.VelocityType;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;


import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.XMLUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class WebAPI implements ViewTool {

	private HttpServletRequest request;
	private HttpServletResponse response;
	private IdentifierAPI identAPI = APILocator.getIdentifierAPI();

	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	private UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

	Context ctx;
	User user = null;

    boolean ADMIN_MODE=false;
    boolean PREVIEW_MODE=false;
    boolean EDIT_MODE = false;
    long langId;
    long defaultLang  = langAPI.getDefaultLanguage().getId();
	/**
	 * @param  obj  the ViewContext that is automatically passed on view tool initialization, either in the request or the application
	 * @return
	 * @see         ViewTool, ViewContext
	 */
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		this.response = context.getResponse();
		
		ctx = context.getVelocityContext();

		this.langId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
		try {
		    user =  userAPI.getLoggedInUser(request);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}


		
		PageMode mode = PageMode.get(request);
          ADMIN_MODE = mode.isAdmin;
          PREVIEW_MODE = mode==PageMode.PREVIEW_MODE;
          EDIT_MODE = mode==PageMode.EDIT_MODE;
        
	}

	// Utility Methods
	public int parseInt(String num) {
		try {
			return Integer.parseInt(num);
		} catch (Exception e) {
			return 0;
		}
	}

	public long parseLong(String num) {
		try {
			return Long.parseLong(num);
		} catch (Exception e) {
			return 0;
		}
	}

	//Needed to support any kind of conversion from velocity
	public int parseInt(int num) {
		return num;
	}

	//Needed to support any kind of conversion from velocity
	public long parseLong(long num) {
		return num;
	}

	//Needed to support any kind of conversion from velocity
	public int parseInt(long num) {
		return (int)num;
	}

	//Needed to support any kind of conversion from velocity
	public long parseLong(int num) {
		return num;
	}

	public int castToInt(long num) {
		return (int) num;
	}

	public String toString(long num) {
		try {
			return Long.toString(num);
		} catch (Exception e) {
			Logger.error(this.getClass(), "toString(long) Error: " + e);
			return "0";
		}
	}



	static final String[] WEEKDAY_NAME = { "None", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
	"Saturday" };

	public String dateToHTMLPrettyDay(int year, int month, int day) {
		GregorianCalendar cal = new GregorianCalendar(year, month, day);
		String dayName = WEEKDAY_NAME[cal.get(java.util.Calendar.DAY_OF_WEEK)] + " " + day;
		return dayName;
	}

	public String dateToHTML(Date myDate) {
		return UtilMethods.dateToHTMLDate(myDate);
	}

	public String dateToHTMLDateTimeRange(Date from, Date to) {
		return UtilMethods.dateToHTMLDateTimeRange(from, to, GregorianCalendar.getInstance().getTimeZone());
	}

	public String dateToHTMLTimeRange(Date from, Date to) {

		String ret = UtilMethods.dateToHTMLTime(from, GregorianCalendar.getInstance().getTimeZone());
		if (from.compareTo(to) != 0) {
			ret += " - " + UtilMethods.dateToHTMLTime(to, GregorianCalendar.getInstance().getTimeZone());
		}
		return ret;
	}

	public String dateToHTML(Date myDate, String format) {
		return UtilMethods.dateToHTMLDate(myDate, format);
	}

	public String trimToUpper(String input) {
		return input.trim().toUpperCase();
	}

	public String trim(String input) {
		return input.trim();
	}

	public String htmlLineBreak(String input) {
		return UtilMethods.htmlLineBreak(input);
	}

	public boolean isSet(String input) {
		return UtilMethods.isSet(input);
	}

	public String obfuscateCreditCard(String ccnum) {
		return UtilMethods.obfuscateCreditCard(ccnum);
	}

	public String capitalize(String str) {
		if (str.length() > 1) {
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		}
		return str;
	}

	public List<String> splitString(String str, String sep) {
		ArrayList<String> list = new ArrayList<String>();
		String[] splitArr = str.split(sep);
		for (int i = 0; i < splitArr.length; i++) {
			list.add(splitArr[i]);
		}
		return list;
	}

	public String encodeURL(String url) {
		return UtilMethods.encodeURL(url);
	}

	public String htmlEncode(String html)
	{
		return UtilHTML.htmlEncode(html);
	}

	public String getIdentifierInode(String childInode) throws DotIdentifierStateException, DotDataException {
		try {
			return identAPI.findFromInode(childInode).getInode();
		} catch (NumberFormatException e) {
			return "";
		}
	}

	public String getShortMonthName(int month) {
		return UtilMethods.getShortMonthName(month);
	}

	public String getShortMonthName(String month) {
		return UtilMethods.getShortMonthName(month);
	}



	public String prettyShortenString(String text, String maxLength) {
		return UtilMethods.prettyShortenString(text, Integer.parseInt(maxLength));
	}

	public String dateToLongPrettyHTMLDate(Date myDate) {
		return UtilMethods.dateToLongPrettyHTMLDate(myDate);
	}

	public boolean canParseContent(String identifier, boolean isWorking ) {
	  return true;
	}
	public boolean canParseContent(String parsePath) {
	  return true;
	}


	public String getContentIdentifier(String parsePath) {
		StringTokenizer st = new StringTokenizer(parsePath, "/.");
		String x = "0";
		while (st.hasMoreTokens()) {
			String y = st.nextToken();
			if (st.hasMoreTokens()) {
				x = y;
			}
		}
		String language = "";
		if (x.indexOf("_") > -1) {
			Logger.debug(this, "x=" + x);
			language = x.substring(x.indexOf("_") + 1, x.length());
			Logger.debug(this, "language=" + language);
			x = x.substring(0, x.indexOf("_"));
			Logger.debug(this, "x=" + x);
		}
		return x;
	}

	/*
	 * Use only in preview mode, this methods hits the db!!
	 * http://jira.dotmarketing.net/browse/DOTCMS-2110
	 */
	public String getContentInode(String parsePath) {
		ContentletAPI conAPI = APILocator.getContentletAPI();
		String id = "";
		if(parsePath.indexOf(VelocityType.CONTENT.fileExtension) == -1 || parsePath.indexOf(VelocityType.CONTENT_MAP.fileExtension) == -1){
			id = parsePath;
		}else{
			id = getContentIdentifier(parsePath);
		}
		Identifier iden = new Identifier();
		try {
			iden = APILocator.getIdentifierAPI().find(id);
		} catch (DotDataException e1) {
			Logger.error(WebAPI.class, e1.getMessage(), e1);
		}
		String idenInode = iden.getInode();
		Language language = langAPI.getDefaultLanguage();
		long languageId = language.getId();
		Contentlet cont = null;
		try{
			cont = conAPI.findContentletByIdentifier(idenInode, true, languageId, user, true);
		}catch(Exception e){
			Logger.debug(this, e.getMessage());
			return "0";
		}

		return  cont.getInode();
	}

	public String getContentPermissions(String parsePath) {
		String id = "";
		if(parsePath.indexOf(VelocityType.CONTENT.fileExtension) == -1){
			id = parsePath;
		}else{
			id = getContentIdentifier(parsePath);
		}
		return String.valueOf(ctx.get("EDIT_CONTENT_PERMISSION" + id));
	}


	public String xmlEscape(String x){

		return XMLUtils.xmlEscape(x);

	}



	public int stringLength(String text){

		if(UtilMethods.isSet(text)){
			return text.length();
		}
		return 0;
	}

	public String subString(String text, int begin, int end){

		if(UtilMethods.isSet(text)){
			text = text.substring(begin, end);
		}

		return text;
	}

	public boolean isImage(String text){

		return UtilMethods.isImage(text);
	}

	public String getAssetPath(final String path) throws DotStateException, DotDataException, PortalException, SystemException, DotSecurityException{
	    Host host =  WebAPILocator.getHostWebAPI().getCurrentHost(request);
	    
	    if(path.startsWith("//")){
	      String[] paths = path.split("/");
	      String hostname = paths[2];
	      StringWriter newPath = new StringWriter();
	      for(int i=3;i<paths.length;i++){
	        newPath.append("/").append(paths[i]);
	      }
	      host = WebAPILocator.getHostWebAPI().resolveHostName(hostname, user, !ADMIN_MODE);
	      return this.getAssetPath(newPath.toString(), host.getIdentifier());
	    }
	    
		return this.getAssetPath(path, host.getIdentifier());
	}

	public String getAssetPath(String path, String hostStr) throws DotDataException, DotSecurityException{
	  Host host = APILocator.getHostAPI().find(hostStr, user, !ADMIN_MODE);
	  return this.getAssetPath(path, host);
	}
	
	

	public String getAssetPath(final String path, final Host host){
		try{
          if(path == null ) return null;
		  Identifier ident = identAPI.find(host, path);
		  Optional<ContentletVersionInfo> cvi;
		  
		  // Language Fall Back
		  cvi = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), langId);

		  if(!cvi.isPresent() && defaultLang != langId){
		    cvi = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), defaultLang);
		  }

		  if(!cvi.isPresent()) {
		  	throw new DotDataException("Can't find Contentlet-version-info. Identifier: " + ident.getId() + ". Lang:" + defaultLang);
		  }
		  String conInode = (PREVIEW_MODE && EDIT_MODE) ? cvi.get().getWorkingInode()
				  : cvi.get().getLiveInode();
		  FileAsset file  = APILocator.getFileAssetAPI().fromContentlet(APILocator.getContentletAPI().find(conInode,  user, true));
		  
		  return APILocator.getFileAssetAPI().getRealAssetPath(conInode, file.getUnderlyingFileName());

		}catch (Exception e) {
			Logger.warn(this, e.getMessage());
			Logger.debug(this, e.getMessage(),e);
			return null;
		}
	}

	public String getAssetInode(String path){
		Host host = null;
		try{
			return getAssetInode(path, host);
		}catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
			return null;
		}
	}

	public String getAssetInode(String path, String hostId){
		try{
			return getAssetInode(path, APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), true));
		}catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
			return null;
		}
	}

	public String getAssetInode(String path, Host host){
		try{
			if(!UtilMethods.isSet(host)){
				host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			}
			if(path == null ) return "";
			path = path.replaceAll("\\.\\.", "");
			path = path.replaceAll("WEB-INF", "");

			while(path.indexOf("//") > -1){
				path = path.replaceAll("//", "/");
			}
			
			HttpSession session = request.getSession();

			Identifier id = APILocator.getIdentifierAPI().find(host, path);
			if(id!=null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")){
				User localUser = APILocator.getUserAPI().getAnonymousUser();
				if( user!=null){
					localUser =  user;
				}

				Contentlet cont;
				if(ADMIN_MODE && (EDIT_MODE || PREVIEW_MODE)){
					cont = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), localUser, true);
				}else{
					cont = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), localUser, true);
				}
				if(cont!=null && InodeUtils.isSet(cont.getInode())){
					return cont.getInode();
				}
			}
			return null;
		}catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
			return null;
		}
	}

	/**
	 * This method gives to you the sub URI of a complete request URI given the deepness desired.
	 * E.G. URI = /alumni/relations/aaa/index.dot then getSubURI(2) = /alumni/relations/
	 * @param deepness
	 * @return
	 */
	public String getSubURIByDepth (int depth) {
		String subURI = "/";
		String completeURI = request.getRequestURI();
		String[] splittedURI = completeURI.split("\\/");
		for (int i = 1; i <= depth; i++)
			if(i < splittedURI.length)
				subURI += splittedURI[i] + "/";
		return subURI;
	}

	public String getConfigVar(String varName) {
		return Config.getStringProperty(varName);
	}

	public static String formatDate(Date date,String format)
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			String returnValue = sdf.format(date);
			return returnValue;
		}
		catch(Exception ex)
		{
			Logger.debug(WebAPI.class,ex.toString());
			return "error date";
		}
	}

	public static void isCreateFormEmpty(Object form, HttpServletResponse response){


		try {

			if(form == null){
				response.sendRedirect("/dotCMS/createAccount");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Logger.error(WebAPI.class,e.getMessage(),e);
		}

	}

	public String toMonthFormat(int month)
	{
		return UtilMethods.getMonthName(month);
	}

	public boolean isPartner()
	{
		boolean isPartner = false;
		if (request.getSession().getAttribute("isPartner") != null &&
				request.getSession().getAttribute("isPartner").equals("true"))
		{
			isPartner = true;
		}
		return isPartner;
	}

	public boolean equalsNumbers(float one,float two)
	{
		return one == two;
	}

	public String toPriceFormat(double price)
	{
		return UtilMethods.toPriceFormat(price);
	}

	public String toPriceFormat(float price)
	{
		return UtilMethods.toPriceFormat(price);
	}

	public String getUserFullName()
	{
		String fullName = "";
		if (request.getSession().getAttribute(WebKeys.CMS_USER) != null)
		{
			User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
			fullName = user.getFullName();
		}
		return fullName;
	}

	public User getLoggedUser()
	{
		if (request.getSession().getAttribute(WebKeys.CMS_USER) != null)
		{
			return (User) request.getSession().getAttribute(WebKeys.CMS_USER);
		}
		return null;
	}

	public String getUserEmail()
	{
		String email = "";
		if (request.getSession().getAttribute(WebKeys.CMS_USER) != null)
		{
			User user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
			email = UtilMethods.getUserEmail(user);
		}
		return email;
	}


	public String toCCFormat(String creditCard)
	{
		String shortCreditCard = "";
		shortCreditCard = UtilMethods.obfuscateCreditCard(creditCard);
		return shortCreditCard;
	}



	public Inode getLiveFileAsset (Identifier id) throws DotStateException, DotDataException, DotSecurityException {
		return (Inode) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(),false);
	}

	@Deprecated
	public Inode getLiveFileAsset (long identifierInode) throws DotDataException, DotStateException, DotSecurityException {
		return getLiveFileAsset (String.valueOf(identifierInode));
	}

	public Inode getLiveFileAsset (String identifierInode) throws DotDataException, DotStateException, DotSecurityException {
		return getLiveFileAsset(APILocator.getIdentifierAPI().find(identifierInode));
	}

	public static String javaScriptify(String fixme) {
		fixme = UtilMethods.javaScriptify(fixme);
		return UtilMethods.escapeSingleQuotes(fixme);
	}

	public int getActualYear () {
		Calendar cal = new GregorianCalendar ();
		return cal.get(Calendar.YEAR);
	}

	public boolean isBiggerThan(String float1, String float2) {
		try {
			return (Float.parseFloat(float1) > Float.parseFloat(float2));
		} catch (Exception e) {
			return false;
		}
	}

	public String getCurrentMonth()
	{
		Date today = new Date();
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(today);
		int month = gc.get(GregorianCalendar.MONTH);
		return ("" + month);
	}

	public String todayDateString() {
		GregorianCalendar cal = new GregorianCalendar();
		int month = cal.get(GregorianCalendar.MONTH);
		month++;
		int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
		int year = cal.get(GregorianCalendar.YEAR);

		String monthString = (month < 9 ? "0" + Integer.toString(month) : Integer.toString(month));
		String dayString = (day < 9 ? "0" + Integer.toString(day) : Integer.toString(day));

		return monthString + "/" + dayString + "/" + year;
	}

	public String startWeekDateString()
	{
		GregorianCalendar cal = new GregorianCalendar();
		while(cal.get(GregorianCalendar.DAY_OF_WEEK) != GregorianCalendar.MONDAY)
		{
			cal.add(GregorianCalendar.DAY_OF_YEAR,-1);
		}
		int month = cal.get(GregorianCalendar.MONTH);
		month++;
		int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
		int year = cal.get(GregorianCalendar.YEAR);

		String monthString = (month < 9 ? "0" + Integer.toString(month) : Integer.toString(month));
		String dayString = (day < 9 ? "0" + Integer.toString(day) : Integer.toString(day));

		return monthString + "/" + dayString + "/" + year;
	}

	public String endWeekDateString()
	{
		GregorianCalendar cal = new GregorianCalendar();
		while(cal.get(GregorianCalendar.DAY_OF_WEEK) != GregorianCalendar.SUNDAY)
		{
			cal.add(GregorianCalendar.DAY_OF_YEAR,1);
		}
		int month = cal.get(GregorianCalendar.MONTH);
		month++;
		int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
		int year = cal.get(GregorianCalendar.YEAR);

		String monthString = (month < 9 ? "0" + Integer.toString(month) : Integer.toString(month));
		String dayString = (day < 9 ? "0" + Integer.toString(day) : Integer.toString(day));

		return monthString + "/" + dayString + "/" + year;
	}

	public String startMonthDateString()
	{
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(GregorianCalendar.DAY_OF_MONTH,1);
		int month = cal.get(GregorianCalendar.MONTH);
		month++;
		int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
		int year = cal.get(GregorianCalendar.YEAR);

		String monthString = (month < 9 ? "0" + Integer.toString(month) : Integer.toString(month));
		String dayString = (day < 9 ? "0" + Integer.toString(day) : Integer.toString(day));

		return monthString + "/" + dayString + "/" + year;
	}

	public String endMonthDateString()
	{
		GregorianCalendar cal = new GregorianCalendar();
		int month = cal.get(GregorianCalendar.MONTH);
		month++;
		int day = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		int year = cal.get(GregorianCalendar.YEAR);

		String monthString = (month < 9 ? "0" + Integer.toString(month) : Integer.toString(month));
		String dayString = (day < 9 ? "0" + Integer.toString(day) : Integer.toString(day));

		return monthString + "/" + dayString + "/" + year;
	}

	public String isInArray(String value,String array)
	{
		String[] arrayString = array.split(",");
		return isInArray(value,arrayString);
	}

	public String isInArray(String value,String[] array)
	{
		String returnValue = "";
		for(int i = 0;array != null && i < array.length;i++)
		{
			if(array[i].equals(value))
			{
				returnValue = "CHECKED";
				break;
			}
		}
		return returnValue;
	}

	public String isInArray(String[] values,String[] array)
	{
		String returnValue = "";
		for (String val : values)
			for(int i = 0;array != null && i < array.length;i++)
			{
				if(array[i].equals(val))
				{
					return "CHECKED";
				}
			}
		return returnValue;
	}








	public User getUserByLongLiveCookie() {

		String _dotCMSID = "";
		if(!UtilMethods.isSet(UtilMethods.getCookieValue(request.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE))) {
			Cookie idCookie = CookieUtil.createCookie();
		}
		_dotCMSID = UtilMethods.getCookieValue(request.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);
		UserProxy up;
		try {
			up = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxyByLongLiveCookie(_dotCMSID,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		if (UtilMethods.isSet(up) &&  UtilMethods.isSet(up.getUserId())) {
			try {
				return APILocator.getUserAPI().loadUserById(up.getUserId(),APILocator.getUserAPI().getSystemUser(),false);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(),e);
				return null;
			}
		}
		else {
			return null;
		}

	}

	public Object getCategoriesByNonLoggedUser() {

		HttpSession session = request.getSession();

		return session.getAttribute(com.dotmarketing.util.WebKeys.NON_LOGGED_IN_USER_CATS);

	}

	public void setVelocityVar (String varName, Object value) {
		ctx.put(varName, value);
	}

	public String getIdentifierByURI(String URI) throws PortalException, SystemException, DotDataException, DotSecurityException{
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		Identifier id = APILocator.getIdentifierAPI().find(host, URI);
		return id.getInode();
	}

	/**
	 * This method return the identifier object from cache
	 * @param childInode
	 * @return Identifier
	 * @author Oswaldo Gallango
	 * @version 1.0
	 * @throws DotDataException
	 * @throws DotIdentifierStateException
	 * @since 1.5
	 */
	public Identifier getIdentifierByInode(String childInode) throws DotIdentifierStateException, DotDataException {
		try {
			return identAPI.findFromInode(childInode);
		} catch (NumberFormatException e) {
			return new Identifier();
		}
	}



	/**
	 * This Method set the HttpServletResponse status with the specified
	 * error code. is the value pass is equals to zero, the status is set
	 * with 404 error
	 * @param code The HttpServletResponse error code
	 * @author Oswaldo Gallango
	 * @version 1.0
	 * @since 1.5
	 */
	public void setErrorResponseCode(int code){
		if(code == 0){
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}else{
			response.setStatus(code);
		}
	}

	@Deprecated
	public boolean doesUserHasPermissionOverFile (long fileInode, int permission) throws DotDataException  {
		return doesUserHasPermissionOverFile (String.valueOf(fileInode),permission);
	}

	public boolean doesUserHasPermissionOverFile (String fileInode, int permission) throws DotDataException {

		Permissionable fileAsset = null;
		Identifier ident = getIdentifierByInode(fileInode);
		if(ident.getAssetType().equals("contentlet")){
			try {
				fileAsset = APILocator.getContentletAPI().find(fileInode, user, false);
			} catch (DotSecurityException e) {
				Logger.error(this, e.getMessage());
				return false;
			}
		}
		return perAPI.doesUserHavePermission(fileAsset, permission, user, false);
	}
	
	
	
	
	public Host resolveHostName(String hostName){
		
		try{
			return APILocator.getHostAPI().resolveHostName(hostName, user, true);
		}
		catch(Exception e){
			Logger.warn(this.getClass(), e.getMessage());
		}
		return null;
		
		
		
	}
	
	
	public Identifier findIdentifierById(String id) {
	    try{
            return identAPI.find(id);
        }
        catch(Exception e){
            Logger.warn(this.getClass(), e.getMessage());
        }
        return null;
	}
	
	public boolean contentHasLiveVersion(String identifier) throws Exception {
	    long lang=Long.parseLong((String)request.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE));
	    Optional<ContentletVersionInfo> cvi=APILocator.getVersionableAPI().getContentletVersionInfo(identifier, lang);
	    return cvi.isPresent() && UtilMethods.isSet(cvi.get().getLiveInode());
	}
	
}
