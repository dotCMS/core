This plugin can set connections to a Salesforce Server, 
retrieve roles keys stored on a field and sync the roles for
the logged-in user. This is made by using OAuth 2.0 User-Password 
flow as authorization method, where Salesforce assigns an access 
token to the current user session. This access token is required
for getting information from Salesforce server.

Please see for more info
https://help.salesforce.com/help/doc/en/remoteaccess_oauth_username_password_flow.htm

This plugin works for frontend and backend login. Also, it
overrides /html/portal/login.jsp file with a custom one that 
hides the "Forgot Password" link.

Notes:

1) in PLUGIN_FOLDER/conf/dotmarketing-ext.properties file, this 
variable must be set to true:

SALESFORCE_LOGIN_FILTER_ON=true

2) in PLUGIN_FOLDER/conf/plugin.properties file, these
variables are required

#SALESFORCE URL FOR REQUESTING ACCESS TOKEN
salesforce_token_request_url=https://test.salesforce.com/services/oauth2/token
#SALESFORCE VARIABLES - REQUIRED FOR USER-PASSWORD FLOW AUTH AND ACCESS TOKEN RETRIEVAL
salesforce_grant_type=password
salesforce_client_id=xxxx
salesforce_client_secret=xxxx
salesforce_username=xxxx
salesforce_password=xxxx
salesforce_api_security_token=xxxx
#EXPECTED RETURN FORMAT, COULD BE json, xml OR urlencoded
salesforce_return_format=json
#URL TO MAKE SOSL SEARCH ON SALESFORCE (MUST INCLUDE API VERSION)
salesforce_search_url=/services/data/v26.0/search
#OBJECT TO SEARCH ON SALESFORCE. MUST CONTAIN ONE OF THESE VALUES: CONTACT, USER, ACCOUNT
salesforce_search_object=CONTACT
#ROLE FIELD TO MATCH FOR ROLES SYNC. THIS FIELD MUST EXIST ON THE PREVIOUS SEARCH OBJECT
salesforce_search_object_field=AccessRights__c
#SAVE LOG MESSAGES ON DOTCMS-USERACTIVITY LOG
save_log_info_useractivity_log=true
#SAVE LOG MESSAGES ON DOTCMS LOG
save_log_info_dotcms_log=true

3) SSL for Tomcat must be enabled. OAuth 2.0 works only with https connections.

Please see for more info
http://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html

4) Frontend/Backend login must be set to email address. 