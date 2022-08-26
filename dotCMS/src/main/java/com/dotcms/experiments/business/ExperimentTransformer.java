package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vavr.control.Try;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.postgresql.util.PGobject;

/**
 * DBTransformer that converts DB objects into {@link Experiment} instances
 */
public class ExperimentTransformer implements DBTransformer {
    final List<Experiment> list;

    final static ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();


    public ExperimentTransformer(List<Map<String, Object>> initList){
        List<Experiment> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Experiment> asList() {

        return this.list;
    }

    private static Experiment transform(Map<String, Object> map)  {
        return Experiment.builder().pageId((String) map.get("page_id")).name((String) map.get("name"))
                .description((String) map.get("description"))
                .id((String) map.get("id"))
                .status(Experiment.Status.valueOf((String) map.get("status")))
                .trafficProportion(getTrafficProportion(map.get("traffic_proportion")))
                .trafficAllocation(((Double) map.get("traffic_allocation")).floatValue())
                .modDate(Try.of(()->((java.sql.Timestamp) map.get("mod_date")).toInstant())
                        .getOrNull())
                .scheduling(Optional.ofNullable(getScheduling(map.get("scheduling"))))
                .archived(ConversionUtils.toBooleanFromDb(map.get("archived")))
                .creationDate(Try.of(()->((java.sql.Timestamp) map.get("creation_date")).toInstant())
                        .getOrNull())
                .createdBy((String) map.get("created_by"))
                .lastModifiedBy((String) map.get("last_modified_by"))
                .build();
    }

    private static TrafficProportion getTrafficProportion(Object traffic_proportion) {
        if(DbConnectionFactory.isPostgres()) {
            PGobject json = (PGobject) traffic_proportion;
            return Try.of(()->mapper.readValue(json.getValue(), TrafficProportion.class))
                            .getOrNull();
        } else  {
            return Try.of(()->mapper.readValue((String) traffic_proportion, TrafficProportion.class))
                    .getOrNull();
        }
    }

    private static Scheduling getScheduling(Object scheduling) {
        if(DbConnectionFactory.isPostgres()) {
            PGobject json = (PGobject) scheduling;
            return Try.of(()->mapper.readValue(json.getValue(), Scheduling.class))
                    .getOrNull();
        } else  {
            return Try.of(()->mapper.readValue((String) scheduling, Scheduling.class))
                    .getOrNull();
        }
    }
}
