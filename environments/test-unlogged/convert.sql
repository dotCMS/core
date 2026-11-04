-- One statement: any error rolls back every table rewrite, including timeout/cycle failures.
DO $experiment$
DECLARE
    pending oid[];
    candidate record;
    progressed boolean;
    converted integer := 0;
    started timestamptz := clock_timestamp();
BEGIN
    IF current_database() !~ '^dotcms[1-4]?$'
       OR shobj_description((SELECT oid FROM pg_database WHERE datname = current_database()),
                          'pg_database') IS DISTINCT FROM 'dotcms-disposable-unlogged-38427' THEN
        RAISE EXCEPTION 'UNLOGGED experiment requires a freshly marked disposable dotcms database';
    END IF;
    PERFORM set_config('lock_timeout', '10s', true);
    -- Only ordinary, non-extension application tables are supported by this experiment.
    IF to_regclass('public.contentlet') IS NULL OR to_regclass('public.inode') IS NULL THEN
        RAISE EXCEPTION 'UNLOGGED experiment requires an initialized application schema';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
               WHERE n.nspname = 'public' AND c.relkind IN ('p', 'f', 'm')) THEN
        RAISE EXCEPTION 'UNLOGGED experiment does not support partitioned/foreign/materialized tables';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_inherits i JOIN pg_class c
                   ON c.oid IN (i.inhrelid, i.inhparent)
                   JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public') THEN
        RAISE EXCEPTION 'UNLOGGED experiment does not support table inheritance';
    END IF;
    SELECT array_agg(c.oid ORDER BY c.oid) INTO pending
      FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relpersistence = 'p'
       AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.classid = 'pg_class'::regclass
                       AND d.objid = c.oid AND d.deptype = 'e');
    WHILE coalesce(cardinality(pending), 0) > 0 LOOP
        progressed := false;
        FOR candidate IN SELECT c.oid, n.nspname, c.relname
                           FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                          WHERE c.oid = ANY(pending) ORDER BY c.oid LOOP
            -- A logged referencing child must be converted before its parent. Self-FKs are safe.
            IF EXISTS (SELECT 1 FROM pg_constraint fk JOIN pg_class child ON child.oid = fk.conrelid
                        WHERE fk.contype = 'f' AND fk.confrelid = candidate.oid
                          AND fk.conrelid <> candidate.oid AND child.relpersistence = 'p') THEN
                CONTINUE;
            END IF;
            EXECUTE format('ALTER TABLE %I.%I SET UNLOGGED', candidate.nspname, candidate.relname);
            pending := array_remove(pending, candidate.oid);
            converted := converted + 1;
            progressed := true;
        END LOOP;
        IF NOT progressed THEN
            RAISE EXCEPTION 'UNLOGGED experiment blocked by foreign-key cycle or out-of-scope logged child';
        END IF;
    END LOOP;
    RAISE NOTICE 'UNLOGGED experiment: database=%, converted=%, conversion_ms=%',
        current_database(), converted, round(extract(epoch FROM clock_timestamp() - started) * 1000);
END
$experiment$;
