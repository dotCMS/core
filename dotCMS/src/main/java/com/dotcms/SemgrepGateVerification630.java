package com.dotcms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * TEMPORARY verification fixture for dotCMS/private-issues#630.
 *
 * <p>This class deliberately contains blocking Semgrep findings (SQL injection
 * via string concatenation and OS command injection) to prove end-to-end that a
 * red Semgrep result fails the PR workflow's {@code Finalize / Final Status}
 * check and blocks the PR from entering the merge queue.
 *
 * <p>DO NOT MERGE. This file and its draft PR are deleted once the gate is verified.
 */
public class SemgrepGateVerification630 {

    public ResultSet lookup(final Connection conn, final String userInput) throws SQLException {
        final Statement st = conn.createStatement();
        return st.executeQuery("SELECT * FROM users WHERE name = '" + userInput + "'");
    }

    public Process run(final String userInput) throws IOException {
        return Runtime.getRuntime().exec("sh -c " + userInput);
    }

    public ResultSet lookup_1B(final Connection conn, final String userInput) throws SQLException {
        final Statement st = conn.createStatement();
        return st.executeQuery("SELECT * FROM users WHERE name = '" + userInput + "'");
    }

    public Process run_2D(final String userInput) throws IOException {
        return Runtime.getRuntime().exec("sh -c " + userInput);
    }
    
}
