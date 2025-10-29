package com.dotcms.rest.api.v1;

import static org.junit.Assert.assertEquals;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class DotObjectMapperProviderTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Metod to test: {@link DotObjectMapperProvider#createDefaultMapper()}
     * When: You have a exepriment and try to serialize it
     * Should: Serializ Scheduling's Instant field like a timestamp
     * @throws JsonProcessingException
     */
    @Test
    public void serializeExperiment() throws JsonProcessingException {
        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now().plus(30, ChronoUnit.DAYS);

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment testingExperiment = new ExperimentDataGen()
                .name("Testing Experiment")
                .description("Testing Experiment Description")
                .page(htmlPageAsset)
                .scheduling(Scheduling.builder().startDate(startDate).endDate(endDate).build())
                .next();

        final ObjectMapper defaultMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        final String json = defaultMapper.writeValueAsString(testingExperiment);
        final Map experimentAsMap = defaultMapper.readValue(json, Map.class);

        final Map<String, Long> scheduling = (Map<String, Long>) experimentAsMap.get("scheduling");
        assertEquals((Long) endDate.toEpochMilli(), scheduling.get("endDate"));
        assertEquals((Long) startDate.toEpochMilli(), scheduling.get("startDate"));
    }
}
