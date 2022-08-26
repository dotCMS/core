package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.postgresql.util.PGobject;

public class ExperimentsFactoryImpl implements
        ExperimentsFactory {

    final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    public static final String INSERT_EXPERIMENT = "INSERT INTO experiment(id, page_id, name, description, status, " +
            "traffic_proportion, traffic_allocation, mod_date, scheduling, "
            + "archived, creation_date, created_by, last_modified_by) "
            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String UPDATE_EXPERIMENT = "UPDATE experiment set name=?, description=?, status=?, " +
            "traffic_proportion=?, traffic_allocation=?, mod_date=?, scheduling=?, archived=?, "
            + " creation_date=?, created_by=?, last_modified_by=?"
            + " WHERE id=?";

    public static final String FIND_EXPERIMENT_BY_ID = "SELECT * FROM experiment WHERE id = ?";

    public static final String DELETE_EXPERIMENT = "DELETE FROM experiment WHERE id = ?";

    @Override
    public Experiment save(Experiment experiment) throws DotDataException {
        String experimentId;
        if(experiment.id().isEmpty() || find(experiment.id().get()).isEmpty()) {
            experimentId = insertInDB(experiment);
        } else {
            experimentId = updateInDB(experiment);
        }

        final Optional<Experiment> saved = find(experimentId);

        if(saved.isEmpty()) {
            throw new DotDataException("Unable to retrieve saved/updated Experiment");
        }

        return saved.get();
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

    private String insertInDB(final Experiment experiment) throws DotDataException {
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for saves ");

        DotConnect dc = new DotConnect();
        dc.setSQL(INSERT_EXPERIMENT);
        dc.addParam(experiment.id().get());
        dc.addParam(experiment.pageId());
        dc.addParam(experiment.name());
        dc.addParam(experiment.description());
        dc.addParam(experiment.status().name());
        setJSONParam(dc, experiment.trafficProportion());
        dc.addParam(experiment.trafficAllocation());
        dc.addParam(Timestamp.from(experiment.modDate()));

        if(experiment.scheduling().isPresent()) {
            setJSONParam(dc, experiment.scheduling().get());
        } else {
            dc.addObject(null);
        }

        dc.addParam(experiment.archived());
        dc.addParam(Timestamp.from(experiment.creationDate()));
        dc.addParam(experiment.createdBy());
        dc.addParam(experiment.lastModifiedBy());
        dc.loadResult();

        return experiment.id().get();
    }

    private String updateInDB(final Experiment experiment) throws DotDataException {
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for updates ");

        DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_EXPERIMENT);
        dc.addParam(experiment.name());
        dc.addParam(experiment.description());
        dc.addParam(experiment.status().name());
        setJSONParam(dc, experiment.trafficProportion());
        dc.addParam(experiment.trafficAllocation());
        dc.addParam(Timestamp.from(experiment.modDate()));

        if(experiment.scheduling().isPresent()) {
            setJSONParam(dc, experiment.scheduling().get());
        } else {
            dc.addObject(null);
        }

        dc.addParam(experiment.archived());
        dc.addParam(Timestamp.from(experiment.creationDate()));
        dc.addParam(experiment.createdBy());
        dc.addParam(experiment.lastModifiedBy());
        dc.addParam(experiment.id().get());
        dc.loadResult();

        return experiment.id().get();
    }

    private void setJSONParam(final DotConnect dc, Object json) throws DotDataException {
        final String jsonStr = Try.of(()->
                        mapper.writeValueAsString(json))
                .getOrNull();
        if(DbConnectionFactory.isPostgres()) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(jsonStr)).getOrElseThrow(
                    () -> new DotDataException("Invalid Traffic Proportion"));
            dc.addObject(jsonObject);
        } else {
            dc.addParam(jsonStr);
        }
    }
}
