<%@page import="com.dotcms.content.elasticsearch.business.ESMappingAPIImpl"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESMigrationUtil"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.content.business.ContentMappingAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.viewtools.StructuresWebAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>

<%


ESMigrationUtil emu = new ESMigrationUtil();


//response.setContentType("text/plain");


long then  = System.currentTimeMillis();

emu.migrateAllStructures();


out.println("Migration took :" + (System.currentTimeMillis() - then) + "ms");




%>




