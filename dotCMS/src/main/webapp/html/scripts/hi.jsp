<%@page import="com.dotmarketing.util.UUIDGenerator"%>
<%@page import="org.elasticsearch.action.delete.DeleteRequest"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="org.elasticsearch.action.index.IndexRequest"%>
<%@page import="org.elasticsearch.client.Client"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESClient"%>
<%

Client client=new ESClient().getClient();
String working=APILocator.getIndiciesAPI().loadIndicies().working;
String live=APILocator.getIndiciesAPI().loadIndicies().live;
String id=UUIDGenerator.generateUuid();
client.index(new IndexRequest(working,"content",id).source("{\"forms.formid\":\"yyy\"}")).actionGet(5000);
client.index(new IndexRequest(live,"content",id).source("{\"forms.formid\":\"yyy\"}")).actionGet(5000);


client.delete(new DeleteRequest(working,"content",id)).actionGet(5000);
client.delete(new DeleteRequest(live,"content",id)).actionGet(5000);

%>