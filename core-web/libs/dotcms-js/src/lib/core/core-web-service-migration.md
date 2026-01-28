# CoreWebService Migration Plan

## Overview

The `CoreWebService` is deprecated and should be replaced with Angular's `CoreWebService` directly. This document outlines a step-by-step migration plan for a Cursor agent to systematically migrate all usages across the monorepo.

## Current State Analysis

### CoreWebService Business Logic

The `CoreWebService` contains the following business logic that must be analyzed during migration:

1. **URL Normalization** (`getFixedUrl`) - **IMPORTANT: Must be handled during migration**:
   - If URL starts with `api`, prepends `/` → Result: `/api/...`
   - If URL starts with `v[1-9]`, prepends `/api/` → Result: `/api/v1/...`
   - Example: `v1/users/current/` → `/api/v1/users/current/`
   - Example: `api/content/...` → `/api/content/...`

2. **Default Headers** (`getDefaultRequestHeaders`):
   - `Accept: */*`
   - `Content-Type: application/json`
   - Special handling for `multipart/form-data` (removes Content-Type to let browser set boundary)

3. **Error Handling**:
   - Redirects to `/public/login` on 401 Unauthorized
   - Emits HTTP errors via subjects for subscription (`subscribeToHttpError`)
   - Custom error types: `CwError`, `NETWORK_CONNECTION_ERROR`, `SERVER_RESPONSE_ERROR`

4. **Response Wrapper**:
   - `requestView` returns `ResponseView<T>` wrapper that provides:
     - Access to `entity`, `contentlets`, `tempFiles`
     - Header access via `header()` method
     - `i18nMessagesMap` access
     - `errorsMessages` getter
     - `existError(errorCode)` method

### Files Using CoreWebService

There are **189 files** using `CoreWebService` in the monorepo:

- **~15 services** in `libs/data-access/`
- **~10 services** in `libs/dotcms-js/`
- **~15 services** in `apps/dotcms-ui/`
- **~5 services** in `libs/dot-rules/`
- **~144 test files** (`.spec.ts`)

### Existing Infrastructure

The codebase already has:

1. **`ServerErrorInterceptor`** (`apps/dotcms-ui/src/app/shared/interceptors/server-error.interceptor.ts`):
   - Catches all HTTP errors and delegates to `DotHttpErrorManagerService`

2. **`DotHttpErrorManagerService`** (`libs/data-access/src/lib/dot-http-error-manager/`):
   - Handles 401 → redirects to login (if no user)
   - Handles 403, 404, 500, 400, 204 with user-friendly messages
   - Supports localized error messages

3. **`CoreWebServiceMock`** (`libs/utils-testing/src/lib/core-web.service.mock.ts`):
   - Used in all tests, simplifies mock behavior

---

## URL Normalization Rules

**CRITICAL**: During migration, you MUST analyze each URL and convert it to the correct format. Do NOT use any utility function - the URL should be explicit and correct from the start.

### Conversion Rules

| Current URL Pattern | Correct URL Format | Example |
|---------------------|-------------------|---------|
| `v1/...` | `/api/v1/...` | `v1/users/current/` → `/api/v1/users/current/` |
| `v2/...` | `/api/v2/...` | `v2/pages/render` → `/api/v2/pages/render` |
| `api/...` | `/api/...` | `api/content/query` → `/api/content/query` |
| `/api/v1/...` | `/api/v1/...` | Already correct, no change needed |

### Examples of URL Corrections

```typescript
// BEFORE (incorrect - missing prefix)
url: 'v1/users/current/'

// AFTER (correct - explicit full path)
'/api/v1/users/current/'
```

```typescript
// BEFORE (incorrect - missing leading slash)
url: 'api/content/respectFrontendRoles/false/render/false/query/...'

// AFTER (correct - with leading slash)
'/api/content/respectFrontendRoles/false/render/false/query/...'
```

```typescript
// BEFORE (already has /api/ prefix)
url: '/api/v1/toolgroups/gettingstarted/_addtouser'

// AFTER (no change needed)
'/api/v1/toolgroups/gettingstarted/_addtouser'
```

---

## Migration Strategy

### Phase 1: Service Migration Patterns

For each service using `CoreWebService`, apply the appropriate migration pattern:

#### Pattern A: Simple GET Request

**Before:**
```typescript
@Injectable()
export class DotCurrentUserService {
  private coreWebService = inject(CoreWebService);
  private currentUsersUrl = 'v1/users/current/';

  getCurrentUser(): Observable<DotCurrentUser> {
    return this.coreWebService
      .request<DotCurrentUser>({
        url: this.currentUsersUrl
      })
      .pipe(map((res: DotCurrentUser) => res));
  }
}
```

