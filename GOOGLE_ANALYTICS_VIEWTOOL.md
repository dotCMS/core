# Google Analytics 4 (GA4) ViewTool - Usage Guide

## Overview

The `$googleAnalytics` ViewTool provides a simple way to include Google Analytics 4 tracking code in your Velocity templates. This replaces the previous auto-injection approach with explicit, developer-controlled placement.

## Configuration

Set the GA4 tracking ID on your site via the dotCMS REST API or admin UI:

### Via REST API
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"googleAnalytics": "G-XXXXXXXXXX"}' \
  "https://your-dotcms-instance.com/api/v1/sites/your-site-id"
```

### Via Admin UI
1. Navigate to **System** → **Sites**
2. Select your site
3. Find the **Google Analytics** field
4. Enter your GA4 tracking ID (format: `G-XXXXXXXXXX`)
5. Save

## Velocity Template Usage

### Basic Usage

Place the tracking code in your template (typically before the closing `</body>` tag):

```velocity
<!DOCTYPE html>
<html>
<head>
    <title>$title</title>
</head>
<body>
    <h1>Welcome to my site</h1>
    
    ## Your page content here
    
    ## Include GA4 tracking code
    $googleAnalytics.trackingCode
</body>
</html>
```

### With Null Check

To avoid rendering anything when no tracking ID is configured:

```velocity
#if($googleAnalytics.trackingId)
    $googleAnalytics.trackingCode
#end
```

### Conditional Based on User Consent

Include tracking only when user has given consent:

```velocity
#if($userConsent && $googleAnalytics.trackingId)
    $googleAnalytics.trackingCode
#end
```

### Getting Just the Tracking ID

If you need just the tracking ID for custom implementation:

```velocity
#set($gaId = $googleAnalytics.trackingId)
#if($gaId)
    <!-- My custom GA implementation -->
    <script>
        console.log("GA Tracking ID: $gaId");
    </script>
#end
```

## API Reference

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getTrackingId()` | String | Returns the GA4 tracking ID (e.g., "G-XXXXXXXXXX") or `null` if not set |
| `getTrackingCode()` | String | Returns the complete GA4 tracking script HTML, or empty string if not set |

### Generated Output

When a tracking ID like `G-ABC123XYZ` is configured, `$googleAnalytics.trackingCode` generates:

```html
<!-- Google tag (gtag.js) -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-ABC123XYZ"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-ABC123XYZ');
</script>
```

## Benefits

✅ **Full Control** - You decide exactly where the tracking code appears  
✅ **Consent Management** - Easy to conditionally include based on user consent  
✅ **No Performance Overhead** - No automatic HTML parsing or response wrapping  
✅ **Transparency** - Clear what's happening, easier to debug  
✅ **Flexibility** - Can customize placement, add conditions, or modify output  

## Common Patterns

### Pattern 1: Include in Layout Template

```velocity
## layout.vtl
<!DOCTYPE html>
<html>
<head>
    #parse($template.head)
</head>
<body>
    #parse($template.body)
    
    ## Analytics at the end of body
    $googleAnalytics.trackingCode
</body>
</html>
```

### Pattern 2: Conditional for Production Only

```velocity
#if($request.serverName.contains("production.com"))
    $googleAnalytics.trackingCode
#end
```

### Pattern 3: With Cookie Consent

```velocity
#if($cookietool.get("analytics_consent").value == "true")
    $googleAnalytics.trackingCode
#end
```

### Pattern 4: Debug Mode

```velocity
#if($googleAnalytics.trackingId)
    #if($request.getParameter("debug"))
        <!-- GA Tracking ID: $googleAnalytics.trackingId -->
    #end
    $googleAnalytics.trackingCode
#end
```

## Troubleshooting

### Tracking code not appearing?

1. **Check the tracking ID is set** on your site:
   ```velocity
   Tracking ID: $googleAnalytics.trackingId
   ```

2. **Verify the template includes the tool**:
   ```velocity
   #if($googleAnalytics)
       Tool is available
   #else
       Tool is NOT available
   #end
   ```

3. **Check for syntax errors** in your Velocity template

4. **View page source** to see if the code was rendered

### Tracking not working in Google Analytics?

1. Verify the tracking ID format is correct (`G-XXXXXXXXXX`)
2. Check your browser's Network tab for requests to `gtag.js`
3. Use Google Analytics DebugView or Tag Assistant
4. Ensure tracking ID exists in your GA4 property

## Migration from Auto-Inject

If you were using the previous auto-injection approach:

1. **Add the ViewTool to your templates**:
   - Add `$googleAnalytics.trackingCode` before `</body>` in your layout templates
   - Or add to individual page templates as needed

2. **Remove environment variable**:
   - `GOOGLE_ANALYTICS_AUTO_INJECT` is no longer used
   - Can be removed from your configuration

3. **No data migration needed**:
   - Tracking ID remains in the site's `googleAnalytics` field
   - No changes to site configuration required

## Privacy & GDPR

When using Google Analytics, consider:

- **Consent Management**: Use conditional rendering based on user consent
- **Cookie Notice**: Inform users about analytics cookies
- **Data Processing Agreement**: Ensure you have one with Google
- **Privacy Policy**: Update to mention Google Analytics usage

Example with consent:
```velocity
#if($cookieConsent.hasAnalyticsConsent())
    $googleAnalytics.trackingCode
#else
    <!-- Analytics tracking disabled - no consent -->
#end
```

## Support

For more information:
- [Google Analytics 4 Documentation](https://support.google.com/analytics/answer/9304153)
- [dotCMS ViewTools Documentation](https://www.dotcms.com/docs/latest/velocity-tools)
- [dotCMS Site API](https://www.dotcms.com/docs/latest/site-resource-api)
