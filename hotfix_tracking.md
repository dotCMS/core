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

24. https://github.com/dotCMS/core/issues/19686 : Error when editing multilingual content that is referenced on monolingual HTML pages

25. https://github.com/dotCMS/core/issues/19639	: Content API not returning name or title in dotAsset

26. https://github.com/dotCMS/core/issues/19636	: Bundling fails when adding content to a bundle then attepting to push or download the bundle

27. https://github.com/dotCMS/core/issues/19634	: Push publishing a page fails due to invalid urlMap detail page

28. https://github.com/dotCMS/core/issues/19621 : Stop eating errors

29. https://github.com/dotCMS/core/issues/19608 : Contentlets lose inherited permissions until cache is flushed

30. https://github.com/dotCMS/core/issues/19578	: [OSGI] : Overriding Classes does not work in latest master

31. https://github.com/dotCMS/core/issues/19564	: 301 and 302 Vanity URL redirects do not pass querystrings

32. https://github.com/dotCMS/core/issues/19555	: Deleting a user is not replacing the owner on identifier table

33. https://github.com/dotCMS/core/issues/19547	: Network Tab not reporting on Nodes

34. https://github.com/dotCMS/core/issues/19536	: CSV Importer should use _dotraw for text key and unique fields

35. https://github.com/dotCMS/core/issues/19513	: Fix possible NPE in MimeTypeUtils

36. https://github.com/dotCMS/core/issues/19498	: Add Resolved VanityUrl as request attribute

37. https://github.com/dotCMS/core/issues/19497	: PP Sometimes fails because of file-based containers

38. https://github.com/dotCMS/core/issues/19489 : If a field is marked unique, we should es map it as keyword

39. https://github.com/dotCMS/core/issues/19486	: Wrong defaults

40. https://github.com/dotCMS/core/issues/19458	: [Content] : Tag field from user returns different data compared to system Tag field

41. https://github.com/dotCMS/core/issues/19452	: URL maps which match the URL map pattern and content values fail with 404 on customer system

42. https://github.com/dotCMS/core/issues/19449 : GraphQL failing to retrieve image info

43. https://github.com/dotCMS/core/issues/19337	: Content Search screen not filtering on "select" fields - 5.2x+ - reproducable in demo

44. https://github.com/dotCMS/core/issues/19098	: With SAML enabled, explicit logout takes you to the native login page, instead of re-authing through SAML

45. https://github.com/dotCMS/core/issues/19044	: Unpublished related content appearing in Preview Mode / Live mode & Are published when Pushing to S3 - 5.3x

46. https://github.com/dotCMS/core/issues/18605	: ReindexThread always runs - never stops

47. https://github.com/dotCMS/core/issues/18505	: JSONTool does not return sub arrays

48. https://github.com/dotCMS/core/issues/19890	: Custom Page Layout is not sending in Push PublishCustom Page Layout is not sending in Push Publish

49. https://github.com/dotCMS/core/issues/19910	: Google Translate Sub-action is sending error with even with valid translation key