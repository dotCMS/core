/*
 * Copyright 2003-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.view.tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

/**
 *  <p>browser-sniffing tool (session or request scope requested, session scope advised).</p>
 *  <p></p>
 * <p><b>Usage:</b></p>
 * <p>BrowserSniffer defines properties that are used to test the client browser, operating system, device...
 * Apart from properties related to versioning, all properties are booleans.</p>
 * <p>The following properties are available:</p>
 * <ul>
 * <li><i>Versioning:</i>version majorVersion minorVersion geckoVersion</li>
 * <li><i>Browser:</i>mosaic netscape nav2 nav3 nav4 nav4up nav45 nav45up nav6 nav6up navgold firefox safari
 * ie ie3 ie4 ie4up ie5 ie5up ie55 ie55up ie6 opera opera3 opera4 opera5 opera6 opera7 lynx links
 * aol aol3 aol4 aol5 aol6 neoplanet neoplanet2 amaya icab avantgo emacs mozilla gecko webtv staroffice
 * lotusnotes konqueror</li>
 * <li><i>Operating systems:</i>win16 win3x win31 win95 win98 winnt windows win32 win2k winxp winme dotnet
 * mac macosx mac68k macppc os2 unix sun sun4 sun5 suni86 irix irix5 irix6 hpux hpux9 hpux10 aix aix1 aix2 aix3 aix4
 * linux sco unixware mpras reliant dec sinix freebsd bsd vms x11 amiga</li>
 * <li><i>Devices:</i>palm audrey iopener wap blackberry</li>
 * <li><i>Features:</i>javascript css css1 css2 dom0 dom1 dom2</li>
 * <li><i>Special:</i>robot (true if the page is requested by a robot, i.e. when one of the following properties is true:
 * wget getright yahoo altavista lycos infoseek lwp webcrawler linkexchange slurp google java)
 * </ul>
 *
 * Thanks to Lee Semel (lee@semel.net), the author of the HTTP::BrowserDetect Perl module.
 * See also http://www.zytrax.com/tech/web/browser_ids.htm and http://www.mozilla.org/docs/web-developer/sniffer/browser_type.html
 *
 * @author <a href="mailto:claude@renegat.net">Claude Brisson</a>
 * @since VelocityTools 1.2
 * @version $Revision$ $Date$
 */
public class BrowserSnifferTool implements ViewTool
{
    private String userAgent = null;
    private String version = null;
    private int majorVersion = -1;
    private int minorVersion = -1;
    private String geckoVersion = null;

    public BrowserSnifferTool()
    {
    }

    public void init(Object initData)
    {
        HttpServletRequest req;
        if(initData instanceof ViewContext)
        {
            req = ((ViewContext)initData).getRequest();
        }
        else if(initData instanceof HttpServletRequest)
        {
            req = (HttpServletRequest)initData;
        }
        else
        {
            throw new IllegalArgumentException("Was expecting " + ViewContext.class +
                                               " or " + HttpServletRequest.class);
        }
        userAgent = req.getHeader("User-Agent").toLowerCase();
    }

    /* Generic getter for unknown tests
     */
    public boolean get(String key)
    {
        return test(key);
    }

    /* Versioning */

    public String getVersion()
    {
        parseVersion();
        return version;
    }

    public int getMajorVersion()
    {
        parseVersion();
        return majorVersion;
    }

    public int getMinorVersion()
    {
        parseVersion();
        return minorVersion;
    }

    public String getGeckoVersion()
    {
        parseVersion();
        return geckoVersion;
    }

    /* Browsers */

    public boolean getGecko()
    {
        return test("gecko");
    }

    public boolean getFirefox()
    {
        return test("firefox") || test("firebird") || test("phoenix");
    }

    public boolean getSafari()
    {
        return test("safari") || test("applewebkit");
    }

    public boolean getNetscape()
    {
        return !getFirefox() && !getSafari() && test("mozilla") &&
               !test("spoofer") && !test("compatible") && !test("opera") && 
               !test("webtv") && !test("hotjava");
    }

    public boolean getNav2()
    {
        return getNetscape() && getMajorVersion() == 2;
    }

    public boolean getNav3()
    {
        return getNetscape() && getMajorVersion() == 3;
    }

    public boolean getNav4()
    {
        return getNetscape() && getMajorVersion() == 4;
    }

    public boolean getNav4up()
    {
        return getNetscape() && getMajorVersion() >= 4;
    }

