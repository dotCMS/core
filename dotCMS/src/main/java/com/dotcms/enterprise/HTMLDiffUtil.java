package com.dotcms.enterprise;

import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.repackage.org.eclipse.compare.rangedifferencer.RangeDifference;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.diff.HtmlCleaner;
import com.dotmarketing.util.diff.XslFilter;
import com.dotmarketing.util.diff.html.HTMLDiffer;
import com.dotmarketing.util.diff.html.HtmlSaxDiffOutput;
import com.dotmarketing.util.diff.html.TextNodeComparator;
import com.dotmarketing.util.diff.html.dom.DomTreeBuilder;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class HTMLDiffUtil {

    private final static String CSSES_REGEX = "<link[^>]+?(stylesheet)[^>]*?>";
    private final static String HREF_REGEX = "href=[\"'](.*?)[\"']";
    
    
	/**
	 * This method will return an HTML representation of the diff between the live and working versions of an HTML page.
	 * @param page the identifier of the HTMLPage to be diffed
	 * @param user the user viewing the diff
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static String htmlDiffPage(IHTMLPage page, User user) throws IOException, SAXException, TransformerConfigurationException, DotSecurityException, DotDataException{
		return htmlDiffPage(page, user, null, page.getLanguageId());
	}

	/**
	 * This method will return an HTML representation of the diff between the live and working versions of an HTML page.
	 * @param page the identifier of the HTMLPage to be diffed
	 * @param user the user viewing the diff
	 * @param contentId the identifier of the URLMapped Content (optional)
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 * @throws DotDataException
	 */
	public static String htmlDiffPage(final IHTMLPage page,
									  final User user,
									  String contentId,
									  final long languageId) throws IOException, SAXException,

			TransformerConfigurationException, DotSecurityException, DotDataException {
		HTMLPageAssetAPI hapi = APILocator.getHTMLPageAssetAPI();

		PermissionAPI papi = APILocator.getPermissionAPI();
		if(!papi.doesUserHavePermission(page, PermissionAPI.PERMISSION_EDIT, user)){
			throw new DotSecurityException("User " + user.getFullName() + " id " + user.getUserId() + " does not have read perms on page :" + page.getIdentifier());
		}

		Identifier pageIdentfier = APILocator.getIdentifierAPI().find(page.getIdentifier());
		new PageLoader().invalidate(page, PageMode.EDIT_MODE, PageMode.PREVIEW_MODE);
		String live = "";
		String working = "";
		if(page.isContent()){
			if(!UtilMethods.isSet(contentId)){
				contentId = page.getInode();
			}

			final Host host = APILocator.getHostAPI().find(pageIdentfier.getHostId(), user, false);

			live = getLiveVersionHtml(page, user, host, languageId);
			working = hapi.getHTML(page.getURI(), host, false, contentId, user, languageId, Constants.USER_AGENT_DOTCMS_HTMLPAGEDIFF);
		}else{
			live = hapi.getHTML(page, true, contentId, user, Constants.USER_AGENT_DOTCMS_HTMLPAGEDIFF);
			working = hapi.getHTML(page, false, contentId, user, Constants.USER_AGENT_DOTCMS_HTMLPAGEDIFF);
		}
		String nothingChanged  = "";
		try {
			nothingChanged  = LanguageUtil.get(user, "nothing-changed");
		} catch (LanguageException e) {

		}

		/*
		Before to try to compare the content we need to apply first a simple clean-up as some
		white space characters can affect the comparison.
		 */
		live = live.replaceAll(">\\s+<", "><");
		working = working.replaceAll(">\\s+<", "><");
		if(live.equals(working)){
			return nothingChanged ;
		}
		XslFilter filter = new XslFilter();
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();

		TransformerHandler result = tf.newTransformerHandler();
		StringWriter ret = new StringWriter();
		result.setResult(new StreamResult(ret));
		org.xml.sax.ContentHandler postProcess = filter.xsl(result, "diff/xsl/htmlheader.xsl");



        Pattern cesses = Pattern.compile(CSSES_REGEX);
        Pattern hrefs = Pattern.compile(HREF_REGEX);
        Matcher matcher = cesses.matcher(live);

        List<String> csses = new ArrayList<String>();
        while (matcher.find()) {
            String link = matcher.group();
            Matcher matcher2 = hrefs.matcher(link);
            if (matcher2.find()) {
                csses.add(matcher2.group(1));
            }
        }

       

		Locale locale = Locale.getDefault();

		String prefix = "diff";

		HtmlCleaner cleaner = new HtmlCleaner();

		InputSource oldSource = new InputSource(new StringReader(live));

		InputSource newSource = new InputSource(new StringReader(working));

		DomTreeBuilder oldHandler = new DomTreeBuilder();

		cleaner.cleanAndParse(oldSource, oldHandler);

		TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale);

		DomTreeBuilder newHandler = new DomTreeBuilder();

		cleaner.cleanAndParse(newSource, newHandler);

		TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale);

		postProcess.startDocument();

		postProcess.startElement("", "diffreport", "diffreport", new AttributesImpl());

		doCSS(csses, postProcess);

		postProcess.startElement("", "diff", "diff", new AttributesImpl());

		HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess, prefix);

		HTMLDiffer differ = new HTMLDiffer(output);

		/*
		Before to return something lets check again if there are differences between versions,
		we can not trust always in the equals comparison above, that's way we apply a double check (won't affect performance).
		 */
		RangeDifference[] differences = differ.getDifferences(leftComparator, rightComparator);
		if ( differences == null || differences.length == 0 ) {
			return nothingChanged;
		}
		//Ok, we can continue, we really have some differences
		differ.diff(leftComparator, rightComparator);


		postProcess.endElement("", "diff", "diff");

		postProcess.endElement("", "diffreport", "diffreport");

		postProcess.endDocument();


		return ret.toString();

	}

	protected static String getLiveVersionHtml(final IHTMLPage page, final User user, final Host host, long languageId)
			throws DotDataException, DotSecurityException {

		try {
			return APILocator.getHTMLPageAssetAPI()
					.getHTML(page.getURI(), host, true, page.getInode(), user, languageId,
							Constants.USER_AGENT_DOTCMS_HTMLPAGEDIFF);
		} catch (final ResourceNotFoundException e) {
			return "";
		}
	}

    private static void doCSS(final List<String> css, ContentHandler handler) throws SAXException {

        handler.startElement("", "css", "css",

                new AttributesImpl());

        for(String cssLink : css){

            AttributesImpl attr = new AttributesImpl();

            attr.addAttribute("", "href", "href", "CDATA", cssLink);

            attr.addAttribute("", "type", "type", "CDATA", "text/css");

            attr.addAttribute("", "rel", "rel", "CDATA", "stylesheet");

            handler.startElement("", "link", "link",

                    attr);

            handler.endElement("", "link", "link");

        }



        handler.endElement("", "css", "css");



    }




}
