package com.dotmarketing.common.db;

import com.dotmarketing.db.DbConnectionFactory;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test for {@link DotConnect}
 * @author jsanca
 */
public class DotConnectTest {


    /**
     * Method to test: {@link DotConnect#loadInt(String, Connection)}
     * Given Scenario: Make a query to a company table and check if there is any company
     * ExpectedResult: Expected count at least one company
     *
     */
    @Test
    public void test_loadInt() {

        try (final Connection connection = DbConnectionFactory.getConnection()) {

            boolean exist = Try.of(()->new DotConnect()
                    .setSQL(" SELECT count(*) as count from Company")
                    .loadInt("count", connection)).getOrElse(-1) > 0;

            Assert.assertTrue(exist);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