    public boolean getNav45()
    {
        return getNetscape() && getMajorVersion() == 4 &&
               getMinorVersion() == 5;
    }

    public boolean getNav45up()
    {
        return getNetscape() && getMajorVersion() >= 5 ||
               getNav4() && getMinorVersion() >= 5;
    }

    public boolean getNavgold()
    {
        return test("gold");
    }

    public boolean getNav6()
    {
        return getNetscape() && getMajorVersion() == 5; /* sic */
    }

    public boolean getNav6up()
    {
        return getNetscape() && getMajorVersion() >= 5;
    }

    public boolean getMozilla()
    {
        return getNetscape() && getGecko();
    }

    public boolean getIe()
    {
        return test("msie") && !test("opera") ||
               test("microsoft internet explorer");
    }

    public boolean getIe3()
    {
        return getIe() && getMajorVersion() < 4;
    }

    public boolean getIe4()
    {
        return getIe() && getMajorVersion() == 4;
    }

    public boolean getIe4up()
    {
        return getIe() && getMajorVersion() >= 4;
    }

    public boolean getIe5()
    {
        return getIe() && getMajorVersion() == 5;
    }

    public boolean getIe5up()
    {
        return getIe() && getMajorVersion() >= 5;
    }

    public boolean getIe55()
    {
        return getIe() && getMajorVersion() == 5 && getMinorVersion() >= 5;
    }

    public boolean getIe55up()
    {
        return (getIe5() && getMinorVersion() >= 5) ||
               (getIe() && getMajorVersion() >= 6);
    }

    public boolean getIe6()
    {
        return getIe() && getMajorVersion() == 6;
    }

    public boolean getIe6up()
    {
        return getIe() && getMajorVersion() >= 6;
    }

    public boolean getNeoplanet()
    {
        return test("neoplanet");
    }

    public boolean getNeoplanet2()
    {
        return getNeoplanet() && test("2.");
    }

    public boolean getAol()
    {
        return test("aol");
    }

    public boolean getAol3()
    {
        return test("aol 3.0") || getAol() && getIe3();
    }

    public boolean getAol4()
    {
        return test("aol 4.0") || getAol() && getIe4();
    }

    public boolean getAol5()
    {
        return test("aol 5.0");
    }

    public boolean getAol6()
    {
        return test("aol 6.0");
    }

    public boolean getAolTV()
    {
        return test("navio") || test("navio_aoltv");
    }

    public boolean getOpera()
    {
        return test("opera");
    }

    public boolean getOpera3()
    {
        return test("opera 3") || test("opera/3");
    }

    public boolean getOpera4()
    {
        return test("opera 4") || test("opera/4");
    }

    public boolean getOpera5()
    {
        return test("opera 5") || test("opera/5");
    }

    public boolean getOpera6()
    {
        return test("opera 6") || test("opera/6");
    }

    public boolean getOpera7()
    {
        return test("opera 7") || test("opera/7");
    }

    public boolean getHotjava()
    {
        return test("hotjava");
    }

    public boolean getHotjava3()
    {
        return getHotjava() && getMajorVersion() == 3;
    }

    public boolean getHotjava3up()
    {
        return getHotjava() && getMajorVersion() >= 3;
    }

    public boolean getAmaya()
    {
        return test("amaya");
    }

    public boolean getCurl()
    {
        return test("libcurl");
    }

    public boolean getStaroffice()
    {
        return test("staroffice");
    }

    public boolean getIcab()
    {
        return test("icab");
    }

    public boolean getLotusnotes()
    {
        return test("lotus-notes");
    }

    public boolean getKonqueror()
    {
        return test("konqueror");
    }

    public boolean getLynx()
    {
        return test("lynx");
    }

    public boolean getLinks()
    {
        return test("links");
    }

    public boolean getWebTV()
    {
        return test("webtv");
    }

    public boolean getMosaic()
    {
        return test("mosaic");
    }

    public boolean getWget()
    {
        return test("wget");
    }

    public boolean getGetright()
    {
        return test("getright");
    }

    public boolean getLwp()
    {
        return test("libwww-perl") || test("lwp-");
    }

    public boolean getYahoo()
    {
        return test("yahoo");
    }

    public boolean getGoogle()
    {
        return test("google");
    }

    public boolean getJava()
    {
        return test("java") || test("jdk");
    }

    public boolean getAltavista()
    {
        return test("altavista");
    }

