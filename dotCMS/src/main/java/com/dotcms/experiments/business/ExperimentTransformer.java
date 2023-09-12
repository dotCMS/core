package com.dotcms.experiments.business;

import static com.dotcms.experiments.business.ExperimentsAPI.EXPERIMENT_LOOKBACK_WINDOW;

import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Goals;
import com.dotcms.experiments.model.RunningIds;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBColumnToJSONConverter;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DBTransformer that converts DB objects into {@link Experiment} instances
 */
public class ExperimentTransformer implements DBTransformer<Experiment> {
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

    private Experiment transform(Map<String, Object> map)  {
        final RunningIds runningIds = map.get("running_ids") != null ?
                DBColumnToJSONConverter.getObjectFromDBJson(map.get("running_ids"), RunningIds.class) :
                new RunningIds();

        return Experiment.builder().pageId((String) map.get("page_id")).name((String) map.get("name"))
                .description((String) map.get("description"))
                .id((String) map.get("id"))
                .status(Status.valueOf((String) map.get("status")))
                .trafficProportion(DBColumnToJSONConverter.
                        getObjectFromDBJson(map.get("traffic_proportion"), TrafficProportion.class))
                .trafficAllocation(ConversionUtils.toLong(map.get("traffic_allocation"), 0L))
                .modDate(Try.of(()->((Timestamp) map.get("mod_date")).toInstant())
                        .getOrNull())
                .scheduling(Optional.ofNullable(DBColumnToJSONConverter
                        .getObjectFromDBJson(map.get("scheduling"), Scheduling.class)))
                .creationDate(Try.of(()->((Timestamp) map.get("creation_date")).toInstant())
                        .getOrNull())
                .createdBy((String) map.get("created_by"))
                .lastModifiedBy((String) map.get("last_modified_by"))
                .goals(Optional.ofNullable(DBColumnToJSONConverter
                        .getObjectFromDBJson(map.get("goals"), Goals.class)))
                .lookBackWindowExpireTime(ConversionUtils.toInt(map.get("lookback_window"), EXPERIMENT_LOOKBACK_WINDOW))
                .runningIds(runningIds)
                .build();
    }
}
