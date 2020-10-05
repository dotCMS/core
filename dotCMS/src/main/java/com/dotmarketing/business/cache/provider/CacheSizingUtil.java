package com.dotmarketing.business.cache.provider;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import net.bytebuddy.agent.ByteBuddyAgent;


public class CacheSizingUtil {


    final int standardSampleSize = Config.getIntProperty("CACHE_SIZER_SAMPLE_SIZE", 20);

    public String averageSizePretty(final Map<String, Object> cacheMap) {

        return UtilMethods.prettyByteify(averageSize(cacheMap));

    }


    public long averageSize(final Map<String, Object> cacheMap) {

        if (cacheMap.size() == 0) {
            return 0;
        }

        long totalSize = 0;

        final int numberToSample = Math.min(cacheMap.size(), standardSampleSize);


        for (int i = 0; i < numberToSample; i++) {
            List<String> keysAsArray = new ArrayList<String>(cacheMap.keySet());
            Random r = new Random();
            Object object = cacheMap.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
            if(object instanceof Optional && ((Optional)object).isPresent()) {
                object = ((Optional)object).get();
            }
            totalSize += sizeOf(object);
        }
        return totalSize / numberToSample;

    }

    public long sizeOf(Object object) {
        
        
        return Math.max(sizeOfSerialized(object),sizeOfClass(object) );
                       
    }
    
    
    
    

    /**
     * Recurses the Object Graph to attempt to find an objects size
     * @param object
     * @return
     */
    long sizeOfMemory(Object object) {

        return GraphLayout.parseInstance(object).totalSize();

    }
    
    
    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    /**
     * Serializes an object to attempt to find an object's size
     * @param object
     * @return
     */
    long sizeOfSerialized(Object object) {


        if (!(object instanceof Serializable)) {
            Logger.warn(this.getClass(), object.getClass()  + " not serializable:" + object);
            return -1;
        }
        byteOutputStream.reset();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream)) {
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            objectOutputStream.close();

            return byteOutputStream.toByteArray().length;
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "failed writing:" +object.getClass()  + " : " + e.getMessage(),  e);
            return 0;
        }

    }
    
    /**
     * returns the size of an objects class, based on the classes properties
     * @param object
     * @return
     */
    long sizeOfClass(Object object) {

        return ClassLayout.parseClass(object.getClass()).instanceSize();
    }
    
    
    
    
    


}


