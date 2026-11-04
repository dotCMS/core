# Apps Configuration Permission Model

Apps configuration (the per-site values stored for each app, such as provider keys, endpoints and
login settings) is an administrator setting, like Maintenance and other instance-level settings.

## Who can do what

| Operation | `AppsAPI` methods | Allowed for |
|-----------|-------------------|-------------|
| Change configuration | `saveSecrets`, `saveSecret`, `deleteSecrets`, `deleteSecret` | CMS administrators and the system user |
| Read configuration values | `getSecrets` | CMS administrators and the system user |
| Upload an app descriptor or remove an app | `createAppDescriptor`, `removeApp` | CMS administrators and the system user |
| List apps, descriptors and which sites are configured | `listAppKeys`, `getAppDescriptors`, `getAppDescriptor`, `filterSitesForAppKey`, `computeWarningsBySite` | CMS administrators and users with the Apps portlet |

Other callers get a `DotSecurityException`, which REST resources return as 403. Users with the Apps
portlet can still open the portlet and see which apps are configured on which sites; opening a
configuration, saving it, deleting it, uploading a descriptor or removing an app needs an
administrator. The rule lives in
`AppsAPIImpl.userCannotManageAppConfiguration`, so every caller of these methods (the Apps REST
resource, the dotAuth resource, plugins) follows it.

Code that reads an app's configuration at runtime on behalf of the platform (AI, analytics, SAML,
translation, Velocity secrets) passes `APILocator.systemUser()` and is not affected. Pass the
requesting user only from endpoints that are themselves limited to administrators.

## Legacy delegation switch

The feature flag `FEATURE_FLAG_APPS_ALLOW_PORTLET_CONFIGURATION` (declared in `FeatureFlagName`,
default `false`, environment variable `DOT_FEATURE_FLAG_APPS_ALLOW_PORTLET_CONFIGURATION`) lets
users with the Apps portlet change and read Apps configuration, upload descriptors and remove apps
as before. It is retained for one release for installations that delegated Apps
configuration and will then be removed.

## Log lines

When a user who is not an administrator is turned away (switch off), one WARN line per request:

```
Apps configuration for app '<appKey>' can only be changed by an administrator (user <userId>)
```

(`read` instead of `changed` for a read of values.) Configuration values are never logged.

When the switch allows a non-administrator, one WARN line per calling class:

```
Apps configuration change by a non-administrator allowed by FEATURE_FLAG_APPS_ALLOW_PORTLET_CONFIGURATION; this setting is deprecated and will be removed (caller <class>)
```
