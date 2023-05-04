#DOTCMS_CORE


This maintenance release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/20725 : Error upgrading to 21.06 using a managed database

2. https://github.com/dotCMS/core/issues/20647 : Problems the first time you try to navigate to the network tab

3. https://github.com/dotCMS/core/issues/20636 : Improving error messages for WorkflowAPIImpl

4. https://github.com/dotCMS/core/issues/20629 : EMA should always expect UTF8

5. https://github.com/dotCMS/core/issues/20578 : MonitorResource should cache good responses / fix IPUtils

6. https://github.com/dotCMS/core/issues/20557 : Site Resource improvements

7. https://github.com/dotCMS/core/issues/20505 : Unable to create a multilingual blog

8. https://github.com/dotCMS/core/issues/20501 : Workflows: Can not copy a workflow with Notify Assigned Actionlet is used

9. https://github.com/dotCMS/core/issues/20666 : Move to a glibc based docker image

10. https://github.com/dotCMS/core/issues/20405 : URL field values are not included in Pages exported from Content->Search

11. https://github.com/dotCMS/core/issues/20519 : Class in rows are lost when you remove a row #20519

12. https://github.com/dotCMS/core/issues/20677 : Users without the 'Login As' Role are still able to select Login As and see all Users #20677

13. https://github.com/dotCMS/core/issues/20721 : "Login As" Data Constraints #20721

14. https://github.com/dotCMS/core/issues/20674 : Unable to pick time in earlier zone #20674

15. https://github.com/dotCMS/core/issues/20659 : The Set Response Headers Rule Action does not allow single quotes in the value #20659

16. https://github.com/dotCMS/core/issues/20623 : Container displays Add button twice under specific situations #20623

17. https://github.com/dotCMS/core/issues/20719 : NPE on tags via graphql if no tags present #20719

18. https://github.com/dotCMS/core/issues/20690 : Add PushPublishingListener to dotCDN #20690

19. https://github.com/dotCMS/core/issues/20688 : Rest API Update Site do not update siteName #20688

20. https://github.com/dotCMS/core/issues/20685 : [Vanity URL] : Missing parameters in Vanity URL cause problems in API #20685

21. https://github.com/dotCMS/core/issues/20649 : IndexPolicy do not respected on wf toolview #20649

22. https://github.com/dotCMS/core/issues/20642 : WYSIWYG fields insert image does not respect the setting of WYSIWYG_IMAGE_URL_PATTERN #20642

23. https://github.com/dotCMS/core/issues/20640 : Tika failing to init, hanging dotCMS startup #20640

24. https://github.com/dotCMS/core/issues/20638 : Update Site do not update aliases #20638

25. https://github.com/dotCMS/core/issues/20597 : Generating resource link for File Asset with Legacy Identifier is failing #20597

26. https://github.com/dotCMS/core/issues/20591 : Don't try to move or rewrite license.zip file on startup. #20591

27. https://github.com/dotCMS/core/issues/20494 : Error retrieving content references in multilingual setup #20494

28. https://github.com/dotCMS/core/issues/20504 : Create a "Bulk Move" Action for Content #20504

29. https://github.com/dotCMS/core/issues/7342 : System allows to create exact same language more than twice #7342

30. https://github.com/dotCMS/core/issues/20714 : NPE trying to fix folder conflicts #20714

31. https://github.com/dotCMS/core/issues/20707 : Advance Image Transformations and SASS compilation not working on Static Publish and Time Machine #20707

32. https://github.com/dotCMS/core/issues/20669 : Error deploying a fresh install using mssql #20669

33. https://github.com/dotCMS/core/issues/20469 : When generating a site statically, we need to generate the default language last. #20469

34. https://github.com/dotCMS/core/issues/19215 : Add the option to show fields in a relationship field's overview #19215

35. https://github.com/dotCMS/core/issues/18014 : FileTool breaks contentlet rendering if the file/image is not live #18014

36. https://github.com/dotCMS/core/issues/18111 : Copy Workflow results in incorrect next step #18111

37. https://github.com/dotCMS/core/issues/19931	: Automatically delete old ES indices (indexes)

38. https://github.com/dotCMS/core/issues/20799 : When try to import a Bundle get a OutOfMemoryError #20799

39. https://github.com/dotCMS/core/issues/20965 : Response code in Integrity Checker producing a 502 #20965

40. https://github.com/dotCMS/core/issues/20616 : Button for Navigation Reorder is not working correctly #20616

41. https://github.com/dotCMS/core/issues/20746 : Content Export tool is not exporting relationships correctly #20746

42. https://github.com/dotCMS/core/issues/20722 : [Reindex] : Perform Site check before Folder check #20722

43. https://github.com/dotCMS/core/issues/20452 : "Back end user" role breaks front end user permissions #20452

44. https://github.com/dotCMS/core/issues/18301 : SASS not working with multiple independent SCSS files. Causes hanging on front-end #18301

