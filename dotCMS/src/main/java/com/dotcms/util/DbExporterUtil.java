package com.dotcms.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RuntimeUtils;
import com.dotmarketing.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class to export Postgres DB dump to a sql.gz file
 *
 * @author vico
 */
public class DbExporterUtil {

    /**
     * pg_dump binary distribution.
     */
    public enum PG_ARCH {
        ARM("pg_dump_aarch64"), AMD64("pg_dump_amd64"), OSX_INTEL("pg_dump_x86_64");

        public static final Map<String, PG_ARCH> ARCHS = ImmutableMap.of(
                RuntimeUtils.AMD64_ARCH, AMD64,
                RuntimeUtils.ARM64_ARCH, ARM,
                RuntimeUtils.X86_64_ARCH, OSX_INTEL
        );
        private final String pgDump;

        PG_ARCH(final String pgDump) {
            this.pgDump = pgDump;
        }

        public static String pgDump() {
            final String currentArch = System.getProperty("os.arch");
            return Optional
                    .ofNullable(ARCHS.get(currentArch.toLowerCase()))
                    .map(arch -> arch.pgDump)
                    .orElseThrow(() -> new DotRuntimeException("Unable to determine architecture for : " + currentArch))
                    ;
        }
    }

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
     * Resolves the path for pg_dump binary.
     * It tries to find it by calling the `which pg_dump` command. If it cannot locate the command there, then copies it
     * from the classpath to the dynamic content path.
     */
    static final Lazy<String> PG_DUMP_PATH = Lazy.of(() -> {
        final Optional<String> pgDumpOpt = findExecutable("pg_dump");
        if (pgDumpOpt.isPresent()) {
            return pgDumpOpt.get();
        }

        if (RuntimeUtils.isInsideDocker()) {
            Logger.error(
                    DbExporterUtil.class,
                    "Running inside a Docker container and pg_dump could not be located cannot create DB dump");
            throw new DotRuntimeException("Cannot locate pg_dump binary inside Docker container");
        }

        final File pgDump = Paths.get(ConfigUtils.getDynamicContentPath(), "bin", "pg_dump").toFile();
        if (!pgDump.exists()) {
            copyPgDump(pgDump);
        }

        return pgDump.getAbsolutePath();
    });

    /**
     * Utility private and empty constructor.
     */
    private DbExporterUtil() {}


    /**
     * Copies pg_dumb binary to a defined folder for it to called later.
     *
     * @param pgDumpFile pg_dump binary file
     */
    @VisibleForTesting
    static void copyPgDump(final File pgDumpFile) {
        if (Files.notExists(pgDumpFile.toPath())) {
            try {
                Files.createDirectories(pgDumpFile.toPath());
            } catch (IOException e) {
                Logger.error(
                        DbExporterUtil.class,
                        String.format("Error creating directories %s", pgDumpFile.getAbsolutePath()),
                        e);
                throw new DotRuntimeException(e);
            }
        }

        pgDumpFile.delete();

        final String pgDump = PG_ARCH.pgDump();
        Logger.info(DbExporterUtil.class, "Copying pg_dump from: " + pgDump + " to " + pgDumpFile);
        try (final InputStream in = DbExporterUtil.class.getResourceAsStream("/" + pgDump);
             final OutputStream out = Files.newOutputStream(pgDumpFile.toPath())) {
            Logger.info(DbExporterUtil.class, String.format("In resource: %s", pgDump));
            Logger.info(DbExporterUtil.class, String.format("Out resource: %s", pgDumpFile.toPath()));
            IOUtils.copy(in, out);
        } catch (Exception e) {
            Logger.error(
                    DbExporterUtil.class,
                    String.format("Could not copy PG dump for file: %s", pgDumpFile.getName()),
                    e);
            pgDumpFile.delete();
            throw new DotRuntimeException(e);
        }
        pgDumpFile.setExecutable(true);
    }

    /**
     * Calls the pg_dumb binary with necessary arguments and redirects standard output to {@link InputStream}.
     * Now includes proper error handling to detect pg_dump failures while preserving binary output integrity.
     *
     * @return input stream with commands output
     * @throws IOException
     */
    public static InputStream exportSql() throws IOException {
        final String dbUrl = getDatasource().getDbUrl();
        final String[] commandAndArgs = getCommandAndArgs(PG_DUMP_PATH.get(), dbUrl);
        
        Logger.info(DbExporterUtil.class, "Executing pg_dump with connection: " + 
                   dbUrl.replaceAll(":[^:@]*@", ":****@") + " (password masked)");
        
        // Create ProcessBuilder to have full control over process execution
        final ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);
        
