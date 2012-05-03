Plugin requires a role in the table cms_role with role_key = 'dotcms.org.maintenance' 
that is the role to which by default task are reassigned to. This role all permission on all content type in order to access them 
after task escalation.

To add plugin manual functionality is necessary to add Tab called EXPIRY CONTENT; maybe tabs must be able only to Admin User.

Plugin deploy will also add a scheduled job who will run once a day (01:00 PM) and move all task blocked for more than 20 days to 
Maintenance Role.