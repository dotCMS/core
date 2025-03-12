## DOTCMS BACKPORT LIST

This maintenance release includes the following code fixes:

**Release-23.10.24 LTS**

1. https://github.com/dotCMS/core/issues/26481 : Pubsub Connection should prefer SSL #26481
2. https://github.com/dotCMS/core/issues/26890 : GraphQL Dates not using the right format #26890
3. https://github.com/dotCMS/core/issues/26374 : Use of Png filter on images results in a 404 #26374
4. https://github.com/dotCMS/core/issues/26391 : custom REST CORS header configuration is not applied to responses #26391
5. https://github.com/dotCMS/core/issues/26897 : Make DB connectionTimeout configurable #26897
6. https://github.com/dotCMS/core/issues/26934 : Add Catalina Log Back #26934
7. https://github.com/dotCMS/core/issues/26896 : Avoid Refreshing Properties #26896
8. https://github.com/dotCMS/core/issues/26933 : StorageProvider should load lazily #26933
9. https://github.com/dotCMS/core/issues/26926 : Add left indexes to inode.inode and identifier.id #26926
10. https://github.com/dotCMS/core/issues/26970 : GraphQL Cache not working #26970
11. https://github.com/dotCMS/core/issues/26931 : Add Cache when getting Versionable info #26931
12. https://github.com/dotCMS/core/issues/26980: SiteSelector not showing all possible sites for limited user #26980
13. https://github.com/dotCMS/core/issues/26915 : Metadata Generation locks system start up #26915
14. https://github.com/dotCMS/core/issues/26840 : "Edit Content" screen slow - Unnecessary SQL queries #26840
15. https://github.com/dotCMS/core/issues/27047 : Binary File upload to a content type fails (Multi-part upload) #27047
16. https://github.com/dotCMS/core/issues/27039 : Content Search Portlet not showing all language versions #27039
17. https://github.com/dotCMS/core/issues/25478 : When filter by All in the Browser Portlet we are showing the No DEFAULT Version of the Contentlet #25478
18. https://github.com/dotCMS/core/issues/25901 : Unable to reuse content inside the variants #25901
19. https://github.com/dotCMS/core/issues/26272 : Losing Experiments and their results when Moving Page to Different Path #26272
20. https://github.com/dotCMS/core/issues/26550 : Improve performance to the Experiment results end point #26550
21. https://github.com/dotCMS/core/issues/26683 : Unable to add content on second Language to Page Variant #26683
22. https://github.com/dotCMS/core/issues/23623 : [BE] Change Split Traffic #23623
23. https://github.com/dotCMS/core/issues/26273 : Experiments not Considering Redirection from Vanity URLs #26273
24. https://github.com/dotCMS/core/issues/26452 : Unable to add forms to experiment variants on dotCMS #26452
25. https://github.com/dotCMS/core/issues/27063 : Experiments result dates in chart dates are incorrect by one day. #27063
26. https://github.com/dotCMS/core/issues/27324 : Sort List Experiments by most recent modification #27324
27. https://github.com/dotCMS/core/issues/27193 : Experiments: Handling Empty Analytics Key in App Configuration in dotCMS #27193
28. https://github.com/dotCMS/core/issues/24082 : Language Keys API throws 400 when a duplicate key exists #24082
29. https://github.com/dotCMS/core/issues/25233 : sysPublishDate no longer appears to be part of the ElasticSearch object we create for indexing #25233
30. https://github.com/dotCMS/core/issues/26706 : Change Default PUBSUB Provider #26706
31. https://github.com/dotCMS/core/issues/26825 : Change order of config precidence #26825
32. https://github.com/dotCMS/core/issues/26439 : Relationship fields not respecting the order identifiers are sent via the workflow API #26439
33. https://github.com/dotCMS/core/issues/26693 : Edit Permissions Individually stuck when editing folder with legacy ID #26693
34. https://github.com/dotCMS/core/issues/26640 : Cyrillic URLs encoding issue when configured by Vanity URL Redirect #26640
35. https://github.com/dotCMS/core/issues/26861 : Can not create in rest a new diff lang version #26861
36. https://github.com/dotCMS/core/issues/27007 : Content export with Publish Date Parameter leads to 400 Bad Request error #27007
37. https://github.com/dotCMS/core/issues/27601 : Unhandled Exception: Failed to Fetch Error Not Caught in Preview with SEO Feature #27601 
38. https://github.com/dotCMS/core/issues/26459 : Unable to copy a contentlet having Site/Folder Field #26459
39. https://github.com/dotCMS/core/issues/22372 : Users can create fields with reserved var names #22372
40. https://github.com/dotCMS/core/issues/26796 : Reserved names fields should be compared in a case-insensitive manner to avoid errors and inconsistencies. #26796
40. https://github.com/dotCMS/core/issues/26680 : Restoring Reorder Rows Functionality in Content Type Editing #26680
41. https://github.com/dotCMS/core/issues/26774 : text fields validation limit to 255 chars is not longer required #26774
42. https://github.com/dotCMS/core/issues/26605 : Relation_type field too small #26605
43. https://github.com/dotCMS/core/issues/22921 : Workflow API unable to archive contentlet #22921
44. https://github.com/dotCMS/core/issues/26521 : [UI] Create a Middleware to use Storybook with dotCMS #26521
45. https://github.com/dotCMS/core/issues/26657 : [UI] - Update the endpoints used by the extensions with the new URLS #26657
46. https://github.com/dotCMS/core/issues/26664 : [UI] Setup a flag to use the middleware in storybook #26664
47. https://github.com/dotCMS/core/issues/26519 : [UI] Review AIContentPromptExtension #26519
48. https://github.com/dotCMS/core/issues/26551 : [UI] Review AIImagePromptExtension prompt extension #26551
49. https://github.com/dotCMS/core/issues/26520 : [UI] Add DotMessageService to Block Editor to able translate the labels/messages #26520
50. https://github.com/dotCMS/core/issues/26665 : [UI] - Disable the input while is pending/loading #26665
51. https://github.com/dotCMS/core/issues/26666 : [UI] When the BlockEditor lose the focus, and get it again the generated content is duplicated #26666
52. https://github.com/dotCMS/core/issues/26895 : [UI] Update AIService of Block Editor with new endpoint shape #26895
53. https://github.com/dotCMS/core/issues/26912 : [UI] AI actions menu selection is not working as should. #26912
54. https://github.com/dotCMS/core/issues/26556 : [UI] Not load the AI extensions if the plugin is not properly installed and configured #26556
55. https://github.com/dotCMS/core/issues/26899 : [UI] When image loading place holder loose focus, the placeholder stays in the editor. #26899
56. https://github.com/dotCMS/core/issues/27033 : Fix AI Plugin Detection for AI Extensions in Block Editor Components (Sidebars) #27033
57. https://github.com/dotCMS/core/issues/27051 : Don't save loading nodes in the block editor. #27051
58. https://github.com/dotCMS/core/issues/26505 : [UI] - Make AI Image Block Production-ready #26505
59. https://github.com/dotCMS/core/issues/27155 : Endpoint Change for AI Plugin Detection in dotCMS #27155
60. https://github.com/dotCMS/core/issues/26853 : [UI] Implement Handle Errors from endpoint #26853
61. https://github.com/dotCMS/core/issues/25296 : Limited users cannot create content types on System Host #25296
62. https://github.com/dotCMS/core/issues/26542 : Hide the download button for bundles pushed to static environments #26542
63. https://github.com/dotCMS/core/issues/26415 : Template Builder: System Template should create layout always #26415
64. https://github.com/dotCMS/core/issues/22385 : Custom templates should push with their page no matter the filter that is selected #22385
65. https://github.com/dotCMS/core/issues/25891 : [Empty Starter] : The Language Variable Content Type is not visible from the Content portlet #25891
66. https://github.com/dotCMS/core/issues/27832 : Return IDP configuration Id instead of site id as part of the ACS URL in the SAML authentication request #27832
67. Enable Experiments feature by default
68. https://github.com/dotCMS/core/issues/27883 : fix(perf) : Prevent UtilMethods.isImage from blocking #27883
69. https://github.com/dotCMS/core/issues/22698 : Limited user without Search Portlet can't Edit Content From Edit Page Portlet #22698
70. https://github.com/dotCMS/core/issues/25729 : Order of comments in Task details modal. #25729
71. https://github.com/dotCMS/core/issues/25653 : Importing Category lists via CSV - import button is disabled #25653
72. https://github.com/dotCMS/core/issues/26815 : Unable to compare history of older versions (Older than latest 20) #26815
73. https://github.com/dotCMS/core/issues/26224 : Need to add the user who create the bundle when you have bundles from other users #26224
74. https://github.com/dotCMS/core/issues/26201 : Deletion of Unpublished Bundles through API #26201
75. https://github.com/dotCMS/core/issues/26170 : Unable to view more than 20 items in a publishing queue bundle #26170
76. https://github.com/dotCMS/core/issues/27775 : Page API Returning Content Related to Page Asset #27775
77. https://github.com/dotCMS/core/issues/27187 : remove unneeded DB hit from edit content screen #27187
78. https://github.com/dotCMS/core/issues/27384 : Update local.dotcms.site SSL cert for 2024 #27384
79. https://github.com/dotCMS/core/issues/26004 : Image field doesn't export as a header in CSV #26004
80. https://github.com/dotCMS/core/issues/23195 : Site Browser: Slow loading folder with many items #23195
81. https://github.com/dotCMS/core/issues/27894 : Security: Critical Vulnerability in Postgres JDBC Driver #27894
82. https://github.com/dotCMS/core/issues/27909 : Invalid role check when accessing resource #27909
83. https://github.com/dotCMS/core/issues/27910 : Log too verbose in certain situations #27910
84. https://github.com/dotCMS/core/issues/27453 : Make experience plugin into a system plugin #27453
85. https://github.com/dotCMS/core/issues/27516 : Secrets can not find the inode on certain url #27516
86. https://github.com/dotCMS/core/issues/28110 : Only run startup tasks if they need to be run #28110
87. https://github.com/dotCMS/core/issues/28105 : PDF as Binary / upload field doesn't show preview #28105
88. https://github.com/dotCMS/core/issues/27563 : Site or Folder field does not show on the relate content window #27563
89. https://github.com/dotCMS/core/issues/27878 : System Table Blocks on Load #27878
90. https://github.com/dotCMS/core/issues/27361 : CSV Content import cannot process host or folder information #27361
91. https://github.com/dotCMS/core/issues/26582 : [Site Browser] : Open folders get collapsed after moving away from portlet #26582
92. https://github.com/dotCMS/core/issues/25903 : Key/Value field escaping colon and comma characters to HTML encoded version. #25903
93. https://github.com/dotCMS/core/issues/24698 : Cannot push publish to S3 buckets in the us-east-2 region #24698
94. https://github.com/dotCMS/core/issues/28230 : spike(performance): test short lived permission cache #28230
95. https://github.com/dotCMS/core/issues/28173 : System objects should not be push published #28173
96. https://github.com/dotCMS/core/issues/27928 : Investigate why endpoint_ids in the table publishing_pushed_assets are null #27928
97. https://github.com/dotCMS/core/issues/27531 : Google Maps does not work on the rules page #27531
98. https://github.com/dotCMS/core/issues/26660 : Fix Menu label if we are missing the translation key #26660
99. https://github.com/dotCMS/core/issues/26283 : Relationship throwing error when the child content is a copy of original child content #26283
100. https://github.com/dotCMS/core/issues/26597 : Textareas need to be embiggened. #26597
101. https://github.com/dotCMS/core/issues/28360 : Move Async Email Actionlet to Core #28360
102. https://github.com/dotCMS/core/issues/28089 : Pound char not decoded when using Rules or Vanity URLs #28089
103. https://github.com/dotCMS/core/issues/28284 : Content Types not Showing if System is true #28284
104. https://github.com/dotCMS/core/issues/28306 : Can't Import Page Assets using the export file #28306
105. https://github.com/dotCMS/core/issues/28352 : Send Email sub-action doesn't set the current user to $dotcontent viewtool #28352
106. https://github.com/dotCMS/core/issues/28689 : Update LTS Telemetry plugin #28689
107. https://github.com/dotCMS/private-issues/issues/34
108. https://github.com/dotCMS/core/issues/28785 : APIs return incorrect information for pages with fields using the variable name "description" #28785
109. https://github.com/dotCMS/core/issues/28695 : UT210901 UpdateDateTimezones has wrong DST query #28695
110. https://github.com/dotCMS/core/issues/28760 : Template Evaluation for Non-Default Language Pages #28760
111. https://github.com/dotCMS/core/issues/28769 : Language Fallback not working. #28769
112. https://github.com/dotCMS/core/issues/28204 : Search filter in the theme selection menu fails to load sites #28204
113. https://github.com/dotCMS/core/issues/28609 : Remove old vulnerability-scan.yml workflow #28609
114. https://github.com/dotCMS/core/issues/23292 : Password validation failed for few characters #23292
115. https://github.com/dotCMS/core/issues/23131 : Remove 10k Push Publishing Limit #23131
116. https://github.com/dotCMS/core/issues/28897 : Content Resource v1 hits the db #28897
117. https://github.com/dotCMS/core/issues/28890 : LanguageUtil.getLanguageId is always hitting the db #28890
118. https://github.com/dotCMS/core/issues/26421 : Block Editor: Add align-justify option to menu #26421
119. https://github.com/dotCMS/core/issues/27537 : Download database not working in latest version #27537
120. https://github.com/dotCMS/core/issues/27871 : refactor pg_dump inclusion in our docker image #27871
121. SI-72
122. https://github.com/dotCMS/core/issues/29240 : UT keep running even though one failed #29240
123. https://github.com/dotCMS/core/issues/29304 : Revert telemetry plugin to 24.05.29 #29304
124. https://github.com/dotCMS/core/issues/28508 : Add Search Box for Bundle ID #28508
125. https://github.com/dotCMS/core/issues/28509 : Remove three dots at the end of the Bundle ID #28509
126. https://github.com/dotCMS/core/issues/28201 : AI Content Block inserts rich content as a single paragraph #28201
127. https://github.com/dotCMS/core/issues/26987 : Set Response Headers Rule Action does not allow double quotes in the value #26987
128. https://github.com/dotCMS/core/issues/26477 : Search filter can't find/filter images #26477
129. https://github.com/dotCMS/core/issues/27297 : Edit Page: Edit Contentlet Dialog Language Support #27297
130. https://github.com/dotCMS/core/issues/26413 : Template Builder: Container Layout Editing Issue #26413
131. https://github.com/dotCMS/core/issues/27816 : Content Displacement Bug when Editing Template #27816
132. https://github.com/dotCMS/core/issues/28163 : 'alive' and 'startup' healthcheck APIs return 503 on seemingly healthy app #28163
133. https://github.com/dotCMS/core/issues/26546 : Enable better logging for getPageByPath in HTMLPageAssetAPIImpl.java #26546
134. https://github.com/dotCMS/core/issues/28366 : Uploaded images in another language than the default one do not inherit permissions #28366
135. https://github.com/dotCMS/core/issues/28838 : Category Child Permissions Not Loading #28838
136. https://github.com/dotCMS/core/issues/29079 : fileAsset Required Error while importing FileAsset through CSV #29079
137. https://github.com/dotCMS/core/issues/29209 : Wrong url when exporting file asset #29209
138. https://github.com/dotCMS/core/issues/29222 : Telemetry: not getting data after last release of the plugin #29222
139. https://github.com/dotCMS/core/issues/29254 : Add a new main tag for LTSs #29254
140. https://github.com/dotCMS/core/issues/28857 : dotAsset is Breaking FileViewStrategy #28857
141. https://github.com/dotCMS/core/issues/29259 : Page API: Image field content gone when request page with depth #29259
142. https://github.com/dotCMS/core/issues/29667 : Update CLEANUP_BUNDLES_OLDER_THAN_DAYS default value to 4 #29667
143. https://github.com/dotCMS/core/issues/29162 : Slow performance with imports in some cases #29162
144. https://github.com/dotCMS/core/issues/28779 : IndexOutOfBoundsException in sitesearch portlet #28779
145. https://github.com/dotCMS/core/issues/29256 : Many to One Relationships Not Copied in Copy Site #29256
146. https://github.com/dotCMS/core/issues/29392 : Saving folder names with special characters leads to incorrect encoding #29392
147. https://github.com/dotCMS/core/issues/29213 : Copying a contentlet without having Edit Permissions causes copy to have incorrect permissions. #29213
148. https://github.com/dotCMS/core/issues/28580 : fix: avoid NPE on Patch WF API when there is not any contentlet map #28580
149. https://github.com/dotCMS/core/issues/28489 : Creating new content don't respect language selected #28489
150. https://github.com/dotCMS/core/issues/28814 : Rules permission checkbox is not visible #28814
151. https://github.com/dotCMS/core/issues/29293 : Cannot change folder URL to lowercase #29293
152. https://github.com/dotCMS/core/issues/29321 : An Asset name starts with number and specific alphabets considers as an Image #29321
153. https://github.com/dotCMS/core/issues/29668 : Spike: PP bundles not being processed by Receiver #29668
154. https://github.com/dotCMS/core/issues/29719 : relax ES checks in /api/v1/probes/startup #29719
155. https://github.com/dotCMS/core/issues/30083 : Startup timeouts due to indexer concurrency issues #30083
156. https://github.com/dotCMS/core/issues/28576 : GraphQL __icon__ field violates GraphQL introspection convention prohibiting __ prefixes #28576
157. https://github.com/dotCMS/core/issues/28855 : When page is locked, hover text still says "Lock Page" #28855
158. https://github.com/dotCMS/core/issues/28735 : Locked page error text wrapping incorrectly #28735
159. https://github.com/dotCMS/core/issues/30420 : Fix Test CircuitBreakerUrlTest.testGet and RemoteAnnouncementsLoaderIntegrationTest.TestAnnouncementsLoader #30420
160. https://github.com/dotCMS/core/issues/29535 : Investigate and Resolve Session Already Invalidated Issue for PATCH API Call with Bearer Token #29535
161. https://github.com/dotCMS/core/issues/29938 : Many to One Relationship Not Maintained When Copied to New Host #29938
162. https://github.com/dotCMS/core/issues/30156 : Create Notifications for LTS already EOL or upcoming EOL #30156
163. https://github.com/dotCMS/core/issues/30243 : Intermittent 404 issues for customers who came from another DB engine #30243
164. https://github.com/dotCMS/core/issues/26271 : [UI] Text in experiment data results needs be aligned #26271
165. https://github.com/dotCMS/core/issues/26399 : [UI] Change Experiment mod date display from simple date to date/time #26399
166. https://github.com/dotCMS/core/issues/26750 : [UI] Experiments reports - JS error when bayesianResult is null #26750
167. https://github.com/dotCMS/core/issues/26738 : [UI] Change button with new style in Variations #26738
168. https://github.com/dotCMS/core/issues/27141 : Responsive Design Issue with Experiment Results Chart Not Resizing Properly #27141
169. https://github.com/dotCMS/core/issues/27451 : Registering page events after the Experiment ended #27451
170. https://github.com/dotCMS/core/issues/27584 : Edit mode navigation render is broken #27584
171. https://github.com/dotCMS/core/issues/27748 : Render Page on Ended Experiment Variant #27748
172. https://github.com/dotCMS/core/issues/27906 : Experiment: Take the Site account when validate if redirect is needed #27906
173. https://github.com/dotCMS/core/issues/28238 : Orphan running experiments breaking other API calls #28238
174. https://github.com/dotCMS/core/issues/28514 : Running Experiment Cache is not working right #28514
175. https://github.com/dotCMS/core/issues/28507 : Refresh Running Experiment Cache after abort one Experiment #28507
176. https://github.com/dotCMS/core/issues/28588 : Make MultiTree Multi variant cache cluster aware #28588
177. https://github.com/dotCMS/core/issues/30045 : Aligment not correct in A/B testing - Edit traffic split #30045
178. https://github.com/dotCMS/core/issues/30743 : pg_dump fails when passwords are complex #30743
179. https://github.com/dotCMS/core/issues/30724 : Task URL redirecting only to language_id=1 #30724
180. https://github.com/dotCMS/core/issues/28511 : Rename Title Column #28511
181. https://github.com/dotCMS/core/issues/28512 : Rename Date Updated Column #28512
182. https://github.com/dotCMS/core/issues/28780 : Add Hint support for relationship fields #28780
183. https://github.com/dotCMS/core/issues/30457 : FileAssetContainers not found when using case #30457
184. https://github.com/dotCMS/core/issues/30969 : SAML: Retrieve the relay state on the SAML Endpoint #30969
185. https://github.com/dotCMS/core/issues/30804 : The file with name of webPageContent.vtl can't GET and throws 404 #30804
186. https://github.com/dotCMS/core/issues/30993 : Remove calls to ElasticReadOnlyCommand and disable EsReadOnlyMonitorJob  #30468
187. https://github.com/dotCMS/core/issues/30993 : In 24.12.10 the EsReadOnlyMonitorJob still shows error #30993
188. https://github.com/dotCMS/core/issues/26503 : CORS not applied when specifying resource #26503
189. https://github.com/dotCMS/core/issues/28365 : Add Host to the reference tab on Content #28365
190. https://github.com/dotCMS/core/issues/28579 : Add a way to bulk Reset Permissions #28579
191. https://github.com/dotCMS/core/issues/30761 : Data is not getting saved in the workflow #30761
192. https://github.com/dotCMS/core/issues/30998 : [PP]: Filter option relationships is not being respected #30998
193. https://github.com/dotCMS/core/issues/31343 : Wysiwyg fields hide when there are multiple wysiwyg fields in a content type #31343
