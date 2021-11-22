package com.dotcms.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.io.File.separator;

/**
 * Utility class to export Postgres DB dump to a sql.gz file
 *
 * @author vic
 */
public class DbExporterUtil {
    static final List<String> PG_DUMP_OPTIONS = Arrays.asList(
            "--no-comments",
            "--exclude-table-data=analytic_summary",
            "--exclude-table-data=analytic_summary_404",
            "--exclude-table-data=analytic_summary_content",
            "--exclude-table-data=analytic_summary_pages",
            "--exclude-table-data=analytic_summary_period",
            "--exclude-table-data=analytic_summary_referer",
            "--exclude-table-data=analytic_summary_visits",
            "--exclude-table-data=analytic_summary_workstream",
            "--exclude-table-data=clickstream",
            "--exclude-table-data=clickstream_404",
            "--exclude-table-data=clickstream_request",
            "--exclude-table-data=cluster_server",
            "--exclude-table-data=cluster_server_action",
            "--exclude-table-data=cluster_server_uptime",
            "--exclude-table-data=cms_roles_ir",
            "--exclude-table-data=dist_reindex_journal",
            "--exclude-table-data=dot_cluster",
            "--exclude-table-data=fileassets_ir",
            "--exclude-table-data=folders_ir",
            "--exclude-table-data=htmlpages_ir",
            "--exclude-table-data=indicies",
            "--exclude-table-data=notification",
            "--exclude-table-data=publishing_bundle",
            "--exclude-table-data=publishing_bundle_environment",
            "--exclude-table-data=publishing_pushed_assets",
            "--exclude-table-data=publishing_queue",
            "--exclude-table-data=publishing_queue_audit",
            "--exclude-table-data=schemes_ir",
            "--exclude-table-data=sitelic",
            "--exclude-table-data=structures_ir",
            "--exclude-table-data=system_event",
            "-Z7",
            "-O",
            "-x",
            "--quote-all-identifiers"
    );
    /**
     * pg_dump binary distribution.
     */
    public enum PG_ARCH {
        ARM("pg_dump_aarch64"), AMD64("pg_dump_amd64"), OSX_INTEL("pg_dump_x86_64");

        final private String pgDump;

        PG_ARCH(String pgDump) {
            this.pgDump = pgDump;
        }

        public static String pgDump() {
            if ("amd64".equalsIgnoreCase(System.getProperty("os.arch"))) {
                return AMD64.pgDump;
            }
            if ("aarch64".equalsIgnoreCase(System.getProperty("os.arch"))) {
                return ARM.pgDump;
            }
            if ("x86_64".equalsIgnoreCase(System.getProperty("os.arch"))) {
                return OSX_INTEL.pgDump;
            }
            throw new DotRuntimeException("Unable to determine architecture for : " + System.getenv("os.arch"));
        }
    }
    
    /**
     * Utility private and empty constructor.
     */
    private DbExporterUtil() {}

