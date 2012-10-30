package com.dotmarketing.viewtools;

import java.io.StringWriter;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.util.ServiceLocator;

import com.dotmarketing.util.UtilMethods;

/**
 * This class is used to generate blogs string into HTML
 * using one of the following markup languages: MediaWiki, Confluence, Textile, TracWiki and TWiki
 * @author Oswaldo
 *
 */
public class WikiTool implements ViewTool {

	private final String mediaWiki = "MediaWiki";
	private final String textile = "Textile";
	private final String confluence="Confluence";
	private final String tracwiki="TracWiki";
	private final String twiki = "TWiki";

	/**
	 * Converts text into a partial html using the MediaWiki markup language
	 * @param text Text to convert
	 * @return HTML String
	 */
	public String mediawiki(String text){
		String htmlContent = wikiToHTML(text,false,null, mediaWiki);
		return htmlContent;
	}

	/**
	 * Converts text into a partial html using the MediaWiki markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, this value is not set
	 * @return HTML String
	 */
	public String mediawikiToHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,false,cssPath, mediaWiki);
		return htmlContent;
	}
	
	/**
	 * Converts text into a full html using the MediaWiki markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String mediawikiToFullHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,true,cssPath, mediaWiki);
		return htmlContent;
	}
	
	/**
	 * Converts text into a partial html using the Textile markup language
	 * @param text Text to convert
	 * @return HTML String
	 */
	public String textile(String text){
		String htmlContent = wikiToHTML(text,false,null, textile);
		return htmlContent;
	}

	/**
	 * Converts text into a partial html using the Textile markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String textileToHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,false,cssPath, textile);
		return htmlContent;
	}
	
	/**
	 * Converts text into a full html using the Textile markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String textileToFullHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,true,cssPath, textile);
		return htmlContent;
	}
	
	/**
	 * Converts text into a partial html using the Confluence markup language
	 * @param text Text to convert
	 * @return HTML String
	 */
	public String confluence(String text){
		String htmlContent = wikiToHTML(text,false,null, confluence);
		return htmlContent;
	}

	/**
	 * Converts text into a partial html using the Confluence markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String confluenceToHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,false,cssPath, confluence);
		return htmlContent;
	}
	
	/**
	 * Converts text into a full html using the Confluence markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String confluenceToFullHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,true,cssPath, confluence);
		return htmlContent;
	}
	
	/**
	 * Converts text into a partial html using the TrackWiki markup language
	 * @param text Text to convert
	 * @return HTML String
	 */
	public String tracwiki(String text){
		String htmlContent = wikiToHTML(text,false,null, tracwiki);
		return htmlContent;
	}

	/**
	 * Converts text into a partial html using the TrackWiki markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String tracwikiToHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,false,cssPath, tracwiki);
		return htmlContent;
	}
	
	/**
	 * Converts text into a full html using the TrackWiki markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String tracwikiToFullHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,true,cssPath, tracwiki);
		return htmlContent;
	}
	
	/**
	 * Converts text into a partial html using the TWiki markup language
	 * @param text Text to convert
	 * @return HTML String
	 */
	public String twiki(String text){
		String htmlContent = wikiToHTML(text,false,null, twiki);
		return htmlContent;
	}

	/**
	 * Converts text into a partial html using the TWiki markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String twikiToHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,false,cssPath, twiki);
		return htmlContent;
	}
	
	/**
	 * Converts text into a full html using the TWiki markup language
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @return HTML String
	 */
	public String twikiToFullHTML(String text,String cssPath){
		String htmlContent = wikiToHTML(text,true,cssPath, twiki);
		return htmlContent;
	}
	/**
	 * Converts string into and html using one of the following markup languages:
	 * MediaWiki, Confluence, Textile, TracWiki and TWiki
	 * @param text Text to convert
	 * @param markupLanguage Name of the markupt language to use
	 * @return HTML String
	 */
	public String wikiOtherLanguageToHTML(String text,String markupLanguage){
		String htmlContent = wikiToHTML(text,false,null, markupLanguage);
		return htmlContent;
	}
	
	/**
	 * Converts string into a parcial HTML using the markup language (MediaWiki, Confluence, Textile, TracWiki and TWiki) and css path specified
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @param markupLanguage Type of Wiki Markup language to use. Values allowed: MediaWiki, Confluence, Textile, TracWiki, and TWiki
	 * @return HTML String
	 */
	public String wikiOtherLanguageToHTML(String text,String cssPath,String markupLanguage){
		String htmlContent = wikiToHTML(text,false,cssPath, markupLanguage);
		return htmlContent;
	}
	
	/**
	 * Converts string into an full HTML using the markup language (MediaWiki, Confluence, Textile, TracWiki and TWiki) and css path specified
	 * @param text Text to convert
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @param markupLanguage Type of Wiki Markup language to use. Values allowed: MediaWiki, Confluence, Textile, TracWiki, and TWiki
	 * @return HTML String
	 */
	public String wikiOtherLanguageToFullHTML(String text,String cssPath,String markupLanguage){
		String htmlContent = wikiToHTML(text,true,cssPath, markupLanguage);
		return htmlContent;
	}
	
	/**
	 * Converts string into an full or parcial HTML, using the markup language (MediaWiki, Confluence, Textile, TracWiki and TWiki) and css path specified
	 * @param text Text to convert
	 * @param fullHTML True if you want to generate the full html body; false if you want to generate just the given text 
	 * @param cssPath Path to the css file you want ot use. If it is null, no addtional css file will be included
	 * @param markupLanguage Type of Wiki Markup language to use. Values allowed: MediaWiki, Confluence, Textile, TracWiki, and TWiki
	 * @return HTML String
	 */
	@SuppressWarnings("deprecation")
	public String wikiToHTML(String text,boolean fullHTML,String cssPath, String markupLanguage){
		StringWriter writer = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
		// avoid the <html> and <body> tags 
		builder.setEmitAsDocument(fullHTML);

		if(UtilMethods.isSet(cssPath)){
			if(cssPath.indexOf(",") != -1){
				for(String path : cssPath.split(",")){
					// Add a CSS stylesheet as <link type="text/css" rel="stylesheet" href="styles/test.css"/>
					builder.addCssStylesheet(path);
				}
			}else{
				// Add a CSS stylesheet as <link type="text/css" rel="stylesheet" href="styles/test.css"/>
				builder.addCssStylesheet(cssPath);
			}
		}

		MarkupLanguage markupLanguagueObj = ServiceLocator.getInstance().getMarkupLanguage(markupLanguage);
		MarkupParser parser = new MarkupParser(markupLanguagueObj);
		parser.setBuilder(builder);
		parser.parse(text);

		return writer.toString();
	}
	
	/**
	 * Initializes this instance using the given data
	 */
	@Override
	public void init(Object initData) {

	}


}