        try {
            final Process process = processBuilder.start();
            
            // Create a custom InputStream that wraps the process InputStream
            // and monitors the process for completion and errors
            return new ProcessInputStream(process);
            
        } catch (Exception e) {
            Logger.error(DbExporterUtil.class, "Failed to start pg_dump process: " + e.getMessage(), e);
            throw new DotRuntimeException("Database export failed to start: " + e.getMessage(), e);
        }
    }
    
    /**
     * Custom InputStream that wraps a Process InputStream and monitors for process completion and errors.
     * This preserves binary data integrity while still providing error detection.
     */
    private static class ProcessInputStream extends InputStream {
        private final Process process;
        private final InputStream processInputStream;
        private boolean processChecked = false;
        
        public ProcessInputStream(Process process) {
            this.process = process;
            this.processInputStream = new java.io.BufferedInputStream(process.getInputStream());
        }
        
        @Override
        public int read() throws IOException {
            int result = processInputStream.read();
            
            // If we've reached end of stream, check process exit status
            if (result == -1 && !processChecked) {
                checkProcessCompletion();
            }
            
            return result;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = processInputStream.read(b, off, len);
            
            // If we've reached end of stream, check process exit status
            if (result == -1 && !processChecked) {
                checkProcessCompletion();
            }
            
            return result;
        }
        
        @Override
        public void close() throws IOException {
            try {
                processInputStream.close();
                if (!processChecked) {
                    checkProcessCompletion();
                }
            } finally {
                process.destroyForcibly();
            }
        }
        
        private void checkProcessCompletion() throws IOException {
            processChecked = true;
            
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    // Read error stream to get failure details
                    String errorOutput = "";
                    try (InputStream errorStream = process.getErrorStream()) {
                        errorOutput = IOUtils.toString(errorStream, java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        Logger.warn(DbExporterUtil.class, "Could not read pg_dump error output: " + e.getMessage());
                    }
                    
                    String errorMessage = "pg_dump failed with exit code " + exitCode;
                    if (!errorOutput.trim().isEmpty()) {
                        errorMessage += ": " + errorOutput.trim();
                    }
                    
                    Logger.error(DbExporterUtil.class, errorMessage);
                    throw new IOException("Database export failed: " + errorMessage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("pg_dump process was interrupted", e);
            }
        }
        
        @Override
        public int available() throws IOException {
            return processInputStream.available();
        }
        
        @Override
        public long skip(long n) throws IOException {
            return processInputStream.skip(n);
        }
        
        @Override
        public boolean markSupported() {
            return processInputStream.markSupported();
        }
        
        @Override
        public synchronized void mark(int readlimit) {
            processInputStream.mark(readlimit);
        }
        
        @Override
        public synchronized void reset() throws IOException {
            processInputStream.reset();
        }
    }

    /**
     * Verifies that pg_dump is located and callable.
     *
     * @return {@link Optional} with actual output of the command when called successfully,
     * otherwise an empty {@link Optional}
     */
    public static Optional<String> isPgDumpAvailable() {
        return DbConnectionFactory.isPostgres()
                ? RuntimeUtils.runProcessAndGetOutput(PG_DUMP_PATH.get(), "--version")
                : Optional.empty();
    }

    /**
     * Creates the necessary list of commands required to dump a DB.
     *
     * @param command initial command
     * @param dbUrl DB connection url
     * @return array of commands
     */
    private static String[] getCommandAndArgs(final String command, final String dbUrl) {
        final List<String> finalCmd = Stream.of(command, dbUrl).collect(Collectors.toList());
        finalCmd.addAll(PG_DUMP_OPTIONS);
        return finalCmd.toArray(new String[0]);
    }

    /**
     * Looks for pg_dump binary by calling the `which <binary to find>` command.
     *
     * @param programToFind binary to find
     * @return path to the located binary
     */
    @VisibleForTesting
    static Optional<String> findExecutable(final String programToFind) {
        return RuntimeUtils.runProcessAndGetOutput("which", programToFind);
    }

    /**
     * Gets the current datasource to get DB connection info.
     *
     * @return DB connection info
     */
    private static DataSourceAttributes getDatasource() {
        try {
            final HikariDataSource hds = (HikariDataSource) DbConnectionFactory.getDataSource();
            return new DataSourceAttributes(hds.getUsername(), hds.getPassword(), hds.getJdbcUrl());
        } catch (Exception e) {
            Logger.error(DbExporterUtil.class, "Could acquire datasource", e);
            throw new DotRuntimeException(e);
        }
    }


}
