plugin-dotcms-email 
=====================

This plugin provides a Workflow Actionlet that can be used to send arbitrary emails to a user or users.  Coupling this plugin with some of the more advanced workflow features and you can build automated responders, timed responses and even user triggers automatic follow up emails that are customized to the data you have collected in the form submittal

Every field in the email can contain velocity and can access the submitted content.  So, to send a custom email to the email address stored in a field called userEmail, put $content.userEmail in the 'to email' field and the system will replace it with the variables from the content  

The attachment field can work 2 ways.  You can pass it a path, such as "/images/logo.png" and it will include the file on that path, or you can pass it the velocity variable for a field on the submitted content - this will attach the associated content to the email.

This was written for Dotcms 2.5 

Please make sure the following entries are in your OSGI exported packages file

```
com.dotmarketing.portlets.workflows.actionlet,
org.apache.velocity.context,
com.dotmarketing.beans,
com.dotmarketing.business,
com.dotmarketing.cmis.proxy,
com.dotmarketing.portlets.contentlet.model,
com.dotmarketing.portlets.workflows.actionlet,
com.dotmarketing.portlets.workflows.model,
com.dotmarketing.util,
com.dotmarketing.osgi,
com.liferay.portal.util,
com.dotmarketing.portlets.contentlet.business,
org.apache.felix.http.api.ExtHttpService,
org.apache.felix.http.api,
com.dotmarketing.portlets.structure.model,
javax.mail.internet,
com.dotmarketing.portlets.fileassets.business,

```
