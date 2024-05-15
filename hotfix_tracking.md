#DOTCMS_CORE

This maintenance release includes the following code fixes:

**Release-23.01.1**

1. https://github.com/dotCMS/core/issues/23280 : Unable to assign contentlets in a secondary language from the task screen if there is a working version of the default language #23280
2. https://github.com/dotCMS/core/issues/23890 : Navigation showing duplicating results when multilingual page and requesting non-default lang #23890
3. https://github.com/dotCMS/core/issues/23440 : Categories being removed from content items when updating content via CSV file upload #23440
4. https://github.com/dotCMS/core/issues/23761 : Unable to start LTS release using MSSQL #23761
5. https://github.com/dotCMS/core/issues/23810 : Asset download missing directories 8 and 9 #23810
6. https://github.com/dotCMS/core/issues/24075 : dotcache - Make Velocity Context caching optional
7. https://github.com/dotCMS/core/issues/24158 : CMSFilter updating language param #24158
8. https://github.com/dotCMS/core/issues/24059 : We are not respecting the widget language when you have a multilingual page have a working version in non-default language #24059
9. https://github.com/dotCMS/core/issues/24247 : Nested dotcache causes issues #24247
10. https://github.com/dotCMS/core/issues/24138 : SAML Allow Users with repeat email addresses #24138
11. https://github.com/dotCMS/core/issues/22151 : Allow greater configuration of S3Client to target additional endpoints beyond AWS #22151
12. https://github.com/dotCMS/core/issues/24351 : Configuration of S3Client to target endpoint unable to sent css files outside AWS #24351
13. https://github.com/dotCMS/core/issues/24358 : File Browser Only Showing Content in the Backend Language #24358
14. https://github.com/dotCMS/core/issues/24272 : File Browse Not Working as Expected #24272

**Release-23.01.2**

15. https://github.com/dotCMS/core/issues/22667 : Cannot change date or time fields to never on an existing contentlet #22667
16. https://github.com/dotCMS/core/issues/23982 : Improve startup performance #23982
17. https://github.com/dotCMS/core/issues/24344 : The content is becoming published automatically after unpublishing it manually without specifying any publish date #24344
18. https://github.com/dotCMS/core/issues/24380 : Error upgrading in Postgres #24380
19. https://github.com/dotCMS/core/issues/24395 : Remove issuer check from JWT API keys #24395
20. https://github.com/dotCMS/core/issues/24436 : Task05380ChangeContainerPathToAbsolute repeating site if container was added several times
21. https://github.com/dotCMS/core/issues/24441 : Unable to copy a folder when the folder contains a page that has two versions live and draft #24441
22. https://github.com/dotCMS/core/issues/24245 : Publishing contentlet takes a long time when there are many locales/relationships #24245
23. https://github.com/dotCMS/core/issues/23889 : Design Template Permissions Tab Doesn't Load #23889
24. https://github.com/dotCMS/core/issues/23807 : Make Image Permits less restrictive #23807
25. https://github.com/dotCMS/core/issues/23915 : Increase Password Hash Iterations #23915
26. https://github.com/dotCMS/core/issues/24093 : Create background task that converts fat content into json content #24093
27. https://github.com/dotCMS/core/issues/23449 : Filter does not work on the image selector for image fields #23449
28. https://github.com/dotCMS/core/issues/23396 : Double quotes in content title - error trying to show data in relate table. #23396
29. https://github.com/dotCMS/core/issues/24565 : Unable to save content converted from WYSIWYG to Block Editor #24565

**Release-23.01.3**

30. https://github.com/dotCMS/core/issues/24299 : Can't push publish content with a block editor field #24299
31. https://github.com/dotCMS/core/issues/24444 : File Browser Dialog doesn't show files in default lang as fallback if they doesn't exist in requested lang #24444
32. https://github.com/dotCMS/core/issues/24781 : Performance Optimization - don't force field invalidations for non-velocity fields #24781
33. https://github.com/dotCMS/core/issues/23764 : Take the Paragraph option out of the Allowed Blocks field #23764
34. https://github.com/dotCMS/core/issues/23237 : Bock Editor: Allow user to insert image from local files #23237
35. https://github.com/dotCMS/core/issues/23920 : BLOCK EDITOR: Can't add image when white list the blocks #23920
36. https://github.com/dotCMS/core/issues/24230 : Block Editor: Markdown content with JavaScript code is breaking the app #24230
37. https://github.com/dotCMS/core/issues/23863 : Bock Editor: Allow user to insert videos from local files #23863
38. https://github.com/dotCMS/core/issues/23436 : Block Editor: Create video block node and insert from dotCMS content #23436
39. https://github.com/dotCMS/core/issues/24422 : Rendering converted wysiwyg field fails #24422
40. https://github.com/dotCMS/core/issues/24465 : Block editor add video.vtl to render a video by default #24465
41. https://github.com/dotCMS/core/issues/23846 : StoryBlock - Embedding a content item to itself causes stackoverflow #23846
42. https://github.com/dotCMS/core/issues/24285 : Block Editor: Error uploading images in a different default language. #24285
43. https://github.com/dotCMS/core/issues/25037 : PP an existing bundle it doesn't consider Timezone #25037