45. https://github.com/dotCMS/core/issues/20791 : [REST API] : Page API returns publishDate and modDate fields with the same value #20791

46. https://github.com/dotCMS/core/issues/20801 : Warning log message incomplete for not allowed categories when retrieving content #20801

47. https://github.com/dotCMS/core/issues/21313 : Push publishing sometimes changes the order of contentlets in the multi-tree table on multilingual pages #21313

48. https://github.com/dotCMS/core/issues/20995 : Use a different connection for PostgresPubSub.publish #20995

49. https://github.com/dotCMS/core/issues/21393 : log4j2 zero day exploit #21393

50. https://github.com/dotCMS/core/issues/20793 : Upgrade log4j version to permit JsonLayout custom key-value pairs #20793

51. https://github.com/dotCMS/core/issues/21392 : Make userId immutable #21392

52. https://github.com/dotCMS/core/issues/21485 : Upgrade log4j2 to 2.17.1 #21485

53. https://github.com/dotCMS/core/issues/21537 : [Static Publishing] : Problem with multi-language contents #21537

54. https://github.com/dotcms/core/issues/21267 : ReindexThread dies with bad data #21267, 2nd inclusion with additional fixes

**Release-21.06.7**

56. https://github.com/dotCMS/core/issues/21204 : [Site Copy] : Copying a Site randomly fails #21204
57. https://github.com/dotCMS/core/issues/21363 : db passwords with characters (specifically @ and possibly others) will break pub/sub due to the connection string #21363
58. https://github.com/dotCMS/core/issues/21252 : Add the ability to stop/abort a workflow on velocity script actionlet #21252
59. https://github.com/dotCMS/core/issues/21097 : Past Time Machine not working #21097
60. https://github.com/dotCMS/core/issues/20773 : SAML - Allow expression substitution from SAML roles mapped to dotCMS roles by role key #20773
61. https://github.com/dotCMS/core/issues/20805 : Unable to push publish user #20805
62. https://github.com/dotCMS/core/issues/20757 : We need to obfuscate some environmental variables #20757
63. https://github.com/dotCMS/core/issues/21791 : Sanitizing file name #21791
64. https://github.com/dotCMS/core/issues/20971 : Large Bundles make the viewing publishing queue slow #20971
65. https://github.com/dotCMS/core/issues/21699 : [Push Publishing] : Single quote in content's title breaks JavaScript code in the portlet #21699

**Release-21.06.8**

66. https://github.com/dotCMS/core/issues/20655 : DotAsset Title is presented different on two search types #20655
67. https://github.com/dotCMS/core/issues/21516 : Change to FILE_SYSTEM for Metadata Storage #21516
68. https://github.com/dotCMS/core/issues/20446 : Unable to load very large images #20446
69. https://github.com/dotCMS/core/issues/21617 : BinaryMap still hits fs #21617
70. https://github.com/dotCMS/core/issues/21658 : [SASS] : Modification to .SCSS file is not refreshing compiled CSS file. #21658
71. https://github.com/dotCMS/core/issues/21811 : Limit of 250 Widgets and Forms in Content Selector Popup #21811
72. https://github.com/dotCMS/core/issues/21249 : [Content] : Problems editing pages for Users with both FE and BE access #21249
73. https://github.com/dotCMS/core/issues/21613 : URL Mapped content is not showing up in site search when using the include functionality #21613
74. https://github.com/dotCMS/core/issues/21670 : #editContentlet macro does not work in the LTS release even though it does not have inline editing #21670
75. https://github.com/dotCMS/core/issues/21714 : content in a "drafted state" can fail to push publish. #21714
76. https://github.com/dotCMS/core/issues/20850 : When you remove a Site/Folder field it moves all contentlets to SYSTEM_HOST regardless of where the Content Type lives #20850

**Release-21.06.9**

77. https://github.com/dotCMS/core/issues/22390 : Disallow Matrix Parameters in URIs #22390
78. https://github.com/dotCMS/core/issues/22410 : Change logo in backoffice UI not working for all logo sizes #22410
79. https://github.com/dotCMS/core/issues/22430 : Do not cache bad shorty DB results #22430

**Release-21.06.10**

80. https://github.com/dotCMS/core/issues/22478 : NormalizationFilter does not set javax.servlet.forward.request_uri #22478
81. https://github.com/dotCMS/core/issues/22498 : Normalization filter is too aggressive #22498

**Release-21.06.11**

