## DOTCMS BACKPORT LIST

This maintenance release includes the following code fixes:

Release-25.07.10 LTS

1. https://github.com/dotCMS/core/issues/32578 : Special characters in key being removed for a key value field on a content type #32578
2. https://github.com/dotCMS/core/issues/32545 : api/v1/workflow/actions/default/fire/PUBLISH not respecting default value #32545
3. https://github.com/dotCMS/core/issues/32583 : NPE in on content edit (Not on Beta version) when content type named "Event" lacks calendar fields #32583
4. https://github.com/dotCMS/core/issues/33006 : Backport security fix to LTSs #33006
5. https://github.com/dotCMS/core/issues/32989 : Upgrade Tomcat to Latest Version 9 Patch Release #32989
6. https://github.com/dotCMS/core/issues/32929 : Core: Create user not set user role #32929
7. https://github.com/dotCMS/core/issues/33093 : Update image reference breaking out build #33093
8. https://github.com/dotCMS/core/issues/33031 : Reset Approvals sub-action not working as expected #33031
9. https://github.com/dotCMS/core/issues/32224 : UVE Query Parameters Are Being Filtered Breaking Widget Logic #32224
10. https://github.com/dotCMS/core/issues/33138 : Add Step Id to GET /api/v1/workflow/tasks/history/comments/{contentletIdentifier} endpoint #33138
11. https://github.com/dotCMS/core/issues/32860 : dotcms.log file stops receiving log data #32860
12. https://github.com/dotCMS/core/issues/33628 : [TASK] Update java-base to include native java.net.http module #33628
13. https://github.com/dotCMS/core/issues/33473 : [DEFECT] Putting host in 404 cache when it actually exists and it's active #33473
14. https://github.com/dotCMS/core/issues/33302 : [DEFECT] Cache Provider Init problem #33302
15. https://github.com/dotCMS/core/issues/33265 : [FEATURE] disregard common query params as Page Cache keys #33265
16. https://github.com/dotCMS/core/issues/33255 : Stackoverflow error during startup when trying to migrate from earlier version to later ones because of contentlets living on System Host #33255
17. https://github.com/dotCMS/core/issues/32802 : Long file names in the list view are clipped instead of being truncated gracefully #32802
18. https://github.com/dotCMS/core/issues/33617 : [FEATURE] Ability to update user email via REST API (PUT /api/v1/users/{id}) #33617
19. https://github.com/dotCMS/core/issues/33096 : Content Editor: References tab #33096
20. https://github.com/dotCMS/core/issues/32581 : Apply a recommended fix for SQL Injection in dotCMS/core #32581
21. https://github.com/dotCMS/core/issues/33768 : [TASK] Backport security issue 482 #33768
22. https://github.com/dotCMS/core/issues/33767 : [TASK] Backport security fix #33767
23. https://github.com/dotCMS/core/issues/34278 : [DEFECT] PublisherQueueJob (StatefulJob) updates Elasticsearch index but fails to commit DB changes when processing large batches #34278
24. https://github.com/dotCMS/core/issues/33434 : [DEFECT] #editContentlet macro doesn't properly escape apostrophes #33434 
25. https://github.com/dotCMS/core/issues/34163 : Fix Apache Tika XXE CVE-2025-66516 #34163
26. https://github.com/dotCMS/core/issues/34954 : security: upgrade Apache Tomcat to 9.0.113+ to fix CVE-2025-66614 #34954
27. https://github.com/dotCMS/core/issues/34749 : Publishing queue: clicking pending bundle redirects to Users/first tab instead of bundle details #34749
28. https://github.com/dotCMS/core/issues/34130 : [DEFECT] Incorrect and repeated titles for Site Search results [Url Mapped Content] #34130
29. https://github.com/dotCMS/core/issues/33908 : Content Import Fails Silently When Importing Large CSV Files (Struts Request Recycling Issue Causes Backend Exception) #33908
30. https://github.com/dotCMS/core/issues/34212 : fix: Add FIPS mode detection and auto-disable APR SSL Engine #34212
31. https://github.com/dotCMS/core/issues/34888 : Rules include endpoint improve Error Message : return proper HTTP error (400/404) for invalid id instead of server error #34888
32. https://github.com/dotCMS/core/issues/34786 : Disable exposed /api/application.wadl endpoint #34786
33. https://github.com/dotCMS/core/issues/35235 : fix(security): Upgrade vulnerable dependencies flagged by OWASP Dependency Check #35235
34. https://github.com/dotCMS/core/issues/35568 : fix(lang-var-migration): unique_fields duplicate key cascades and aborts entire migration transaction #35568
35. https://github.com/dotCMS/core/issues/35793 : deps: bump Apache Tomcat 9.0.113 → 9.0.118 (resolves 6 published CVEs) #35793
36. https://github.com/dotCMS/core/issues/35536 : Basic Auth plugin issue: SpeedyAssetServlet rejects anonymously-readable assets when an unrelated Basic Authorization header is present #35536
37. https://github.com/dotCMS/core/issues/36147 : Saving contentlets with many-to-many relationship fields and large related-content volumes is extremely slow #36147
38. https://github.com/dotCMS/core/issues/33784 : [DEFECT] Full cache flush shutdown h22 threadpool #33784
39. https://github.com/dotCMS/core/issues/33645 : [DEFECT] NPE when fetching content via content api with depth 3 and high limit #33645
40. https://github.com/dotCMS/core/issues/35896 : fix(security): upgrade Bouncy Castle 1.70 → 1.84 (CVE-2025-14813) #35896
41. https://github.com/dotCMS/core/issues/36739 : Remove repackaged dot.commons-io dependency (CVE-2024-47554 follow-up to #35235) #36739
42. https://github.com/dotCMS/core/issues/36592 : security: upgrade Apache Tomcat 9.0.118 → 9.0.120 on main #36592
43. https://github.com/dotCMS/core/issues/34454 : [TASK] Optimize heavy SQL queries #34454
44. https://github.com/dotCMS/private-issues/issues/651 : Stored XSS → RCE: low-priv backend user name bypasses Xss filter, runs in admin session, deploys OSGi bundle #651
45. https://github.com/dotCMS/private-issues/issues/642 : sec: Privilege Escalation + RCE via OSGi bundle upload (low-priv backend user → CMS Administrator) #642
46. https://github.com/dotCMS/core/issues/36851 : Native, configurable HTML minification in the core rendering engine #36851
47. https://github.com/dotCMS/core/issues/37085 : Upgrade BouncyCastle to 1.85 across all three bundled locations (CVE-2026-59638) #37085
48. https://github.com/dotCMS/core/issues/34435 : [DEFECT] Portlet cache throws an error when using the Redis cache provider #34435
49. https://github.com/dotCMS/core/issues/37252 : Backport private-issues/issues/668 into 25.07 #37252
50. https://github.com/dotCMS/core/issues/37603 : Binary field stuck on infinite loading spinner when creating new content (legacy Dojo/JSP editor) #37603
51. https://github.com/dotCMS/core/issues/35996 : security(deps): upgrade bundled DOMPurify in TinyMCE / Monaco / Swagger UI assets (CVE-2025-26791 follow-up) #35996
52. https://github.com/dotCMS/core/issues/37956 : Backport the AWS SDK v1 upgrade (#37904) into 25.07 LTS #37956
53. https://github.com/dotCMS/core/pull/37474 : fix(network): handle IPv6 addresses in IPUtils checks #37474
54. https://github.com/dotCMS/core/pull/37549 : fix(velocity): harden SecureIntrospector restricted-class coverage #37549
55. https://github.com/dotCMS/core/pull/37947 : fix(webdav): confine temp storage writes to the temp directory #37947
56. https://github.com/dotCMS/core/pull/38101 : fix(rest): require admin and Maintenance portlet to tail log files #38101
57. https://github.com/dotCMS/core/issues/37592 : Remove pgjdbc-ng and PostgresPubSubImpl (superseded by JDBCPubSubImpl) #37592
58. https://github.com/dotCMS/core/pull/37626 : chore(deps): bump six libraries with published security fixes #37626
59. https://github.com/dotCMS/core/pull/38023 : fix(publishing): validate bundle receipt and restrict endpoint management #38023
60. https://github.com/dotCMS/core/pull/38095 : chore(push-publish): remove leftover debug logging #38095
61. https://github.com/dotCMS/core/pull/38099 : fix(achecker): validate request values and harden the embedded database access #38099
62. https://github.com/dotCMS/core/pull/38097 : fix(workflow): record the requesting user on bulk task actions #38097
63. https://github.com/dotCMS/core/pull/38114 : fix(workflows): encode scheme, step and action names in legacy admin JSPs #38114
64. https://github.com/dotCMS/core/pull/38106 : chore(push-publish): compare endpoint keys with MessageDigest.isEqual #38106
65. https://github.com/dotCMS/core/pull/38113 : fix(folders): require EDIT on the source folder when moving it #38113
66. https://github.com/dotCMS/core/pull/38077 : fix(permissions): require EDIT_PERMISSIONS before PermissionAjax changes an asset's permissions #38077
67. https://github.com/dotCMS/core/pull/38121 : fix(publishing): check permissions before deleting push history #38121
68. https://github.com/dotCMS/core/pull/38096 : fix(containers): restrict sortContentletsBy to the supported sort columns #38096
69. https://github.com/dotCMS/core/pull/38126 : fix(legacy-ui): check user permissions in legacy Ajax and Struts handlers #38126
70. https://github.com/dotCMS/core/pull/38112 : fix(rest): validate remote URL targets and redirects up front; align role membership reads #38112
71. https://github.com/dotCMS/core/pull/38107 : fix(graphql): scope response cache entries to the caller and request context #38107
72. https://github.com/dotCMS/core/pull/38128 : fix(rest): return lock owner name to back-end users and normalize forgot-password response #38128
73. https://github.com/dotCMS/core/pull/38149 : fix(publishing): require access rules for uploaded bundles and plugin jars in received bundles #38149
74. https://github.com/dotCMS/core/pull/38127 : fix(tags): check the user's permissions when deleting tags #38127
75. https://github.com/dotCMS/core/pull/38098 : feat(apps): manage Apps configuration as an administrator setting #38098
76. https://github.com/dotCMS/core/pull/38120 : fix(users): only a CMS Administrator can change a user's roles #38120
77. https://github.com/dotCMS/core/pull/38178 : fix(js): load JavaScript modules through the content API #38178
78. https://github.com/dotCMS/core/pull/38176 : fix(docker): update the JDK truststore in the image #38176 #999
79. https://github.com/dotCMS/core/pull/38083 : chore(saml): bump com.dotcms.samlbundle to 26.10.17 #38083 #725
80. https://github.com/dotCMS/core/pull/36591 : fix(saml): return front-end users to the requested page after login (validate the post-login destination) #36591 #879
81. https://github.com/dotCMS/core/pull/38296 : fix(docker): block the compromised eMudhra intermediate CAs in the JDK #38296 #999
83. https://github.com/dotCMS/private-issues/issues/1037 : chore(deps): upgrade Angular to 19.2 and Nx to 20 in core-web #1037
