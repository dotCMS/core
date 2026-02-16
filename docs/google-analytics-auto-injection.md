# Google Analytics Tracking Code Auto-Injection

## Overview

The Google Analytics auto-injection feature automatically injects GA4 or Universal Analytics tracking code into HTML pages when the `googleAnalytics` field is populated on a dotCMS site.

## How It Works

The `GoogleAnalyticsWebInterceptor` is a Web Interceptor that:

1. **Reads the tracking ID** from the site's `googleAnalytics` field
2. **Detects the format** (GA4 vs Universal Analytics) based on the tracking ID prefix
3. **Injects the appropriate tracking code** before the `</body>` tag in HTML responses
4. **Skips injection** in edit/preview modes to avoid tracking during content editing

## Configuration

### Enable/Disable Auto-Injection

Set the environment variable to control the feature:

```bash
# Enable auto-injection (default)
GOOGLE_ANALYTICS_AUTO_INJECT=true

# Disable auto-injection
GOOGLE_ANALYTICS_AUTO_INJECT=false
```

## Setting Up Google Analytics

### Via REST API

Use the Site Resource API to set the tracking ID:

```bash
# Set GA4 tracking ID
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"googleAnalytics": "G-XXXXXXXXXX"}' \
  "https://your-dotcms-instance.com/api/v1/sites/your-site-id"

# Set Universal Analytics tracking ID
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"googleAnalytics": "UA-XXXXXXXXX-1"}' \
  "https://your-dotcms-instance.com/api/v1/sites/your-site-id"
```

### Via dotCMS Admin UI

1. Navigate to **Sites** in the admin interface
2. Select your site
3. Find the **Google Analytics** field
4. Enter your tracking ID:
   - For GA4: `G-XXXXXXXXXX`
   - For UA: `UA-XXXXXXXXX-X`
5. Save the site configuration

## Supported Tracking ID Formats

### Google Analytics 4 (GA4)

Format: `G-XXXXXXXXXX`

The injected code will look like:

```html
<!-- Google tag (gtag.js) -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX');
</script>
```

### Universal Analytics (UA)

Format: `UA-XXXXXXXXX-X`

The injected code will look like:

```html
<!-- Google Analytics -->
<script>
(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');
ga('create', 'UA-XXXXXXXXX-X', 'auto');
ga('send', 'pageview');
</script>
```

## When Injection Occurs

The tracking code is injected **only** when all of the following conditions are met:

1. ✅ Auto-injection is enabled (`GOOGLE_ANALYTICS_AUTO_INJECT=true`)
2. ✅ The current page is in **LIVE** mode (not EDIT_MODE or PREVIEW_MODE)
3. ✅ The response content type is **text/html**
4. ✅ The `googleAnalytics` field is populated on the current site
5. ✅ The HTML contains a `</body>` closing tag

## Injection Location

The tracking code is injected **immediately before the closing `</body>` tag**, following Google's best practices for optimal page load performance.

Example:

```html
<!DOCTYPE html>
<html>
<head>
    <title>My Page</title>
</head>
<body>
    <h1>Content goes here</h1>
    
    <!-- Google Analytics tracking code injected here -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
    <script>
      window.dataLayer = window.dataLayer || [];
      function gtag(){dataLayer.push(arguments);}
      gtag('js', new Date());
      gtag('config', 'G-XXXXXXXXXX');
    </script>
</body>
</html>
```

## Privacy and GDPR Considerations

### Important Notes:

- **Auto-injection loads Google Analytics immediately** without checking for user consent
- For GDPR compliance, you may need to:
  1. Disable auto-injection: `GOOGLE_ANALYTICS_AUTO_INJECT=false`
  2. Implement your own consent management solution
  3. Manually inject GA tracking only after obtaining user consent

### Manual Implementation (with Consent)

If you need consent management, disable auto-injection and implement GA manually in your templates:

```velocity
## In your template
#if($site.googleAnalytics && $userHasConsented)
<script async src="https://www.googletagmanager.com/gtag/js?id=$site.googleAnalytics"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', '$site.googleAnalytics');
</script>
#end
```

## Troubleshooting

### Tracking Code Not Appearing

1. **Check the site configuration**: Verify the `googleAnalytics` field is set
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
     "https://your-dotcms-instance.com/api/v1/sites/your-site-id"
   ```

2. **Verify auto-injection is enabled**: Check environment variable
   ```bash
   echo $GOOGLE_ANALYTICS_AUTO_INJECT
   ```

3. **Check page mode**: Ensure you're viewing in LIVE mode (not logged into the admin)

4. **Inspect HTML source**: View page source and search for `gtag.js` or `analytics.js`

5. **Check server logs**: Look for debug messages from `GoogleAnalyticsWebInterceptor`

### Tracking Code Injected Multiple Times

- Check that you haven't manually added GA tracking code to your templates
- Only one tracking code should be present per page

## Architecture

### Implementation Details

- **Class**: `com.dotcms.analytics.GoogleAnalyticsWebInterceptor`
- **Pattern**: WebInterceptor with HttpServletResponseWrapper
- **Registration**: Automatically registered in `InterceptorFilter`
- **Execution Order**: Runs before the `AnalyticsTrackWebInterceptor`

### Code Flow

1. Request arrives → `intercept()` called
2. Check configuration, page mode, and tracking ID
3. If conditions met, wrap response with `GAResponseWrapper`
4. Page renders normally (HTML captured in wrapper)
5. After rendering → `afterIntercept()` called
6. Response wrapper injects tracking code before `</body>`
7. Modified HTML sent to browser

## Examples

### Example 1: Simple Site Setup

```bash
# Set GA4 tracking for your site
export GOOGLE_ANALYTICS_AUTO_INJECT=true
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"googleAnalytics": "G-ABC123DEF4"}' \
  "https://dotcms.example.com/api/v1/sites/default"
```

Visit your site in a browser → tracking code automatically injected!

### Example 2: Disable for Development

```bash
# Disable auto-injection for local development
export GOOGLE_ANALYTICS_AUTO_INJECT=false
```

### Example 3: Multiple Sites

Each site can have its own tracking ID:

```bash
# Site 1 with GA4
curl -X PUT -H "Content-Type: application/json" \
  -d '{"googleAnalytics": "G-SITE1TRACK"}' \
  "https://dotcms.example.com/api/v1/sites/site1"

# Site 2 with UA
curl -X PUT -H "Content-Type: application/json" \
  -d '{"googleAnalytics": "UA-12345678-1"}' \
  "https://dotcms.example.com/api/v1/sites/site2"
```

## Migration from Manual Implementation

If you were previously manually adding GA tracking code:

1. **Remove manual GA code** from templates
2. **Set the tracking ID** via the `googleAnalytics` field
3. **Enable auto-injection**: `GOOGLE_ANALYTICS_AUTO_INJECT=true`
4. **Test** to ensure tracking works correctly

## Related Documentation

- [Google Analytics 4 Documentation](https://developers.google.com/analytics/devguides/collection/ga4)
- [Universal Analytics Documentation](https://developers.google.com/analytics/devguides/collection/analyticsjs)
- [dotCMS Site API](https://www.dotcms.com/docs/latest/site-resource-api)

## Support

For issues or questions:
- Check the dotCMS logs: Look for `GoogleAnalyticsWebInterceptor` messages
- Verify your tracking ID is valid in Google Analytics
- Ensure your site is in LIVE mode when testing
