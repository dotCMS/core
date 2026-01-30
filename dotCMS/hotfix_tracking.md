## DOTCMS BACKPORT LIST

This maintenance release includes the following code fixes:

**Release-24.12.27 LTS**

1. https://github.com/dotCMS/core/issues/30969 : SAML: Retrieve the relay state on the SAML Endpoint #30969
2. https://github.com/dotCMS/core/issues/30804 : The file with name of webPageContent.vtl can't GET and throws 404 #30804
3. https://github.com/dotCMS/core/issues/30993 : In 24.12.10 the EsReadOnlyMonitorJob still shows error #30993
4. https://github.com/dotCMS/core/issues/30660 : Unable to use Templates across Multiple Sites #30660
5. https://github.com/dotCMS/core/issues/30984 : UVE: Fix Trailing Slash Issue in dotCMS #30984
6. https://github.com/dotCMS/core/issues/30982 : Files without an extension cannot be edited / saved #30982
7. https://github.com/dotCMS/core/issues/31034 : Empty allowed file type not being saved #3103
8. https://github.com/dotCMS/core/issues/30694 : [Localization] : Special chars are not being migrated correctly #30694
9. https://github.com/dotCMS/core/issues/30761 : Data is not getting saved in the workflow #30761
10. https://github.com/dotCMS/core/issues/31185 : Fix for IllegalStateException when stopping re-satrting an OSGI plugin with Jersey resources #31185
11. https://github.com/dotCMS/core/issues/31142 : UVE: Edit Mode not working in Firefox #31142
12. https://github.com/dotCMS/core/issues/31563 : Creating a file in a secondary language won't return the file information #31563
13. https://github.com/dotCMS/core/issues/31343 : Wysiwyg fields hide when there are multiple wysiwyg fields in a content type #31343
14. https://github.com/dotCMS/core/issues/31300 : VTL Syntax highlighting missing in the new code editor modal #31300
15. https://github.com/dotCMS/core/issues/31143 : Block Editor Extension Not Loading in dotCMS #31143
16. https://github.com/dotCMS/core/issues/31019 : Token Invalid in CubeJS Server after token renew #31019
17. https://github.com/dotCMS/core/issues/31207 : Improve performance to retrieve Analytics Data #31207
18. https://github.com/dotCMS/core/issues/31022 : Host save as null in PAGE_REQUEST events #31022
19. https://github.com/dotCMS/core/issues/30875 : CA Search: Define queries for the help dialog #30875
20. https://github.com/dotCMS/core/issues/30802 : Update clickhouse data to use new field/property names #30802
21. https://github.com/dotCMS/core/issues/29869 : Unable to authenticate a servlet request #29869
22. https://github.com/dotCMS/core/issues/30353 : DropOldContentVersionsJob not running on large dataset #30353
23. https://github.com/dotCMS/core/issues/31216 : Push Publishing not escaping comma in title. #31216
24. https://github.com/dotCMS/core/issues/30544 : The last role in the Roles list is partially visible #30544
25. https://github.com/dotCMS/core/issues/31509 : Content Import Functionality Disabled When "Content Search" Tool is Hidden for Specific Roles #31509
26. https://github.com/dotCMS/core/issues/31564 : Special Character & Truncated in Category Name After Creation #31564
27. https://github.com/dotCMS/core/issues/31752 : UVE: App URL Configuration Not Being Respected #31752
28. https://github.com/dotCMS/core/issues/31649 : Filter on checkbox field not working as expected (Content search portal and relationship filter) #31649
29. https://github.com/dotCMS/core/issues/31980 : #dotParse and #dotCache throw NumberFormatException when TTL argument is null #31980
30. https://github.com/dotCMS/core/pull/31972
31. https://github.com/dotCMS/core/issues/31804 : UVE: Content Panel Section Can No Longer Be Hidden #31804
32. https://github.com/dotCMS/core/issues/31709 : Edit Button in Block Editor Only Visible for Blog Content Type #31709
33. https://github.com/dotCMS/core/issues/31601 : Relate Button in a Relationship Field is not working #31601
34. https://github.com/dotCMS/core/pull/32057
35. https://github.com/dotCMS/core/issues/31933 : The "Select All Content" option is intermittently not appearing for some users across different devices and browsers #31933
36. https://github.com/dotCMS/core/issues/31994 : SAML: skip RequestedAuthenticationContext #31994
37. https://github.com/dotCMS/core/issues/31919 : UVE: Incorrect Language Layout Loaded When Editing Multilingual Pages #31919
38. https://github.com/dotCMS/core/issues/32137 : Task241015ReplaceLanguagesWithLocalesPortlet cast issue #32137
39. https://github.com/dotCMS/core/issues/31141 : Automated Unpublishing Fails for Content with Inactive/Deleted Last Modified Users #31141
40. https://github.com/dotCMS/core/issues/32106 : Indent does not work in the Block Editor #32106
41. https://github.com/dotCMS/core/issues/32186 : Experiments: Variant is sporadically not added to index #32186
42. https://github.com/dotCMS/core/issues/30864 : Display Bug in Template Dropdown: Missing Templates and Console Error #30864
43. https://github.com/dotCMS/core/issues/32131 : IntegrityChecker serialization error #32131
44. https://github.com/dotCMS/core/issues/31894 : Contentlets with Relationships breaks block editor #31894
45. https://github.com/dotCMS/core/issues/32414 : Old Edit Content: Template Selector is losing current template reference #32414
46. https://github.com/dotCMS/core/issues/32522 : [Block Editor] Contentlet included by the second block editor is not shown in a page #32522
47. https://github.com/dotCMS/core/issues/31790#issuecomment-2941061548 : Using Containers in theme files
48. https://github.com/dotCMS/core/issues/32473 : [Edit Contentlet]: Binary field editor not showing for JS files #32473
49. https://github.com/dotCMS/core/issues/32423 : [Push Publishing] : Error when pushing Advanced Templates #32423
50. https://github.com/dotCMS/core/issues/32130 : Users with CMS Admin Role don’t update correctly if you change User Details #32130
51. https://github.com/dotCMS/core/issues/32075 : When a folder contains Parentheses dotCLI pulls error. #32075
52. https://github.com/dotCMS/core/issues/31859 : Apps Export Configurations Error Handling #31859
53. https://github.com/dotCMS/core/issues/31792 : Static Push Fails with uppercase extensions #31792
54. https://github.com/dotCMS/core/issues/29324 : Unable to Upload Image from Custom Tool Group #29324
55. https://github.com/dotCMS/core/issues/32127 : Bring Back older versions of Container missing #32127
56. https://github.com/dotCMS/core/issues/31688 : dotAI Auto Update Index is not creating/updating indexes #31688
57. https://github.com/dotCMS/core/issues/31077 : PrimeNG Datepicker Displays Incorrectly When Appended to Body #31077
58. https://github.com/dotCMS/core/issues/32578 : Special characters in key being removed for a key value field on a content type #32578
59. https://github.com/dotCMS/core/issues/32545 : api/v1/workflow/actions/default/fire/PUBLISH not respecting default value #32545
60. https://github.com/dotCMS/core/issues/32413 : [Containers]: Pagination is not working #32413
61. https://github.com/dotCMS/core/issues/32346 : Edit Contentlet Screen Closes Automatically on Save in Page Preview #32346
62. https://github.com/dotCMS/core/issues/32583 : NPE in on content edit (Not on Beta version) when content type named "Event" lacks calendar fields #32583
63. https://github.com/dotCMS/core/issues/33006 : Backport security fix to LTSs #33006
64. https://github.com/dotCMS/core/issues/32989 : Upgrade Tomcat to Latest Version 9 Patch Release #32989
65. https://github.com/dotCMS/core/issues/32929 : Core: Create user not set user role #32929
66. https://github.com/dotCMS/core/issues/33093 : Update image reference breaking out build #33093
67. https://github.com/dotCMS/core/issues/33031 : Reset Approvals sub-action not working as expected #33031
68. https://github.com/dotCMS/core/issues/33138 : Add Step Id to GET /api/v1/workflow/tasks/history/comments/{contentletIdentifier} endpoint #33138
69. https://github.com/dotCMS/core/issues/32860 : dotcms.log file stops receiving log data #32860
70. https://github.com/dotCMS/core/issues/32224 : UVE Query Parameters Are Being Filtered Breaking Widget Logic #32224
71. https://github.com/dotCMS/core/issues/33473 : [DEFECT] Putting host in 404 cache when it actually exists and it's active #33473
72. https://github.com/dotCMS/core/issues/33302 : [DEFECT] Cache Provider Init problem #33302
73. https://github.com/dotCMS/core/issues/33265 : [FEATURE] disregard common query params as Page Cache keys #33265
74. https://github.com/dotCMS/core/issues/32802 : Long file names in the list view are clipped instead of being truncated gracefully #32802
75. https://github.com/dotCMS/core/issues/32249 : Starter zip file entries being created with leading slash #32249
76. https://github.com/dotCMS/core/issues/31348 : OSGI Rest Resource fails upon Redeploy with message: IllegalArgumentException: object is not an instance of declaring class #31348
77. https://github.com/dotCMS/core/issues/33617 : [FEATURE] Ability to update user email via REST API (PUT /api/v1/users/{id}) #33617
78. https://github.com/dotCMS/core/issues/33096 : Content Editor: References tab #33096
79. https://github.com/dotCMS/core/issues/32581 : Apply a recommended fix for SQL Injection in dotCMS/core #32581
80. https://github.com/dotCMS/core/issues/33768 : [TASK] Backport security issue 482 #33768
81. https://github.com/dotCMS/core/issues/33767 : [TASK] Backport security fix #33767
82. https://github.com/dotCMS/core/issues/34278 : [DEFECT] PublisherQueueJob (StatefulJob) updates Elasticsearch index but fails to commit DB changes when processing large batches #34278
83. https://github.com/dotCMS/core/issues/33434 : [DEFECT] #editContentlet macro doesn't properly escape apostrophes #33434
84. https://github.com/dotCMS/core/issues/34131 : Log Files view does not display log files from subdirectories #34131

