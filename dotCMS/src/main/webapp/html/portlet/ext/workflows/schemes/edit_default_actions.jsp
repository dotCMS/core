<%@page import="com.liferay.portal.language.LanguageUtil"%>


<%
    String schemeId = request.getParameter("schemeId");
%>


<div>
    <div>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-New")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionNEW" id="defaultActionNEW" onChange='schemeAdmin.changeWorkflowDefaultAction("NEW","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Edit")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionEDIT" id="defaultActionEDIT" onChange='schemeAdmin.changeWorkflowDefaultAction("EDIT","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Publish")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionPUBLISH" id="defaultActionPUBLISH" onChange='schemeAdmin.changeWorkflowDefaultAction("PUBLISH","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Unpublish")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionUNPUBLISH" id="defaultActionUNPUBLISH" onChange='schemeAdmin.changeWorkflowDefaultAction("UNPUBLISH","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Archive")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionARCHIVE" id="defaultActionARCHIVE" onChange='schemeAdmin.changeWorkflowDefaultAction("ARCHIVE","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Unarchive")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionUNARCHIVE" id="defaultActionUNARCHIVE" onChange='schemeAdmin.changeWorkflowDefaultAction("UNARCHIVE","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Delete")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionDELETE" id="defaultActionDELETE" onChange='schemeAdmin.changeWorkflowDefaultAction("DELETE","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Destroy")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionDESTROY" id="defaultActionDESTROY" onChange='schemeAdmin.changeWorkflowDefaultAction("DESTROY","<%=schemeId%>")' required="false" tabindex="-1">
                </select>
            </dd>
        </dl>

    </div>

</div>
