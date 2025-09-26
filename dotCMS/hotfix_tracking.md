## DOTCMS BACKPORT LIST

This maintenance release includes the following code fixes:

**Release-24.04.24 LTS**

1. https://github.com/dotCMS/core/issues/28427 : Can't import Apps from old versions #28427
2. Update Telemetry Plugin
3. https://github.com/dotCMS/core/issues/28230 : spike(performance): test short lived permission cache #28230
4. https://github.com/dotCMS/core/issues/28173 : System objects should not be push published #28173
5. https://github.com/dotCMS/core/issues/28360 : Move Async Email Actionlet to Core #28360
6. https://github.com/dotCMS/core/issues/28089 : Pound char not decoded when using Rules or Vanity URLs #28089
7. https://github.com/dotCMS/core/issues/28121 : Include AI portlet by default #28121
8. https://github.com/dotCMS/core/issues/28284 : Content Types not Showing if System is true #28284
9. https://github.com/dotCMS/core/issues/28306 : Can't Import Page Assets using the export file #28306
10. https://github.com/dotCMS/core/issues/28352 : Send Email sub-action doesn't set the current user to $dotcontent viewtool #28352
11. https://github.com/dotCMS/core/issues/28689 : Update LTS Telemetry plugin #28689
12. https://github.com/dotCMS/private-issues/issues/34
13. https://github.com/dotCMS/core/issues/28695 : UT210901 UpdateDateTimezones has wrong DST query #28695
14. https://github.com/dotCMS/core/issues/28760 : Template Evaluation for Non-Default Language Pages #28760
15. https://github.com/dotCMS/core/issues/28769 : Language Fallback not working. #28769
16. https://github.com/dotCMS/core/issues/28785 : APIs return incorrect information for pages with fields using the variable name "description" #28785
17. https://github.com/dotCMS/core/issues/28609 : Remove old vulnerability-scan.yml workflow #28609
18. https://github.com/dotCMS/core/issues/23292 : Password validation failed for few characters #23292
19. https://github.com/dotCMS/core/issues/23131 : Remove 10k Push Publishing Limit #23131
20. https://github.com/dotCMS/core/issues/28897 : Content Resource v1 hits the db #28897
21. https://github.com/dotCMS/core/issues/28890 : LanguageUtil.getLanguageId is always hitting the db #28890
22. https://github.com/dotCMS/core/issues/26316 : Edit embedded content-let's through Block Editor field #26316
23. https://github.com/dotCMS/core/issues/28863 : Redis implementation should support ACL (username + password) authentication #28863
24. https://github.com/dotCMS/core/issues/29240 : UT keep running even though one failed #29240
25. https://github.com/dotCMS/core/issues/29250 : Add Annotations to WorkflowTask Bean #29250
26. https://github.com/dotCMS/core/issues/28678 : Add a command to init dotCMS with a custom database #28678
27. https://github.com/dotCMS/core/issues/29304 : Revert telemetry plugin to 24.05.29 #29304
28. SI-72
29. https://github.com/dotCMS/core/issues/28084 : Force HTML highlighting for .vtl files #28084
30. https://github.com/dotCMS/core/issues/28508 : Add Search Box for Bundle ID #28508
31. https://github.com/dotCMS/core/issues/28509 : Remove three dots at the end of the Bundle ID #28509
32. https://github.com/dotCMS/core/issues/28163 : 'alive' and 'startup' healthcheck APIs return 503 on seemingly healthy app #28163
33. https://github.com/dotCMS/core/issues/26546 : Enable better logging for getPageByPath in HTMLPageAssetAPIImpl.java #26546
34. https://github.com/dotCMS/core/issues/28366 : Uploaded images in another language than the default one do not inherit permissions #28366
35. https://github.com/dotCMS/core/issues/28838 : Category Child Permissions Not Loading #28838
36. https://github.com/dotCMS/core/issues/29079 : fileAsset Required Error while importing FileAsset through CSV #29079
37. https://github.com/dotCMS/core/issues/29209 : Wrong url when exporting file asset #29209
38. https://github.com/dotCMS/core/issues/29222 : Telemetry: not getting data after last release of the plugin #29222
39. https://github.com/dotCMS/core/issues/27959 : Integration tests for AI Viewtools #27959
40. https://github.com/dotCMS/core/pull/28781 : Extracting logic to truncate first 400 chars from prompt. #28781
41. https://github.com/dotCMS/core/issues/28719 : Write Postman Tests for Generative AI Endpoints #28719
42. https://github.com/dotCMS/core/issues/28770 : dotAI: register EmbeddingContentListener #28770
43. https://github.com/dotCMS/core/pull/28929 : Fixing issues detected by Sonar #28929
44. https://github.com/dotCMS/core/issues/28721 : Write Postman Tests for Embeddings AI Endpoints #28721
45. https://github.com/dotCMS/core/issues/28720 : Write Postman Tests for Search AI Endpoints #28720
46. https://github.com/dotCMS/core/issues/28722 : Write Postman Tests for Completions AI Endpoints #28722
47. https://github.com/dotCMS/core/issues/28990 : dotAI throws Text Embedding Error #28990
48. https://github.com/dotCMS/core/issues/28839 : Block Editor: Bubble Menu actions and marks are not working #28839
49. https://github.com/dotCMS/core/issues/29162 : Slow performance with imports in some cases #29162
50. https://github.com/dotCMS/core/issues/28857 : dotAsset is Breaking FileViewStrategy #28857
51. https://github.com/dotCMS/core/issues/29259 : Page API: Image field content gone when request page with depth #29259
52. https://github.com/dotCMS/core/issues/29667 : Update CLEANUP_BUNDLES_OLDER_THAN_DAYS default value to 4 #29667
53. https://github.com/dotCMS/core/issues/28779 : IndexOutOfBoundsException in sitesearch portlet #28779
54. https://github.com/dotCMS/core/issues/29256 : Many to One Relationships Not Copied in Copy Site #29256
55. https://github.com/dotCMS/core/issues/29392 : Saving folder names with special characters leads to incorrect encoding #29392
56. https://github.com/dotCMS/core/issues/30000 : Add More Logs that helps us investigate 404 issues #30000
57. https://github.com/dotCMS/core/issues/29781 : Language Issue for selecting Contentlets/images in Block Editor #29781
58. https://github.com/dotCMS/core/issues/29213 : Copying a contentlet without having Edit Permissions causes copy to have incorrect permissions. #29213
59. https://github.com/dotCMS/core/issues/28613 : [Content Edit] : Binary D&D not working in Firefox #28613
60. https://github.com/dotCMS/core/issues/28580 : fix: avoid NPE on Patch WF API when there is not any contentlet map #28580
61. https://github.com/dotCMS/core/issues/28489 : Creating new content don't respect language selected #28489
62. https://github.com/dotCMS/core/issues/28814 : Rules permission checkbox is not visible #28814
63. https://github.com/dotCMS/core/issues/29293 : Cannot change folder URL to lowercase #29293
64. https://github.com/dotCMS/core/issues/29321 : An Asset name starts with number and specific alphabets considers as an Image #29321
65. https://github.com/dotCMS/core/issues/29668 : Spike: PP bundles not being processed by Receiver #29668
66. https://github.com/dotCMS/core/issues/29719 : relax ES checks in /api/v1/probes/startup #29719
67. https://github.com/dotCMS/core/issues/30083 : Startup timeouts due to indexer concurrency issues #30083
68. https://github.com/dotCMS/core/issues/30237 : Error adding new content when no lang param is send #30237
69. https://github.com/dotCMS/core/issues/28735 : Locked page error text wrapping incorrectly #28735
70. https://github.com/dotCMS/core/issues/28855 : When page is locked, hover text still says "Lock Page" #28855
71. https://github.com/dotCMS/core/issues/28576 : GraphQL __icon__ field violates GraphQL introspection convention prohibiting __ prefixes #28576
72. https://github.com/dotCMS/core/issues/29355 : Sessions expiring unexpectedly when using Redis #29355
73. https://github.com/dotCMS/core/issues/30420 : Fix Test CircuitBreakerUrlTest.testGet and RemoteAnnouncementsLoaderIntegrationTest.TestAnnouncementsLoader #30420
74. https://github.com/dotCMS/core/issues/29535 : Investigate and Resolve Session Already Invalidated Issue for PATCH API Call with Bearer Token #29535
75. https://github.com/dotCMS/core/issues/29938 : Many to One Relationship Not Maintained When Copied to New Host #29938
76. https://github.com/dotCMS/core/issues/30156 : Create Notifications for LTS already EOL or upcoming EOL #30156
77. https://github.com/dotCMS/core/issues/30243 : Intermittent 404 issues for customers who came from another DB engine #30243
78. https://github.com/dotCMS/core/issues/28514 : Running Experiment Cache is not working right #28514
79. https://github.com/dotCMS/core/issues/28507 : Refresh Running Experiment Cache after abort one Experiment #28507
80. https://github.com/dotCMS/core/issues/28588 : Make MultiTree Multi variant cache cluster aware #28588
81. https://github.com/dotCMS/core/issues/30045 : Aligment not correct in A/B testing - Edit traffic split #30045
82. https://github.com/dotCMS/core/issues/30743 : pg_dump fails when passwords are complex #30743
83. https://github.com/dotCMS/core/issues/30724 : Task URL redirecting only to language_id=1 #30724
84. https://github.com/dotCMS/core/issues/28511 : Rename Title Column #28511
85. https://github.com/dotCMS/core/issues/28512 : Rename Date Updated Column #28512
86. https://github.com/dotCMS/core/issues/28780 : Add Hint support for relationship fields #28780
87. https://github.com/dotCMS/core/issues/29501 : optimize sql query causing slowness on high-traffic sites #29501
88. https://github.com/dotCMS/core/issues/30053 : Overlay Misalignment in Content Type while relating content #30053
89. https://github.com/dotCMS/core/issues/30457 : FileAssetContainers not found when using case #30457
90. https://github.com/dotCMS/core/issues/30619 : Run dotCMS with jemalloc #30619
91. https://github.com/dotCMS/core/issues/30969 : SAML: Retrieve the relay state on the SAML Endpoint #30969
92. https://github.com/dotCMS/core/issues/31034 : Empty allowed file type not being saved #31034
93. https://github.com/dotCMS/core/issues/30804 : The file with name of webPageContent.vtl can't GET and throws 404 #30804
94. https://github.com/dotCMS/core/issues/30982 : Files without an extension cannot be edited / saved #30982
95. https://github.com/dotCMS/core/issues/30468 : Remove calls to ElasticReadOnlyCommand and disable EsReadOnlyMonitorJob  #30468
96. https://github.com/dotCMS/core/issues/30993 : In 24.12.10 the EsReadOnlyMonitorJob still shows error #30993
97. https://github.com/dotCMS/core/issues/26503 : CORS not applied when specifying resource #26503
98. https://github.com/dotCMS/core/issues/28365 : Add Host to the reference tab on Content #28365
99. https://github.com/dotCMS/core/issues/28579 : Add a way to bulk Reset Permissions #28579
100. https://github.com/dotCMS/core/issues/28939 : New Divider Button under Workflows should be under FF #28939
101. https://github.com/dotCMS/core/issues/30761 : Data is not getting saved in the workflow #30761
102. https://github.com/dotCMS/core/issues/30998 : [PP]: Filter option relationships is not being respected #30998
103. https://github.com/dotCMS/core/issues/31343 : Wysiwyg fields hide when there are multiple wysiwyg fields in a content type #31343
104. https://github.com/dotCMS/core/issues/31300 : VTL Syntax highlighting missing in the new code editor modal #31300
105. https://github.com/dotCMS/core/issues/31143 : Block Editor Extension Not Loading in dotCMS #31143
106. https://github.com/dotCMS/core/issues/31019 : Token Invalid in CubeJS Server after token renew #31019
107. https://github.com/dotCMS/core/issues/31207 : Improve performance to retrieve Analytics Data #31207
108. https://github.com/dotCMS/core/issues/31563 : Creating a file in a secondary language won't return the file information #31563
109. https://github.com/dotCMS/core/issues/29869 : Unable to authenticate a servlet request #29869
110. https://github.com/dotCMS/core/issues/31509 : Content Import Functionality Disabled When "Content Search" Tool is Hidden for Specific Roles #31509
111. https://github.com/dotCMS/core/issues/31216 : Push Publishing not escaping comma in title. #31216
112. https://github.com/dotCMS/core/issues/30544 : The last role in the Roles list is partially visible #30544
113. https://github.com/dotCMS/core/issues/31564 : Special Character & Truncated in Category Name After Creation #31564
114. https://github.com/dotCMS/core/issues/31649 : Filter on checkbox field not working as expected (Content search portal and relationship filter) #31649
115. https://github.com/dotCMS/core/issues/31601 : Relate Button in a Relationship Field is not working #31601
116. https://github.com/dotCMS/internal-infrastructure/issues/1294 : [DEFECT] Migrate from deprecated GitHub Actions cache service (all repos). #1294
117. https://github.com/dotCMS/core/issues/31933 : The "Select All Content" option is intermittently not appearing for some users across different devices and browsers #31933
118. https://github.com/dotCMS/core/issues/31707 : Update announcements url #31707
119. https://github.com/dotCMS/core/issues/29736 : Remove "Permissions" and "Relationships Legacy" fields from the field selections sidebar in the Content Type editor #29736
120. https://github.com/dotCMS/core/issues/29534 : Login-as popup show's in the incorrect position. #29534
121. https://github.com/dotCMS/core/issues/31994 : SAML: skip RequestedAuthenticationContext #31994
122. https://github.com/dotCMS/core/issues/30731 : Company information doesn't show in comapny field after the relogin #30731
123. https://github.com/dotCMS/core/issues/29238 : Attaching files feature on tasks is not properly working #29238
124. https://github.com/dotCMS/core/issues/31141 : Automated Unpublishing Fails for Content with Inactive/Deleted Last Modified Users #31141
125. https://github.com/dotCMS/core/issues/32106 : Indent does not work in the Block Editor #32106
126. https://github.com/dotCMS/core/issues/32131 : IntegrityChecker serialization error #32131
127. https://github.com/dotCMS/core/issues/32186 : Experiments: Variant is sporadically not added to index #32186
128. https://github.com/dotCMS/core/issues/32522 : [Block Editor] Contentlet included by the second block editor is not shown in a page #32522
129. https://github.com/dotCMS/core/issues/32473 : [Edit Contentlet]: Binary field editor not showing for JS files #32473
130. https://github.com/dotCMS/core/issues/32130 : Users with CMS Admin Role don’t update correctly if you change User Details #32130
131. https://github.com/dotCMS/core/issues/32075 : When a folder contains Parentheses dotCLI pulls error. #32075
132. https://github.com/dotCMS/core/issues/31859 : Apps Export Configurations Error Handling #31859
133. https://github.com/dotCMS/core/issues/31792 : Static Push Fails with uppercase extensions #31792
134. https://github.com/dotCMS/core/issues/29324 : Unable to Upload Image from Custom Tool Group #29324
135. https://github.com/dotCMS/core/issues/32127 : Bring Back older versions of Container missing #32127
136. https://github.com/dotCMS/core/issues/28417 : Unable to bring back old versions of templates #28417
137. https://github.com/dotCMS/core/issues/31077 : PrimeNG Datepicker Displays Incorrectly When Appended to Body #31077
138. https://github.com/dotCMS/core/issues/32578 : Special characters in key being removed for a key value field on a content type #32578
139. https://github.com/dotCMS/core/issues/32545 : api/v1/workflow/actions/default/fire/PUBLISH not respecting default value #32545
140. https://github.com/dotCMS/core/issues/32413 : [Containers]: Pagination is not working #32413
141. https://github.com/dotCMS/core/issues/32583 : NPE in on content edit (Not on Beta version) when content type named "Event" lacks calendar fields #32583
142. https://github.com/dotCMS/core/issues/33006 : Backport security fix to LTSs #33006
143. https://github.com/dotCMS/core/issues/31452 : Remove Outdated Cache Restore Step in Maven (Legacy) Release Workflow #31452
144. https://github.com/dotCMS/core/issues/32989 : Upgrade Tomcat to Latest Version 9 Patch Release #32989
145. https://github.com/dotCMS/core/issues/32929 : Core: Create user not set user role #32929
146. https://github.com/dotCMS/core/issues/33093 : Update image reference breaking out build #33093
147. https://github.com/dotCMS/core/issues/33031 : Reset Approvals sub-action not working as expected #33031
148. https://github.com/dotCMS/core/issues/33138 : Add Step Id to GET /api/v1/workflow/tasks/history/comments/{contentletIdentifier} endpoint #33138



