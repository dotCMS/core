#DOTCMS_CORE


This maintenance release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/19302 : If dotSecretsStore.p12 gets corrupt, interceptors start to fail
   ( Because of the differences between master and this version, additional classes had to be included for the official PR to work )

2. https://github.com/dotCMS/core/issues/19310 : Handle Runtime Exception on Jersey

3. https://github.com/dotCMS/core/issues/19304 : Edit Mode: Adding Content on Page missing pagination

4. https://github.com/dotCMS/core/issues/19267 : Limited User can not edit Categories

5. https://github.com/dotCMS/core/issues/19255 : Support GraphQL Schema invalidation across cluster

6. https://github.com/dotCMS/core/issues/19181 : "UPLOAD NEW FILE" button does not work in Image/File fields

7. https://github.com/dotCMS/core/issues/19165 : [Layout] : Custom layout doesn't refresh after saving changes

8. https://github.com/dotCMS/core/issues/19500 : SQL Injection Vulnerability in api /api/v1/containers

9. https://github.com/dotCMS/core/issues/19338 : [core] : GIF files not showing up correctly in the backend

10. https://github.com/dotCMS/core/issues/19335 : After upload image the list is not refreshing

11. https://github.com/dotCMS/core/issues/19152 : PP Filters reloads when flushing cache

12. https://github.com/dotCMS/core/issues/19075 : Incorrect filter name in the bundle details

13. https://github.com/dotCMS/core/issues/19566 : [logging] : General logging improvements

14. https://github.com/dotCMS/core/issues/18834 : Wonky Field Reordering

15. https://github.com/dotCMS/core/issues/19854 : [core] : Page Mode set incorrectly with limited user

16. https://github.com/dotCMS/core/issues/19831 : [core] : URL validation for new pages under Site root is not correct

17. https://github.com/dotCMS/core/issues/19796 : depth>=3 doesn't work for self-related content

18. https://github.com/dotCMS/core/issues/19773 : SAML needs to transform user ID sent by IdP into a valid dotCMS user ID

19. https://github.com/dotCMS/core/issues/19753 : Thumbnail creator filter is not working for .pdf files

20. https://github.com/dotCMS/core/issues/19715 : Make XStream initialization static

21. https://github.com/dotCMS/core/issues/19560 : Add a "Download" button to the Log Files tab in the Maintenance portlet

22. https://github.com/dotCMS/core/issues/19728	: graphql query of [Blog] -> [related File with Category field] produces error

23. https://github.com/dotCMS/core/issues/19725 : GraphQL freezes when PP new content