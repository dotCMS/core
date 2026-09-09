import { HttpBackend, HttpXhrBackend, provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { appConfig } from '../app.config';

/**
 * Which HTTP backend this app runs on, asserted rather than assumed.
 *
 * **Why this test exists.** Angular 22 made `FetchBackend` the default: `withFetch` is deprecated
 * as "not required anymore". The fetch API **cannot report upload progress** — it has no equivalent
 * of `xhr.upload`, so `HttpEventType.UploadProgress` never fires and a `reportProgress: true`
 * request silently reports nothing on the way up. Content Drive's bulk upload shows bytes sent
 * against the declared total, so it needs the Xhr backend, which is what Angular's own guidance for
 * `withXhr` says it is for: *"Use this feature if you want to report progress on uploads as the Xhr
 * API supports it."*
 *
 * That default flipped underneath us on the version bump, and the symptom was invisible: no error,
 * no warning, an indicator that simply never showed a number. Nothing in the code said "XHR" for a
 * grep to find, and the absence of `withFetch` no longer means XHR — which is exactly the reasoning
 * that missed it. So the requirement gets a test rather than a comment.
 *
 * **`withXhr` is not deprecated; `withFetch` is.** Worth stating plainly, because the pairing
 * invites the opposite assumption. `withFetch` is tagged `@deprecated` ("not required anymore"),
 * while `withXhr` carries no deprecation at all — only a critical note scoped to **server-side
 * rendering**, where the Node `xhr2` shim is deprecated and due for removal in Angular 23 because
 * it mishandles redirects. dotAdmin is a browser SPA and is not server-rendered, so that note does
 * not apply, and `HttpXhrBackend` itself remains supported.
 *
 * If browser XHR is ever withdrawn too, the fallback is a raw `XMLHttpRequest` inside the upload
 * service, or dropping to an indeterminate indicator. This test is what would make that a decision
 * rather than a silent regression.
 */
describe('dotcms-ui HTTP backend', () => {
    it('should use the Xhr backend, because fetch cannot report upload progress', () => {
        // The app's own providers, not a stand-in: what matters is that *this* application is
        // configured that way, and a synthetic `provideHttpClient(withXhr())` would pass while the
        // real config had drifted back.
        TestBed.configureTestingModule({
            providers: appConfig.providers
        });

        expect(TestBed.inject(HttpBackend)).toBeInstanceOf(HttpXhrBackend);
    });

    it('should not be on the default backend, which reports nothing while a body uploads', () => {
        // The inverse, so the first assertion cannot pass by accident: without `withXhr` the
        // backend is something else entirely, and that is the state this app was silently left in.
        TestBed.configureTestingModule({
            providers: [provideHttpClient()]
        });

        expect(TestBed.inject(HttpBackend)).not.toBeInstanceOf(HttpXhrBackend);
    });
});
