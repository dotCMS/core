#DOTCMS_CORE


This maintenance release includes the following code fixes:

1. https://github.com/dotCMS/core/issues/19302 : If dotSecretsStore.p12 gets corrupt, interceptors start to fail
   ( Because of the differences between master and this version, additional classes had to be included for the official PR to work )