**Release-23.01.4**

44. https://github.com/dotCMS/core/issues/25008 : Date format has changed for fields in newer versions of dotCMS #25008
45. https://github.com/dotCMS/core/issues/25097 : Update Normalization Filter #25097
46. https://github.com/dotCMS/core/issues/24286 : Creating a content in a third language in a row throws an error #24286
47. https://github.com/dotCMS/core/issues/24133 : User is unable to lock content because of validation limit on locked_by column of contentlet_version_info #24133
48. https://github.com/dotCMS/core/issues/25044 : PP content set as working state instead of live state when there is archived version in other language #25044
49. https://github.com/dotCMS/core/issues/24705 : PP a folder with Archived Content Fails #24705
50. https://github.com/dotCMS/core/issues/25203 : getResizeUri Method Returns An Incorrect URL #25203
51. https://github.com/dotCMS/core/issues/24937 : Unable to upload File Asset using Podman #24937
52. https://github.com/dotCMS/core/issues/23395 : When you hit the Publish action from the tasks page you get the Push Publish dialog #23395
53. https://github.com/dotCMS/core/issues/22872 : Lucene queries for personas returning unexpected results #22872
54. https://github.com/dotCMS/core/issues/23175 : Add a "Velocity Secrets" app and viewtool #23175
55. https://github.com/dotCMS/core/issues/23199 : Unable to create contentlet through workflow API even if user has appropriate permissions #23199
56. https://github.com/dotCMS/core/issues/23924 : Image editor Download button not working #23924
57. https://github.com/dotCMS/core/issues/25212 : Unable to edit the template using the layout from edit mode #25212
58. https://github.com/dotCMS/core/issues/25230 : Block Editor Search not working for special chars #25230

**Release-23.01.5**

59. Fix defaultPath field variable
60. https://github.com/dotCMS/core/issues/18123 : Add depth option to page API #18123
61. https://github.com/dotCMS/core/issues/25217 : New method of generating shorty IDs to include the language ID is creating issues with static PP #25217
62. https://github.com/dotCMS/core/issues/25437 : Add SAML the ability to retrieve non-stardard attributes #25437
63. https://github.com/dotCMS/core/issues/25556 : UPDATE_PORTLET_LAYOUTS event is sent without user limitation #25556
64. https://github.com/dotCMS/core/issues/23276 : Content detailed page throwing 404 when URL contains a trailing slash. #23276
65. https://github.com/dotCMS/core/issues/24424 : Error when right click on role name #24424
66. https://github.com/dotCMS/core/issues/25120 : Stopped sites are not listed #25120
67. https://github.com/dotCMS/core/issues/25136 : Site Browser is not sorting items by default #25136
68. https://github.com/dotCMS/core/issues/25189 : Data mixing issue when ordering values of Content Type with multiple key/value fields #25189
69. https://github.com/dotCMS/core/issues/25293 : Date format has changed for fields in newer versions of dotCMS #25293
70. https://github.com/dotCMS/core/issues/24415 : Related content doesn't match with Content Language. #24415
71. https://github.com/dotCMS/core/issues/25258 : WYSIWYG not adding images when it's not default lang #25258
72. https://github.com/dotCMS/core/issues/24840 : Clean up dojo folder #24840
73. https://github.com/dotCMS/core/issues/24843 : Remove jaxws libraries #24843
74. https://github.com/dotCMS/core/issues/25224 : PP - Integrity Checker, fixing conflicts is setting up identifier columns as null #25224
75. https://github.com/dotCMS/core/issues/25121 : Pages - Block Editor - NPE #25121
76. https://github.com/dotCMS/core/issues/23948 : Stop redirecting all BE page traffic to edit mode #23948
77. https://github.com/dotCMS/core/issues/25720 : Adding insert order to sql query #25723
78. https://github.com/dotCMS/core/issues/25775 : OPTIONS requests to /api/\* return HTTP 500, causing CORS failures #25775