82. https://github.com/dotCMS/core/issues/19358 : Large amounts of content in structure can deadlock the DB when repermissioning #19358
83. https://github.com/dotCMS/core/issues/19569 : Rely on Tomcat's RemoteIpValve for dns resolution - was: External service interaction (DNS) #19569
84. https://github.com/dotCMS/core/issues/21619 : ContentTypeFactoryImpl Getting Config values cause unneeded load #21619
85. https://github.com/dotCMS/core/issues/21624 : [Push Publishing] : Pushing single archived content un-publishes all its versions in receiving instance #21624
86. https://github.com/dotCMS/core/issues/22603 : missing locales for velocity #22603
87. https://github.com/dotCMS/core/issues/22124 : When logged in to front end, automatically redirect to back-end edit mode #22124
88. https://github.com/dotCMS/core/issues/22204 : We are not notifying the UI about the bad request error changing password #22204
89. https://github.com/dotCMS/core/issues/22149 : The push publish button in the expansion menu on the users portlet doesn't just push the selected user, it pushes all users #22149
90. https://github.com/dotCMS/core/issues/22237 : Limited User Can't Open Page if it has a content that doesn't have permissions over #22237
91. https://github.com/dotCMS/core/issues/22266 : Issue with static push publishing when pushing with a default language other than English #22266
92. https://github.com/dotCMS/core/issues/22349 : Missing database migration tasks when migrating from <=5.1.6 #22349
93. https://github.com/dotCMS/core/issues/22696 : Can't save changes in Work Flow if user with special characters or new user is added. #22696
94. https://github.com/dotCMS/core/issues/20491 : Many-to-many relationships between a single content type removes relationships on save/publish #20491
95. https://github.com/dotCMS/core/issues/21482 : [REST] : Updating one Binary Field is removing the value from the other Binary Field #21482
96. https://github.com/dotCMS/core/issues/22852 : Update java version of java-base #22852

**Release-21.06.12**

97. https://github.com/dotCMS/core/issues/23015 : Bulk update while importing is extremely slow #23015
98. https://github.com/dotCMS/core/issues/22582 : Import/Export Content Tool generating diff key/value field #22582
99. https://github.com/dotCMS/core/issues/23474 : Switch Site endpoint is nor working if user doesn't have access to Sites Portlet #23474
100. https://github.com/dotCMS/core/issues/21400 : TempFileApi should only import by URL if user is admin #21400
101. https://github.com/dotCMS/core/issues/22512 : Allow follow redirects to be configured #22512
102. https://github.com/dotCMS/core/issues/22522 : Remote URL access should be run in a threadpool #22522
103. https://github.com/dotCMS/core/issues/23365 : Placeholder Issue #23365
104. https://github.com/dotCMS/core/issues/23401 : Insure zip file integrity #23401

**Release-21.06.13**

105. https://github.com/dotCMS/core/issues/23440 : Categories being removed from content items when updating content via CSV file upload #23440
106. https://github.com/dotCMS/core/issues/21832 : [Static Push] Removing a Page removes all files related to the page, not only the page #21832
107. https://github.com/dotCMS/core/issues/21929 : PullRelatedField is not pulling the content in any order #21929
108. https://github.com/dotCMS/core/issues/22236 : Pagination not working when querying related content by Rest API #22236
109. https://github.com/dotCMS/core/issues/21735 : Content with 2 Relationship Fields of Same Type Also Shows Non-Available Languages #21735
110. https://github.com/dotCMS/core/issues/22323 : Relationships will get cleared if you Save a contentlet before they are loaded #22323
111. https://github.com/dotCMS/core/issues/22549 : Relationship is not showing the right parent #22549
112. https://github.com/dotCMS/core/issues/22667 : Cannot change date or time fields to never on an existing contentlet #22667
113. https://github.com/dotCMS/core/issues/22729 : Image Selector Issues #22729
114. https://github.com/dotCMS/core/issues/22756 : Removing Categories through REST API #22756
115. https://github.com/dotCMS/core/issues/22763 : Copy folder duplicates content on pages #22763
116. https://github.com/dotCMS/core/issues/22850 : Catch Throwable in PublisherJobQueue #22850
117. https://github.com/dotCMS/core/issues/22910 : Multilingual content related slows down edit content screen #22910
118. https://github.com/dotCMS/core/issues/23009 : Add a way to set categories on workflow #23009
119. https://github.com/dotCMS/core/issues/23062 : PP queue breaks if push of a bundle fails #23062
120. https://github.com/dotCMS/core/issues/23128 : UI showing wrong date when changing Timezones #23128
121. https://github.com/dotCMS/core/issues/23341 : Menu Nav in the BE not showing options when collapse #23341
122. https://github.com/dotCMS/core/issues/23280 : Unable to assign contentlets in a secondary language from the task screen if there is a working version of the default language #23280
123. https://github.com/dotCMS/core/issues/23384 : Thumbnailing Large/Complex pdfs can blow heap #23384
124. https://github.com/dotCMS/core/issues/21415 : Prevent XMLTool from fetching embedded entities #21415

**Release-21.06.14**

125. https://github.com/dotCMS/core/issues/22840 : Add relationship is broken in many to one relationship #22840

**Release-21.06.15**

125. https://github.com/dotCMS/core/issues/24781 : Performance Optimization - don't force field invalidations for non-velocity fields #24781