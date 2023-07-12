package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExperimentsFactoryImpl implements
        ExperimentsFactory {

    public static final String INSERT_EXPERIMENT = "INSERT INTO experiment(id, page_id, name, description, status, " +
            "traffic_proportion, traffic_allocation, mod_date, scheduling, "
            + "creation_date, created_by, last_modified_by, goals, lookback_window, running_ids) "
            + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?, ?)";

    public static final String UPDATE_EXPERIMENT = "UPDATE experiment set name=?, description=?, status=?, " +
            "traffic_proportion=?, traffic_allocation=?, mod_date=?, scheduling=?, "
            + " creation_date=?, created_by=?, last_modified_by=?, goals=?, lookback_window=?, running_ids=?"
            + " WHERE id=?";

    public static final String FIND_EXPERIMENT_BY_ID = "SELECT * FROM experiment WHERE id = ?";

    public static final String DELETE_EXPERIMENT = "DELETE FROM experiment WHERE id = ?";

    public static final String LIST_EXPERIMENTS_BASE = "SELECT * FROM experiment WHERE 1=1 ";

    public static final String PAGE_ID_FILTER = "AND page_id = ? ";

    public static final String NAME_FILTER = "AND name LIKE ?";

    public static final String STATUS_FILTER = "SELECT * from experiment WHERE status = ?";

    @Override
    public void save(Experiment experiment) throws DotDataException {
        if(experiment.id().isEmpty() || find(experiment.id().get()).isEmpty()) {
             insertInDB(experiment);
        } else {
            updateInDB(experiment);
        }
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
        Experiment experiment = null; // experimentsCache.get(id); TODO

//        if(experiment==null){
            final List<Map<String, Object>> results = new DotConnect()
                    .setSQL(FIND_EXPERIMENT_BY_ID)
                    .addParam(id)
                    .loadObjectResults();
            if (results.isEmpty()) {
                Logger.debug(this, "Experiment with id: " + id + " not found");
                return Optional.empty();
            }

            experiment = TransformerLocator.createExperimentTransformer(results).findFirst();

//            if(experiment != null && experiment.getId() != null) {
//                experimentsCache.add(id, experiment);
//            }
//        }

        return Optional.of(experiment);
    }

    @Override
    public List<Experiment> list(ExperimentFilter filter) throws DotDataException {
        final StringBuilder query = new StringBuilder(LIST_EXPERIMENTS_BASE);

        if(filter.pageId().isPresent()) {
            query.append(PAGE_ID_FILTER);
        }

        if(filter.name().isPresent()) {
            query.append(NAME_FILTER);
        }

        if(filter.statuses().isPresent()) {
            query.append("INTERSECT ");

            final String statusFilter = filter.statuses().get().stream().map((status -> STATUS_FILTER) ).collect
                    (Collectors.joining(" UNION DISTINCT "));
            query.append(statusFilter);
        }

        final DotConnect dc = new DotConnect();
        dc.setSQL(query.toString());

        if(filter.pageId().isPresent()) {
            dc.addParam(filter.pageId().get());
        }

        if(filter.name().isPresent()) {
            dc.addParam("%"+filter.name().get()+"%");
        }

        if(filter.statuses().isPresent() && UtilMethods.isSet(filter.statuses().get())) {
            filter.statuses().get().forEach((status)-> dc.addParam(status.name()));
        }

        final List<Map<String, Object>> results = dc.loadObjectResults();

        return TransformerLocator.createExperimentTransformer(results).list;
    }

    private void insertInDB(final Experiment experiment) throws DotDataException {
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for saves ");

        DotConnect dc = new DotConnect();
        dc.setSQL(INSERT_EXPERIMENT);
        dc.addParam(experiment.id().get());
        dc.addParam(experiment.pageId());
        dc.addParam(experiment.name());
        dc.addParam(experiment.description().orElse(""));
        dc.addParam(experiment.status().name());
        dc.addJSONParam(experiment.trafficProportion());
        dc.addParam(experiment.trafficAllocation());
        dc.addParam(Timestamp.from(experiment.modDate()));
        dc.addJSONParam(experiment.scheduling().isPresent()?experiment.scheduling().get():null);
        dc.addParam(Timestamp.from(experiment.creationDate()));
        dc.addParam(experiment.createdBy());
        dc.addParam(experiment.lastModifiedBy());
        dc.addJSONParam(experiment.goals().isPresent()?experiment.goals().get():null);
        dc.addParam(experiment.lookBackWindowExpireTime());
        dc.addJSONParam(experiment.runningIds());
        dc.loadResult();
    }

    private void updateInDB(final Experiment experiment) throws DotDataException {
        DotPreconditions.checkArgument(experiment.id().isPresent(), "Experiment id is "
                + "required for updates ");

        DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_EXPERIMENT);
        dc.addParam(experiment.name());
        dc.addParam(experiment.description().orElse(""));
        dc.addParam(experiment.status().name());
        dc.addJSONParam(experiment.trafficProportion());
        dc.addParam(experiment.trafficAllocation());
        dc.addParam(Timestamp.from(experiment.modDate()));
        dc.addJSONParam(experiment.scheduling().isPresent()?experiment.scheduling().get():null);
        dc.addParam(Timestamp.from(experiment.creationDate()));
        dc.addParam(experiment.createdBy());
        dc.addParam(experiment.lastModifiedBy());
        dc.addJSONParam(experiment.goals().isPresent()?experiment.goals().get():null);
        dc.addParam(experiment.lookBackWindowExpireTime());
        dc.addJSONParam(experiment.runningIds());
        dc.addParam(experiment.id().get());
        dc.loadResult();
    }
}
