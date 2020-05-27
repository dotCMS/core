<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script type="text/javascript" src="/html/portlet/ext/workflows/schemes/workflow_js.jsp" ></script>

<style type="text/css">
	@import "/html/portlet/ext/workflows/schemes/workflow.css"; 
	@import "/html/js/dragula-3.7.2/dragula.min.css"; 
</style>	

<div class="portlet-wrapper">
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">
			<li>
				<a href="javascript:schemeAdmin.show()"><%=LanguageUtil.get(pageContext, "Workflow")%></a>
			</li>
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "Schemes")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	
	<%if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){ %>
        <style>
            #dotAjaxMainHangerDiv, #dotAjaxMainDiv { height: 100%; }
            .portlet-wrapper { display: flex; height: 100%; justify-content: center; }
            .unlicense-content { align-self: center; text-align: center; }
            .unlicense-content i, .unlicense-content h4 { color: #b3b1b8 }
            .unlicense-content h4 { font-size:24px; font-weight:400; line-height:36px; }
            .unlicense-content p { font-size:14px; font-weight:400; line-height:21px; margin-block-end:14px; margin-block-start:14px; }
            .unlicense-content ul { display: inline-block; margin-block-end: 14px; margin-block-start: 14px;}
            .unlicense-content ul li { line-height: 21px; list-style-type:disc; text-align: left; }
            .unlicense-content ul li a { color: var(--color-main); font-size:14px; }
            .request-license-button { background: var(--color-main); border: solid 1px transparent; border-radius: 2px; color: #fff; display: inline-block; line-height: 36px; padding: 0 24px; text-decoration: none; text-transform: uppercase; }
            .request-license-button:hover { background: var(--color-main_mod); border: solid 1px transparent; }
        </style>
        <div class="portlet-wrapper">
            <div class="unlicense-content">
                <i class="material-icons" style="font-size: 120px;">device_hub</i>
                <h4><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.workflow-schemes")%></h4>
                <p><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.workflow-schemes")%> <%=LanguageUtil.get(pageContext, "only-available-in-enterprise")%></p>
                <ul>
                    <li>
                        <a target="_blank" href="https://dotcms.com/product/features/feature-list"><%=LanguageUtil.get(pageContext, "Learn-more-about-dotCMS-Enterprise")%></a>
                    </li>
                    <li>
                        <a target="_blank" href="https://dotcms.com/contact-us/"><%=LanguageUtil.get(pageContext, "Contact-Us-for-more-Information")%></a>
                    </li>
                </ul>
                <p style="display: block;">
                    <a class="request-license-button" href="https://dotcms.com/licensing/request-a-license-3/index"><%=LanguageUtil.get(pageContext, "request-trial-license")%></a>
                </p>
            </div>
        </div>
	<%}else{ %>
		<div id="hangWorkflowMainHere">
		
		</div>
	<%} %>

</div>

<div dojoType="dijit.Dialog" style="display: none" id="addEditScheme">
	<div id="addEditSchemeHanger"></div>
</div>

