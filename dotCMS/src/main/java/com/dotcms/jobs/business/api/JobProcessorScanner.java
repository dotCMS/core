package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

@ApplicationScoped
public class JobProcessorScanner {


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

    private Index getJandexIndex() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("META-INF/jandex.idx");
        if (input == null) {
            throw new IOException("Jandex index not found");
        }
        IndexReader reader = new IndexReader(input);
        return reader.read();
    }

}
