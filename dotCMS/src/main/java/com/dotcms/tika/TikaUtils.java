package com.dotcms.tika;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.apache.felix.framework.OSGISystem;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.xml.sax.ContentHandler;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;

public class TikaUtils {

    public static final int SIZE = 1024;
    public static final int DEFAULT_META_DATA_MAX_SIZE = 5;



    
    public TikaUtils()  {

        OSGISystem.getInstance().initializeFramework();
        
    }
    
    
    private static Lazy<DefaultDetector> detector = Lazy.of(()->{
        for(Bundle bundle : OSGISystem.getInstance().getBundles()) {
            ServiceReference<Detector> detectorRef= bundle.getBundleContext().getServiceReference(Detector.class);
            
            if(detectorRef!=null) {
                return  (DefaultDetector) bundle.getBundleContext().getService(detectorRef);
            }
        }
        throw new DotRuntimeException("Unable to find the tika Detector service");
        
    });
    
    private static Lazy<DefaultParser> parser = Lazy.of(()->{
        for(Bundle bundle : OSGISystem.getInstance().getBundles()) {
            System.out.println("loading >> " + bundle.getSymbolicName());
            ServiceReference<Parser> parserRef = bundle.getBundleContext().getServiceReference(Parser.class);
            
            if(parserRef!=null) {
                return  (DefaultParser) bundle.getBundleContext().getService(parserRef);
            }
        }
        throw new DotRuntimeException("Unable to find the tika Detector service");
        
    });





    /**
     * Similar as {@link #getMetaDataMap(String, File, boolean)} but includes tika collection
     * and the max length of the binary file to parse.
     * Also, it is not storing anything on the file system as the reference method {@link #getMetaDataMap(String, File, boolean)}
     * This one does everything on memory, means forceMemory is always true and the file system cache has to be performed on upper layers
     */
    public Map<String, Object> getForcedMetaDataMap(final File binFile,
                                                    final int maxLength) {

        if (!binFile.exists() || binFile.length() == 0) {
            Logger.error(this.getClass(),
                    "Unable to get file Meta Data, OSGI Framework not initialized");
            return Map.of(FileAssetAPI.SIZE_FIELD, 0);
        }

        final Map<String, Object> metaMap = new TreeMap<>();
        

        try (InputStream stream = TikaInputStream.get(binFile.toPath())) {
            // no worry about the limit and less time to process.
            Metadata meta = new Metadata();
            ContentHandler handler = new BodyContentHandler();
            ParseContext context = new ParseContext();
            
            parser.get().parse(stream,handler, meta ,context);

            System.out.println("Meta:" + meta);
            for(String key : meta.names()) {
                System.err.println("key:" + key + " >> " + meta.get(key));
                metaMap.put(key, meta.get(key));
            }
            
            
            
            // Creating the meta data map to use by our content
            // metaMap.putAll(this.buildMetaDataMap());
            // metaMap.put(FileAssetAPI.CONTENT_FIELD, content);
        } catch (Throwable e) {
            Logger.warnAndDebug(this.getClass(),e);

        } finally {
            metaMap.put(FileAssetAPI.SIZE_FIELD, binFile.length());
            metaMap.put("length", binFile.length());
        }

        return metaMap;
    }


    


    /**
     * Reads INDEX_METADATA_FIELDS from configuration
     * @return
     */
    public static Set<String> getConfiguredMetadataFields(){
        final String configFields=Config.getStringProperty("INDEX_METADATA_FIELDS", "width,height,contentType,author,keywords,fileSize,content,length,title");

        if (UtilMethods.isSet(configFields)) {

            return new HashSet<>(Arrays.asList( configFields.split(",")));

        }
        return Collections.emptySet();
    }

    /**
     * Filters fields from a map given a set of fields to be kept
     * @param metaMap
     * @param configFieldsSet
     */
    public static void filterMetadataFields(final Map<String, ?> metaMap, final Set<String> configFieldsSet){

        if (UtilMethods.isSet(metaMap) && UtilMethods.isSet(configFieldsSet)) {
            metaMap.entrySet()
                    .removeIf(entry -> !configFieldsSet.contains("*")
                            && !checkIfFieldMatches(entry.getKey(), configFieldsSet));
        }
    }

    /**
     * Verifies if a string matches in a set of regex/strings
     * @param key
     * @param configFieldsSet
     * @return
     */
    private static boolean checkIfFieldMatches(final String key, final Set<String> configFieldsSet){
        final Predicate<String> condition = e -> key.matches(e);
        return configFieldsSet.stream().anyMatch(condition);
    }





    /**
     * Detects the media type of the given file. The type detection is
     * based on the document content and a potential known file extension.
     *
     * @param file the file
     * @return detected media type
     * @throws IOException if the file can not be read
     */
    public String detect(File file) throws IOException {

        try (InputStream stream = TikaInputStream.get(file.toPath())) {
            return detector.get().detect(stream, new Metadata()).getType();
        }
    }

    private Map<String, String> buildMetaDataMap() {

        Map<String, String> metaMap = new HashMap<>();
        /*
        for (int i = 0; i < this.tikaService.metadataNames().length; i++) {
            String name = this.tikaService.metadataNames()[i];
            if (UtilMethods.isSet(name) && this.tikaService.metadataGetName(name) != null) {
                // we will want to normalize our metadata for searching
                String[] x = translateKey(name);
                for (String y : x) {
                    metaMap.put(y, this.tikaService.metadataGetName(name));
                }
            }
        }
*/
        return metaMap;
    }



    private Map<String, String[]> translateMeta = null;

    private Map<String, String[]> getTranslationMap() {
        if (translateMeta == null) {
            synchronized ("translateMeta".intern()) {
                if (translateMeta == null) {
                    translateMeta = new HashMap<>();
                    translateMeta.put("tiff:ImageWidth", new String[]{"tiff:ImageWidth", "width"});
                    translateMeta
                            .put("tiff:ImageLength", new String[]{"tiff:ImageLength", "height"});
                }
            }
        }
        return translateMeta;
    }




}