    public boolean getScooter()
    {
        return test("scooter");
    }

    public boolean getLycos()
    {
        return test("lycos");
    }

    public boolean getInfoseek()
    {
        return test("infoseek");
    }

    public boolean getWebcrawler()
    {
        return test("webcrawler");
    }

    public boolean getLinkexchange()
    {
        return test("lecodechecker");
    }

    public boolean getSlurp()
    {
        return test("slurp");
    }

    public boolean getRobot()
    {
        return getWget() || getGetright() || getLwp() || getYahoo() ||
               getGoogle() || getAltavista() || getScooter() || getLycos() || 
               getInfoseek() || getWebcrawler() || getLinkexchange() || 
               test("bot") || test("spider") || test("crawl") ||
               test("agent") || test("seek") || test("search") || 
               test("reap") || test("worm") || test("find") || test("index") ||
               test("copy") || test("fetch") || test("ia_archive") ||
               test("zyborg");
    }

    /* Devices */

    public boolean getBlackberry()
    {
        return test("blackberry");
    }

    public boolean getAudrey()
    {
        return test("audrey");
    }

    public boolean getIopener()
    {
        return test("i-opener");
    }

    public boolean getAvantgo()
    {
        return test("avantgo");
    }

    public boolean getPalm()
    {
        return getAvantgo() || test("palmos");
    }

    public boolean getWap()
    {
        return test("up.browser") || test("nokia") || test("alcatel") ||
               test("ericsson") || userAgent.indexOf("sie-") == 0 || 
               test("wmlib") || test(" wap") || test("wap ") || 
               test("wap/") || test("-wap") || test("wap-") ||
               userAgent.indexOf("wap") == 0 || 
               test("wapper") || test("zetor");
    }

    /* Operating System */

    public boolean getWin16()
    {
        return test("win16") || test("16bit") || test("windows 3") ||
               test("windows 16-bit");
    }

    public boolean getWin3x()
    {
        return test("win16") || test("windows 3") || test("windows 16-bit");
    }

    public boolean getWin31()
    {
        return test("win16") || test("windows 3.1") || test("windows 16-bit");
    }

    public boolean getWin95()
    {
        return test("win95") || test("windows 95");
    }

    public boolean getWin98()
    {
        return test("win98") || test("windows 98");
    }

    public boolean getWinnt()
    {
        return test("winnt") || test("windows nt") || test("nt4") || test("nt3");
    }

    public boolean getWin2k()
    {
        return test("nt 5.0") || test("nt5");
    }

    public boolean getWinxp()
    {
        return test("nt 5.1");
    }

    public boolean getDotnet()
    {
        return test(".net clr");
    }

    public boolean getWinme()
    {
        return test("win 9x 4.90");
    }

    public boolean getWin32()
    {
        return getWin95() || getWin98() || getWinnt() || getWin2k() || 
               getWinxp() || getWinme() || test("win32");
    }

    public boolean getWindows()
    {
        return getWin16() || getWin31() || getWin95() || getWin98() || 
               getWinnt() || getWin32() || getWin2k() || getWinme() || 
               test("win");
    }

    public boolean getMac()
    {
        return test("macintosh") || test("mac_");
    }

    public boolean getMacosx()
    {
        return test("macintosh") || test("mac os x");
    }

    public boolean getMac68k()
    {
        return getMac() && (test("68k") || test("68000"));
    }

    public boolean getMacppc()
    {
        return getMac() && (test("ppc") || test("powerpc"));
    }

    public boolean getAmiga()
    {
        return test("amiga");
    }

    public boolean getEmacs()
    {
        return test("emacs");
    }

    public boolean getOs2()
    {
        return test("os/2");
    }

    public boolean getSun()
    {
        return test("sun");
    }

    public boolean getSun4()
    {
        return test("sunos 4");
    }

    public boolean getSun5()
    {
        return test("sunos 5");
    }

    public boolean getSuni86()
    {
        return getSun() && test("i86");
    }

    public boolean getIrix()
    {
        return test("irix");
    }

    public boolean getIrix5()
    {
        return test("irix5");
    }

    public boolean getIrix6()
    {
        return test("irix6");
    }

    public boolean getHpux()
    {
        return test("hp-ux");
    }

    public boolean getHpux9()
    {
        return getHpux() && test("09.");
    }

    public boolean getHpux10()
    {
        return getHpux() && test("10.");
    }

    public boolean getAix()
    {
        return test("aix");
    }

