package com.dotmarketing.util.starter;

import static org.junit.jupiter.api.Assertions.*;

import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.util.Config;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImportStarterWorkflowCleanupTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidRulesAbortEnabledImport(boolean enabled,
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final var invalid = java.nio.file.Files.writeString(directory.resolve("rules.json"), "{").toFile();
        try {
            Config.setProperty(AssetStorageFeature.FLAG, enabled);
            final org.junit.jupiter.api.function.Executable importRules = () ->
                    com.dotmarketing.portlets.rules.util.RulesImportExportUtil.getInstance().importRules(invalid);
            if (enabled) {
                assertThrows(java.io.IOException.class, importRules);
            } else {
                assertDoesNotThrow(importRules);
            }
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void populatedWorkflowGraphDeletesInDependencyOrderOnlyWhenEnabled(boolean enabled) throws Exception {
        final String previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        // Match the workflow foreign keys in postgres.sql, including escalation and task references.
        final List<String> tables = List.of("cms_role", "structure", "folder", "contentlet", "contentlet_version_info", "company", "language",
                "inode", "identifier", "template", "template_containers", "template_version_info",
                "dot_containers", "container_version_info", "links", "link_version_info", "category", "relationship",
                "workflow_scheme", "workflow_action",
                "workflow_step", "workflow_task", "workflow_history", "workflow_comment", "workflowtask_files",
                "workflow_action_class", "workflow_action_class_pars", "workflow_action_step",
                "workflow_scheme_x_structure", "workflow_action_mappings");
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID());
             var sql = connection.createStatement()) {
            Config.setProperty(AssetStorageFeature.FLAG, enabled);
            for (String ddl : """
                    create table cms_role(id int primary key);
                    create table language(id int primary key);
                    create table company(id int primary key, default_language_id int references language(id));
                    create table inode(inode int primary key);
                    create table identifier(id int primary key);
                    create table template(inode int primary key references inode(inode), identifier int references identifier(id));
                    create table dot_containers(inode int primary key references inode(inode));
                    create table links(inode int primary key references inode(inode));
                    create table template_containers(template_id int references identifier(id));
                    create table template_version_info(working_inode int references template(inode), live_inode int references template(inode));
                    create table container_version_info(working_inode int references dot_containers(inode), live_inode int references dot_containers(inode));
                    create table link_version_info(working_inode int references links(inode), live_inode int references links(inode));
                    create table category(inode int primary key references inode(inode));
                    create table relationship(inode int primary key references inode(inode));
                    create table structure(inode int primary key);
                    create table folder(inode int primary key, default_file_type int references structure(inode));
                    create table contentlet(inode int primary key, structure_inode int references structure(inode),
                        language_id int references language(id));
                    create table contentlet_version_info(working_inode int references contentlet(inode),
                        live_inode int references contentlet(inode));
                    create table workflow_scheme(id int primary key);
                    create table workflow_action(id int primary key, next_assign int not null references cms_role(id));
                    create table workflow_step(id int primary key, scheme_id int references workflow_scheme(id),
                        escalation_action int references workflow_action(id));
                    create table workflow_task(id int primary key, assigned_to int references cms_role(id),
                        status int references workflow_step(id));
                    create table workflow_history(id int primary key, workflowtask_id int references workflow_task(id));
                    create table workflow_comment(id int primary key, workflowtask_id int references workflow_task(id));
                    create table workflowtask_files(id int primary key, workflowtask_id int references workflow_task(id));
                    create table workflow_action_class(id int primary key, action_id int references workflow_action(id));
                    create table workflow_action_class_pars(id int primary key,
                        workflow_action_class_id int references workflow_action_class(id));
                    create table workflow_action_step(action_id int references workflow_action(id),
                        step_id int references workflow_step(id));
                    create table workflow_scheme_x_structure(id int primary key, scheme_id int references workflow_scheme(id),
                        structure_id int references structure(inode));
                    create table workflow_action_mappings(id int primary key);
                    insert into cms_role values (1);
                    insert into language values (1);
                    insert into company values (1, 1);
                    insert into inode values (1);
                    insert into identifier values (1);
                    insert into template values (1, 1);
                    insert into dot_containers values (1);
                    insert into links values (1);
                    insert into template_containers values (1);
                    insert into template_version_info values (1, 1);
                    insert into container_version_info values (1, 1);
                    insert into link_version_info values (1, 1);
                    insert into category values (1);
                    insert into relationship values (1);
                    insert into structure values (1);
                    insert into folder values (1, 1);
                    insert into contentlet values (1, 1, 1);
                    insert into contentlet_version_info values (1, 1);
                    insert into workflow_scheme values (1);
                    insert into workflow_action values (1, 1);
                    insert into workflow_step values (1, 1, 1);
                    insert into workflow_task values (1, 1, 1);
                    insert into workflow_history values (1, 1);
                    insert into workflow_comment values (1, 1);
                    insert into workflowtask_files values (1, 1);
                    insert into workflow_action_class values (1, 1);
                    insert into workflow_action_class_pars values (1, 1);
                    insert into workflow_action_step values (1, 1);
                    insert into workflow_scheme_x_structure values (1, 1, 1);
                    insert into workflow_action_mappings values (1);
                    """.split(";")) {
                if (!ddl.isBlank()) {
                    sql.execute(ddl);
                }
            }
            if (enabled) {
                deleteTables(sql, tables);
                for (String table : tables) {
                    try (var rows = sql.executeQuery("select count(*) from " + table)) {
                        assertTrue(rows.next());
                        assertEquals(0, rows.getInt(1), table + " must be empty before import");
                    }
                }
            } else {
                final SQLException failure = assertThrows(SQLException.class, () -> deleteTables(sql, tables));
                assertEquals("23503", failure.getSQLState(), "Legacy cleanup still encounters the recorded foreign key");
                assertFalse(ImportStarterUtil.getTablesToIgnore().contains("workflow_action"),
                        "Disabled mode must retain the original cleanup list");
            }
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
        }
    }

    private static void deleteTables(Statement sql, List<String> fixtureTables) throws SQLException {
        for (String table : ImportStarterUtil.getTablesToIgnore()) {
            if (fixtureTables.contains(table)) {
                sql.executeUpdate("delete from " + table);
            }
        }
    }
}