**After:**
```typescript
import { HttpClient } from '@angular/common/http';

@Injectable()
export class DotCurrentUserService {
  private http = inject(HttpClient);
  private currentUsersUrl = '/api/v1/users/current/';  // ✅ Corrected URL

  getCurrentUser(): Observable<DotCurrentUser> {
    return this.http.get<DotCurrentUser>(this.currentUsersUrl);
  }
}
```

#### Pattern B: Request with Body (POST/PUT/PATCH/DELETE)

**Before:**
```typescript
loginUser(params: DotLoginParams): Observable<User> {
  return this.coreWebService
    .requestView<User>({
      body: { userId: login, password, ... },
      method: 'POST',
      url: 'v1/authentication'
    })
    .pipe(pluck('entity'));
}
```

**After:**
```typescript
import { HttpClient } from '@angular/common/http';
import { DotCMSResponse } from '@dotcms/dotcms-models';

loginUser(params: DotLoginParams): Observable<User> {
  return this.http
    .post<DotCMSResponse<User>>('/api/v1/authentication', {  // ✅ Corrected URL
      userId: login,
      password,
      ...
    })
    .pipe(map((res) => res.entity));
}
```

#### Pattern C: Request Needing Headers (Pagination)

**Before:**
```typescript
get<T>(url?: string): Observable<T> {
  return this.coreWebService
    .requestView({
      params,
      url: cleanURL || this.url
    })
    .pipe(
      map((response: ResponseView<T>) => {
        this.paginationPerPage = parseInt(
          response.header(PaginatorService.PAGINATION_PER_PAGE_HEADER_NAME),
          10
        );
        return response.entity;
      })
    );
}
```

**After:**
```typescript
import { HttpClient, HttpResponse } from '@angular/common/http';
import { DotCMSResponse } from '@dotcms/dotcms-models';

get<T>(url?: string): Observable<T> {
  // Note: Ensure url is already in correct format when passed
  const requestUrl = cleanURL || this.url;

  return this.http
    .get<DotCMSResponse<T>>(requestUrl, {
      params,
      observe: 'response'
    })
    .pipe(
      map((response: HttpResponse<DotCMSResponse<T>>) => {
        this.paginationPerPage = parseInt(
          response.headers.get(PaginatorService.PAGINATION_PER_PAGE_HEADER_NAME),
          10
        );
        return response.body.entity;
      })
    );
}
```

#### Pattern D: subscribeToHttpError (Only in LoginService)

**Before:**
```typescript
this.coreWebService.subscribeToHttpError(HttpCode.UNAUTHORIZED).subscribe(() => {
  this.logOutUser();
});
```

**After:**

This pattern is only used in `LoginService`. Since `DotHttpErrorManagerService` already handles 401 errors and redirects to login, this subscription can be removed. The `ServerErrorInterceptor` already catches all HTTP errors.

### Phase 2: Use Existing DotCMSResponse Interface

Use the `DotCMSResponse` interface that already exists in `@dotcms/dotcms-models`:

**Location:** `libs/dotcms-models/src/lib/dot-request-response.model.ts`

```typescript
import { DotCMSResponse } from '@dotcms/dotcms-models';
```

This interface is already defined as:

```typescript
export interface DotCMSResponse<T = unknown> {
  entity: T;
  errors: string[];
  i18nMessagesMap: Record<string, unknown>;
  messages: string[];
  pagination: unknown;
  permissions: string[];
}
```

**Note:** Some endpoints return `contentlets` or `tempFiles` instead of `entity`. For these cases, create a specific response type:

```typescript
// For endpoints that return contentlets (like content search)
interface DotContentSearchResponse<T> {
  contentlets: T;
}

// For temp file endpoints
interface DotTempFileResponse<T> {
  tempFiles: T;
}
```

---

## Migration Order (Priority)

### Step 1: High Priority - Core Services
Migrate these first as they are foundational:

1. `libs/dotcms-js/src/lib/core/login.service.ts`
2. `libs/dotcms-js/src/lib/core/site.service.ts`
3. `libs/dotcms-js/src/lib/core/dotcms-config.service.ts`

### Step 2: Data Access Layer
Migrate all services in `libs/data-access/`:

1. `dot-current-user.service.ts`
2. `dot-devices.service.ts`
3. `dot-personas.service.ts`
4. `dot-themes.service.ts`
5. `dot-page-render.service.ts`
6. `dot-page-layout.service.ts`
7. `dot-edit-page.service.ts`
8. `dot-contentlet-locker.service.ts`
9. `dot-crud.service.ts`
10. `add-to-bundle.service.ts`
11. `push-publish.service.ts`
12. `paginator.service.ts` (needs header access)
13. `dot-temp-file-upload.service.ts`
14. `dot-push-publish-filters.service.ts`
15. `dot-personalize.service.ts`