**Release-23.01.6**

79. https://github.com/dotCMS/core/issues/25618 : Metadata keywords don't return in response to ES query and Content api #25618
80. https://github.com/dotCMS/core/issues/25726 : Block Editor: Copy - Paste Word Doc creates an image instead of a paragraph in Chrome and Mac #25726
81. https://github.com/dotCMS/core/issues/25660 : Add support for h4, h5, h6 headlines in Block Editor #25660
82. https://github.com/dotCMS/core/issues/24716 : Fix Undo/Redo Issues in Block Editor #24716
83. https://github.com/dotCMS/core/issues/25797 : When uploading multiple files, the files are always in English #25797
84. https://github.com/dotCMS/core/issues/25371 : PP: pushing new folder with limited user fails #25371
85. https://github.com/dotCMS/core/issues/25636 : We require a SAML app property to map old SAML configuration IDs to site IDs #25636
86. https://github.com/dotCMS/core/issues/22533 : Relay State SAML improvement
87. https://github.com/dotCMS/core/issues/25896 : Creating a contentlet in a 2nd language removes related content #25896
88. https://github.com/dotCMS/core/issues/23733 : Stream Starter Zip Generation #23733
89. https://github.com/dotCMS/core/issues/24829 : NavTool method getNav must return only published links #24829
90. https://github.com/dotCMS/core/issues/25229 : PP - Integrity Checker, fixing conflicts is not populating the contentlet_as_json field #25229
91. https://github.com/dotCMS/core/issues/25440 : Unable to Publish (all) of a folder #25440
92. https://github.com/dotCMS/core/issues/24176 : "(Default)" marker in Site Search interferes with reindexing #24176
93. https://github.com/dotCMS/core/issues/25510 : Export assets without old versions #25510
94. https://github.com/dotCMS/core/issues/25666 : Pagination not working on related endpoint #25666
95. https://github.com/dotCMS/core/issues/25567 : Unable to Search for Non-English Pages with the Block Editor Hyperlink Feature #25567
96. https://github.com/dotCMS/core/issues/24683 : Copy button on Page Edit portlet should copy the entire URL, not just the path #24683

**Release-23.01.7**

97. https://github.com/dotCMS/core/issues/24294 : Implement Redisson Session sharing #24294
98. https://github.com/dotCMS/core/issues/24990 : Punch List : Redis Session Manager #24990
99. https://github.com/dotCMS/core/issues/25570 : Redis / Session-less testing and improvements #25570
100.    https://github.com/dotCMS/core/issues/21855 : Unable to save content of a Widget with WYSIWYG Code view #21855
101.    https://github.com/dotCMS/core/issues/22507 : Make instance restart function capable of restarting the cluster #22507
102.    https://github.com/dotCMS/core/issues/23254 : Categories: Add child count to the categories list response #23254
103.    https://github.com/dotCMS/core/issues/23554 : Can't close query dialog on content search screen #23554
104.    https://github.com/dotCMS/core/issues/23669 : properties from ENV vars do not need XML encoding #23669
105.    https://github.com/dotCMS/core/issues/25789 : Tighten up MultiPartSecurityRequestWrapper #25789
106.    https://github.com/dotCMS/core/issues/26019 : PubSubListener with vanilla postgres #26019
107.    https://github.com/dotCMS/core/issues/26046 : Radio Button no longer accept 1/0 as valid when searching in ES #26046
108.    https://github.com/dotCMS/core/issues/26131 : Suggested tags don't work when the content is located in a folder #26131
109.    https://github.com/dotCMS/core/issues/26158 : Remove :persona from tag when doing ES #26158
110.    https://github.com/dotCMS/core/issues/26159 : Remove Persona Icon from Tag Field #26159

**Release-23.01.8**

