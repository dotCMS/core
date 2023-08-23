package com.dotcms.tika;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;

/**
 * Public interface that should be use to expose {@link org.apache.tika.Tika} services through OSGI
 *
 * @author Jonathan Gamba
 * 1/16/18
 */
public interface TikaProxyService extends Serializable {

    String EXCEPTION_ZERO_BYTE_FILE_EXCEPTION = "org.apache.tika.exception.ZeroByteFileException";

    /**
     * Get the value associated to a metadata name. If many values are assiociated
     * to the specified name, then the first one is returned.
     *
     * @param name of the metadata.
     * @return the value associated to the specified metadata name.
     */
    public String metadataGetName(String name);

    /**
     * Returns an array of the names contained in the metadata.
     *
     * @return Metadata names
     */
    public String[] metadataNames();

    /**
     * Detects the media type of the given document. The type detection is
     * based on the content of the given document stream and the name of the
     * document.
     * <p>
     * If the document stream supports the
     * {@link InputStream#markSupported() mark feature}, then the stream is
     * marked and reset to the original position before this method returns.
     * Only a limited number of bytes are read from the stream.
     * <p>
     * The given document stream is <em>not</em> closed by this method.
     *
     * @param stream the document stream
     * @param name document name
     * @return detected media type
     * @throws IOException if the stream can not be read
     * @since Apache Tika 0.9
     */
    public String detect(InputStream stream, String name) throws IOException;

    /**
     * Detects the media type of the given document. The type detection is
     * based on the content of the given document stream and any given
     * document metadata. The document stream can be <code>null</code>,
     * in which case only the given document metadata is used for type
     * detection.
     * <p>
     * If the document stream supports the
     * {@link InputStream#markSupported() mark feature}, then the stream is
     * marked and reset to the original position before this method returns.
     * Only a limited number of bytes are read from the stream.
     * <p>
     * The given document stream is <em>not</em> closed by this method.
     * <p>
     * Unlike in the {@link #parse(InputStream, Metadata)} method, the
     * given document metadata is <em>not</em> modified by this method.
     *
     * @param stream the document stream, or <code>null</code>
     * @return detected media type
     * @throws IOException if the stream can not be read
     */
    public String detect(InputStream stream) throws IOException;

    /**
     * Detects the media type of the given document. The type detection is
     * based on the first few bytes of a document and the document name.
     * <p>
     * For best results at least a few kilobytes of the document data
     * are needed. See also the other detect() methods for better
     * alternatives when you have more than just the document prefix
     * available for type detection.
     *
     * @param prefix first few bytes of the document
     * @param name document name
     * @return detected media type
     * @since Apache Tika 0.9
     */
    public String detect(byte[] prefix, String name);

    /**
     * Detects the media type of the given document. The type detection is
     * based on the first few bytes of a document.
     * <p>
     * For best results at least a few kilobytes of the document data
     * are needed. See also the other detect() methods for better
     * alternatives when you have more than just the document prefix
     * available for type detection.
     *
     * @param prefix first few bytes of the document
     * @return detected media type
     * @since Apache Tika 0.9
     */
    public String detect(byte[] prefix);

    /**
     * Detects the media type of the given file. The type detection is
     * based on the document content and a potential known file extension.
     * <p>
     * Use the {@link #detect(String)} method when you want to detect the
     * type of the document without actually accessing the file.
     *
     * @param file the file
     * @return detected media type
     * @throws IOException if the file can not be read
     * @see #detect(Path)
     */
    public String detect(File file) throws IOException;

    /**
     * Detects the media type of the resource at the given URL. The type
     * detection is based on the document content and a potential known
     * file extension included in the URL.
     * <p>
     * Use the {@link #detect(String)} method when you want to detect the
     * type of the document without actually accessing the URL.
     *
     * @param url the URL of the resource
     * @return detected media type
     * @throws IOException if the resource can not be read
     */
    public String detect(URL url) throws IOException;

    /**
     * Detects the media type of a document with the given file name.
     * The type detection is based on known file name extensions.
     * <p>
     * The given name can also be a URL or a full file path. In such cases
     * only the file name part of the string is used for type detection.
     *
     * @param name the file name of the document
     * @return detected media type
     */
    public String detect(String name);

