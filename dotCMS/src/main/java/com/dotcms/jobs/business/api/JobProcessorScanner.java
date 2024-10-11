package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotmarketing.util.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

/**
 * Scans the classpath for classes that implement the JobProcessor interface.
 * This class uses Jandex to scan the classpath for classes that implement the JobProcessor interface.
 */
@ApplicationScoped
public class JobProcessorScanner {

    /**
     * Discovers all classes that implement the JobProcessor interface.
     * @return A list of classes that implement the JobProcessor interface.
     */
    public List<Class<? extends JobProcessor>> discoverJobProcessors() {
        List<Class<? extends JobProcessor>> jobProcessors = new ArrayList<>();
        try {

            Index index = getJandexIndex();
            DotName jobProcessorInterface = DotName.createSimple(JobProcessor.class.getName());

            Collection<ClassInfo> implementors = index.getAllKnownImplementors(jobProcessorInterface);

            for (ClassInfo classInfo : implementors) {
                String className = classInfo.name().toString();

                Class<?> clazz = Class.forName(className);
                if (JobProcessor.class.isAssignableFrom(clazz)) {
                    jobProcessors.add((Class<? extends JobProcessor>) clazz);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            Logger.error(JobProcessorScanner.class, "Error discovering JobProcessors", e);

        }
        return jobProcessors;
    }

    /**
     * Reads the Jandex index file.
     * @return The Jandex index.
     * @throws IOException If the Jandex index file cannot be read.
     */
    private Index getJandexIndex() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("META-INF/jandex.idx");
        if (input == null) {
            throw new IOException("Jandex index not found");
        }
        IndexReader reader = new IndexReader(input);
        return reader.read();
    }

}