### Step 3: dotcms-ui Services
Migrate services in `apps/dotcms-ui/src/app/api/services/`:

1. `dot-account-service.ts`
2. `notifications-service.ts`
3. `dot-templates/dot-templates.service.ts`
4. `dot-containers/dot-containers.service.ts`
5. `dot-apps/dot-apps.service.ts`
6. `add-to-menu/add-to-menu.service.ts`

### Step 4: Other Services
1. `apps/dotcms-ui/.../dot-contentlet-editor.service.ts`
2. `apps/dotcms-ui/.../dot-page-selector.service.ts`
3. `apps/dotcms-ui/.../field.service.ts`
4. `apps/dotcms-ui/.../dot-field-variables.service.ts`
5. `apps/dotcms-ui/.../dot-relationship.service.ts`
6. `apps/dotcms-ui/.../dot-container-contentlet.service.ts`

### Step 5: Rules Module
1. `libs/dot-rules/src/lib/services/Rule.ts`
2. `libs/dot-rules/src/lib/components/restdropdown/RestDropdown.ts`

### Step 6: dotcdn
1. `apps/dotcdn/src/app/dotcdn.service.ts`

---

## Test Migration

### Current Test Pattern (DEPRECATED)
Tests currently use `CoreWebServiceMock` and the deprecated `HttpClientTestingModule`:

```typescript
// ❌ OLD - Deprecated pattern
TestBed.configureTestingModule({
  imports: [HttpClientTestingModule],  // ❌ Deprecated
  providers: [
    { provide: CoreWebService, useClass: CoreWebServiceMock },
    MyService
  ]
});
```

### New Test Pattern with provideHttpClient

Use `provideHttpClient()` and `provideHttpClientTesting()` instead:

> ⚠️ **IMPORTANT**: `provideHttpClient()` must come **before** `provideHttpClientTesting()`, as `provideHttpClientTesting()` will overwrite parts of `provideHttpClient()`.

```typescript
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

describe('MyService', () => {
  let service: MyService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        MyService
      ]
    });

    service = TestBed.inject(MyService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should get data', () => {
    service.getData().subscribe((data) => {
      expect(data).toEqual(mockData);
    });

    const req = httpTesting.expectOne('/api/v1/endpoint');  // ✅ Use correct URL
    expect(req.request.method).toBe('GET');
    req.flush({ entity: mockData });
  });
});
```

### New Test Pattern with Spectator (Preferred)

For services, use Spectator's `createHttpFactory` which simplifies HTTP testing:

```typescript
import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';
import { MyService } from './my.service';

describe('MyService', () => {
  let spectator: SpectatorHttp<MyService>;
  const createHttp = createHttpFactory(MyService);

  beforeEach(() => spectator = createHttp());

  it('should get data', () => {
    spectator.service.getData().subscribe();
    spectator.expectOne('/api/v1/endpoint', HttpMethod.GET);
  });

  it('should post data', () => {
    spectator.service.postData({ name: 'test' }).subscribe();

    const req = spectator.expectOne('/api/v1/endpoint', HttpMethod.POST);
    expect(req.request.body).toEqual({ name: 'test' });
  });

  it('should handle concurrent requests', () => {
    spectator.service.loadMultiple().subscribe();

    const reqs = spectator.expectConcurrent([
      { url: '/api/v1/resource1', method: HttpMethod.GET },
      { url: '/api/v1/resource2', method: HttpMethod.GET }
    ]);

    spectator.flushAll(reqs, [{ entity: data1 }, { entity: data2 }]);
  });
});
```

### Migration Checklist for Tests

For each `.spec.ts` file:

- [ ] Remove `HttpClientTestingModule` import
- [ ] Remove `CoreWebService` and `CoreWebServiceMock` from providers
- [ ] Choose testing approach:
  - **Option A**: Use `provideHttpClient()` + `provideHttpClientTesting()`
  - **Option B**: Use Spectator's `createHttpFactory` (preferred for service tests)
- [ ] Update URL expectations to use corrected URLs (e.g., `/api/v1/...`)

---

## Checklist for Each Service Migration

For each service, the Cursor agent should:

- [ ] 1. Read the current service file
- [ ] 2. Identify all `coreWebService.request()` and `coreWebService.requestView()` calls
- [ ] 3. **Analyze each URL and correct it**:
   - If URL starts with `v1/`, `v2/`, etc. → add `/api/` prefix
   - If URL starts with `api/` → add `/` prefix
   - If URL already starts with `/api/` → no change needed