    /**
     * Parses the given document and returns the extracted text content.
     * Input metadata like a file name or a content type hint can be passed
     * in the given metadata instance. Metadata information extracted from
     * the document is returned in that same metadata instance.
     * <p>
     * The returned reader will be responsible for closing the given stream.
     * The stream and any associated resources will be closed at or before
     * the time when the {@link Reader#close()} method is called.
     *
     * @param stream the document to be parsed
     * @return extracted text content
     * @throws IOException if the document can not be read or parsed
     */
    public Reader parse(InputStream stream) throws IOException;

    /**
     * Parses the given file and returns the extracted text content.
     * <p>
     * Metadata information extracted from the document is returned in
     * the supplied metadata instance.
     *
     * @param file the file to be parsed
     * @return extracted text content
     * @throws IOException if the file can not be read or parsed
     * @see #parse(Path)
     */
    public Reader parse(File file) throws IOException;

    /**
     * Parses the resource at the given URL and returns the extracted
     * text content.
     *
     * @param url the URL of the resource to be parsed
     * @return extracted text content
     * @throws IOException if the resource can not be read or parsed
     */
    public Reader parse(URL url) throws IOException;

    /**
     * Parses the given document and returns the extracted text content.
     * The given input stream is closed by this method.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains
     * only up to {@link #getMaxStringLength()} first characters extracted
     * from the input document. Use the {@link #setMaxStringLength(int)}
     * method to adjust this limitation.
     * <p>
     * <strong>NOTE:</strong> Unlike most other Tika methods that take an
     * {@link InputStream}, this method will close the given stream for
     * you as a convenience. With other methods you are still responsible
     * for closing the stream or a wrapper instance returned by Tika.
     *
     * @param stream the document to be parsed
     * @return extracted text content
     * @throws IOException if the document can not be read
     */
    public String parseToString(InputStream stream) throws Exception;

    /**
     * Parses the given file and returns the extracted text content.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains
     * only up to {@link #getMaxStringLength()} first characters extracted
     * from the input document. Use the {@link #setMaxStringLength(int)}
     * method to adjust this limitation.
     *
     * @param file the file to be parsed
     * @return extracted text content
     * @throws IOException if the file can not be read
     * @see #parseToString(Path)
     */
    public String parseToString(File file) throws Exception;

    /**
     * Parses the resource at the given URL and returns the extracted
     * text content.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains
     * only up to {@link #getMaxStringLength()} first characters extracted
     * from the input document. Use the {@link #setMaxStringLength(int)}
     * method to adjust this limitation.
     *
     * @param url the URL of the resource to be parsed
     * @return extracted text content
     * @throws IOException if the resource can not be read
     */
    public String parseToString(URL url) throws Exception;

    /**
     * Parses the given document as a Plain Text document
     * and returns the extracted text content.
     * The given input stream is closed by this method.
     * <p>
     * To avoid unpredictable excess memory use, the returned string contains
     * only up to {@link #getMaxStringLength()} first characters extracted
     * from the input document. Use the {@link #setMaxStringLength(int)}
     * method to adjust this limitation.
     * <p>
     * <strong>NOTE:</strong> Unlike most other Tika methods that take an
     * {@link InputStream}, this method will close the given stream for
     * you as a convenience. With other methods you are still responsible
     * for closing the stream or a wrapper instance returned by Tika.
     *
     * @param stream the document to be parsed
     * @return extracted text content
     * @throws Exception if the document can not be read
     */
    public String parseToStringAsPlainText(InputStream stream) throws Exception;

    /**
     * Returns the maximum length of strings returned by the
     * parseToString methods.
     *
     * @return maximum string length, or -1 if the limit has been disabled
     * @since Apache Tika 0.7
     */
    public int getMaxStringLength();

    /**
     * Sets the maximum length of strings returned by the parseToString
     * methods.
     *
     * @param maxStringLength maximum string length,
     * or -1 to disable this limit
     * @since Apache Tika 0.7
     */
    public void setMaxStringLength(int maxStringLength);

    /**
     * Creates a TikaInputStream from the file at the given path.
     * <p>
     * Note that you must always explicitly close the returned stream to
     * prevent leaking open file handles.
     *
     * @param binFile input file
     * @return a TikaInputStream instance
     * @throws IOException if an I/O error occurs
     */
    public InputStream tikaInputStreamGet(File binFile) throws IOException;

    public String toString();

}