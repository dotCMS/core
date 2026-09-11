/**
 * Shared helpers for /qa recorded verification runs.
 *
 * Import from a throwaway script created in core-web/ and INJECT chromium from there —
 * bare specifiers resolve from this file's location, which has no node_modules, so the
 * throwaway script must own the playwright-core import:
 *
 *   import { chromium } from 'playwright-core';
 *   import { launchRecorded, openLogin, login, navigateMenu, introCard, showCursor,
 *            criteriaPanel, humanClick, banner, visible, visibleArea, waitFor,
 *            makeChecker, finalize, PACE } from '../.claude/skills/qa/scripts/qa-helpers.mjs';
 *   const { browser, context, page } = await launchRecorded(chromium, VIDEO_DIR);
 *
 * Uses page.type (portable across older playwright-core versions that lack pressSequentially).
 */
export const BASE = process.env.DOTCMS_URL || 'https://localhost:8443';

/** Pacing for a watchable, human-speed recording (ms). */
export const PACE = { beat: 1200, intro: 5000 };

/**
 * Launch a recorded 1280x720 session. Pass in chromium from playwright-core.
 * slowMo paces every Playwright action so the video reads like manual QA —
 * keep it 100–200; it slows the recording, it does not change what is asserted.
 */
export async function launchRecorded(chromium, videoDir, { headless = true, slowMo = 150 } = {}) {
    const browser = await chromium.launch({ headless, slowMo });
    const context = await browser.newContext({
        ignoreHTTPSErrors: true,
        viewport: { width: 1280, height: 720 },
        recordVideo: { dir: videoDir, size: { width: 1280, height: 720 } }
    });
    const page = await context.newPage();
    return { browser, context, page };
}

