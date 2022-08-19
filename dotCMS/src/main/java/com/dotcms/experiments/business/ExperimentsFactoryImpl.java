package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.JsonMapper;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.postgresql.util.PGobject;

public class ExperimentsFactoryImpl implements
        ExperimentsFactory {

    final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

    public static final String INSERT_EXPERIMENT = "INSERT INTO experiment(id, page_id, name, description, status, " +
            "traffic_type, traffic_proportion, traffic_allocation, mod_date, start_date, end_date, ready_to_start) "
            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String UPDATE_EXPERIMENT = "UPDATE experiment set name=?, description=?, status=?, " +
            "traffic_type=?, traffic_proportion=?, traffic_allocation=?, mod_date=?, start_date=?, end_date=?, "
            + "ready_to_start=? "
            + "WHERE id=?";

    public static final String FIND_EXPERIMENT_BY_ID = "SELECT * FROM experiment WHERE id = ?";

    @Override
    public Experiment save(Experiment experiment) throws DotDataException {
        if(find(experiment.getId()).isEmpty()) {
            insertInDB(experiment);
        } else {
            updateInDB(experiment);
        }

        return experiment;
    }

    @Override
    public Experiment archive(Experiment experiment) {
        return null;
    }

    @Override
    public void delete(Experiment experiment) {

    }

    @Override
    public Optional<Experiment> find(final String id) throws DotDataException {
        Experiment experiment = null; // experimentsCache.get(id);

//        if(experiment==null){
            final List<Map<String, Object>> results = new DotConnect()
                    .setSQL(FIND_EXPERIMENT_BY_ID)
                    .addParam(id)
                    .loadObjectResults();
            if (results.isEmpty()) {
                Logger.debug(this, "Experiment with id: " + id + " not found");
                return Optional.empty();
            }

            experiment = (Experiment) TransformerLocator.createExperimentTransformer(results).findFirst();

//            if(experiment != null && experiment.getId() != null) {
//                experimentsCache.add(id, experiment);
//            }
//        }

        return Optional.of(experiment);
    }

    private void insertInDB(final Experiment experiment) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(INSERT_EXPERIMENT);
        dc.addParam(experiment.getId());
        dc.addParam(experiment.getPageId());
        dc.addParam(experiment.getName());
        dc.addParam(experiment.getDescription());
        dc.addParam(experiment.getStatus().name());
        dc.addParam(experiment.getTrafficProportion().getType().name());
        final String trafficProportionAsJSON = Try.of(()->
                        objectWriter.writeValueAsString(experiment.getTrafficProportion()))
                .getOrNull();
        if(DbConnectionFactory.isPostgres()) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(trafficProportionAsJSON)).getOrElseThrow(
                    () -> new DotDataException("Invalid Traffic Proportion"));
            dc.addObject(jsonObject);
        } else {
            dc.addParam(trafficProportionAsJSON);
        }
        dc.addParam(experiment.getTrafficAllocation());
        dc.addParam(experiment.getModDate());
        dc.addParam(experiment.getStartDate());
        dc.addParam(experiment.getEndDate());
        dc.addParam(experiment.isReadyToStart());
        dc.loadResult();
    }

    private void updateInDB(final Experiment experiment) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_EXPERIMENT);
        dc.addParam(experiment.getName());
        dc.addParam(experiment.getDescription());
        dc.addParam(experiment.getStatus().name());
        dc.addParam(experiment.getTrafficProportion().getType().name());
        final String trafficProportionAsJSON = Try.of(()->
                        objectWriter.writeValueAsString(experiment.getTrafficProportion()))
                .getOrNull();
        if(DbConnectionFactory.isPostgres()) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(trafficProportionAsJSON)).getOrElseThrow(
                    () -> new DotDataException("Invalid Traffic Proportion"));
            dc.addObject(jsonObject);
        } else {
            dc.addParam(trafficProportionAsJSON);
        }
        dc.addParam(experiment.getTrafficAllocation());
        dc.addParam(experiment.getModDate());
        dc.addParam(experiment.getStartDate());
        dc.addParam(experiment.getEndDate());
        dc.addParam(experiment.isReadyToStart());
        dc.addParam(experiment.getId());
        dc.loadResult();
    }
}