- [ ] 4. Replace `CoreWebService` injection with `HttpClient`
- [ ] 5. Convert each method:
   - Replace `request()` with appropriate `http.get/post/put/delete()`
   - Replace `requestView().pipe(pluck('entity'))` with `http.method().pipe(map(res => res.entity))`
   - Add `observe: 'response'` if headers are needed
- [ ] 6. Remove `CoreWebService` import
- [ ] 7. Update the corresponding `.spec.ts` file:
   - Remove `HttpClientTestingModule` (deprecated)
   - Remove `CoreWebService` and `CoreWebServiceMock` from providers
   - Use `provideHttpClient()` + `provideHttpClientTesting()` OR Spectator's `createHttpFactory`
   - Update URL expectations to use the corrected URL format (e.g., `/api/v1/...`)
- [ ] 8. Run `yarn nx run <project>:lint` to check for errors
- [ ] 9. Run `yarn nx run <project>:test` to verify tests pass

---

## Deprecation Timeline

1. **Phase 1** (Current): Mark as `@deprecated`
2. **Phase 2**: Migrate all services in `libs/data-access/`
3. **Phase 3**: Migrate remaining services
4. **Phase 4**: Remove `CoreWebService`, `ResponseView`, and `CoreWebServiceMock`

---

## Commands for Agent

### Find all usages
```bash
grep -r "CoreWebService" --include="*.ts" libs apps | grep -v ".spec.ts" | grep -v "node_modules"
```

### Run tests for affected projects
```bash
yarn nx affected -t test
```

### Lint affected projects
```bash
yarn nx affected -t lint
```

---

## Notes

1. **Do NOT remove `CoreWebService` until all migrations are complete**
2. The `ServerErrorInterceptor` already handles global error management
3. `DotHttpErrorManagerService` handles 401 redirects, so `subscribeToHttpError` pattern is largely unnecessary
4. **Use `DotCMSResponse` from `@dotcms/dotcms-models`** - Do NOT use the one from `CoreWebService`
5. For endpoints returning `contentlets` or `tempFiles`, create specific response interfaces
6. Some services may need the full response (with headers) - use `observe: 'response'` in those cases
7. Watch for services that access `i18nMessagesMap` or `errorsMessages` from `ResponseView`
8. **URLs must be explicit and correct** - no utility functions for normalization

---

## Example Full Migration

### Before: `dot-devices.service.ts`

```typescript
import { Observable } from 'rxjs';
import { Injectable, inject } from '@angular/core';
import { pluck } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotDevice } from '@dotcms/dotcms-models';

@Injectable()
export class DotDevicesService {
  private coreWebService = inject(CoreWebService);

  get(): Observable<DotDevice[]> {
    return this.coreWebService
      .requestView({
        url: [
          'api',                    // ❌ Missing leading slash
          'content',
          'respectFrontendRoles/false',
          'render/false',
          'query/+contentType:previewDevice +live:true +deleted:false +working:true',
          'limit/40/orderby/title'
        ].join('/')
      })
      .pipe(pluck('contentlets'));
  }
}
```

### After: `dot-devices.service.ts`

```typescript
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { DotDevice } from '@dotcms/dotcms-models';

// Response type for content search endpoints that return contentlets
interface DotContentSearchResponse<T> {
  contentlets: T;
}

@Injectable()
export class DotDevicesService {
  private http = inject(HttpClient);

  get(): Observable<DotDevice[]> {
    const url = [
      '/api',                    // ✅ Corrected with leading slash
      'content',
      'respectFrontendRoles/false',
      'render/false',
      'query/+contentType:previewDevice +live:true +deleted:false +working:true',
      'limit/40/orderby/title'
    ].join('/');

    return this.http
      .get<DotContentSearchResponse<DotDevice[]>>(url)
      .pipe(map((response) => response.contentlets));
  }
}
```

### Before: `dot-current-user.service.ts`

```typescript
@Injectable()
export class DotCurrentUserService {
  private coreWebService = inject(CoreWebService);
  private currentUsersUrl = 'v1/users/current/';  // ❌ Missing /api/ prefix

  getCurrentUser(): Observable<DotCurrentUser> {
    return this.coreWebService
      .request<DotCurrentUser>({
        url: this.currentUsersUrl
      })
      .pipe(map((res: DotCurrentUser) => res));
  }
}
```

### After: `dot-current-user.service.ts`

```typescript
import { HttpClient } from '@angular/common/http';

@Injectable()
export class DotCurrentUserService {
  private http = inject(HttpClient);
  private currentUsersUrl = '/api/v1/users/current/';  // ✅ Corrected URL

  getCurrentUser(): Observable<DotCurrentUser> {
    return this.http.get<DotCurrentUser>(this.currentUsersUrl);
  }
}
```

