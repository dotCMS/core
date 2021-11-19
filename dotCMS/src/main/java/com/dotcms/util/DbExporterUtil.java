package com.dotcms.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.io.File.*;

public class DbExporterUtil {
    public enum PG_ARCH {
        ARM("pg_dump_aarch64"), AMD64("pg_dump_amd64"), OSX_INTEL("pg_dump_x86_64");

        final private String pgdump;

        PG_ARCH(String pgdump) {
            this.pgdump = pgdump;
        }

        public static String pgdump() {
            if ("amd64".equalsIgnoreCase(System.getProperty("os.arch"))) {
                return AMD64.pgdump;
            }
            if ("aarch64".equalsIgnoreCase(System.getProperty("os.arch"))) {
                return ARM.pgdump;
            }
            if ("x86_64".equalsIgnoreCase(System.getProperty("os.arch"))) {
                return OSX_INTEL.pgdump;
            }
            throw new DotRuntimeException("Unable to determine architecture for : " + System.getenv("os.arch"));
        }
    }
    
    private static final SimpleDateFormat datetostring = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");

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

    @VisibleForTesting
    static void copyPgDump(final File pgDumpFile) {
        pgDumpFile.mkdirs();
        pgDumpFile.delete();
        
        Logger.info(DbExporterUtil.class, "Copying pg_dump from: " + PG_ARCH.pgdump() + " to " + pgDumpFile);
        try (final InputStream in = DbExporterUtil.class.getResourceAsStream("/" + PG_ARCH.pgdump());
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

    static InputStream exportSql() throws IOException {
        final ProcessBuilder pb = new ProcessBuilder(
                pgDumpPath.get(),
                getDatasource().getDbUrl(),
                "--no-comments",
                "-T clickstream*",
                "-T dashboard*",
                "-Z7",
                "-O",
                "-x")
                .redirectErrorStream(true);
        return new BufferedInputStream(pb.start().getInputStream());
    }

    public static File exportToFile() {
        final String hostName = Try.of(() ->
                APILocator.getHostAPI()
                        .findDefaultHost(
                                APILocator.systemUser(),
                                false)
                        .getHostname())
                .getOrElse("dotcms");
        final String fileName = StringUtils.sanitizeFileName(hostName) + "_db_" + datetostring.format(new Date()) + ".sql.gz";
        final File file = new File(ConfigUtils.getAbsoluteAssetsRootPath()
                + File.separator
                + "server"
                + File.separator
                + "sql_backups"
                + File.separator
                + fileName);
        final long starter = System.currentTimeMillis();
        Logger.info(DbExporterUtil.class, "DB Dumping database to  : " + file);
        if (file.exists()) {
            throw new DotRuntimeException("DB Dump already exists:" + file);
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
                "- took: " + humanReadableFormat(Duration.of(System.currentTimeMillis() -starter, ChronoUnit.MILLIS)));

        return file;
    }

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
            return Optional.empty();
        } finally {
            if (processHolder != null) {
                processHolder.destroy();
            }
        }
    }

    private static DataSourceAttributes getDatasource() {
        try {
            HikariDataSource hds = (HikariDataSource) DbConnectionFactory.getDataSource();
            return new DataSourceAttributes(hds.getUsername(), hds.getPassword(), hds.getJdbcUrl());
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    private static String humanReadableFormat(final Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}
