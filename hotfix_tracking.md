#DOTCMS_CORE


This maintenance release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/20725 : Error upgrading to 21.06 using a managed database

2. https://github.com/dotCMS/core/issues/20647 : Problems the first time you try to navigate to the network tab

3. https://github.com/dotCMS/core/issues/20636 : Improving error messages for WorkflowAPIImpl

4. https://github.com/dotCMS/core/issues/20629 : EMA should always expect UTF8

5. https://github.com/dotCMS/core/issues/20578 : MonitorResource should cache good responses / fix IPUtils

6. https://github.com/dotCMS/core/issues/20557 : Site Resource improvements

7. https://github.com/dotCMS/core/issues/20505 : Unable to create a multilingual blog

8. https://github.com/dotCMS/core/issues/20501 : Workflows: Can not copy a workflow with Notify Assigned Actionlet is used

9. https://github.com/dotCMS/core/issues/20666 : Move to a glibc based docker image

10. https://github.com/dotCMS/core/issues/20405 : URL field values are not included in Pages exported from Content->Search

11. https://github.com/dotCMS/core/issues/20519 : Class in rows are lost when you remove a row #20519

12. https://github.com/dotCMS/core/issues/20677 : Users without the 'Login As' Role are still able to select Login As and see all Users #20677

13. https://github.com/dotCMS/core/issues/20721 : "Login As" Data Constraints #20721

14. https://github.com/dotCMS/core/issues/20674 : Unable to pick time in earlier zone #20674

15. https://github.com/dotCMS/core/issues/20659 : The Set Response Headers Rule Action does not allow single quotes in the value #20659

16. https://github.com/dotCMS/core/issues/20623 : Container displays Add button twice under specific situations #20623

17. https://github.com/dotCMS/core/issues/20719 : NPE on tags via graphql if no tags present #20719

18. https://github.com/dotCMS/core/issues/20690 : Add PushPublishingListener to dotCDN #20690

19. https://github.com/dotCMS/core/issues/20688 : Rest API Update Site do not update siteName #20688

20. https://github.com/dotCMS/core/issues/20685 : [Vanity URL] : Missing parameters in Vanity URL cause problems in API #20685

21. https://github.com/dotCMS/core/issues/20649 : IndexPolicy do not respected on wf toolview #20649

22. https://github.com/dotCMS/core/issues/20642 : WYSIWYG fields insert image does not respect the setting of WYSIWYG_IMAGE_URL_PATTERN #20642

23. https://github.com/dotCMS/core/issues/20640 : Tika failing to init, hanging dotCMS startup #20640

24. https://github.com/dotCMS/core/issues/20638 : Update Site do not update aliases #20638

25. https://github.com/dotCMS/core/issues/20597 : Generating resource link for File Asset with Legacy Identifier is failing #20597

26. https://github.com/dotCMS/core/issues/20591 : Don't try to move or rewrite license.zip file on startup. #20591

27. https://github.com/dotCMS/core/issues/20494 : Error retrieving content references in multilingual setup #20494

28. https://github.com/dotCMS/core/issues/20504 : Create a "Bulk Move" Action for Content #20504

29. https://github.com/dotCMS/core/issues/7342 : System allows to create exact same language more than twice #7342

30. https://github.com/dotCMS/core/issues/20714 : NPE trying to fix folder conflicts #20714

31. https://github.com/dotCMS/core/issues/20707 : Advance Image Transformations and SASS compilation not working on Static Publish and Time Machine #20707

32. https://github.com/dotCMS/core/issues/20669 : Error deploying a fresh install using mssql #20669

33. https://github.com/dotCMS/core/issues/20469 : When generating a site statically, we need to generate the default language last. #20469

34. https://github.com/dotCMS/core/issues/19215 : Add the option to show fields in a relationship field's overview #19215

35. https://github.com/dotCMS/core/issues/18014 : FileTool breaks contentlet rendering if the file/image is not live #18014

36. https://github.com/dotCMS/core/issues/18111 : Copy Workflow results in incorrect next step #18111

37. https://github.com/dotCMS/core/issues/19931	: Automatically delete old ES indices (indexes)

38. https://github.com/dotCMS/core/issues/20799 : When try to import a Bundle get a OutOfMemoryError #20799

39. https://github.com/dotCMS/core/issues/20965 : Response code in Integrity Checker producing a 502 #20965

40. https://github.com/dotCMS/core/issues/20616 : Button for Navigation Reorder is not working correctly #20616

41. https://github.com/dotCMS/core/issues/20746 : Content Export tool is not exporting relationships correctly #20746

42. https://github.com/dotCMS/core/issues/20722 : [Reindex] : Perform Site check before Folder check #20722

43. https://github.com/dotCMS/core/issues/20452 : "Back end user" role breaks front end user permissions #20452

44. https://github.com/dotCMS/core/issues/18301 : SASS not working with multiple independent SCSS files. Causes hanging on front-end #18301

45. https://github.com/dotCMS/core/issues/20791 : [REST API] : Page API returns publishDate and modDate fields with the same value #20791

46. https://github.com/dotCMS/core/issues/20801 : Warning log message incomplete for not allowed categories when retrieving content #20801

47. https://github.com/dotCMS/core/issues/21313 : Push publishing sometimes changes the order of contentlets in the multi-tree table on multilingual pages #21313

48. https://github.com/dotCMS/core/issues/20995 : Use a different connection for PostgresPubSub.publish #20995

49. https://github.com/dotCMS/core/issues/21393 : log4j2 zero day exploit #21393

50. https://github.com/dotCMS/core/issues/20793 : Upgrade log4j version to permit JsonLayout custom key-value pairs #20793

51. https://github.com/dotCMS/core/issues/21392 : Make userId immutable #21392

52. https://github.com/dotCMS/core/issues/21485 : Upgrade log4j2 to 2.17.1 #21485

53. https://github.com/dotCMS/core/issues/21537 : [Static Publishing] : Problem with multi-language contents #21537

54. https://github.com/dotcms/core/issues/21267 : ReindexThread dies with bad data #21267, 2nd inclusion with additional fixes

55. https://github.com/dotCMS/core/issues/21204 : [Site Copy] : Copying a Site randomly fails #21204

56. https://github.com/dotCMS/core/issues/21363 : db passwords with characters (specifically @ and possibly others) will break pub/sub due to the connection string #21363

57. https://github.com/dotCMS/core/issues/21252 : Add the ability to stop/abort a workflow on velocity script actionlet #21252

58. https://github.com/dotCMS/core/issues/21097 : Past Time Machine not working #21097