111. https://github.com/dotCMS/core/issues/26391 : custom REST CORS header configuration is not applied to responses #26391
112. https://github.com/dotCMS/core/issues/25193 : Unable to specify resample_opts - value not carried all the way down the method call #25193
113. https://github.com/dotCMS/core/issues/25489 : Block Editor: Add Superscript and Subscript Mark #25489
114. https://github.com/dotCMS/core/issues/23777 : LanguageKeys do not fall back to default language #23777
115. https://github.com/dotCMS/core/issues/23903 : Delay response when user authentication invalid #23903
116. https://github.com/dotCMS/core/issues/23955 : Allow user to easily preview URL Mapped Content #23955
117. https://github.com/dotCMS/core/issues/24797 : Block Editor Crash when the Same Contentlet is Added #24797
118. https://github.com/dotCMS/core/issues/23181 : Modifying designer template removes content from pages #23181
119. https://github.com/dotCMS/core/issues/25411 : Showing a binary field to showFields in a Relationship Field shows path instead of thumbnail #25411
120. https://github.com/dotCMS/core/issues/25827 : Recreating a field with same name diff type uses the same id #25827
121. https://github.com/dotCMS/core/issues/25870 : Using showFields field variable replicates title to all items #25870
122. https://github.com/dotCMS/core/issues/26374 : Use of Png filter on images results in a 404 #26374
123. https://github.com/dotCMS/core/issues/24921 : Filtering does not put exact matches on the top of the site dropdown when searching #24921
124. https://github.com/dotCMS/core/issues/24894 : There is no way to configure WYSIWYG to use the virtual path of an image when inserting one #24894
125. https://github.com/dotCMS/core/issues/24816 : The site search index portlet doesn't render, which leads to errors from elasticsearch when there are many indices #24816
126. https://github.com/dotCMS/core/issues/25127 : CMS Admin should be able to view all the bundles regardless of the user who create it #25127
127. https://github.com/dotCMS/core/issues/25174 : Advance template is missing the Publish button added to the template designer #25174
128. https://github.com/dotCMS/core/issues/24086 : Add indexes on workflow tables #24086

**Release-23.01.9**

129. https://github.com/dotCMS/core/issues/18575 : DotCMS uses incorrect language when adding new content #18575
130. https://github.com/dotCMS/core/issues/21529 : The JSON viewtool does not support valid JSON types, only strings #21529
131. https://github.com/dotCMS/core/issues/22534 : Caching empty containers in the Velocity2 cache when accessing in a language where the content doesn't exist #22534
132. https://github.com/dotCMS/core/issues/24167 : Relationship fields couldn't update with workflow apis #24167
133. https://github.com/dotCMS/core/issues/24227 : [SASS] : Minify CSS output by default #24227
134. https://github.com/dotCMS/core/issues/24266 : Content Type publish and expire date are set to empty #24266
135. https://github.com/dotCMS/core/issues/24291 : Ensure that sessions can be serialized and replicated across servers #24291
136. https://github.com/dotCMS/core/issues/24292 : Content Palette Should Not Show Archived Content #24292
137. https://github.com/dotCMS/core/issues/24406 : Block Editor adds trailing space to hyperlinks or marks #24406
138. https://github.com/dotCMS/core/issues/24468 : Osgi always restart #24468
139. https://github.com/dotCMS/core/issues/24490 : Content with an invalid detail page fails to render/instanciate #24490
140. https://github.com/dotCMS/core/issues/24775 : LogViewer connection times out. #24775
141. https://github.com/dotCMS/core/issues/24885 : Define startup and liveness probes #24885
142. https://github.com/dotCMS/core/issues/25613 : MonitorResource throws exception if DOT_SYSTEM_STATUS_API_IP_ACL is not set #25613
143. https://github.com/dotCMS/core/issues/26481 : Pubsub Connection should prefer SSL #26481
144. https://github.com/dotCMS/core/issues/26774 : text fields validation limit to 255 chars is not longer required #26774
145. https://github.com/dotCMS/core/issues/24874 : Unable to import starter.zip #24874

**Release-23.01.10**

146. https://github.com/dotCMS/core/issues/27008 : Adding DEBUG logging to the SASS compilation process #27008
147. https://github.com/dotCMS/core/issues/24488 : Fixing ContentletAPITest #24488
148. https://github.com/dotCMS/core/issues/26980: SiteSelector not showing all possible sites for limited user #26980
149. https://github.com/dotCMS/core/issues/26970 : GraphQL Cache not working #26970
150. https://github.com/dotCMS/core/issues/26934 : Add Catalina Log Back #26934
151. https://github.com/dotCMS/core/issues/26926 : Add left indexes to inode.inode and identifier.id #26926
152. https://github.com/dotCMS/core/issues/26890 : GraphQL Dates not using the right format #26890

