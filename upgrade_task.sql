CREATE TABLE events_to_track (
    experiment_key varying(36),
	event_key varying(36),
    parameters varying(255)
);

CREATE TABLE variant (
    name varying(36),
	key varying(36),
    domain varying(36)
);

CREATE TABLE experiment_variant (
    experiment_key varying(36),
    variant_key varying(36),
	traffic_percentage real,
	original BOOLEAN
);
             
CREATE TABLE Experiment (
    name varying(36),
	key varying(36),
    status varying(10),
    uniquePerVisitor BOOLEAN,
    lookBackWindowMinutes int,
	pageInode varying(36)
);

CREATE TABLE experiment_rules (
    experiment_key varying(36),
    rule_id varying(36)
);
