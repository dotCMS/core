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

32. https://github.com/dotCMS/core/issues/19547	: Network Tab not reporting on Nodes

33. https://github.com/dotCMS/core/issues/19536	: CSV Importer should use _dotraw for text key and unique fields

34. https://github.com/dotCMS/core/issues/19513	: Fix possible NPE in MimeTypeUtils

35. https://github.com/dotCMS/core/issues/19498	: Add Resolved VanityUrl as request attribute

36. https://github.com/dotCMS/core/issues/19497	: PP Sometimes fails because of file-based containers

37. https://github.com/dotCMS/core/issues/19489 : If a field is marked unique, we should es map it as keyword

38. https://github.com/dotCMS/core/issues/19486	: Wrong defaults

39. https://github.com/dotCMS/core/issues/19458	: [Content] : Tag field from user returns different data compared to system Tag field

40. https://github.com/dotCMS/core/issues/19452	: URL maps which match the URL map pattern and content values fail with 404 on customer system

41. https://github.com/dotCMS/core/issues/19449 : GraphQL failing to retrieve image info

42. https://github.com/dotCMS/core/issues/19337	: Content Search screen not filtering on "select" fields - 5.2x+ - reproducable in demo

43. https://github.com/dotCMS/core/issues/19098	: With SAML enabled, explicit logout takes you to the native login page, instead of re-authing through SAML

44. https://github.com/dotCMS/core/issues/19044	: Unpublished related content appearing in Preview Mode / Live mode & Are published when Pushing to S3 - 5.3x

45. https://github.com/dotCMS/core/issues/18605	: ReindexThread always runs - never stops

46. https://github.com/dotCMS/core/issues/18505	: JSONTool does not return sub arrays

47. https://github.com/dotCMS/core/issues/19890	: Custom Page Layout is not sending in Push PublishCustom Page Layout is not sending in Push Publish

48. https://github.com/dotCMS/core/issues/19910	: Google Translate Sub-action is sending error with even with valid translation key

49. https://github.com/dotCMS/core/issues/19832	: [core] : Legacy IDs are not compatible with Shorty API

50. https://github.com/dotCMS/core/issues/19927	: Adding more log to the JsonTOOL

51. https://github.com/dotCMS/core/issues/19319	: Allow shutdown from backend of dotCMS

52. https://github.com/dotCMS/core/issues/19620	: Push Publish Batch Button selects all assets

53. https://github.com/dotCMS/core/issues/20189 : [SAML] : Authentication process must check for both User ID and Email

54. https://github.com/dotCMS/core/issues/20156 : User is logged out when accessing content if the Role does not have the 'Content' portlet tool group

55. https://github.com/dotCMS/core/issues/20122 : Cluster Ids cannot contain underscores.

56. https://github.com/dotCMS/core/issues/20068 : Allow portal.properties to be overridden by environmental variables

57. https://github.com/dotCMS/core/issues/20063 : Send Cookies Secure and HttpOnly

58. https://github.com/dotCMS/core/issues/19679 : Don't let rule PP fail

59. https://github.com/dotCMS/core/issues/20053 : Anonymous users cannot fire actions when specified by their identifier

60. https://github.com/dotCMS/core/issues/20013 : Potential Timezone Bug

61. https://github.com/dotCMS/core/issues/19511 : Contentlets with future publish date prevent Page Asset from being published.

62. https://github.com/dotCMS/core/issues/19993 : [workflow] : The "Send an Email" sub-action fails if executed before "Save content" sub-action 

63. https://github.com/dotCMS/core/issues/19992 : Elasticsearch RestHighLevelClient does not handle RuntimeExceptions

64. https://github.com/dotCMS/core/issues/19974 : Performance issues with GraphQL under load

65. https://github.com/dotCMS/core/issues/19951 : Date time field, should respect the time zone from format or company

66. https://github.com/dotCMS/core/issues/19926 : NPE on every page request after setting: ENABLE_NAV_PERMISSION_CHECK=true

67. https://github.com/dotCMS/core/issues/19913 : [code] : Re-adding parent relationship after removal throws an error

68. https://github.com/dotCMS/core/issues/19877 : [core] : Remove unnecessary Web Token 

69. https://github.com/dotCMS/core/issues/19813 : Allow mail session to be configured via environmental variables

70. https://github.com/dotCMS/core/issues/20041 : Time machine is causing an error with the index

71. https://github.com/dotCMS/core/issues/20136 : [Push Publish] : Selecting the REMOVE option in Push Publishing modal is not working

72. https://github.com/dotCMS/core/issues/20197 : Cannot relate content to a macrolanguage if there is the same language with a country code

73. https://github.com/dotCMS/core/issues/20164 : JsonTool parsing

74. https://github.com/dotCMS/core/issues/20250 : [Push Publishing] : Improving error message when finding unique content match

75. https://github.com/dotCMS/core/issues/20232 : [Integrity Checker] : Improving error message when fixing File Asset conflict

76. https://github.com/dotCMS/core/issues/20252 : Dates with format yyyy-MM-dd HH:mm:ss.SSS are mapped as texts in ES

77. https://github.com/dotCMS/core/issues/19723 : Only 10 parent related contents retrieved when editing a child content

78. https://github.com/dotCMS/core/issues/20147 : If UrlMap specifies an invalid field, then you can't open the edit content screen

79. https://github.com/dotCMS/core/issues/19412 : "Permission Individually" on a folder defaults to all permissions of its parent, not just the inheritable ones

80. https://github.com/dotCMS/core/issues/20364 : XStream throws exception when posting bad XML

81. https://github.com/dotCMS/core/issues/19500 : SQL Injection Vulnerability in api /api/v1/containers

82. https://github.com/dotCMS/core/issues/20636 : Improving error messages for WorkflowAPIImpl

83. https://github.com/dotCMS/core/issues/20629 : EMA should always expect UTF8

84. https://github.com/dotCMS/core/issues/20578 : MonitorResource should cache good responses / fix IPUtils

85. https://github.com/dotCMS/core/issues/20568 : configuration: limit bulk request size

86. https://github.com/dotCMS/core/issues/20501 : Workflows: Can not copy a workflow with Notify Assigned Actionlet is used

87. https://github.com/dotCMS/core/issues/20499 : Problems trying to login from a PC if you are using mssql as db

88. https://github.com/dotCMS/core/issues/20488 : Workflow: Can not import when next assign role or permissions not exists

89. https://github.com/dotCMS/core/issues/20450 : [REST] : Filtering Endpoint in User Resource is incorrectly parsing some parameters

90. https://github.com/dotCMS/core/issues/20416 : Add a header if there is a matching vanity url

91. https://github.com/dotCMS/core/issues/20412 : Set Cache-control header based on HTMLPage cacheTTL