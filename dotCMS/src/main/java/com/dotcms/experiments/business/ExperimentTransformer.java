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
 * DBTransformer that converts DB objects into Template instances
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
        final Experiment experiment;
        experiment = new Experiment((String) map.get("pageId"), (String) map.get("name"),
                (String) map.get("description"));
        experiment.setId((String) map.get("id"));
        experiment.setStatus(Experiment.Status.valueOf((String) map.get("status")));
        experiment.setTrafficProportion(new TrafficProportion(
                TrafficProportion.Type.valueOf((String) map.get("traffic_type")),
                        (Map<String, Float>) map.get("traffic_allocation")));

        experiment.setModDate((LocalDateTime) map.get("mod_date"));
        experiment.setStartDate((LocalDateTime) map.get("start_date"));
        experiment.setEndDate((LocalDateTime) map.get("end_date"));
        experiment.setReadyToStart(ConversionUtils.toBooleanFromDb(map.get("ready_to_start")));
        return experiment;
    }
}