    public boolean getAix1()
    {
        return test("aix 1");
    }

    public boolean getAix2()
    {
        return test("aix 2");
    }

    public boolean getAix3()
    {
        return test("aix 3");
    }

    public boolean getAix4()
    {
        return test("aix 4");
    }

    public boolean getLinux()
    {
        return test("linux");
    }

    public boolean getSco()
    {
        return test("sco") || test("unix_sv");
    }

    public boolean getUnixware()
    {
        return test("unix_system_v");
    }

    public boolean getMpras()
    {
        return test("ncr");
    }

    public boolean getReliant()
    {
        return test("reliantunix");
    }

    public boolean getDec()
    {
        return test("dec") || test("osf1") || test("delalpha") ||
               test("alphaserver") || test("ultrix") || test("alphastation");
    }

    public boolean getSinix()
    {
        return test("sinix");
    }

    public boolean getFreebsd()
    {
        return test("freebsd");
    }

    public boolean getBsd()
    {
        return test("bsd");
    }

    public boolean getX11()
    {
        return test("x11");
    }

    public boolean getUnix()
    {
        return getX11() || getSun() || getIrix() || getHpux() || getSco() ||
               getUnixware() || getMpras() || getReliant() || getDec() || 
               getLinux() || getBsd() || test("unix");
    }

    public boolean getVMS()
    {
        return test("vax") || test("openvms");
    }

    /* Features */

    /* Since support of those features is often partial, the sniffer returns true
        when a consequent subset is supported. */

    public boolean getCss()
    {
        return (getIe() && getMajorVersion() >= 4) || 
               (getNetscape() && getMajorVersion() >= 4) || 
               getGecko() || 
               getKonqueror() || 
               (getOpera() && getMajorVersion() >= 3) || 
               getSafari() || 
               getLinks();
    }

    public boolean getCss1()
    {
        return getCss();
    }

    public boolean getCss2()
    {
        return getIe() &&
               (getMac() && getMajorVersion() >= 5) ||
               (getWin32() && getMajorVersion() >= 6) || 
               getGecko() || // && version >= ?
               (getOpera() && getMajorVersion() >= 4) || 
               (getSafari() && getMajorVersion() >= 2) || 
               (getKonqueror() && getMajorVersion() >= 2);
    }

    public boolean getDom0()
    {
        return (getIe() && getMajorVersion() >= 3) || 
               (getNetscape() && getMajorVersion() >= 2) || 
               (getOpera() && getMajorVersion() >= 3) || 
               getGecko() || 
               getSafari() || 
               getKonqueror();
    }

    public boolean getDom1()
    {
        return (getIe() && getMajorVersion() >= 5) || 
               getGecko() || 
               (getSafari() && getMajorVersion() >= 2) || 
               (getOpera() && getMajorVersion() >= 4) || 
               (getKonqueror() && getMajorVersion() >= 2);
    }

    public boolean getDom2()
    {
        return (getIe() && getMajorVersion() >= 6) || 
               (getMozilla() && getMajorVersion() >= 5.0) || 
               (getOpera() && getMajorVersion() >= 7) || 
               getFirefox();
    }

    public boolean getJavascript()
    {
        return getDom0(); // good approximation
    }

    /* Helpers */

    private boolean test(String key)
    {
        return userAgent.indexOf(key) != -1;
    }