    /**
     * Executes pg_dump binary and stores its output in a generated SQL gzipped file.
     *
     * @return file with DB dump
     */
    public static File exportToFile() {
        final String hostName = Try.of(() ->
                        APILocator.getHostAPI()
                                .findDefaultHost(
                                        APILocator.systemUser(),
                                        false)
                                .getHostname())
                .getOrElse("dotcms");
        final String fileName = StringUtils.sanitizeFileName(hostName)
                + "_db_"
                + DateUtil.EXPORTING_DATE_FORMAT.format(new Date())
                + ".sql.gz";
        final File file = new File(ConfigUtils.getAbsoluteAssetsRootPath()
                + separator
                + "server"
                + separator
                + "sql_backups"
                + separator
                + fileName);
        final long starter = System.currentTimeMillis();
        Logger.info(DbExporterUtil.class, "DB Dumping database to  : " + file);
        if (file.exists()) {
            throw new DotRuntimeException("DB Dump already exists: " + file);
        }

        file.mkdirs();
        file.delete();

        try (final InputStream input = exportSql();
             final OutputStream output = Files.newOutputStream(file.toPath())) {
            IOUtils.copy(input, output);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Logger.info(DbExporterUtil.class, "Database Dump Complete : " + file);
        Logger.info(
                DbExporterUtil.class,
                "- took: " + DateUtil.humanReadableFormat(
                        Duration.of(
                                System.currentTimeMillis() -starter,
                                ChronoUnit.MILLIS)));

        return file;
    }

    /**
     * Resolves the path for pg_dump binary.
     * It tries to find it by calling the `which pg_dump` command. If it cannot locate the command there, then copies it
     * from the classpath to the dynamic content path.
     */
    static final Lazy<String> pgDumpPath = Lazy.of(() -> {
        final Optional<String> pgDumpOpt = findExecutable("pg_dump");
        if (pgDumpOpt.isPresent()) {
            return pgDumpOpt.get();
        }

        final File pgDump = new File(ConfigUtils.getDynamicContentPath() + separator + "bin" + separator + "pg_dump");
        if (!pgDump.exists()) {
            copyPgDump(pgDump);
        }

        return pgDump.getAbsolutePath();
    });

    /**
     * Copies pg_dumb binary to a defined folder for it to called later.
     *
     * @param pgDumpFile pg_dump binary file
     */
    @VisibleForTesting
    static void copyPgDump(final File pgDumpFile) {
        pgDumpFile.mkdirs();
        pgDumpFile.delete();
        
        Logger.info(DbExporterUtil.class, "Copying pg_dump from: " + PG_ARCH.pgDump() + " to " + pgDumpFile);
        try (final InputStream in = DbExporterUtil.class.getResourceAsStream("/" + PG_ARCH.pgDump());
             final OutputStream out = Files.newOutputStream(pgDumpFile.toPath())) {
            System.out.println("in: " + in);
            System.out.println("out:" + out);
            
            IOUtils.copy(in, out);
        } catch (Exception e) {
            e.printStackTrace();
            pgDumpFile.delete();
            throw new DotRuntimeException(e);
        }
        pgDumpFile.setExecutable(true);
    }

    /**
     * Calls the pg_dumb binary with necessary arguments and redirects standard output to {@link InputStream}.
     *
     * @return input stream with commands output
     * @throws IOException
     */
    static InputStream exportSql() throws IOException {
        final ProcessBuilder pb = new ProcessBuilder(
                getCommandAndArgs(pgDumpPath.get(), getDatasource().getDbUrl()))
                .redirectErrorStream(true);
        return new BufferedInputStream(pb.start().getInputStream());
    }

    private static List<String> getCommandAndArgs(final String command, final String dbUrl) {
        final List<String> finalCmd = Arrays.asList(command, dbUrl);
        finalCmd.addAll(PG_DUMP_OPTIONS);
        return finalCmd;
    }

    /**
     * Looks for pg_dump binary by calling the `which <binary to find>` command.
     *
     * @param programToFind binary to find
     * @return path to the located binary
     */
    @VisibleForTesting
    static Optional<String> findExecutable(final String programToFind) {
        final List<String> commands = ImmutableList.<String>builder()
                .add("which")
                .add(programToFind)
                .build();

        Process processHolder = null;
        try {
            final ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            final Process process = Try.of(pb::start).getOrElseThrow(DotRuntimeException::new);
            processHolder = process;
            String input = Try.of(() -> IOUtils
                    .toString(process.getInputStream(), Charset.defaultCharset()))
                    .getOrNull();

            final int returnCode = Try.of(process::waitFor).getOrElse(1);
            if (returnCode != 0) {
                return Optional.empty();
            }
            input = UtilMethods.isSet(input) ? input.replace("\n", "") : null;

            return Optional.ofNullable(input);
        } catch(Exception e) {
            Logger.error(
                    DbExporterUtil.class,
                    String.format("Could not execute the commands %s", String.join(" ", commands)),
                    e);
            return Optional.empty();
        } finally {
            if (processHolder != null) {
                processHolder.destroy();
            }
        }
    }

    /**
     * Gets the current datasource to get DB connection info.
     *
     * @return DB connection info
     */
    private static DataSourceAttributes getDatasource() {
        try {
            HikariDataSource hds = (HikariDataSource) DbConnectionFactory.getDataSource();
            return new DataSourceAttributes(hds.getUsername(), hds.getPassword(), hds.getJdbcUrl());
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

}
