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
