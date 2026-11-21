import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.StringReader;
import org.dom4j.io.XPP3Reader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/** Smoke-check optional XML-pull consumers against the resolved runtime classpath. */
public class LegacyXmlRuntimeCheck {
    public static void main(String[] args) throws Exception {
        for (String removed : new String[] {
                "org.xmlpull.mxp1.MXParser", "com.swabunga.spell.engine.SpellDictionaryHashMap"}) {
            try {
                Class.forName(removed);
                throw new AssertionError("Removed implementation remains: " + removed);
            } catch (ClassNotFoundException expected) {
                // The XML-pull API and maintained MXParser replacement remain available.
            }
        }
        XStream stream = new XStream(new DomDriver("UTF-8"));
        String value = "XML round-trip <&>";
        if (!value.equals(stream.fromXML(stream.toXML(value)))) {
            throw new AssertionError("XStream DomDriver round-trip failed");
        }
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new StringReader("<root>value</root>"));
        if (parser.nextTag() != XmlPullParser.START_TAG || !"root".equals(parser.getName())) {
            throw new AssertionError("XML-pull provider failed");
        }
        if (!"value".equals(new XPP3Reader().read(new StringReader("<root>value</root>"))
                .getRootElement().getText())) {
            throw new AssertionError("dom4j XPP3Reader failed");
        }
        System.out.println("PASS: XStream DomDriver, XML-pull provider "
                + parser.getClass().getName() + ", dom4j XPP3Reader; legacy implementations absent");
    }
}
