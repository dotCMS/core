package org.apache.velocity.runtime.resource.loader;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.ExceptionUtils;
import org.apache.velocity.util.StringUtils;

/**
 * <P>This is a simple template file loader that loads templates
 * from a DataSource instead of plain files.
 *
 * <P>It can be configured with a datasource name, a table name,
 * id column (name), content column (the template body) and a
 * datetime column (for last modification info).
 * <br>
 * <br>
 * Example configuration snippet for velocity.properties:
 * <br>
 * <br>
 * resource.loader = file, ds <br>
 * <br>
 * ds.resource.loader.public.name = DataSource <br>
 * ds.resource.loader.description = Velocity DataSource Resource Loader <br>
 * ds.resource.loader.class = org.apache.velocity.runtime.resource.loader.DataSourceResourceLoader <br>
 * ds.resource.loader.resource.datasource = java:comp/env/jdbc/Velocity <br>
 * ds.resource.loader.resource.table = tb_velocity_template <br>
 * ds.resource.loader.resource.keycolumn = id_template <br>
 * ds.resource.loader.resource.templatecolumn = template_definition <br>
 * ds.resource.loader.resource.timestampcolumn = template_timestamp <br>
 * ds.resource.loader.cache = false <br>
 * ds.resource.loader.modificationCheckInterval = 60 <br>
 * <br>
 * <P>Optionally, the developer can instantiate the DataSourceResourceLoader and set the DataSource via code in
 * a manner similar to the following:
 * <BR>
 * <BR>
 * DataSourceResourceLoader ds = new DataSourceResourceLoader();<BR>
 * ds.setDataSource(DATASOURCE);<BR>
 * Velocity.setProperty("ds.resource.loader.instance",ds);<BR>
 * <P> The property <code>ds.resource.loader.class</code> should be left out, otherwise all the other
 * properties in velocity.properties would remain the same.
 * <BR>
 * <BR>
 *
 * Example WEB-INF/web.xml: <br>
 * <br>
 *  <resource-ref> <br>
 *   <description>Velocity template DataSource</description> <br>
 *   <res-ref-name>jdbc/Velocity</res-ref-name> <br>
 *   <res-type>javax.sql.DataSource</res-type> <br>
 *   <res-auth>Container</res-auth> <br>
 *  </resource-ref> <br>
 * <br>
 *  <br>
 * and Tomcat 4 server.xml file: <br>
 *  [...] <br>
 *  <Context path="/exampleVelocity" docBase="exampleVelocity" debug="0"> <br>
 *  [...] <br>
 *   <ResourceParams name="jdbc/Velocity"> <br>
 *    <parameter> <br>
 *      <name>driverClassName</name> <br>
 *      <value>org.hsql.jdbcDriver</value> <br>
 *    </parameter> <br>
 *    <parameter> <br>
 *     <name>driverName</name> <br>
 *     <value>jdbc:HypersonicSQL:database</value> <br>
 *    </parameter> <br>
 *    <parameter> <br>
 *     <name>user</name> <br>
 *     <value>database_username</value> <br>
 *    </parameter> <br>
 *    <parameter> <br>
 *     <name>password</name> <br>
 *     <value>database_password</value> <br>
 *    </parameter> <br>
 *   </ResourceParams> <br>
 *  [...] <br>
 *  </Context> <br>
 *  [...] <br>
 * <br>
 *  Example sql script:<br>
 *  CREATE TABLE tb_velocity_template ( <br>
 *  id_template varchar (40) NOT NULL , <br>
 *  template_definition text (16) NOT NULL , <br>
 *  template_timestamp datetime NOT NULL  <br>
 *  ) <br>
 *
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author <a href="mailto:david.kinnvall@alertir.com">David Kinnvall</a>
 * @author <a href="mailto:paulo.gaspar@krankikom.de">Paulo Gaspar</a>
 * @author <a href="mailto:lachiewicz@plusnet.pl">Sylwester Lachiewicz</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: DataSourceResourceLoader.java 991660 2010-09-01 19:13:46Z nbubna $
 * @since 1.5
 */
