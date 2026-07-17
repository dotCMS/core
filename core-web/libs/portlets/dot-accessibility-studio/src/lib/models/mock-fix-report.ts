import { FixReport } from './accessibility-studio.models';

/**
 * Sample §6 run report used as a test fixture across the store, run-component,
 * and presenter specs. Mirrors the prototype's fixes: 7 fixed to working, 5
 * reported/skipped — 12 → 5 violations.
 */
export const MOCK_FIX_REPORT: FixReport = {
    runId: 'r_mock_01J',
    page: {
        uri: '/travel/about-us',
        host: 'demo.dotcms.com',
        languageId: 1
    },
    scan: {
        before: { violations: 12 },
        after: { violations: 5 }
    },
    results: [
        {
            ruleId: 'image-alt',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/themes/travel/templates/travel-header.vtl',
            identifier: 'a56e1f00',
            diff: '+ alt="Aerial view of a turquoise coastline at sunset"',
            review: 'Added alt text to the hero image'
        },
        {
            ruleId: 'heading-order',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/themes/travel/templates/travel-header.vtl',
            identifier: 'a56e1f00',
            diff: '- <h3 class="hero-title">\n+ <h2 class="hero-title">',
            review: 'Fixed heading order (h3 → h2)'
        },
        {
            ruleId: 'button-name',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/themes/travel/templates/travel-header.vtl',
            identifier: 'a56e1f00',
            diff: '+ aria-label="Open navigation menu"',
            review: 'Named the menu toggle button'
        },
        {
            ruleId: 'html-has-lang',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/themes/travel/html-head.vtl',
            identifier: 'b1c2d3e4',
            diff: '- <html>\n+ <html lang="en">',
            review: 'Set lang="en" on <html>'
        },
        {
            ruleId: 'region',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/themes/travel/default-template.vtl',
            identifier: 'c3d4e5f6',
            diff: '+ <main id="main-content">\n...\n+ </main>',
            review: 'Wrapped content in a <main> landmark'
        },
        {
            ruleId: 'label',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/containers/newsletter/newsletter-container.vtl',
            identifier: 'd4e5f6a7',
            diff: '+ <label for="newsletter-email">Email address</label>',
            review: 'Added a label to the email input'
        },
        {
            ruleId: 'link-name',
            status: 'fixed-to-working',
            file: '//demo.dotcms.com/application/containers/cards/destination-card.vtl',
            identifier: 'e5f6a7b8',
            diff: '+ aria-label="Read more about $destination.title"',
            review: 'Gave "Read more" links accessible names'
        },
        {
            ruleId: 'color-contrast',
            status: 'reported',
            file: '//demo.dotcms.com/application/themes/travel/css/_variables-custom.scss',
            identifier: 'f6a7b8c9',
            blastRadius: 'token',
            review: 'Hero CTA contrast 3.1:1 — driven by the --brand-fg token',
            reason: 'Fixing the token changes the brand color site-wide; needs a design decision'
        },
        {
            ruleId: 'color-contrast',
            status: 'reported',
            file: '//demo.dotcms.com/application/themes/travel/css/styles.scss',
            identifier: 'a7b8c9d0',
            blastRadius: 'shared-rule',
            review: 'Footer link contrast 4.1:1 — adjust .footer a in styles.scss',
            reason: 'Borderline contrast; a small nudge affects all footer links'
        },
        {
            ruleId: 'color-contrast',
            status: 'reported',
            file: '//demo.dotcms.com/application/themes/travel/css/_nav.scss',
            identifier: 'b8c9d0e1',
            blastRadius: 'shared-rule',
            review: 'Nav links contrast borderline (4.4:1) — theme CSS variable',
            reason: 'Just under the 4.5:1 AA threshold; needs a deliberate color choice'
        },
        {
            ruleId: 'image-alt',
            status: 'skipped',
            reason: 'Alt text lives in a content field; out of v1 scope'
        },
        {
            ruleId: 'link-name',
            status: 'skipped',
            reason: 'Link text comes from a contentlet; out of v1 scope'
        }
    ],
    // Distinct files left changed (the fixed-to-working ones, deduped). The
    // reported .scss files were not modified, so they're excluded.
    changedFiles: [
        '//demo.dotcms.com/application/themes/travel/templates/travel-header.vtl',
        '//demo.dotcms.com/application/themes/travel/html-head.vtl',
        '//demo.dotcms.com/application/themes/travel/default-template.vtl',
        '//demo.dotcms.com/application/containers/newsletter/newsletter-container.vtl',
        '//demo.dotcms.com/application/containers/cards/destination-card.vtl'
    ],
    publishRequired: true
};
