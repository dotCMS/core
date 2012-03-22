package com.dotmarketing.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class HTMLBoxTag extends BodyTagSupport {

	private static final long serialVersionUID = 1L;
	private String _bodyContent;
	private String tableId;
	private String tableCaption;
	

	public int doStartTag() throws JspException {
		return EVAL_BODY_BUFFERED;
	}

	public int doAfterBody() {
		_bodyContent = getBodyContent().getString();

		return SKIP_BODY;
	}

	public int doEndTag() throws JspException {

		java.io.Writer out = pageContext.getOut();

		try {
			
			if (UtilMethods.isSet(tableId)) {
				out.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"80%\" id=\"" + tableId + "\">\n");
			}
			else {
				out.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"80%\">\n");
			}
			if (UtilMethods.isSet(tableCaption)) {
			    out.write ("<caption><font class=\"bg\" size=\"2\"><B>" + tableCaption + "</b></font></caption>");
			}
			out.write("<tr><td valign=\"top\"><img src=\"/html/images/top_corner.gif\"></td>\n");
			out.write("<td background=\"/html/images/line_top.gif\"><img src=\"/html/images/shim.gif\" width=\"1\" height=\"8\"></td>\n");
			out.write("<td bgcolor=\"#9D9DA1\"><img src=\"/html/images/shim.gif\" width=\"1\" height=\"1\"></td></tr>\n");
			out.write("<tr><td background=\"/html/images/side_bg.gif\"></td><td width=\"100%\">\n");

			out.write(_bodyContent);

			out.write("</td><td bgcolor=\"#9D9DA1\"><img src=\"/html/images/shim.gif\" width=\"1\" height=\"1\"></td></tr>\n");
			out.write("<tr><td bgcolor=\"#9D9DA1\" height=\"1\"><img src=\"/html/images/shim.gif\" width=\"1\" height=\"1\"></td>\n");
			out.write("<td bgcolor=\"#9D9DA1\" height=\"1\"><img src=\"/html/images/shim.gif\" width=\"1\" height=\"1\"></td>\n");
			out.write("<td bgcolor=\"#9D9DA1\" height=\"1\"><img src=\"/html/images/shim.gif\" width=\"1\" height=\"1\"></td>\n");
			out.write("</tr></table>\n");

		}
		catch (Exception e) {
	        Logger.warn(this, e.toString(), e);
		}

		return EVAL_PAGE;
	}

	

	/**
	 * Returns the tableId.
	 * @return String
	 */
	public String getTableId() {
		return tableId;
	}

	/**
	 * Sets the tableId.
	 * @param tableId The tableId to set
	 */
	public void setTableId(String tableId) {
		this.tableId = tableId;
	}

    public String getTableCaption() {
        return tableCaption;
    }
    public void setTableCaption(String tableCaption) {
        this.tableCaption = tableCaption;
    }
}
