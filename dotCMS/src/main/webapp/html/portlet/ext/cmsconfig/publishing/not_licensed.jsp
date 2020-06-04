<%@page import="com.liferay.portal.language.LanguageUtil"%>
<style>
    #dotAjaxMainHangerDiv, #dotAjaxMainDiv { height: 100%; }
    .portlet-wrapper { display: flex; height: 100%; justify-content: center; }
    .unlicense-content { align-self: center; margin-top: 24px; text-align: center; }
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
        <i class="material-icons" style="font-size: 120px;">cloud_upload</i>
        <h4><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.publishing-queue")%></h4>
        <p><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.publishing-queue")%> <%=LanguageUtil.get(pageContext, "only-available-in-enterprise")%></p>
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
