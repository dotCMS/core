package com.dotmarketing.servlets.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

@Deprecated
public class TitleImage extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static HashMap hm = new HashMap();
    private static ServletContext ctx;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {

    	ctx = config.getServletContext();
    	
        hm.put("&sp;", "%20");
        hm.put("&excl;", "%21");
        hm.put("&quot;", "%22");
        hm.put("&num;", "%23");
        hm.put("&dollar;", "%24");
        hm.put("&percnt;", "%25");
        hm.put("&amp;", "%26");
        hm.put("&apos;", "%27");
        hm.put("&lpar;", "%28");
        hm.put("&rpar;", "%29");
        hm.put("&ast;", "%2A");
        hm.put("&plus;", "%2B");
        hm.put("&comma;", "%2C");
        hm.put("&hyphen;", "%2D");
        hm.put("&minus;", "%2D");
        hm.put("&period;", "%2E");
        hm.put("&sol;", "%2F");
        hm.put("&colon;", "%3A");
        hm.put("&semi;", "%3B");
        hm.put("&lt;", "%3C");
        hm.put("&equals;", "%3D");
        hm.put("&gt;", "%3E");
        hm.put("&quest;", "%3F");
        hm.put("&commat;", "%40");
        hm.put("&lsqb;", "%5B");
        hm.put("&bsol;", "%5C");
        hm.put("&rsqb;", "%5D");
        hm.put("&circ;", "%5E");
        hm.put("&lowbar;", "%5F");
        hm.put("&horbar;", "%5F");
        hm.put("&grave;", "%60");
        hm.put("&lcub;", "%7B");
        hm.put("&verbar;", "%7C");
        hm.put("&rcub;", "%7D");
        hm.put("&tilde;", "%7E");
        hm.put("&lsquor;", "%82");
        hm.put("&fnof;", "%83");
        hm.put("&ldquor;", "%84");
        hm.put("&hellip;", "%85");
        hm.put("&ldots;", "%85");
        hm.put("&dagger;", "%86");
        hm.put("&Dagger;", "%87");
        hm.put("&permil;", "%89");
        hm.put("&Scaron;", "%8A");
        hm.put("&lsaquo;", "%8B");
        hm.put("&OElig;", "%8C");
        hm.put("&lsquo;", "%91");
        hm.put("&rsquor;", "%91");
        hm.put("&rsquo;", "%92");
        hm.put("&ldquo;", "%93");
        hm.put("&rdquor;", "%93");
        hm.put("&rdquo;", "%94");
        hm.put("&bull;", "%95");
        hm.put("&ndash;", "%96");
        hm.put("&endash;", "%96");
        hm.put("&mdash;", "%97");
        hm.put("&emdash;", "%97");
        hm.put("&tilde;", "%98");
        hm.put("&trade;", "%99");
        hm.put("&scaron;", "%9A");
        hm.put("&rsaquo;", "%9B");
        hm.put("&oelig;", "%9C");
        hm.put("&Yuml;", "%9F");
        hm.put("&nbsp;", "%A0");
        hm.put("&iexcl;", "%a1");
        hm.put("&cent;", "%A2");
        hm.put("&pound;", "%A3");
        hm.put("&curren;", "%A4");
        hm.put("&yen;", "%A5");
        hm.put("&brvbar;", "%A6");
        hm.put("&brkbar;", "%A6");
        hm.put("&sect;", "%A7");
        hm.put("&uml;", "%A8");
        hm.put("&die;", "%A8");
        hm.put("&copy;", "%A9");
        hm.put("&ordf;", "%AA");
        hm.put("&laquo;", "%AB");
        hm.put("&not;", "%AC");
        hm.put("&shy;", "%AD");
        hm.put("&reg;", "%AE");
        hm.put("&macr;", "%AF");
        hm.put("&hibar;", "%AF");
        hm.put("&deg;", "%B0");
        hm.put("&plusmn;", "%B1");
        hm.put("&sup2;", "%B2");
        hm.put("&sup3;", "%B3");
        hm.put("&acute;", "%B4");
        hm.put("&micro;", "%B5");
        hm.put("&para;", "%B6");
        hm.put("&middot;", "%B7");
        hm.put("&cedil;", "%B8");
        hm.put("&sup1;", "%B9");
        hm.put("&ordm;", "%BA");
        hm.put("&raquo;", "%BB");
        hm.put("&frac14;", "%BC");
        hm.put("&frac12;", "%BD");
        hm.put("&half;", "%BD");
        hm.put("&frac34;", "%BE");
        hm.put("&iquest;", "%BF");
        hm.put("&Agrave;", "%C0");
        hm.put("&Aacute;", "%C1");
        hm.put("&Acirc;", "%C2");
        hm.put("&Atilde;", "%C3");
        hm.put("&Auml;", "%C4");
        hm.put("&Aring;", "%C5");
        hm.put("&AElig;", "%C6");
        hm.put("&Ccedil;", "%C7");
        hm.put("&Egrave;", "%C8");
        hm.put("&Eacute;", "%C9");
        hm.put("&Ecirc;", "%CA");
        hm.put("&Euml;", "%CB");
        hm.put("&Igrave;", "%CC");
        hm.put("&Iacute;", "%CD");
        hm.put("&Icirc;", "%CE");
        hm.put("&Iuml;", "%CF");
        hm.put("&ETH;", "%D0");
        hm.put("&Ntilde;", "%D1");
        hm.put("&Ograve;", "%D2");
        hm.put("&Oacute;", "%D3");
        hm.put("&Ocirc;", "%D4");
        hm.put("&Otilde;", "%D5");
        hm.put("&Ouml;", "%D6");
        hm.put("&times;", "%D7");
        hm.put("&Oslash;", "%D8");
        hm.put("&Ugrave;", "%D9");
        hm.put("&Uacute;", "%DA");
        hm.put("&Ucirc;", "%DB");
        hm.put("&Uuml;", "%DC");
        hm.put("&Yacute;", "%DD");
        hm.put("&THORN;", "%DE");
        hm.put("&szlig;", "%DF");
        hm.put("&agrave;", "%E0");
        hm.put("&aacute;", "%E1");
        hm.put("&acirc;", "%E2");
        hm.put("&atilde;", "%E3");
        hm.put("&auml;", "%E4");
        hm.put("&aring;", "%E5");
        hm.put("&aelig;", "%E6");
        hm.put("&ccedil;", "%E7");
        hm.put("&egrave;", "%E8");
        hm.put("&eacute;", "%E9");
        hm.put("&ecirc;", "%EA");
        hm.put("&euml;", "%EB");
        hm.put("&igrave;", "%EC");
        hm.put("&iacute;", "%ED");
        hm.put("&icirc;", "%EE");
        hm.put("&iuml;", "%EF");
        hm.put("&eth;", "%F0");
        hm.put("&ntilde;", "%F1");
        hm.put("&ograve;", "%F2");
        hm.put("&oacute;", "%F3");
        hm.put("&ocirc;", "%F4");
        hm.put("&otilde;", "%F5");
        hm.put("&ouml;", "%F6");
        hm.put("&divide;", "%F7");
        hm.put("&oslash;", "%F8");
        hm.put("&ugrave;", "%F9");
        hm.put("&uacute;", "%FA");
        hm.put("&ucirc;", "%FB");
        hm.put("&uuml;", "%FC");
        hm.put("&yacute;", "%FD");
        hm.put("&thorn;", "%FE");
        hm.put("&yuml;", "%FF");

        String assetPath = Config.getStringProperty("ASSET_PATH");
        File file = new File(Config.CONTEXT.getRealPath(assetPath + "/titleservlet"));
       // File file = new File(config.getServletContext().getRealPath(Config.getStringProperty("PATH_TO_TITLE_IMAGES")));
        file.mkdirs();

        //delete our old crap at startup
        File[] filenames = file.listFiles();

        for (int i = 0; i < filenames.length; i++) {
            filenames[i].delete();
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long time = System.currentTimeMillis();

        String text = request.getParameter("text");
        String parsedQueryString = request.getQueryString();

        
        if (text == null) {
            String[] fonts = new File(ctx.getRealPath("/WEB-INF/fonts/")).list();
            text = "accepted params: text,font,size,color,background,nocache,aa,break";
            response.setContentType("text/html");

            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<body>");
            out.println("<p>");
            out.println("Usage: " + request.getServletPath() + "?params[]<BR>");
            out.println("Acceptable Params are: <UL>");
            out.println("<LI><B>text</B><BR>");
            out.println("The body of the image");
            out.println("<LI><B>font</B><BR>");
            out.println("Available Fonts (in folder '/WEB-INF/fonts/') <UL>");

            for (int i = 0; i < fonts.length; i++) {
                if (!"CVS".equals(fonts[i])) {
                    out.println("<LI>" + fonts[i]);
                }
            }

            out.println("</UL>");

            out.println("<LI><B>size</B><BR>");
            out.println("An integer, i.e. size=100");
            out.println("<LI><B>color</B><BR>");
            out.println("in rgb, i.e. color=255,0,0");
            out.println("<LI><B>background</B><BR>");
            out.println("in rgb, i.e. background=0,0,255");
            out.println("transparent, i.e. background=''");
            out.println("<LI><B>aa</B><BR>");
            out.println("antialias (does not seem to work), aa=true");
            out.println("<LI><B>nocache</B><BR>");
            out.println("if nocache is set, we will write out the image file every hit.  Otherwise, will write it the first time and then read the file");
            out.println("<LI><B>break</B><BR>");
            out.println("An integer greater than 0 (zero), i.e. break=20");
            out.println("</UL>");

            out.println("</UL>");

            out.println("Example:<BR>");
            out.println("&lt;img border=1  src=\"" + request.getServletPath()
                    + "?font=arial.ttf&text=testing&color=255,0,0&size=100\"&gt;<BR>");
            out.println("<img border=1 src='" + request.getServletPath()
                    + "?font=arial.ttf&text=testing&color=255,0,0&size=100'><BR>");

            out.println("</body>");
            out.println("</html>");

            return;
        }

        // get the hash of the all the args
        // use it to build a unique filename;
        String myFile = (request.getQueryString() == null) ? "empty" : PublicEncryptionFactory.digestString(
                parsedQueryString).replace('\\', '_').replace('/', '_');
            
        //myFile = Config.getStringProperty("PATH_TO_TITLE_IMAGES") + myFile + ".png";
        
        String ret="";
        String assetPath = Config.getStringProperty("ASSET_PATH");
        File file = new File(Config.CONTEXT.getRealPath(assetPath + "/titleservlet"));
        if (!file.exists())
        	file.mkdirs();
        ret=Config.CONTEXT.getRealPath(assetPath + "/titleservlet/"+myFile + ".png");
        
        file = new File(ret);
        

        //if we don't have the file, make it
        if (!file.exists() || (request.getParameter("nocache") != null)) {
            StringTokenizer st = null;
            //rip out html equivalants
            Iterator i = hm.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                String key = (String) entry.getKey();
                if (parsedQueryString.indexOf(key) > -1) {
                    String val = (String) entry.getValue();
                    parsedQueryString = UtilMethods.replace(parsedQueryString, key, val);
                }
            }

            //find the real text message
            st = new StringTokenizer(parsedQueryString, "&");
            while (st.hasMoreTokens()) {
                try {
                    String x = st.nextToken();
                    String key = x.split("=")[0];
                    String val = x.split("=")[1];

                    if ("text".equals(key)) {
                        text = val;
                    }
                } catch (Exception e) {

                }
            }
            text = URLDecoder.decode(text, "UTF-8");

            Logger.debug(this.getClass(), "building title image:" + file.getAbsolutePath());
            file.createNewFile();

            try {
                // configure all of the parameters
                //text = java.net.URLEncoder.encode(request.getQueryString());
                String font_file = "/WEB-INF/fonts/arial.ttf";

                if (request.getParameter("font") != null) {
                    font_file = "/WEB-INF/fonts/" + request.getParameter("font");
                }

                font_file = ctx.getRealPath(font_file);

                float size = 20.0f;

                if (request.getParameter("size") != null) {
                    size = Float.parseFloat(request.getParameter("size"));
                }

                Color background = Color.white;
              

                if (request.getParameter("background") != null) {
                	if(request.getParameter("background").equals("transparent"))
                    	try {
                    		background = new Color(Color.TRANSLUCENT);
						} catch (Exception e) {
//							System.out.println("error " +e);
						}
                    else
                	try {
                        st = new StringTokenizer(request.getParameter("background"), ",");
                        int x = Integer.parseInt(st.nextToken());
                        int y = Integer.parseInt(st.nextToken());
                        int z = Integer.parseInt(st.nextToken());
                        background = new Color(x, y, z);
                    } catch (Exception e) {
                    }
                }

                Color color = Color.black;

                if (request.getParameter("color") != null) {
                	try {
                		st = new StringTokenizer(request.getParameter("color"), ",");
                        int x = Integer.parseInt(st.nextToken());
                        int y = Integer.parseInt(st.nextToken());
                        int z = Integer.parseInt(st.nextToken());
                        color = new Color(x, y, z);
                    } catch (Exception e) {
                    	Logger.info(this,e.getMessage());
                    }
                }
                
                int intBreak = 0;
                if (request.getParameter("break") != null) {
                	try {
                		intBreak = Integer.parseInt(request.getParameter("break"));
                    } catch (Exception e) {
                    }
                }
                
                java.util.ArrayList<String> lines = null;
                
                if (intBreak > 0) {
                	lines = new java.util.ArrayList<String>(10);
                	lines.ensureCapacity(10);
            		
                	int start = 0;
                	String line = null;
                	int offSet;
                	
            		for (;;) {
            			try {
            				for (; isWhitespace(text.charAt(start)); ++start);
            				
            				if (isWhitespace(text.charAt(start+intBreak-1))) {
            					lines.add(text.substring(start, start+intBreak));
                				start += intBreak;
            				} else {
            					for (offSet = -1; !isWhitespace(text.charAt(start+intBreak+offSet)); ++offSet);
            					lines.add(text.substring(start, start+intBreak+offSet));
            					start += intBreak+offSet;
            				}
            			} catch (Exception e) {
            				if (text.length() > start)
            					lines.add(leftTrim(text.substring(start)));
            				break;
            			}
            		}
                } else {
                	java.util.StringTokenizer tokens = new java.util.StringTokenizer(text, "|");
                	
                	if (tokens.hasMoreTokens()) {
                		lines = new java.util.ArrayList<String>(10);
                    	lines.ensureCapacity(10);
                		
                		for (; tokens.hasMoreTokens();)
                			lines.add(leftTrim(tokens.nextToken()));
                	}
                }
                
                Font font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(font_file));
                font = font.deriveFont(size);

                BufferedImage buffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = buffer.createGraphics();

                if (request.getParameter("aa") != null) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                } else {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                }

                FontRenderContext fc = g2.getFontRenderContext();
                Rectangle2D fontBounds = null;
                Rectangle2D textLayoutBounds = null;
                TextLayout tl = null;
                boolean useTextLayout = false;
                useTextLayout = Boolean.parseBoolean(request.getParameter("textLayout"));

                // calculate the size of the text
                
                int width = 0;
                int height = 0;
                int offSet = 0;
                if (1 < lines.size()) {
                	
                	int heightMultiplier = 0;
                	int maxWidth = 0;
                	for (; heightMultiplier < lines.size(); ++heightMultiplier) {
                			fontBounds = font.getStringBounds(lines.get(heightMultiplier), fc);                          		
                			tl = new TextLayout(lines.get(heightMultiplier),font,fc);
                			textLayoutBounds = tl.getBounds();
                		if (maxWidth < Math.ceil(fontBounds.getWidth()))
                			maxWidth = (int) Math.ceil(fontBounds.getWidth());
                	}
                	
                	width = maxWidth;
                	int boundHeigh = (int) Math.ceil((!useTextLayout ? fontBounds.getHeight() : textLayoutBounds.getHeight()));
                	height = boundHeigh * lines.size();
                	offSet = ((int) (boundHeigh * 0.2))*(lines.size()-1);
                } else {
                	fontBounds = font.getStringBounds(text, fc);                          		
        			tl = new TextLayout(text,font,fc);
        			textLayoutBounds = tl.getBounds();
                	width = (int) fontBounds.getWidth();
                	height = (int) Math.ceil((!useTextLayout ? fontBounds.getHeight() : textLayoutBounds.getHeight()));                	 
                }

                // prepare some output
                buffer = new BufferedImage(width, height-offSet, BufferedImage.TYPE_INT_ARGB);
                g2 = buffer.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(font);

                // actually do the drawing
                g2.setColor(background);
                if(!background.equals(new Color(Color.TRANSLUCENT)))
                	g2.fillRect(0, 0, width, height);
                
                g2.setColor(color);
                
                if (1 < lines.size()) {
                	for (int numLine = 0; numLine < lines.size(); ++numLine)
                	{
                		int y = (int) Math.ceil((!useTextLayout ? fontBounds.getY() : textLayoutBounds.getY()));
                		g2.drawString(lines.get(numLine), 0,- y * (numLine+1));
                	}
                } else {
                	int y = (int) Math.ceil((!useTextLayout ? fontBounds.getY() : textLayoutBounds.getY()));
                	g2.drawString(text, 0,- y);
                }

                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

                // output the image as png
                ImageIO.write(buffer, "png", out);
                out.close();
            } catch (Exception ex) {
                Logger.info(this, ex.toString());
            }
        }

        // set the content type and get the output stream
        response.setContentType("image/png");

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        OutputStream os = response.getOutputStream();
        byte[] buf = new byte[4096];
        int i = 0;

        while ((i = bis.read(buf)) != -1) {
            os.write(buf, 0, i);
        }

        os.close();
        bis.close();
        Logger.debug(this.getClass(), "time to build title: " + (System.currentTimeMillis() - time) + "ms");

        return;
    }
    
    /**
     * Trim only the left side of a string
     * 
     * @author Armando Siem
     * @param string The String to be processed
     * @return Returns a copy of the string, with leading whitespace omitted.
     */
    private boolean isWhitespace(char c) {
    	int ic = (int) c;
    	char temp = '\u0020';
    	int itemp = (int) temp;
    	return (((int) c) <= ((int) '\u0020'));
    }
    
    /**
     * Trim only the left side of a string
     * 
     * @author Armando Siem
     * @param string The String to be processed
     * @return Returns a copy of the string, with leading whitespace omitted.
     */
    private String leftTrim(String string) {
    	int i = 0;
    	for (; i < string.length(); ++i) {
    		if (!isWhitespace(string.charAt(i)))
    			break;
    	}
    	return string.substring(i);
    }

    
}