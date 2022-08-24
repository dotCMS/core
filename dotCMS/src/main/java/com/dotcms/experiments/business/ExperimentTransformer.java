package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TrafficProportion;
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
import org.postgresql.util.PGobject;

/**
 * DBTransformer that converts DB objects into {@link Experiment} instances
 */
public class ExperimentTransformer implements DBTransformer {
    final List<Experiment> list;

    final static ObjectMapper mapper = new ObjectMapper();


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
        return new Experiment.Builder((String) map.get("page_id"), (String) map.get("name"),
                (String) map.get("description"))
                .id((String) map.get("id"))
                .status(Experiment.Status.valueOf((String) map.get("status")))
                .trafficProportion(getTrafficProportion(map.get("traffic_proportion")))
                .trafficAllocation((Float) map.get("traffic_allocation"))
                .modDate(Try.of(()->((java.sql.Timestamp) map.get("mod_date")).toLocalDateTime())
                        .getOrNull())
                .scheduling(getScheduling(map.get("scheduling")))
                .readyToStart(ConversionUtils.toBooleanFromDb(map.get("ready_to_start")))
                .archived(ConversionUtils.toBooleanFromDb(map.get("archived")))
                .build();
    }

    private static TrafficProportion getTrafficProportion(Object traffic_proportion) {
        if(DbConnectionFactory.isPostgres()) {
            PGobject json = (PGobject) traffic_proportion;
            return Try.of(()->mapper.readValue(json.getValue(), TrafficProportion.class))
                            .getOrNull();
        } else  {
            // TODO pending for mssql
            return null;
        }
    }

    private static Scheduling getScheduling(Object scheduling) {
        if(DbConnectionFactory.isPostgres()) {
            PGobject json = (PGobject) scheduling;
            return Try.of(()->mapper.readValue(json.getValue(), Scheduling.class))
                    .getOrNull();
        } else  {
            // TODO pending for mssql
            return null;
        }
    }
}