**Release-23.01.11**

153. https://github.com/dotCMS/core/issues/24082 : Language Keys API throws 400 when a duplicate key exists #24082
154. https://github.com/dotCMS/core/issues/26439 : Relationship fields not respecting the order identifiers are sent via the workflow API #26439
155. https://github.com/dotCMS/core/issues/25233 : sysPublishDate no longer appears to be part of the ElasticSearch object we create for indexing #25233
156. https://github.com/dotCMS/core/issues/26706 : Change Default PUBSUB Provider #26706
157. https://github.com/dotCMS/core/issues/26693 : Edit Permissions Individually stuck when editing folder with legacy ID #26693
158. https://github.com/dotCMS/core/issues/26640 : Cyrillic URLs encoding issue when configured by Vanity URL Redirect #26640
159. https://github.com/dotCMS/core/issues/27007 : Content export with Publish Date Parameter leads to 400 Bad Request error #27007

**Release-23.01.12**

160. https://github.com/dotCMS/core/issues/22372 : Users can create fields with reserved var names #22372
161. https://github.com/dotCMS/core/issues/26796 : Reserved names fields should be compared in a case-insensitive manner to avoid errors and inconsistencies. #26796
162. https://github.com/dotCMS/core/issues/26459 : Unable to copy a contentlet having Site/Folder Field #26459
163. https://github.com/dotCMS/core/issues/22921 : Workflow API unable to archive contentlet #22921
164. https://github.com/dotCMS/core/issues/26605 : Relation_type field too small #26605

**Release-23.01.13**

165. https://github.com/dotCMS/core/issues/25296 : Limited users cannot create content types on System Host #25296
166. https://github.com/dotCMS/core/issues/22385 : Custom templates should push with their page no matter the filter that is selected #22385
167. https://github.com/dotCMS/core/issues/26415 : Template Builder: System Template should create layout always #26415
168. https://github.com/dotCMS/core/issues/26542 : Hide the download button for bundles pushed to static environments #26542

**Release-23.01.14**

169. https://github.com/dotCMS/core/issues/22698 : Limited user without Search Portlet can't Edit Content From Edit Page Portlet #22698
170. https://github.com/dotCMS/core/issues/25729 : Order of comments in Task details modal. #25729
171. https://github.com/dotCMS/core/issues/25653 : Importing Category lists via CSV - import button is disabled #25653
172. https://github.com/dotCMS/core/issues/26815 : Unable to compare history of older versions (Older than latest 20) #26815
173. https://github.com/dotCMS/core/issues/26224 : Need to add the user who create the bundle when you have bundles from other users #26224
174. https://github.com/dotCMS/core/issues/26201 : Deletion of Unpublished Bundles through API #26201
175. https://github.com/dotCMS/core/issues/26170 : Unable to view more than 20 items in a publishing queue bundle #26170
176. https://github.com/dotCMS/core/issues/27775 : Page API Returning Content Related to Page Asset #27775
177. https://github.com/dotCMS/core/issues/27187 : remove unneeded DB hit from edit content screen #27187
178. https://github.com/dotCMS/core/issues/26004 : Image field doesn't export as a header in CSV #26004
179. https://github.com/dotCMS/core/issues/23195 : Site Browser: Slow loading folder with many items #23195

**Release-23.01.15**

180. https://github.com/dotCMS/core/issues/27894 : Security: Critical Vulnerability in Postgres JDBC Driver #27894
181. https://github.com/dotCMS/core/issues/27910 : Log too verbose in certain situations #27910
182. https://github.com/dotCMS/core/issues/27909 : Invalid role check when accessing resource #27909
183. https://github.com/dotCMS/core/issues/27453 : Make experience plugin into a system plugin #27453

**Release-23.01.16**

184. https://github.com/dotCMS/core/issues/28110 : Only run startup tasks if they need to be run #28110
185. https://github.com/dotCMS/core/issues/28105 : PDF as Binary / upload field doesn't show preview #28105
186. https://github.com/dotCMS/core/issues/27361 : CSV Content import cannot process host or folder information #27361
187. https://github.com/dotCMS/core/issues/25903 : Key/Value field escaping colon and comma characters to HTML encoded version. #25903
188. https://github.com/dotCMS/core/issues/24698 : Cannot push publish to S3 buckets in the us-east-2 region #24698

**Release-23.01.17**
