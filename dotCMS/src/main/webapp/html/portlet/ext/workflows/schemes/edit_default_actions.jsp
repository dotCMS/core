<%@page import="com.liferay.portal.language.LanguageUtil"%>


<div>
    <div class="form-horizontal">
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-New")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionNew" id="defaultActionNew">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Edit")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionEdit" id="defaultActionEdit" onchange="">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Publish")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionPublish" id="defaultActionPublish" onchange="">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Unpublish")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionUnpublish" id="defaultActionUnpublish" onchange="">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Archive")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionArchive" id="defaultActionArchive" onchange="">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Unarchive")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionNew" id="defaultActionUnarchive" onchange="">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Delete")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionDelete" id="defaultActionDelete" onchange="">
                </select>
            </dd>
        </dl>
        <dl>
            <dt><%=LanguageUtil.get(pageContext, "Default-Action-Destroy")%>:</dt>
            <dd>
                <select dojoType="dijit.form.FilteringSelect" name="defaultActionDestroy" id="defaultActionDestroy" onchange="">
                </select>
            </dd>
        </dl>

    </div>

</div>
