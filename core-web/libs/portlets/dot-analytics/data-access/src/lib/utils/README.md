# Analytics Utils (Internal)

⚠️ **These utilities are internal to the data-access library and should not be imported directly from outside.**

Utilities organized by category for better maintainability and discoverability.

## 📁 Structure

### `cube/` - CubeJS Query Building

-   `cube-query-builder.util.ts` - Fluent API for building CubeJS queries
-   `cube-query-builder.util.spec.ts` - Unit tests for query builder

### `browser/` - Browser & Device Detection

-   `userAgentParser.ts` - Comprehensive user agent parsing
    -   Browser detection (Chrome, Safari, Firefox, Edge, etc.)
    -   Device type detection (mobile, tablet, desktop)
    -   OS detection (iOS, Android, Windows, macOS, etc.)

### `data/` - Analytics Data Transformation

-   `analytics-data.utils.ts` - Data transformation utilities
    -   Chart data formatting for Chart.js
    -   Table data transformation
    -   Metric extraction helpers

## 🔄 Internal Usage

These utilities are used internally by the data-access library:

```typescript
// Internal imports within data-access lib
import { createCubeQuery } from '../utils/cube/cube-query-builder.util';
import { parseUserAgent } from '../utils/browser/userAgentParser';
import {
  transformPageViewTimeLineData,
  transformDeviceBrowsersData,
  extractPageViews
} from '../utils/data/analytics-data.utils';
```

**Public API:** Only stores, services, and types are exported from the main index.
