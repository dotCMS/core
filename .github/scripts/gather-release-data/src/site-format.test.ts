import * as fs from 'fs';
import * as path from 'path';

/**
 * Golden-file guard for the SITE changelog editorial format (FR-002, FR-010, SC-004).
 *
 * Site release notes are produced by an LLM step against `prompt-template-site.md`, so the
 * exact prose is not deterministic and cannot be asserted byte-for-byte in CI. Instead this
 * suite guards the two things that ARE stable:
 *   1. a checked-in golden sample conforms to the site editorial format (the reference the
 *      template author writes against, and what "visually indistinguishable from hand-authored
 *      entries" means in practice), and
 *   2. `prompt-template-site.md` exists and instructs that same format — distinct from the
 *      GitHub `prompt-template.md` (which uses `([#N](.../pull/N))` and different sections).
 *
 * The site format was captured from live hand-authored entries on corpsites (2026-07-16):
 * a short prose intro, `### <Section> {#<Anchor>-<version>}` headings, and per-item issue
 * links in the double-bracket form `[[#NNNNN](https://github.com/dotCMS/core/issues/NNNNN)]`.
 */

const VERSION = '26.07.10-01';
const golden = fs.readFileSync(
  path.join(__dirname, '__fixtures__', 'site-release-notes.golden.md'),
  'utf8',
);

// Emoji ranges (pictographic, dingbats, symbols, flags) — the site format forbids emoji.
const EMOJI = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{1F1E6}-\u{1F1FF}\u{2190}-\u{21FF}\u{2B00}-\u{2BFF}️]/u;

describe('site-format golden sample', () => {
  it('opens with a short prose intro naming the version (not a heading, not a bullet)', () => {
    const firstLine = golden.split('\n').find((l) => l.trim().length > 0)!;
    expect(firstLine.startsWith('#')).toBe(false);
    expect(firstLine.startsWith('-')).toBe(false);
    expect(firstLine).toContain(VERSION);
  });

  it('uses the site section headings with per-version anchors', () => {
    expect(golden).toContain(`### Features {#Features-${VERSION}}`);
    expect(golden).toContain(`### Enhancements & Adjustments {#Enhancements-${VERSION}}`);
    expect(golden).toContain(`### Fixes {#Fixes-${VERSION}}`);
  });

  it('links every item to its GitHub issue in the site double-bracket form', () => {
    const links = golden.match(/\[\[#\d+\]\(https:\/\/github\.com\/dotCMS\/core\/issues\/\d+\)\]/g);
    expect(links).not.toBeNull();
    // Every bullet must carry a link.
    const bullets = golden.split('\n').filter((l) => l.trim().startsWith('- '));
    expect(bullets.length).toBeGreaterThan(0);
    expect(links!.length).toBe(bullets.length);
  });

  it('contains no emoji', () => {
    expect(EMOJI.test(golden)).toBe(false);
  });
});

describe('prompt-template-site.md', () => {
  const templatePath = path.join(__dirname, '..', 'prompt-template-site.md');

  it('exists', () => {
    expect(fs.existsSync(templatePath)).toBe(true);
  });

  it('instructs the site editorial format (sections, anchors, issue links, prose intro, no emoji)', () => {
    const t = fs.readFileSync(templatePath, 'utf8');
    expect(t).toContain('Features');
    expect(t).toContain('Enhancements & Adjustments');
    expect(t).toContain('Fixes');
    // Heading anchor convention keyed to the version.
    expect(t).toMatch(/\{#Fixes-/);
    // Issue-link style (issues, not pulls) that distinguishes the site from the GitHub template.
    expect(t).toContain('/issues/');
    expect(t.toLowerCase()).toContain('intro');
    expect(t.toLowerCase()).toContain('no emoji');
  });
});
