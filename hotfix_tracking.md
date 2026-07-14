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
