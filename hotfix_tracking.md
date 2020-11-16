#DOTCMS_CORE


This maintenance release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/19302 : If dotSecretsStore.p12 gets corrupt, interceptors start to fail
   ( Because of the differences between master and this version, additional classes had to be included for the official PR to work )

2. https://github.com/dotCMS/core/issues/19310 : Handle Runtime Exception on Jersey

3. https://github.com/dotCMS/core/issues/19304 : Edit Mode: Adding Content on Page missing pagination

            4. https://github.com/dotCMS/core/issues/19267 : Limited User can not edit Categories

5. https://github.com/dotCMS/core/issues/19181 : "UPLOAD NEW FILE" button does not work in Image/File fields