public class DataSourceResourceLoader extends ResourceLoader
{
    private String dataSourceName;
    private String tableName;
    private String keyColumn;
    private String templateColumn;
    private String timestampColumn;
    private InitialContext ctx;
    private DataSource dataSource;

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#init(org.apache.commons.collections.ExtendedProperties)
     */
    public void init(ExtendedProperties configuration)
    {
        dataSourceName  = StringUtils.nullTrim(configuration.getString("resource.datasource"));
        tableName       = StringUtils.nullTrim(configuration.getString("resource.table"));
        keyColumn       = StringUtils.nullTrim(configuration.getString("resource.keycolumn"));
        templateColumn  = StringUtils.nullTrim(configuration.getString("resource.templatecolumn"));
        timestampColumn = StringUtils.nullTrim(configuration.getString("resource.timestampcolumn"));

        if (dataSource != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DataSourceResourceLoader: using dataSource instance with table \""
                          + tableName + "\"");
                log.debug("DataSourceResourceLoader: using columns \""
                          + keyColumn + "\", \"" + templateColumn + "\" and \""
                          + timestampColumn + "\"");
            }

            log.trace("DataSourceResourceLoader initialized.");
        }
        else if (dataSourceName != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("DataSourceResourceLoader: using \"" + dataSourceName
                          + "\" datasource with table \"" + tableName + "\"");
                log.debug("DataSourceResourceLoader: using columns \""
                          + keyColumn + "\", \"" + templateColumn + "\" and \""
                          + timestampColumn + "\"");
            }

            log.trace("DataSourceResourceLoader initialized.");
        }
        else
        {
            String msg = "DataSourceResourceLoader not properly initialized. No DataSource was identified.";
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Set the DataSource used by this resource loader.  Call this as an alternative to
     * specifying the data source name via properties.
     * @param dataSource The data source for this ResourceLoader.
     */
    public void setDataSource(final DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#isSourceModified(org.apache.velocity.runtime.resource.Resource)
     */
    public boolean isSourceModified(final Resource resource)
    {
        return (resource.getLastModified() !=
                readLastModified(resource, "checking timestamp"));
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getLastModified(org.apache.velocity.runtime.resource.Resource)
     */
    public long getLastModified(final Resource resource)
    {
        return readLastModified(resource, "getting timestamp");
    }

    /**
     * Get an InputStream so that the Runtime can build a
     * template with it.
     *
     *  @param name name of template
     *  @return InputStream containing template
     * @throws ResourceNotFoundException
     */
    public synchronized InputStream getResourceStream(final String name)
        throws ResourceNotFoundException
    {
        if (org.apache.commons.lang.StringUtils.isEmpty(name))
        {
            throw new ResourceNotFoundException("DataSourceResourceLoader: Template name was empty or null");
        }

        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try
        {
            conn = openDbConnection();
            ps = getStatement(conn, templateColumn, name);
            rs = ps.executeQuery();

            if (rs.next())
            {
                InputStream stream = rs.getBinaryStream(templateColumn);
                if (stream == null)
                {
                    throw new ResourceNotFoundException("DataSourceResourceLoader: "
                                                        + "template column for '"
                                                        + name + "' is null");
                }

                return new BufferedInputStream(stream);
            }
            else
            {
                throw new ResourceNotFoundException("DataSourceResourceLoader: "
                                                    + "could not find resource '"
                                                    + name + "'");

            }
        }
        catch (SQLException sqle)
        {
            String msg = "DataSourceResourceLoader: database problem while getting resource '"
                         + name + "': ";

            log.error(msg, sqle);
            throw new ResourceNotFoundException(msg);
        }
        catch (NamingException ne)
        {
            String msg = "DataSourceResourceLoader: database problem while getting resource '"
                         + name + "': ";

            log.error(msg, ne);
            throw new ResourceNotFoundException(msg);
        }
        finally
        {
            closeResultSet(rs);
            closeStatement(ps);
            closeDbConnection(conn);
        }
    }

    /**
     * Fetches the last modification time of the resource
     *
     * @param resource Resource object we are finding timestamp of
     * @param operation string for logging, indicating caller's intention
     *
     * @return timestamp as long
     */
    private long readLastModified(final Resource resource, final String operation)
    {
        long timeStamp = 0;

        /* get the template name from the resource */
        String name = resource.getName();
        if (name == null || name.length() == 0)
        {
            String msg = "DataSourceResourceLoader: Template name was empty or null";
            log.error(msg);
            throw new NullPointerException(msg);
        }
        else
        {
            Connection conn = null;
            ResultSet rs = null;
            PreparedStatement ps = null;

            try
            {
                conn = openDbConnection();
                ps = getStatement(conn, timestampColumn, name);
                rs = ps.executeQuery();

                if (rs.next())
                {
                    Timestamp ts = rs.getTimestamp(timestampColumn);
                    timeStamp = ts != null ? ts.getTime() : 0;
                }
                else
                {
                    String msg = "DataSourceResourceLoader: could not find resource "
                              + name + " while " + operation;
                    log.error(msg);
                    throw new ResourceNotFoundException(msg);
                }
            }
            catch (SQLException sqle)
            {
                String msg = "DataSourceResourceLoader: database problem while "
                            + operation + " of '" + name + "': ";

                log.error(msg, sqle);
                throw ExceptionUtils.createRuntimeException(msg, sqle);
            }
            catch (NamingException ne)
            {
                String msg = "DataSourceResourceLoader: database problem while "
                             + operation + " of '" + name + "': ";

                log.error(msg, ne);
                throw ExceptionUtils.createRuntimeException(msg, ne);
            }
            finally
            {
                closeResultSet(rs);
                closeStatement(ps);
                closeDbConnection(conn);
            }
        }
        return timeStamp;
    }

    /**
     * Gets connection to the datasource specified through the configuration
     * parameters.
     *
     * @return connection
     */
    private Connection openDbConnection() throws NamingException, SQLException
    {
         if (dataSource != null)
         {
            return dataSource.getConnection();
         }

         if (ctx == null)
         {
            ctx = new InitialContext();
         }

         dataSource = (DataSource) ctx.lookup(dataSourceName);

         return dataSource.getConnection();
     }

    /**
     * Closes connection to the datasource
     */
    private void closeDbConnection(final Connection conn)
    {
        if (conn != null)
        {
            try
            {
                conn.close();
            }
            catch (RuntimeException re)
            {
                throw re;
            }
            catch (Exception e)
            {
                String msg = "DataSourceResourceLoader: problem when closing connection";
                log.error(msg, e);
                throw new VelocityException(msg, e);
            }
        }
    }

    /**
     * Closes the result set.
     */
    private void closeResultSet(final ResultSet rs)
    {
        if (rs != null)
        {
            try
            {
                rs.close();
            }
            catch (RuntimeException re)
            {
                throw re;
            }
            catch (Exception e)
            {
                String msg = "DataSourceResourceLoader: problem when closing result set";
                log.error(msg, e);
                throw new VelocityException(msg, e);
            }
        }
    }
    
    /**
     * Closes the PreparedStatement.
     */
    private void closeStatement(PreparedStatement ps)
    {
        if (ps != null)
        {
            try
            {
                ps.close();
            }
            catch (RuntimeException re)
            {
                throw re;
            }
            catch (Exception e)
            {
                String msg = "DataSourceResourceLoader: problem when closing PreparedStatement ";
                log.error(msg, e);
                throw new VelocityException(msg, e);
            }
        }
    }
        

    /**
     * Creates the following PreparedStatement query :
     * <br>
     *  SELECT <i>columnNames</i> FROM <i>tableName</i> WHERE <i>keyColumn</i>
     *     = '<i>templateName</i>'
     * <br>
     * where <i>keyColumn</i> is a class member set in init()
     *
     * @param conn connection to datasource
     * @param columnNames columns to fetch from datasource
     * @param templateName name of template to fetch
     * @return PreparedStatement
     */
    private PreparedStatement getStatement(final Connection conn,
                               final String columnNames,
                               final String templateName) throws SQLException
    {
        PreparedStatement ps = conn.prepareStatement("SELECT " + columnNames + " FROM "+ tableName + " WHERE " + keyColumn + " = ?");
        ps.setString(1, templateName);
        return ps;
    }

}