/** Open the login screen and wait for it to render — the canvas for introCard(). */
export async function openLogin(page) {
    await page.goto(`${BASE}/dotAdmin/`, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('[data-testid="userNameInput"]', { timeout: 30000 });
}

/**
 * Log into dotAdmin the way a human does, narrated: banner + visible cursor from the very
 * first frame, typed credentials, cursor travels to Sign In. Call openLogin() + introCard()
 * BEFORE this (card over the login screen) so the viewer has context before any activity.
 */
export async function login(page, user = 'admin@dotcms.com', pass = 'admin') {
    if (!page.url().startsWith(BASE)) {
        await page.goto(`${BASE}/dotAdmin/`, { waitUntil: 'domcontentloaded' });
    }
    await page.waitForSelector('[data-testid="userNameInput"]', { timeout: 30000 });
    await showCursor(page);
    await banner(page, `Logging in as ${user}`);
    await page.waitForTimeout(1000); // let Angular wire up the form
    await humanClick(page, '[data-testid="userNameInput"]');
    await page.type('[data-testid="userNameInput"]', user, { delay: 20 });
    await humanClick(page, '[data-testid="password"]');
    await page.type('[data-testid="password"]', pass, { delay: 20 });
    await page.waitForSelector('[data-testid="submitButton"]:not([disabled])', { timeout: 15000 });
    await humanClick(page, '[data-testid="submitButton"]');
    await page.waitForURL(/dotAdmin\/#\//, { timeout: 30000 });
    await suppressToasts(page);
}

/**
 * Navigate to a portlet through the left menu, like a user: click the menu group by its
 * visible label, then the submenu entry by its label. Labels are matched case-insensitively
 * on trimmed text. Throws with the list of AVAILABLE labels when a label isn't found, so a
 * failed run tells you what to call instead.
 * The dotAdmin shell is an SPA — overlays (banner/cursor/checklist) survive this navigation.
 */
export async function navigateMenu(page, groupLabel, itemLabel) {
    await banner(page, `Navigating: ${groupLabel} → ${itemLabel}`);
    await page.waitForSelector('[data-testid="nav-item-main"]', { timeout: 30000 });

    // Expand the sidebar if collapsed — a collapsed menu only shows submenus as hover
    // flyouts that close while the cursor travels to them; humans (and this helper)
    // work with the expanded menu and its inline submenus.
    const collapsed = await page.evaluate(() => !!document.querySelector('nav.collapsed'));
    if (collapsed) {
        await humanClick(page, '[data-testid="dot-nav-header-toggle-button"]');
        await page.waitForTimeout(500); // expand animation
    }

    // A link only counts as visible if a click at its center would actually LAND on it
    // (elementFromPoint) — collapsed submenus keep their links in the DOM with nonzero
    // rects (hidden by overflow), so geometry alone lies.
    const findLink = (label) => page.evaluate((label) => {
        const links = [...document.querySelectorAll('a.dot-nav-sub__link')].filter((a) => {
            const r = a.getBoundingClientRect();
            if (r.width < 2 || r.height < 2) return false;
            const hit = document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2);
            return !!hit && (a.contains(hit) || hit.contains(a));
        });
        const labels = links.map((a) => a.textContent.trim());
        const i = labels.findIndex((l) => l.toLowerCase() === label.toLowerCase());
        if (i === -1) return { labels };
        const r = links[i].getBoundingClientRect();
        return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
    }, label);

    // Open the group only if the target link isn't already visible (clicking an open
    // group would close it).
    let item = await findLink(itemLabel);
    if (item.labels) {
        const group = await page.evaluate((label) => {
            const mains = [...document.querySelectorAll('[data-testid="nav-item-main"]')];
            const labels = mains.map((m) =>
                m.querySelector('[data-testid="nav-item-label"]')?.textContent.trim() || '');
            const i = labels.findIndex((l) => l.toLowerCase() === label.toLowerCase());
            if (i === -1) return { labels };
            const r = mains[i].getBoundingClientRect();
            return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
        }, groupLabel);
        if (group.labels) throw new Error(
            `navigateMenu: group "${groupLabel}" not found. Available groups: ${group.labels.join(' | ')}`);
        await page.mouse.move(group.x, group.y, { steps: 12 });
        await page.waitForTimeout(350);
        await page.mouse.click(group.x, group.y, { delay: 60 });
        const opened = await waitFor(async () => !(await findLink(itemLabel)).labels, 5000);
        item = await findLink(itemLabel);
        if (!opened || item.labels) throw new Error(
            `navigateMenu: item "${itemLabel}" not visible under "${groupLabel}". Visible items: ${(item.labels || []).join(' | ')}`);
    }
    const urlBefore = page.url();
    await page.mouse.move(item.x, item.y, { steps: 10 });
    await page.waitForTimeout(350);
    await page.mouse.click(item.x, item.y, { delay: 60 });
    // Trust requires proof the navigation actually happened — a helper that returns after
    // a click that landed nowhere turns every downstream assertion into noise.
    const navigated = await waitFor(() => page.url() !== urlBefore, 10000);
    if (!navigated) throw new Error(
        `navigateMenu: clicked "${itemLabel}" but the route never changed (still ${urlBefore})`);
    await suppressToasts(page);
}

/**
 * Navigate to a dotAdmin hash route DIRECTLY — a teleport, not a human flow.
 * Last resort for surfaces with no menu entry; the banner discloses it on camera.
 * This is a full page load: re-apply showCursor/criteriaPanel/suppressToasts after it.
 */
export async function gotoHash(page, route) {
    await banner(page, `Opening ${route} directly (no menu entry for this screen)`).catch(() => {});
    await page.goto(`${BASE}/dotAdmin/#${route}`, { waitUntil: 'domcontentloaded' });
}

/** Close PrimeNG toasts that overlap toolbar buttons (they reappear after navigation). */
export async function dismissToasts(page) {
    for (let i = 0; i < 5; i++) {
        const t = await page.$('.p-toast-icon-close, .p-toast-message-icon-close');
        if (!t) break;
        await t.click().catch(() => {});
        await page.waitForTimeout(300);
    }
}

/**
 * Keep system toasts (e.g. "API Tokens about to expire") off camera for the whole session
 * via a MutationObserver that closes them as they appear. Cosmetic cleanup only — it
 * touches nothing under verification. Dies on a full page load; login()/navigateMenu()
 * re-apply it, re-call it yourself after gotoHash().
 */
export async function suppressToasts(page) {
    await page.evaluate(() => {
        if (window.__qaToastKiller) return;
        const kill = () => document
            .querySelectorAll('.p-toast-icon-close, .p-toast-message-icon-close')
            .forEach((b) => b.click());
        kill();
        window.__qaToastKiller = new MutationObserver(kill);
        window.__qaToastKiller.observe(document.body, { childList: true, subtree: true });
    });
}

/**
 * Full-screen intro card: issue number/title, the acceptance criteria about to be
 * verified, and provenance (env + fix commit) — so a viewer has context before any
 * action happens. Holds PACE.intro ms, then removes itself.
 */
export async function introCard(page, { issue, title, criteria, commit, seeded }, hold = PACE.intro) {
    await page.evaluate(({ issue, title, criteria, commit, seeded, base }) => {
        const el = document.createElement('div');
        el.id = 'qa-intro';
        Object.assign(el.style, {
            position: 'fixed', inset: '0', zIndex: '100000', background: '#111827',
            color: '#f9fafb', font: '400 16px/1.6 system-ui', padding: '48px 64px',
            display: 'flex', flexDirection: 'column', justifyContent: 'center'
        });
        const li = criteria.map((c, i) => `<li style="margin:4px 0">${i + 1}. ${c}</li>`).join('');
        el.innerHTML =
            `<div style="opacity:.7;letter-spacing:.1em;font-size:13px">dotCMS QA — RECORDED VERIFICATION</div>
             <h1 style="font-size:26px;margin:12px 0 4px">Issue #${issue} — ${title}</h1>
             <div style="margin:16px 0 8px;font-weight:600">Verifying ${criteria.length} acceptance criteria:</div>
             <ol style="list-style:none;margin:0;padding:0">${li}</ol>
             <div style="margin-top:24px;opacity:.7;font-size:13px">Environment: ${base} · Fix commit: ${commit || 'n/a'} · Single unedited take</div>
             <div style="opacity:.7;font-size:13px">Data seeded this run: ${seeded && seeded.length ? seeded.join(', ') : 'none'}</div>`;
        document.body.appendChild(el);
    }, { issue, title, criteria, commit, seeded, base: BASE });
    await page.waitForTimeout(hold);
    await page.evaluate(() => document.getElementById('qa-intro')?.remove());
}

/**
 * Live criteria checklist (bottom-right). Re-render with the full items array after every
 * status change or navigation — it is idempotent. Status: 'pending' | 'running' | 'pass' | 'fail'.
 * A criterion may only be flipped to pass/fail AFTER its DOM assertion returned — the
 * checklist documents the run, it never predicts it.
 */
export async function criteriaPanel(page, items) {
    await page.evaluate((items) => {
        let el = document.getElementById('qa-criteria');
        if (!el) {
            el = document.createElement('div');
            el.id = 'qa-criteria';
            Object.assign(el.style, {
                position: 'fixed', right: '12px', bottom: '12px', zIndex: '99998',
                background: 'rgba(17,24,39,.92)', color: '#f9fafb', borderRadius: '8px',
                font: '500 12px/1.5 system-ui', padding: '10px 14px', maxWidth: '380px',
                pointerEvents: 'none'
            });
            document.body.appendChild(el);
        }
        const icon = { pending: '○', running: '◐', pass: '✔', fail: '✘' };
        const color = { pending: '#9ca3af', running: '#fbbf24', pass: '#34d399', fail: '#f87171' };
        el.innerHTML = items.map((it) =>
            `<div style="color:${color[it.status]};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${icon[it.status]} ${it.text}</div>`
        ).join('');
    }, items);
}

/**
 * Render a cursor dot that follows the REAL mouse events (page.mouse.*) so viewers can
 * see where each click lands. Pure visualization of genuine input — call again after
 * any navigation (listeners don't survive a page load).
 */
export async function showCursor(page) {
    await page.evaluate(() => {
        if (document.getElementById('qa-cursor')) return;
        const dot = document.createElement('div');
        dot.id = 'qa-cursor';
        Object.assign(dot.style, {
            position: 'fixed', width: '18px', height: '18px', borderRadius: '50%',
            background: 'rgba(251,191,36,.55)', border: '2px solid #b45309',
            zIndex: '100001', pointerEvents: 'none', transform: 'translate(-50%,-50%)',
            transition: 'width .15s, height .15s', left: '-100px', top: '-100px'
        });
        document.body.appendChild(dot);
        document.addEventListener('mousemove', (e) => {
            dot.style.left = e.clientX + 'px';
            dot.style.top = e.clientY + 'px';
        }, true);
        document.addEventListener('mousedown', () => { dot.style.width = '28px'; dot.style.height = '28px'; }, true);
        document.addEventListener('mouseup', () => { dot.style.width = '18px'; dot.style.height = '18px'; }, true);
    });
}

/**
 * Click like a human: travel the mouse to the element's center in visible steps,
 * pause a beat, then click at those coordinates. Real input events end-to-end.
 * Accepts an ElementHandle or a selector string.
 */
export async function humanClick(page, target, { pause = 350 } = {}) {
    const handle = typeof target === 'string' ? await page.waitForSelector(target) : target;
    const box = await handle.boundingBox();
    if (!box) throw new Error('humanClick: target has no bounding box (not visible?)');
    const x = box.x + box.width / 2, y = box.y + box.height / 2;
    await page.mouse.move(x, y, { steps: 12 });
    await page.waitForTimeout(pause);
    await page.mouse.click(x, y, { delay: 60 });
}

/** On-screen narration banner so the video is self-documenting. */
export async function banner(page, text) {
    await page.evaluate((t) => {
        let el = document.getElementById('qa-banner');
        if (!el) {
            el = document.createElement('div');
            el.id = 'qa-banner';
            Object.assign(el.style, {
                position: 'fixed', top: '0', left: '0', right: '0', zIndex: '99999',
                background: '#111827', color: '#f9fafb', font: '600 15px/1.4 system-ui',
                padding: '8px 16px', textAlign: 'center', opacity: '0.92',
                pointerEvents: 'none' // overlays must NEVER intercept clicks meant for the app
            });
            document.body.appendChild(el);
        }
        el.textContent = t;
    }, text);
}

/**
 * True when the element exists and is rendered (offsetParent check).
 * Right for absence/presence claims ("button is gone"). For "the user can SEE it",
 * use visibleArea() — an attached but EMPTY container passes this check.
 */
export function visible(page, testid) {
    return page.evaluate((id) => {
        const el = document.querySelector(`[data-testid="${id}"]`);
        return !!el && el.offsetParent !== null;
    }, testid);
}

/**
 * True when the element is rendered AND occupies real pixels (≥ minPx area).
 * Use for any claim the viewer must be able to see on camera — a DOM-attached but
 * visually empty container (e.g. an actions bar with no actions) correctly FAILS this.
 */
export function visibleArea(page, testid, minPx = 100) {
    return page.evaluate(({ id, minPx }) => {
        const el = document.querySelector(`[data-testid="${id}"]`);
        if (!el || el.offsetParent === null) return false;
        const r = el.getBoundingClientRect();
        return r.width * r.height >= minPx;
    }, { id: testid, minPx });
}

/**
 * Park the cursor in neutral space (breadcrumb header area) so the final frames don't
 * show the dot hovering an interactive element that could be misread as state.
 */
export async function parkCursor(page) {
    await page.mouse.move(500, 45, { steps: 8 });
}

/** Poll a predicate — required for anything gated by enter/leave animations. */
export async function waitFor(fn, timeout = 8000, interval = 200) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
        if (await fn()) return true;
        await new Promise((r) => setTimeout(r, interval));
    }
    return false;
}

