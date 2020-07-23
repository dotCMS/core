#DOTCMS_CORE

Version: 22


This release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/18364 : All Vanity URLs intermittently stop working, cache flush required to fix

2. https://github.com/dotCMS/core/issues/18214 : Spaces in File Assets are replaced with "+" instead of "%20"

3. https://github.com/dotCMS/core/issues/18479 : [core] : Key Value field is not returning data in correct order

4. https://github.com/dotCMS/core/issues/18319 : [OSGi] : Concurrent access to the bundle cache is causing problems

5. https://github.com/dotCMS/core/issues/18369 : Cannot edit file asset with indexed Binary field containing specific file content
   NOTE: Changes related to the new drag-and-drop were not included as it doesn't exist in 5.2.8 yet.

6. https://github.com/dotCMS/core/issues/18525 : [core] : Get related parents fails when related content is archived

7. https://github.com/dotCMS/core/issues/18501 : Race condition when Felix inits

8. https://github.com/dotCMS/core/issues/18626 : [WebDAV] : Default Index Policy for files is failing intermittently

9. https://github.com/dotCMS/core/issues/18621 : [Push Publishing] : Wrapper object is dropping the Identifier value

10. https://github.com/dotCMS/core/issues/18616 : Converting relationships results in a new relationship inode

11. https://github.com/dotCMS/core/issues/18245 : MonitorResource consumes too many resources

12. https://github.com/dotCMS/core/issues/18072 : [core] : Logging improvement when content reindex fails

13. https://github.com/dotCMS/core/issues/18673 : [Reindex] : Inconsistent data is causing the reindex to fail

14. https://github.com/dotCMS/core/issues/18641 : [Content Export] : Code improvement required to remove content export limit.

15. https://github.com/dotCMS/core/issues/18697 : [Workflows] : The AVAILABLE WORKFLOW ACTIONS button is failing with specific Lucene queries

16. https://github.com/dotCMS/core/issues/18764 : dojo/parser error in console when adding more than 2 relationship fields - 5.2x

17. https://github.com/dotCMS/core/issues/18848 : [rest] : Bad request when passing 'uri' parameter to endpoint '/v1/folder/sitename/{siteName}/uri/{uri : .+}'

18. https://github.com/dotCMS/support/tree/master/hotfixes/hotfix-legacy-ids_v5.0.3 : Fixes issues related to IDs that are not valid UUIDs
    NOTE: As per Will's request, the fix included in this plugin is intended to be a patch, and will not make it to the official distribution.

19. https://github.com/dotCMS/core/issues/18855 : NPE in ContainerWebAPI.getPersonalizedContentList

20. https://github.com/dotCMS/core/issues/18292 : When try to submit a form get permission error

21. https://github.com/dotCMS/core/issues/18744 : Global URLMaps

22. https://github.com/dotCMS/core/issues/18187 : Aliases not working correctly

23. https://github.com/dotCMS/core/issues/18964 : [REST] : Missing endpoint for retrieving folder tree 