    private void parseVersion()
    {
        try
        {
            if(version != null)
            {
                return; /* parsing of version already done */
            }

            /* generic versionning */
            Matcher v = Pattern.compile(
                    "/"
                    /* Version starts with a slash */
                    +
                    "([A-Za-z]*"
                    /* Eat any letters before the major version */
                    +
                    "( [\\d]* )"
                    /* Major version number is every digit before the first dot */
                    + "\\." /* The first dot */
                    +
                    "( [\\d]* )"
                    /* Minor version number is every digit after the first dot */
                    + "[^\\s]*)" /* Throw away the remaining */
                    , Pattern.COMMENTS).matcher(userAgent);

            if(v.find())
            {
                version = v.group(1);
                try
                {
                    majorVersion = Integer.parseInt(v.group(2));
                    String minor = v.group(3);
                    if(minor.startsWith("0"))minorVersion = 0;
                    else minorVersion = Integer.parseInt(minor);
                }
                catch(NumberFormatException nfe)
                {}
            }

            /* Firefox versionning */
            if(test("firefox"))
            {
                Matcher fx = Pattern.compile(
                        "/"
                        +
                        "(( [\\d]* )"
                        /* Major version number is every digit before the first dot */
                        + "\\." /* The first dot */
                        +
                        "( [\\d]* )"
                        /* Minor version number is every digit after the first dot */
                        + "[^\\s]*)" /* Throw away the remaining */
                        , Pattern.COMMENTS)
                        .matcher(userAgent);
                if(fx.find())
                {
                    version = fx.group(1);
                    try
                    {
                        majorVersion = Integer.parseInt(fx.group(2));
                        String minor = fx.group(3);
                        if(minor.startsWith("0"))minorVersion = 0;
                        else minorVersion = Integer.parseInt(minor);
                    }
                    catch(NumberFormatException nfe)
                    {}
                }
            }

            /* IE versionning */
            if(test("compatible"))
            {
                Matcher ie = Pattern.compile(
                        "compatible;"
                        + "\\s*"
                        + "\\w*" /* Browser name */
                        + "[\\s|/]"
                        +
                        "([A-Za-z]*"
                        /* Eat any letters before the major version */
                        +
                        "( [\\d]* )"
                        /* Major version number is every digit before first dot */
                        + "\\." /* The first dot */
                        +
                        "( [\\d]* )"
                        /* Minor version number is digits after first dot */
                        + "[^\\s]*)" /* Throw away remaining dots and digits */
                        , Pattern.COMMENTS)
                        .matcher(userAgent);
                if(ie.find())
                {
                    version = ie.group(1);
                    try
                    {
                        majorVersion = Integer.parseInt(ie.group(2));
                        String minor = ie.group(3);
                        if(minor.startsWith("0"))minorVersion = 0;
                        else minorVersion = Integer.parseInt(minor);
                    }
                    catch(NumberFormatException nfe)
                    {}
                }
            }

            /* Safari versionning*/
            if(getSafari())
            {
                Matcher safari = Pattern.compile(
                        "safari/"
                        +
                        "(( [\\d]* )"
                        /* Major version number is every digit before first dot */
                        + "(?:"
                        + "\\." /* The first dot */
                        +
                        " [\\d]* )?)"
                        /* Minor version number is digits after first dot */
                        , Pattern.COMMENTS)
                        .matcher(userAgent);
                if(safari.find())
                {
                    version = safari.group(1);
                    try
                    {
                        int sv = Integer.parseInt(safari.group(2));
                        majorVersion = sv / 100;
                        minorVersion = sv % 100;
                    }
                    catch(NumberFormatException nfe)
                    {}
                }
            }

            /* Gecko-powered Netscape (i.e. Mozilla) versions */
            if(getGecko() && getNetscape() && test("netscape"))
            {
                Matcher netscape = Pattern.compile(
                        "netscape/"
                        +
                        "(( [\\d]* )"
                        /* Major version number is every digit before the first dot */
                        + "\\." /* The first dot */
                        +
                        "( [\\d]* )"
                        /* Minor version number is every digit after the first dot */
                        + "[^\\s]*)" /* Throw away the remaining */
                        , Pattern.COMMENTS)
                        .matcher(userAgent);
                if(netscape.find())
                {
                    version = netscape.group(1);
                    try
                    {
                        majorVersion = Integer.parseInt(netscape.group(2));
                        String minor = netscape.group(3);
                        if(minor.startsWith("0"))minorVersion = 0;
                        else minorVersion = Integer.parseInt(minor);
                    }
                    catch(NumberFormatException nfe)
                    {}
                }
            }

            /* last try if version not found */
            if(version == null)
            {
                Matcher mv = Pattern.compile(
                        "[\\w]+/"
                        +
                        "( [\\d]+ );"
                        /* Major version number is every digit before the first dot */
                        , Pattern.COMMENTS)
                        .matcher(userAgent);
                if(mv.find())
                {
                    version = mv.group(1);
                    try
                    {
                        majorVersion = Integer.parseInt(version);
                        minorVersion = 0;
                    }
                    catch(NumberFormatException nfe)
                    {}
                }
            }

            /* gecko engine version */
            if(getGecko())
            {
                Matcher g = Pattern.compile(
                        "\\([^)]*rv:(.*)\\)"
                        ).matcher(userAgent);
                if(g.find())
                {
                    geckoVersion = g.group(1);
                }
            }
        }
        catch(PatternSyntaxException nfe)
        {
            // where should I log ?!
        }
    }

}
