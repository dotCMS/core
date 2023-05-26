package com.dotcms.osgi.tika;

import com.dotcms.tika.TikaProxyService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Jonathan Gamba
 * 1/16/18
 */
public class TikaProxy implements TikaProxyService {

    private static Logger LOG = LoggerFactory.getLogger(TikaProxy.class);
    private Tika tika;
    private Metadata metadata;

    /**
     * Creates a new instance of a {@link TikaProxyService} in order to expose the {@link org.apache.tika.Tika}
     * through OSGI
     *
     * @param tika New instance of a {@link org.apache.tika.Tika} object
     */
    TikaProxy(Tika tika) {
        this.tika = tika;
        this.metadata = new Metadata();
    }

    @Override
    public String metadataGetName(String name) {
        return this.metadata.get(name);
    }

    @Override
    public String[] metadataNames() {
        return this.metadata.names();
    }

    @Override
    public String detect(InputStream stream, String name) throws IOException {
        return this.tika.detect(stream, name);
    }

    @Override
    public String detect(InputStream stream) throws IOException {
        return this.tika.detect(stream, this.metadata);
    }

    @Override
    public String detect(byte[] prefix, String name) {
        return this.tika.detect(prefix, name);
    }

    @Override
    public String detect(byte[] prefix) {
        return this.tika.detect(prefix);
    }

    @Override
    public String detect(File file) throws IOException {
        return this.tika.detect(file);
    }

    @Override
    public String detect(URL url) throws IOException {
        return this.tika.detect(url);
    }

    @Override
    public String detect(String name) {
        return this.tika.detect(name);
    }

    @Override
    public Reader parse(InputStream stream) throws IOException {
        return this.tika.parse(stream, this.metadata);
    }

    @Override
    public Reader parse(File file) throws IOException {
        LOG.info("Parsing File: {}", file.getPath());
        return this.tika.parse(file, this.metadata);
    }

    @Override
    public Reader parse(URL url) throws IOException {
        LOG.info("Parsing URL: {}", url);
        return this.tika.parse(url);
    }

    @Override
    public String parseToString(InputStream stream) throws Exception {
        LOG.info("Parsing toString stream:");
        return this.tika.parseToString(stream, this.metadata);
    }

    @Override
    public String parseToString(File file) throws Exception {
        LOG.info("Parsing toString File: {}", file.getPath());
        return this.tika.parseToString(file);
    }

    @Override
    public String parseToString(URL url) throws Exception {
        LOG.info("Parsing toString URL: {}", url);
        return this.tika.parseToString(url);
    }

    @Override
    public String parseToStringAsPlainText(InputStream stream) throws Exception {
        LOG.info("Parsing stream toString As Plain Text");
        BodyContentHandler handler = new BodyContentHandler(this.tika.getMaxStringLength());

        try {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            parser.parse(stream, handler, metadata);

        } catch (SAXException var8) {
            throw new TikaException("Unexpected SAX processing failure", var8);

        } catch (Exception e) {
            LOG.error("Error parsing stream to string as plain text", e);
            throw e;
        }

        finally {
            stream.close();
        }
        return handler.toString();
    }

    @Override
    public int getMaxStringLength() {
        return this.tika.getMaxStringLength();
    }

    @Override
    public void setMaxStringLength(int maxStringLength) {
        this.tika.setMaxStringLength(maxStringLength);
    }

    @Override
    public InputStream tikaInputStreamGet(File binFile) throws IOException {
        return TikaInputStream.get(binFile.toPath());
    }

    @Override
    public String toString() {
        return this.tika.toString();
    }

}