/** Check collector: const { check, results, allPass } = makeChecker(); */
export function makeChecker() {
    const results = [];
    return {
        results,
        check(name, ok) {
            results.push({ name, ok: !!ok });
            console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}`);
        },
        allPass: () => results.every((r) => r.ok),
        summary: () =>
            `${results.filter((r) => r.ok).length}/${results.length}`
    };
}

/**
 * Show the final verdict banner, print SUMMARY + VIDEO lines, and close everything.
 * Must be called even on error (wrap the run in try/finally) or the video is lost.
 * Pass the criteria items array so the banner counts CRITERIA (what the intro promised),
 * with assertions as supporting detail — "4/4 criteria verified (8 assertions)".
 */
export async function finalize(browser, context, page, checker, { criteria } = {}) {
    try {
        if (checker) {
            await parkCursor(page).catch(() => {});
            const verdict = criteria
                ? `${criteria.filter((c) => c.status === 'pass').length}/${criteria.length} criteria verified (${checker.summary()} assertions)`
                : `(${checker.summary()})`;
            await banner(
                page,
                `QA RESULT: ${checker.allPass() ? 'PASS ✔' : 'FAIL — see log'} — ${verdict}`
            );
            // The recorder is paint-driven: a fully static page stops producing frames and
            // the trailing verdict hold gets truncated from the file. Tiny real cursor
            // moves force frames so the verdict actually survives ~3s on screen.
            for (let i = 0; i < 6; i++) {
                await page.mouse.move(500 + (i % 2 ? 2 : -2), 45, { steps: 2 }).catch(() => {});
                await page.waitForTimeout(500);
            }
            console.log('\nSUMMARY ' + JSON.stringify(checker.results));
        }
    } finally {
        const video = page.video();
        await context.close(); // finalizes the video file
        if (video) console.log('VIDEO ' + (await video.path()));
        await browser.close();
    }
}
