#DOTCMS_CORE


This maintenance release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/21811 : Limit of 250 Widgets and Forms in Content Selector Popup #21811
2. https://github.com/dotCMS/core/issues/21879 : OSGI undeploy does not work #21879
3. https://github.com/dotCMS/core/issues/21781 : Add Ability to Create Unique Field Values Per Site #21781
4. https://github.com/dotCMS/core/issues/21670 : #editContentlet macro does not work in the LTS release even though it does not have inline editing #21670
5. https://github.com/dotCMS/core/issues/21714 : content in a "drafted state" can fail to push publish. #21714
6. https://github.com/dotCMS/core/issues/21613 : URL Mapped content is not showing up in site search when using the include functionality #21613
7. https://github.com/dotCMS/core/issues/22078 : Missing DB_MIN_IDLE parameter #22078
8. https://github.com/dotCMS/core/issues/22055 : OSGI: Read exports from uploaded fragment, then discard fragment #22055

**Release-22.03.2**

9. https://github.com/dotCMS/core/issues/22288 : DotParse not respecting config property #22288
10. https://github.com/dotCMS/core/issues/22094 : No HTML RegEx Invalidates Period (.) When Used #22094
11. https://github.com/dotCMS/core/issues/22287 : Reading osgi import packages fails when osgi version specifies a range #22287
12. https://github.com/dotCMS/core/issues/22319 : Rest API: Pulling Relationships from child #22319
13. https://github.com/dotCMS/core/issues/22390 : Disallow Matrix Parameters in URIs #22390
14. https://github.com/dotCMS/core/issues/22266 : Issue with static push publishing when pushing with a default language other than English #22266
15. https://github.com/dotCMS/core/issues/21882 : OSGI restart needs to be cluster aware #21882
16. https://github.com/dotCMS/core/issues/22410 : Change logo in backoffice UI not working for all logo sizes #22410
17. https://github.com/dotCMS/core/issues/22430 : Do not cache bad shorty DB results #22430
18. https://github.com/dotCMS/core/issues/22137 : Page with show_on_menu selected breaks Reorder Nav #22137
19. https://github.com/dotCMS/core/issues/22237 : Limited User Can't Open Page if it has a content that doesn't have permissions over #22237
20. https://github.com/dotCMS/core/issues/21169 : Change datatype of date fields in MSSQL #21169
21. https://github.com/dotCMS/core/issues/22381 : Update Date/Times before adding Timezone to date fields #22381
22. https://github.com/dotCMS/core/issues/22475 : Update SSL conf for docker build #22475
23. https://github.com/dotCMS/core/issues/22198 : We are changing the relationship selection once that you create the field #22198

**Release-22.03.3**

24. https://github.com/dotCMS/core/issues/22692 : We replace the UserID with a hashed version of the UserID when creating SAML users #22692
25. https://github.com/dotCMS/core/issues/22864 : Categories - $contentlet.categories can be slow #22864
26. https://github.com/dotCMS/core/issues/23062 : PP queue breaks if push of a bundle fails #23062
27. https://github.com/dotCMS/core/issues/22156 : SAML: Redirect is not working #22156
28. https://github.com/dotCMS/core/issues/19358 : Large amounts of content in structure can deadlock the DB when repermissioning #19358
29. https://github.com/dotCMS/core/issues/19569 : Rely on Tomcat's RemoteIpValve for dns resolution - was: External service interaction (DNS) #19569
30. https://github.com/dotCMS/core/issues/22916 : Need a way to run UT in LTS Releases #22916
31. https://github.com/dotCMS/core/issues/21482 : [REST] : Updating one Binary Field is removing the value from the other Binary Field #21482
32. https://github.com/dotCMS/core/issues/21885 : Template show archive not working #21885
33. https://github.com/dotCMS/core/issues/21619 : ContentTypeFactoryImpl Getting Config values cause unneeded load #21619
34. https://github.com/dotCMS/core/issues/21612 : Template Publish UI Suggestion #21612
35. https://github.com/dotCMS/core/issues/21694 : [WebP] : Default quality for WebP filter is breaking image file #21694
36. https://github.com/dotCMS/core/issues/22116 : DB Exporter does not stream output #22116
37. https://github.com/dotCMS/core/issues/22124 : When logged in to front end, automatically redirect to back-end edit mode #22124
38. https://github.com/dotCMS/core/issues/22149 : The push publish button in the expansion menu on the users portlet doesn't just push the selected user, it pushes all users #22149
39. https://github.com/dotCMS/core/issues/22204 : We are not notifying the UI about the bad request error changing password #22204
40. https://github.com/dotCMS/core/issues/22349 : Missing database migration tasks when migrating from <=5.1.6 #22349
41. https://github.com/dotCMS/core/issues/22603 : missing locales for velocity #22603
42. https://github.com/dotCMS/core/issues/22852 : Update java version of java-base #22852
43. https://github.com/dotCMS/core/issues/22605 : Execution of Task211020CreateHostIntegrityCheckerResultTables Fails on MSSQL #22605
44. https://github.com/dotCMS/core/issues/22696 : Can't save changes in Work Flow if user with special characters or new user is added. #22696
45. https://github.com/dotCMS/core/issues/23041 : PublisherQueueJob gets stuck waiting for dependencies #23041
46. https://github.com/dotCMS/core/issues/22951 : Disable submit button after submitting form #22951
47. https://github.com/dotCMS/core/issues/22673 : What's Changed UI issues #22673
48. https://github.com/dotCMS/core/issues/22338 : Content Palette not showing all available content types #22338
49. https://github.com/dotCMS/core/issues/22425 : NavTool method getNav must return only published pages. #22425
50. https://github.com/dotCMS/core/issues/22809 : Missing Nav items when upgrading #22809
51. https://github.com/dotCMS/core/issues/23128 : UI showing wrong date when changing Timezones #23128
52. https://github.com/dotCMS/core/issues/22771 : UT Task210901UpdateDateTimezones not running successfully #22771