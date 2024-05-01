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