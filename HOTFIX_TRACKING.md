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
53. https://github.com/dotCMS/core/issues/21849 : Upgrade tomcat to latest minor version #21849
54. https://github.com/dotCMS/core/issues/21219 : Persist Contentlets as Json : Phase II #21219
55. https://github.com/dotCMS/core/issues/21858 : Remove system fields #21858
56. https://github.com/dotCMS/core/issues/22236 : Pagination not working when querying related content by Rest API #22236
57. https://github.com/dotCMS/core/issues/22763 : Copy folder duplicates content on pages #22763
58. https://github.com/dotCMS/core/issues/22520 : Copy Site does not copy all content when a site has more than 10000 contentlets #22520
59. https://github.com/dotCMS/core/issues/22910 : Multilingual content related slows down edit content screen #22910

**Release-22.03.4**

60. https://github.com/dotCMS/core/issues/23015 : Bulk update while importing is extremely slow #23015
61. https://github.com/dotCMS/core/issues/21966 : Unable to save/publish Page on auth #21966
62. https://github.com/dotCMS/core/issues/22582 : Import/Export Content Tool generating diff key/value field #22582
63. https://github.com/dotCMS/core/issues/22549 : Relationship is not showing the right parent #22549
64. https://github.com/dotCMS/core/issues/22512 : Allow follow redirects to be configured #22512
65. https://github.com/dotCMS/core/issues/22522 : Remote URL access should be run in a threadpool #22522
66. https://github.com/dotCMS/core/issues/23365 : Placeholder Issue #23365
67. https://github.com/dotCMS/core/issues/23401 : Insure zip file integrity #23401
68. https://github.com/dotCMS/core/issues/23474 : Switch Site endpoint is nor working if user doesn't have access to Sites Portlet #23474

**Release-22.03.5**

69. https://github.com/dotCMS/core/issues/23440 : Categories being removed from content items when updating content via CSV file upload #23440
70. https://github.com/dotCMS/core/issues/23761 : Unable to start LTS release using MSSQL #23761
71. https://github.com/dotCMS/core/issues/23280 : Unable to assign contentlets in a secondary language from the task screen if there is a working version of the default language #23280
72. https://github.com/dotCMS/core/issues/21735 : Content with 2 Relationship Fields of Same Type Also Shows Non-Available Languages #21735
73. https://github.com/dotCMS/core/issues/21832 : [Static Push] Removing a Page removes all files related to the page, not only the page #21832
74. https://github.com/dotCMS/core/issues/21929 : PullRelatedField is not pulling the content in any order #21929
75. https://github.com/dotCMS/core/issues/22140 : Contentlet loosing in the page after layout change #22140
76. https://github.com/dotCMS/core/issues/22323 : Relationships will get cleared if you Save a contentlet before they are loaded #22323
77. https://github.com/dotCMS/core/issues/22840 : Add relationship is broken in many to one relationship #22840
78. https://github.com/dotCMS/core/issues/22667 : Cannot change date or time fields to never on an existing contentlet #22667
79. https://github.com/dotCMS/core/issues/22729 : Image Selector Issues #22729
80. https://github.com/dotCMS/core/issues/24272 : File Browse Not Working as Expected #24272
81. https://github.com/dotCMS/core/issues/24358 : File Browser Only Showing Content in the Backend Language #24358
82. https://github.com/dotCMS/core/issues/22756 : Removing Categories through REST API #22756
83. https://github.com/dotCMS/core/issues/22850 : Catch Throwable in PublisherJobQueue #22850
84. https://github.com/dotCMS/core/issues/23810 : Asset download missing directories 8 and 9 #23810
85. https://github.com/dotCMS/core/issues/23384 : Thumbnailing Large/Complex pdfs can blow heap #23384
86. https://github.com/dotCMS/core/issues/23009 : Add a way to set categories on workflow #23009
87. https://github.com/dotCMS/core/issues/23341 : Menu Nav in the BE not showing options when collapse #23341
88. https://github.com/dotCMS/core/issues/23890 : Navigation showing duplicating results when multilingual page and requesting non-default lang #23890
89. https://github.com/dotCMS/core/issues/24158 : CMSFilter updating language param #24158
90. https://github.com/dotCMS/core/issues/24436 : Task05380ChangeContainerPathToAbsolute repeating site if container was added several times
91. https://github.com/dotCMS/core/issues/22508 : Error when try to generate a Bundle with a User #22508
92. https://github.com/dotCMS/core/issues/23078 : Unable to add categories to bundles #23078
93. https://github.com/dotCMS/core/issues/24441 : Unable to copy a folder when the folder contains a page that has two versions live and draft #24441

**Release-22.03.6**

94. https://github.com/dotCMS/core/issues/24245 : Publishing contentlet takes a long time when there are many locales/relationships #24245
95. https://github.com/dotCMS/core/issues/23889 : Design Template Permissions Tab Doesn't Load #23889
96. https://github.com/dotCMS/core/issues/22532 : Relationships are not respecting changes to requiredness #22532
97. https://github.com/dotCMS/core/issues/21652 : [WebP] : Image file using the WebP filter loads with white background in Safari #21652
98. https://github.com/dotCMS/core/issues/21742 : 400 Bad requests (such as malformed URIs) do not make it past Tomcat and display a stacktrace in the browser #21742
99. https://github.com/dotCMS/core/issues/22087 : Content not assigned to workflow can fail to push publish #22087
100. https://github.com/dotCMS/core/issues/22667 : Cannot change date or time fields to never on an existing contentlet #22667
101. https://github.com/dotCMS/core/issues/23267 : Healthcheck function doesn't clean up after itself - 5.3.4.1 #23267
102. https://github.com/dotCMS/core/issues/23449 : Filter does not work on the image selector for image fields #23449
103. https://github.com/dotCMS/core/issues/23807 : Make Image Permits less restrictive #23807
104. https://github.com/dotCMS/core/issues/23396 : Double quotes in content title - error trying to show data in relate table. #23396
