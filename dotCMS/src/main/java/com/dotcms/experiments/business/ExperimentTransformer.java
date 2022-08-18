package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DBTransformer that converts DB objects into {@link Experiment} instances
 */
public class ExperimentTransformer implements DBTransformer {
    final List<Experiment> list;


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
        return new Experiment.Builder((String) map.get("pageId"), (String) map.get("name"),
                (String) map.get("description"))
                .id((String) map.get("id"))
                .status(Experiment.Status.valueOf((String) map.get("status")))
                // TODO fix percentages - this needs to be from the JSON object
                .trafficProportion(new TrafficProportion(
                        TrafficProportion.Type.valueOf((String) map.get("traffic_type")),
                        ((TrafficProportion) map.get("traffic_proportion")).getPercentages()))
                .trafficAllocation((Float) map.get("traffic_allocation"))
                .modDate((LocalDateTime) map.get("mod_date"))
                .startDate((LocalDateTime) map.get("start_date"))
                .endDate((LocalDateTime) map.get("end_date"))
                .readyToStart(ConversionUtils.toBooleanFromDb(map.get("ready_to_start")))
                .build();
    }
}
