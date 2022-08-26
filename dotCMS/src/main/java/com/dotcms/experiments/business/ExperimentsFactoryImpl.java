package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.DotPreconditions;
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
            "traffic_proportion, traffic_allocation, mod_date, scheduling, ready_to_start, "
            + "archived) "
            + "VALUES(?,?,?,?,?,?,?,?,?,?,?)";

    public static final String UPDATE_EXPERIMENT = "UPDATE experiment set name=?, description=?, status=?, " +
            "traffic_proportion=?, traffic_allocation=?, mod_date=?, scheduling=?, "
            + "ready_to_start=?, archived=?"
            + "WHERE id=?";

    public static final String FIND_EXPERIMENT_BY_ID = "SELECT * FROM experiment WHERE id = ?";

    public static final String DELETE_EXPERIMENT = "DELETE FROM experiment WHERE id = ?";

    @Override
    public Experiment save(Experiment experiment) throws DotDataException {
        if(experiment.id().isEmpty() || find(experiment.id().get()).isEmpty()) {
            insertInDB(experiment);
        } else {
            updateInDB(experiment);
        }

        return experiment;
    }

    @Override
    public void delete(Experiment experiment) throws DotDataException {
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for deletion ");

        DotConnect dc = new DotConnect();
        dc.setSQL(DELETE_EXPERIMENT);
        dc.addParam(experiment.id().get());
        dc.loadResult();
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
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for saves ");
        DotPreconditions.checkArgument(experiment.modDate().isPresent(), "Experiment id is "
                + "required for saves ");

        DotConnect dc = new DotConnect();
        dc.setSQL(INSERT_EXPERIMENT);
        dc.addParam(experiment.id().get());
        dc.addParam(experiment.pageId());
        dc.addParam(experiment.name());
        dc.addParam(experiment.description());
        dc.addParam(experiment.status().name());

        final String trafficProportionAsJSON = Try.of(()->
                        objectWriter.writeValueAsString(experiment.trafficProportion()))
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

        dc.addParam(experiment.trafficAllocation());
        dc.addParam(experiment.modDate().get());

        final String schedulingAsJSON = Try.of(()->
                        objectWriter.writeValueAsString(experiment.scheduling()))
                .getOrNull();
        if(DbConnectionFactory.isPostgres()) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(schedulingAsJSON)).getOrElseThrow(
                    () -> new DotDataException("Invalid Scheduling"));
            dc.addObject(jsonObject);
        } else {
            dc.addParam(schedulingAsJSON);
        }

        dc.addParam(experiment.readyToStart());
        dc.addParam(experiment.archived());
        dc.loadResult();
    }

    private void updateInDB(final Experiment experiment) throws DotDataException {
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for updates ");
        DotPreconditions.checkArgument(experiment.modDate().isPresent(), "Experiment id is "
                + "required for updates ");

        DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_EXPERIMENT);
        dc.addParam(experiment.name());
        dc.addParam(experiment.description());
        dc.addParam(experiment.status().name());
        final String trafficProportionAsJSON = Try.of(()->
                        objectWriter.writeValueAsString(experiment.trafficProportion()))
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
        dc.addParam(experiment.trafficAllocation());
        dc.addParam(experiment.modDate().get());
        final String schedulingAsJSON = Try.of(()->
                        objectWriter.writeValueAsString(experiment.scheduling()))
                .getOrNull();
        if(DbConnectionFactory.isPostgres()) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(schedulingAsJSON)).getOrElseThrow(
                    () -> new DotDataException("Invalid Scheduling Proportion"));
            dc.addObject(jsonObject);
        } else {
            dc.addParam(schedulingAsJSON);
        }
        dc.addParam(experiment.readyToStart());
        dc.addParam(experiment.archived());
        dc.addParam(experiment.id().get());
        dc.loadResult();
    }
}
