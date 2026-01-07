import { Logger } from '../../utils/logger';

export type WcagLevel = 'A' | 'AA' | 'AAA';
export type WcagImpact = 'minor' | 'moderate' | 'serious' | 'critical';

interface WcagFinding {
    id: string;
    level: 'A' | 'AA' | 'AAA';
    successCriterion: string; // e.g. "1.1.1"
    guideline: string;        // Human-readable name
    impact: WcagImpact;
    description: string;
    recommendation: string;
    instances?: WcagInstance[];
}

type WcagInstance = {
    line: number;    // 1-based
    column: number;  // 1-based
    snippet: string; // offending tag/attribute/text (trimmed)
};

const logger = new Logger('PAGE_WCAG_ANALYZER');

/**
 * Heuristic WCAG analyzer for static HTML content.
 * This does not execute JS or compute CSS; it uses string/regex analysis.
 */
export function analyzeHtmlForWcag(html: string, level: WcagLevel): WcagFinding[] {
    const findings: WcagFinding[] = [];

    const htmlLower = html.toLowerCase();
    const lineStarts = computeLineStarts(html);

    // 1) <html lang> attribute
    if (!/<html[^>]*\slang=/.test(htmlLower)) {
        const htmlTagMatch = /<html[^>]*>/i.exec(html);
        const pos = htmlTagMatch?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);
        findings.push({
            id: 'html-lang',
            level: 'A',
            successCriterion: '3.1.1',
            impact: 'serious',
            guideline: 'Language of Page',
            description: 'The root <html> element does not declare a primary language.',
            recommendation:
                'Add a valid IETF language code to the <html> element (e.g. <html lang="en">).',
            instances: [
                {
                    line,
                    column,
                    snippet: (htmlTagMatch?.[0] || '<html>').trim()
                }
            ]
        });
    }

    // 2) <title> presence
    if (!/<title>\s*\S+[\s\S]*?<\/title>/i.test(html)) {
        const headMatch = /<head[^>]*>/i.exec(html);
        const pos = headMatch?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);
        findings.push({
            id: 'page-title',
            level: 'A',
            successCriterion: '2.4.2',
            impact: 'serious',
            guideline: 'WCAG 2.4.2 Page Titled (Level A)',
            description: 'No <title> element found in the document head.',
            recommendation:
                'Provide a unique, descriptive <title> that summarizes the page purpose.',
            instances: [
                {
                    line,
                    column,
                    snippet: headMatch?.[0]?.trim() || '<head>'
                }
            ]
        });
    }

    // 3) <img> without alt or empty alt
    const imgMatches = [...html.matchAll(/<img\b[^>]*>/gi)];
    const imgsMissingAlt = imgMatches.filter(m => !/\salt\s*=/i.test(m[0]));
    const imgsEmptyAlt = imgMatches.filter(m => /\salt\s*=\s*""/i.test(m[0]));
    if (imgsMissingAlt.length) {
        findings.push({
          id: 'img-missing-alt',
          level: 'A',
          successCriterion: '1.1.1',
          impact: 'serious',
          guideline: 'Non-text Content',
          description: 'Images are missing an alt attribute.',
          recommendation:
            'Add meaningful alt text to informative images. Use alt="" only for purely decorative images.',
          instances: imgsMissingAlt.map((m) => {
              const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
              return { line, column, snippet: m[0].trim() };
          })
        });
      }
      
      if (imgsEmptyAlt.length) {
        findings.push({
          id: 'img-empty-alt-review',
          level: 'A',
          successCriterion: '1.1.1',
          impact: 'minor',
          guideline: 'Non-text Content',
          description:
            'Images use empty alt attributes. This is valid only if the images are purely decorative.',
          recommendation:
            'Confirm these images are decorative. If they convey information, provide descriptive alt text.',
          instances: imgsEmptyAlt.map((m) => {
              const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
              return { line, column, snippet: m[0].trim() };
          })
        });
      }

    // 4) Links without accessible or descriptive text
    const anchorMatches = [...html.matchAll(/<a\b[^>]*>([\s\S]*?)<\/a>/gi)];
    const linksWithNoText = anchorMatches.filter((m) => {
        const tag = m[0];
        // Remove anchor wrapper and any nested markup
        const linkText = tag
            .replace(/<a\b[^>]*>/i, '')
            .replace(/<\/a>/i, '')
            .replace(/<[^>]+>/g, '')
            .trim();

        return linkText.length === 0;
    });

    if (linksWithNoText.length > 0) {
        findings.push({
            id: 'link-purpose',
            level: 'A',
            successCriterion: '2.4.4',
            impact: 'moderate',
            guideline: 'Link Purpose (In Context)',
            description:
                'One or more links do not have accessible or descriptive text.',
            recommendation:
                'Ensure each link has descriptive text that clearly conveys its purpose or destination.',
            instances: linksWithNoText.map((m) => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return { line, column, snippet: m[0].trim() };
            })
        });
    }

    // 5) Headings order (no skipping levels)
    const headingMatches = [...html.matchAll(/<(h[1-6])\b[^>]*>/gi)];
    let lastLevel = 0;
    const skipped: WcagInstance[] = [];
    headingMatches.forEach((m) => {
        const level = parseInt(m[1].substring(1), 10);
        if (lastLevel && level > lastLevel + 1) {
            const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
            skipped.push({ line, column, snippet: m[0].trim() });
        }
        lastLevel = level;
    });
    if (skipped.length > 0) {
        findings.push({
            id: 'heading-structure',
            level: 'A',
            successCriterion: '1.3.1',
            impact: 'minor',
            guideline: 'Info and Relationships',
            description:
                'Heading levels may not accurately represent the logical structure of the content.',
            recommendation:
                'Review heading levels to ensure they reflect the content hierarchy and programmatic relationships.',
            instances: skipped
        });
    }

    // 6) Form controls without labels (basic heuristic)
    const inputs = [...html.matchAll(/<(input|select|textarea)\b[^>]*>/gi)];
    const controlsMissingLabels = inputs.filter((m) => {
        const tag = m[0];
        const hasAriaLabel = /\baria-label\s*=\s*"/i.test(tag);
        const hasAriaLabelledby = /\baria-labelledby\s*=\s*"/i.test(tag);
        const idMatch = tag.match(/\bid\s*=\s*"([^"]+)"/i);
        const id = idMatch ? idMatch[1] : '';
        const likelyWrapped = /<label[^>]*>[\s\S]*?(<input|<select|<textarea)/i.test(html); // rough
        if (hasAriaLabel || hasAriaLabelledby) return false;
        if (id && new RegExp(`<label[^>]*for\\s*=\\s*"${id}"[^>]*>`, 'i').test(html)) return false;
        return !likelyWrapped;
    });
    if (controlsMissingLabels.length > 0) {
        findings.push({
            id: 'form-control-label',
            level: 'A',
            successCriterion: '3.3.2',
            impact: 'serious',
            guideline: 'Labels or Instructions',
            description:
                'Some form controls do not appear to have programmatically associated labels.',
            recommendation:
                'Ensure each form control has an associated <label>, aria-label, or aria-labelledby that clearly identifies its purpose.',
            instances: controlsMissingLabels.map((m) => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return { line, column, snippet: m[0].trim() };
            })
        });
    }

    // 7) Buttons without an accessible name
    const buttonMatches = [...html.matchAll(/<button\b[^>]*>([\s\S]*?)<\/button>/gi)];
    const unnamedButtons = buttonMatches.filter((m) => {
        const tag = m[0];
        const text = tag.replace(/<button\b[^>]*>/i, '').replace(/<\/button>/i, '').replace(/<[^>]+>/g, '').trim();
        const hasAriaLabel = /\baria-label\s*=\s*"/i.test(tag);
        const hasAriaLabelledby = /\baria-labelledby\s*=\s*"/i.test(tag);
        return text.length === 0 && !hasAriaLabel && !hasAriaLabelledby;
    });
    if (unnamedButtons.length > 0) {
        findings.push({
            id: 'button-accessible-name',
            level: 'A',
            successCriterion: '4.1.2',
            impact: 'moderate',
            guideline: 'Name, Role, Value',
            description:
                'Some buttons do not appear to have an accessible name that can be programmatically determined.',
            recommendation:
                'Provide visible button text or supply an accessible name using aria-label or aria-labelledby.',
            instances: unnamedButtons.map((m) => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return { line, column, snippet: m[0].trim() };
            })
        });
    }

    // 8) Duplicate IDs
    const idMatches = [...html.matchAll(/\bid\s*=\s*"([^"]+)"/gi)];
    const seen = new Set<string>();
    const duplicates: WcagInstance[] = [];
    idMatches.forEach((m) => {
        const id = m[1];
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
        if (seen.has(id)) {
            duplicates.push({ line, column, snippet: `id="${id}"` });
        } else {
            seen.add(id);
        }
    });
    if (duplicates.length > 0) {
        findings.push({
            id: 'duplicate-id',
            level: 'A',
            successCriterion: '4.1.1',
            impact: 'moderate',
            guideline: 'Parsing',
            description:
                'Multiple elements share the same id attribute value, which can break programmatic associations.',
            recommendation:
                'Ensure every id attribute value is unique within the document.',
            instances: duplicates
        });
    }

    // 9) Landmarks
    const hasMain =
    /<main\b[^>]*>/.test(htmlLower) ||
    /\brole\s*=\s*"main"/.test(htmlLower);

    if (!hasMain) {
        const bodyMatch = /<body[^>]*>/i.exec(html);
        const pos = bodyMatch?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);
        findings.push({
            id: 'landmark-main',
            level: 'A',
            successCriterion: '1.3.1',
            impact: 'minor',
            guideline: 'Info and Relationships',
            description:
                'No explicit main landmark was detected to identify the primary content region.',
            recommendation:
                'Consider adding a <main> element or role="main" to clearly define the primary content for assistive technologies.',
            instances: [
                {
                    line,
                    column,
                    snippet: bodyMatch?.[0]?.trim() || '<body>'
                }
            ]
        });
    }

    // 10) AA/AAA reminders (not fully implemented without CSS/JS)
    if (level === 'AA') {
        findings.push({
            id: 'aa-checks-not-implemented',
            level: 'AA',
            successCriterion: 'N/A',
            impact: 'minor',
            guideline: 'Level AA Checks',
            description:
                'Automated AA checks are not yet implemented because some checks require CSS execution and browser rendering.',
            recommendation:
                'Full AA compliance should be verified using a browser-based accessibility testing tool or manual review.'
        });
    }

    if (level === 'AAA') {
        findings.push({
            id: 'aaa-checks-not-implemented',
            level: 'AAA',
            successCriterion: 'N/A',
            impact: 'minor',
            guideline: 'Level AAA Checks',
            description:
                'Automated AAA checks are not yet implemented because some checks require CSS execution and browser rendering.',
            recommendation:
                'Full AAA compliance should be verified using a browser-based accessibility testing tool or manual review.'
        });
    }

    logger.debug('WCAG analysis completed', { level, findingCount: findings.length });
    return findings;
}

export function formatWcagReport(
    uri: string,
    level: WcagLevel,
    findings: WcagFinding[]
): string {
    const counts = findings.reduce<Record<WcagImpact, number>>(
        (acc, f) => {
            acc[f.impact] = (acc[f.impact] || 0) + 1;
            return acc;
        },
        { minor: 0, moderate: 0, serious: 0, critical: 0 }
    );

    const header = [
        'WCAG Analysis Report',
        '====================',
        `URI: ${uri}`,
        `Level: ${level}`,
        '',
        `Summary: ${findings.length} findings (critical: ${counts.critical}, serious: ${counts.serious}, moderate: ${counts.moderate}, minor: ${counts.minor})`,
        ''
    ];

    const details: string[] = [];
    findings.forEach((f, idx) => {
        details.push(
            `${idx + 1}. [${f.impact.toUpperCase()}] ${f.guideline}`,
            `   - Issue: ${f.description}`,
            `   - Recommendation: ${f.recommendation}`
        );
        if (f.instances && f.instances.length > 0) {
            details.push(
                `   - Instances:`,
                ...f.instances.map((inst) => {
                    const snippet = inst.snippet.replace(/\s+/g, ' ');
                    return `     • line ${inst.line}, col ${inst.column}: ${snippet}`;
                })
            );
        }
        details.push('');
    });

    if (findings.length === 0) {
        details.push('No issues detected by static analysis. Consider running a full a11y audit with a browser-based tool for dynamic/visual checks.');
    }

    return [...header, ...details].join('\n');
}

/**
 * SEO analysis
 */
export type SeoSeverity = 'info' | 'minor' | 'moderate' | 'serious';
export type SeoIssue = {
    id: string;
    severity: SeoSeverity;
    description: string;
    recommendation: string;
    instances?: Array<{ line: number; column: number; snippet: string }>;
};

export function analyzeHtmlForSeo(html: string): SeoIssue[] {
    const issues: SeoIssue[] = [];
    const lineStarts = computeLineStarts(html);

    //#region Title
    // Title
    const titleRe = /<title[^>]*>([\s\S]*?)<\/title>/gi;
    const titleMatches = [...html.matchAll(titleRe)];
    const titles = titleMatches.map(m =>
        m[1].replace(/\s+/g, ' ').trim()
    );

    if (titleMatches.length === 0) {
        const headExec = /<head[^>]*>/i.exec(html);
        const pos = headExec?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);

        issues.push({
            id: 'seo-title-missing',
            severity: 'serious',
            description: 'Missing <title> in <head>.',
            recommendation:
                'Add a unique <title> that clearly describes the page’s primary topic (ideally ~10–60 characters).',
            instances: [
                {
                    line,
                    column,
                    snippet: headExec?.[0]?.trim() || '<head>'
                }
            ]
        });
    } else if (titleMatches.length > 1) {
        issues.push({
            id: 'seo-title-multiple',
            severity: 'serious',
            description: `Multiple <title> tags found (${titleMatches.length}).`,
            recommendation:
                'Ensure only one <title> tag exists in the <head> to avoid search engine ambiguity.',
            instances: titleMatches.map(m => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return {
                    line,
                    column,
                    snippet: m[0].trim()
                };
            })
        });
    } else if (titles[0].length === 0) {
        const m = titleMatches[0];
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

        issues.push({
            id: 'seo-title-empty',
            severity: 'serious',
            description: '<title> tag is present but empty.',
            recommendation:
                'Add a descriptive title that reflects the page’s main topic and intent.',
            instances: [
                {
                    line,
                    column,
                    snippet: m[0].trim()
                }
            ]
        });
    } else if (titles[0].length < 10 || titles[0].length > 60) {
        const m = titleMatches[0];
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

        issues.push({
            id: 'seo-title-length',
            severity: 'moderate',
            description: `Title length is ${titles[0].length} characters.`,
            recommendation:
                'Revise the title to concisely describe the page topic (ideally ~10–60 characters) and include primary keywords where natural.',
            instances: [
                {
                    line,
                    column,
                    snippet: m[0].trim()
                }
            ]
        });
    }
    //#endregion

    //#region Meta description
    // Meta description
    const metaDescRe =
        /<meta\b[^>]*\bname\s*=\s*["']description["'][^>]*>/gi;

    const metaDescMatches = [...html.matchAll(metaDescRe)];
    const extractContent = (tag: string): string =>
        /content\s*=\s*["']([^"']*)["']/i.exec(tag)?.[1]?.trim() ?? '';
    if (metaDescMatches.length === 0) {
        const headExec = /<head[^>]*>/i.exec(html);
        const pos = headExec?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);

        issues.push({
            id: 'seo-description-missing',
            severity: 'moderate',
            description: 'Missing meta description.',
            recommendation:
                'Add a concise, compelling meta description that summarizes the page and encourages clicks (ideally ~50–160 characters).',
            instances: [
                {
                    line,
                    column,
                    snippet: headExec?.[0]?.trim() || '<head>'
                }
            ]
        });
    } else if (metaDescMatches.length > 1) {
        issues.push({
            id: 'seo-description-multiple',
            severity: 'moderate',
            description: `Multiple meta descriptions found (${metaDescMatches.length}).`,
            recommendation:
                'Ensure only one meta description is present to avoid search engines ignoring or rewriting it.',
            instances: metaDescMatches.map(m => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return {
                    line,
                    column,
                    snippet: m[0].trim()
                };
            })
        });
    } else {
        const m = metaDescMatches[0];
        const desc = extractContent(m[0]);
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

        if (desc.length === 0) {
            issues.push({
                id: 'seo-description-empty',
                severity: 'moderate',
                description: 'Meta description is present but empty.',
                recommendation:
                    'Write a short, meaningful description that clearly explains the page value and encourages search users to click.',
                instances: [
                    {
                        line,
                        column,
                        snippet: m[0].trim()
                    }
                ]
            });
        } else if (desc.length < 50 || desc.length > 160) {
            issues.push({
                id: 'seo-description-length',
                severity: 'minor',
                description: `Meta description length is ${desc.length} characters.`,
                recommendation:
                    'Revise the description to clearly summarize the page and fit within ~50–160 characters to avoid truncation or rewrites.',
                instances: [
                    {
                        line,
                        column,
                        snippet: m[0].trim()
                    }
                ]
            });
        }
    }
    //#endregion

    //#region Canonical
    // Canonical
    const canonicalRe =
        /<link\b[^>]*\brel\s*=\s*["']canonical["'][^>]*>/gi;
    const canonicalMatches = [...html.matchAll(canonicalRe)];
    const extractHref = (tag: string): string =>
        /href\s*=\s*["']([^"']*)["']/i.exec(tag)?.[1]?.trim() ?? '';

    if (canonicalMatches.length === 0) {
        const headExec = /<head[^>]*>/i.exec(html);
        const pos = headExec?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);

        issues.push({
            id: 'seo-canonical-missing',
            severity: 'moderate',
            description: 'Missing canonical link.',
            recommendation:
                'Add a single <link rel="canonical"> with an absolute URL that represents the preferred version of this page.',
            instances: [
                {
                    line,
                    column,
                    snippet: headExec?.[0]?.trim() || '<head>'
                }
            ]
        });
    } else if (canonicalMatches.length > 1) {
        issues.push({
            id: 'seo-canonical-multiple',
            severity: 'moderate',
            description: `Multiple canonical links found (${canonicalMatches.length}).`,
            recommendation:
                'Ensure only one canonical link is present to avoid search engines ignoring or misinterpreting it.',
            instances: canonicalMatches.map(m => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return {
                    line,
                    column,
                    snippet: m[0].trim()
                };
            })
        });
    } else {
        const m = canonicalMatches[0];
        const href = extractHref(m[0]);
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

        if (!href) {
            issues.push({
                id: 'seo-canonical-empty',
                severity: 'moderate',
                description: 'Canonical link is present but has an empty href.',
                recommendation:
                    'Set the canonical href to the absolute URL of the preferred indexable page.',
                instances: [
                    {
                        line,
                        column,
                        snippet: m[0].trim()
                    }
                ]
            });
        }
    }
    //#endregion

    //#region Robots meta   
    // Robots meta
    const robotsRe =
    /<meta\b[^>]*\bname\s*=\s*["']robots["'][^>]*>/gi;
    const robotsMatches = [...html.matchAll(robotsRe)];
    const extractContentRobots = (tag: string): string =>
    /content\s*=\s*["']([^"']*)["']/i.exec(tag)?.[1]?.trim().toLowerCase() ?? '';

    if (robotsMatches.length > 1) {
        issues.push({
            id: 'seo-robots-multiple',
            severity: 'moderate',
            description: `Multiple robots meta tags found (${robotsMatches.length}).`,
            recommendation:
                'Ensure only one robots meta tag is present to avoid conflicting indexing directives.',
            instances: robotsMatches.map(m => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return {
                    line,
                    column,
                    snippet: m[0].trim()
                };
            })
        });
    } else if (robotsMatches.length === 1) {
        const m = robotsMatches[0];
        const content = extractContentRobots(m[0]);
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

        if (!content) {
            issues.push({
                id: 'seo-robots-empty',
                severity: 'minor',
                description: 'Robots meta tag is present but empty.',
                recommendation:
                    'Remove the empty robots meta tag or explicitly define indexing directives.',
                instances: [
                    {
                        line,
                        column,
                        snippet: m[0].trim()
                    }
                ]
            });
        } else if (content.includes('none') || content.includes('noindex')) {
            issues.push({
                id: 'seo-robots-noindex',
                severity: 'serious',
                description: `Robots meta prevents indexing ("${content}").`,
                recommendation:
                    'Remove noindex/none if this page should be indexed and appear in search results.',
                instances: [
                    {
                        line,
                        column,
                        snippet: m[0].trim()
                    }
                ]
            });
        } else if (content.includes('nofollow')) {
            issues.push({
                id: 'seo-robots-nofollow',
                severity: 'moderate',
                description: 'Robots meta contains nofollow.',
                recommendation:
                    'Remove nofollow unless you intentionally want to block link crawling from this page.',
                instances: [
                    {
                        line,
                        column,
                        snippet: m[0].trim()
                    }
                ]
            });
        }
    }

    //#endregion

    //#region Headings  
    // Headings audit: H1–H6
    const headingRe = /<(h[1-6])\b[^>]*>([\s\S]*?)<\/\1>/gi;
    const headingMatches = [...html.matchAll(headingRe)];

    // Group matches by level
    const headingsByLevel: Record<string, { index: number; text: string }[]> = {
        h1: [], h2: [], h3: [], h4: [], h5: [], h6: []
    };

    headingMatches.forEach(m => {
        const level = m[1].toLowerCase(); // h1–h6
        headingsByLevel[level].push({ index: m.index ?? 0, text: m[2].trim() });
    });

    // --- H1 checks ---
    const h1s = headingsByLevel.h1;

    if (h1s.length === 0) {
        const bodyExec = /<body[^>]*>/i.exec(html);
        const pos = bodyExec?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);

        issues.push({
            id: 'seo-h1-missing',
            severity: 'moderate',
            description: 'No <h1> found.',
            recommendation:
                'Include a single, descriptive <h1> that clearly reflects the page topic and primary keywords.',
            instances: [
                {
                    line,
                    column,
                    snippet: bodyExec?.[0]?.trim() || '<body>'
                }
            ]
        });
    } else {
        const emptyH1s = h1s.filter(h => !h.text);
        if (emptyH1s.length) {
            issues.push({
                id: 'seo-h1-empty',
                severity: 'moderate',
                description: `${emptyH1s.length} <h1> element(s) are empty.`,
                recommendation:
                    'Ensure all <h1> elements contain meaningful, descriptive text reflecting the page topic.',
                instances: emptyH1s.map(h => {
                    const { line, column } = positionFromIndex(lineStarts, h.index);
                    return { line, column, snippet: `<h1>${h.text}</h1>` };
                })
            });
        }

        if (h1s.length > 1) {
            issues.push({
                id: 'seo-h1-multiple',
                severity: 'minor',
                description: `Found ${h1s.length} <h1> elements.`,
                recommendation:
                    'Use a single primary <h1>; structure subheadings with H2–H6 for better SEO hierarchy.',
                instances: h1s.map(h => {
                    const { line, column } = positionFromIndex(lineStarts, h.index);
                    return { line, column, snippet: `<h1>${h.text}</h1>` };
                })
            });
        }
    }

    // --- H2–H6 checks ---
    ['h2','h3','h4','h5','h6'].forEach((level, i) => {
        const hs = headingsByLevel[level];
        hs.forEach(h => {
            if (!h.text) {
                const { line, column } = positionFromIndex(lineStarts, h.index);
                issues.push({
                    id: `seo-${level}-empty`,
                    severity: 'minor',
                    description: `<${level}> is empty.`,
                    recommendation: `Provide meaningful text for all <${level}> headings.`,
                    instances: [{ line, column, snippet: `<${level}></${level}>` }]
                });
            }
        });

        // Hierarchy check: e.g., H3 should not appear before H2
        if (hs.length && i > 0) {
            const prevLevel = ['h1','h2','h3','h4','h5','h6'][i-1];
            if (headingsByLevel[prevLevel].length === 0) {
                hs.forEach(h => {
                    const { line, column } = positionFromIndex(lineStarts, h.index);
                    issues.push({
                        id: `seo-${level}-hierarchy`,
                        severity: 'minor',
                        description: `<${level}> found without preceding <${prevLevel}>.`,
                        recommendation: `Maintain proper heading hierarchy: <${prevLevel}> before <${level}>.`,
                        instances: [{ line, column, snippet: `<${level}>${h.text}</${level}>` }]
                    });
                });
            }
        }
    });
    //#endregion

    //#region Links audit: internal & external
    //Links audit: internal & external
    const linkRe = /<a\b[^>]*href\s*=\s*["']([^"']*)["'][^>]*>/gi;
    const linkMatches = [...html.matchAll(linkRe)];

    linkMatches.forEach(m => {
        const href = m[1].trim();
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

        // Missing or empty href (should not happen with this regex, but defensive)
        if (!href) {
            issues.push({
                id: 'seo-link-empty',
                severity: 'minor',
                description: '<a> tag has an empty href.',
                recommendation: 'Provide a valid URL in href for all <a> tags.',
                instances: [{ line, column, snippet: m[0].trim() }]
            });
            return;
        }

        // Anchor-only / placeholder links
        if (href === '#' || href.toLowerCase() === 'javascript:void(0)') {
            issues.push({
                id: 'seo-link-placeholder',
                severity: 'minor',
                description: `<a> tag has a placeholder href ("${href}").`,
                recommendation: 'Replace placeholder links with valid URLs or remove if decorative.',
                instances: [{ line, column, snippet: m[0].trim() }]
            });
        }

        // Optional: internal vs external classification
        const isInternal = href.startsWith('/') || href.startsWith('#');
        // Here you could later add broken link detection via HTTP requests if desired
    });

    // Detect <a> tags missing href completely
    const aTags = [...html.matchAll(/<a\b[^>]*>/gi)];
    const aWithoutHref = aTags.filter(m => !/href\s*=/i.test(m[0]));

    aWithoutHref.forEach(m => {
        const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
        issues.push({
            id: 'seo-link-missing-href',
            severity: 'minor',
            description: '<a> tag is missing href attribute.',
            recommendation: 'Add href to all <a> tags for proper navigation and SEO.',
            instances: [{ line, column, snippet: m[0].trim() }]
        });
    });
    //#endregion

    //#region Images without alt or empty alt
    // Images without alt or empty alt
    const imgMatchesSeo = [...html.matchAll(/<img\b[^>]*>/gi)];
    const imgsMissingOrEmptyAlt = imgMatchesSeo.filter((m) => {
        const altMatch = /\balt\s*=\s*["']([^"']*)["']/i.exec(m[0]);
        return !altMatch || !altMatch[1].trim();
    });

    if (imgsMissingOrEmptyAlt.length) {
        issues.push({
            id: 'seo-img-alt-missing',
            severity: 'minor',
            description: 'Images missing alt attributes or having empty alt text.',
            recommendation:
                'Add meaningful, concise alt text describing the image content and context to improve SEO.',
            instances: imgsMissingOrEmptyAlt.slice(0, 50).map((m) => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return { line, column, snippet: m[0].trim() };
            })
        });
    }
    //#endregion

    //#region Json LD Schema
    // Structured Data / JSON-LD audit
    const jsonLdRe = /<script\b[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi;
    const jsonLdMatches = [...html.matchAll(jsonLdRe)];

    if (jsonLdMatches.length === 0) {
        const headExec = /<head[^>]*>/i.exec(html);
        const pos = headExec?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);

        issues.push({
            id: 'seo-jsonld-missing',
            severity: 'minor',
            description: 'No JSON-LD structured data found.',
            recommendation: 'Add JSON-LD with @context and @type for rich results and better indexing.',
            instances: [{ line, column, snippet: headExec?.[0]?.trim() || '<head>' }]
        });
    } else {
        jsonLdMatches.forEach(m => {
            const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
            const rawJson = m[1].trim();

            if (!rawJson) {
                issues.push({
                    id: 'seo-jsonld-empty',
                    severity: 'minor',
                    description: 'JSON-LD script is empty.',
                    recommendation: 'Provide valid JSON-LD content for structured data.',
                    instances: [{ line, column, snippet: m[0].trim() }]
                });
                return;
            }

            try {
                const data = JSON.parse(rawJson);

                // Basic validation: must have @context and @type
                if (!data['@context'] || !data['@type']) {
                    issues.push({
                        id: 'seo-jsonld-invalid',
                        severity: 'minor',
                        description: 'JSON-LD missing required @context or @type fields.',
                        recommendation: 'Ensure JSON-LD includes "@context" and "@type" to be valid.',
                        instances: [{ line, column, snippet: m[0].trim() }]
                    });
                }
            } catch (err) {
                issues.push({
                    id: 'seo-jsonld-malformed',
                    severity: 'minor',
                    description: 'JSON-LD contains invalid JSON.',
                    recommendation: 'Correct JSON syntax in the JSON-LD script.',
                    instances: [{ line, column, snippet: m[0].trim() }]
                });
            }
        });
    }
    //#endregion

    //#region Language / hreflang audit
    // Language / hreflang audit
    // Check <html lang="...">
    const htmlTagRe = /<html\b[^>]*>/i;
    const htmlTagMatch = htmlTagRe.exec(html);

    if (!htmlTagMatch) {
        // Unlikely, but defensive
        issues.push({
            id: 'seo-html-missing',
            severity: 'minor',
            description: 'Missing <html> tag.',
            recommendation: 'Ensure the page has a proper <html> tag with a lang attribute.',
            instances: [{ line: 0, column: 0, snippet: '<html>' }]
        });
    } else {
        const langMatch = /lang\s*=\s*["']([^"']+)["']/i.exec(htmlTagMatch[0]);
        const { line, column } = positionFromIndex(lineStarts, htmlTagMatch.index ?? 0);

        if (!langMatch || !langMatch[1].trim()) {
            issues.push({
                id: 'seo-html-lang-missing',
                severity: 'minor',
                description: '<html> tag is missing a lang attribute or it is empty.',
                recommendation:
                    'Add a valid lang attribute (e.g., lang="en") to indicate page language for SEO and accessibility.',
                instances: [{ line, column, snippet: htmlTagMatch[0].trim() }]
            });
        }
    }

    // Check <link rel="alternate" hreflang="...">
    const hreflangRe = /<link\b[^>]*rel=["']alternate["'][^>]*hreflang=["']([^"']+)["'][^>]*>/gi;
    const hreflangMatches = [...html.matchAll(hreflangRe)];

    if (hreflangMatches.length === 0) {
        // Optional: only warn if site is expected to have multiple languages/regions
        // We'll flag as minor anyway
        const headExec = /<head[^>]*>/i.exec(html);
        const pos = headExec?.index ?? 0;
        const { line, column } = positionFromIndex(lineStarts, pos);

        issues.push({
            id: 'seo-hreflang-missing',
            severity: 'minor',
            description: 'No hreflang link tags found.',
            recommendation:
                'Add <link rel="alternate" hreflang="..."> tags for multilingual or multi-regional pages to guide search engines.',
            instances: [{ line, column, snippet: headExec?.[0]?.trim() || '<head>' }]
        });
    } else {
        hreflangMatches.forEach(m => {
            const value = m[1].trim();
            const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);

            if (!value || !/^[a-z]{2}(-[A-Z]{2})?$/.test(value)) {
                issues.push({
                    id: 'seo-hreflang-invalid',
                    severity: 'minor',
                    description: `hreflang value "${value}" is missing or malformed.`,
                    recommendation:
                        'Use valid ISO language codes (e.g., "en", "en-US", "fr").',
                    instances: [{ line, column, snippet: m[0].trim() }]
                });
            }
        });
    }

    //#endregion

    //#region Open Graph / Twitter Card
    // Open Graph / Twitter Card
    const ogTitleRe = /<meta\b[^>]*\bproperty\s*=\s*["']og:title["'][^>]*>/gi;
    const ogDescRe = /<meta\b[^>]*\bproperty\s*=\s*["']og:description["'][^>]*>/gi;
    const twCardRe = /<meta\b[^>]*\bname\s*=\s*["']twitter:card["'][^>]*>/gi;

    const extractOgMetaContent = (tag: string): string =>
        /content\s*=\s*["']([^"']*)["']/i.exec(tag)?.[1]?.trim() ?? '';

    const ogTitleMatches = [...html.matchAll(ogTitleRe)];
    const ogDescMatches = [...html.matchAll(ogDescRe)];
    const twCardMatches = [...html.matchAll(twCardRe)];

    const headTagExec = /<head[^>]*>/i.exec(html);
    const headPos = headTagExec?.index ?? 0;
    const { line: headLine, column: headColumn } = positionFromIndex(lineStarts, headPos);

    // Open Graph title
    if (ogTitleMatches.length === 0) {
        issues.push({
            id: 'seo-og-title-missing',
            severity: 'minor',
            description: 'Missing Open Graph title.',
            recommendation:
                'Add a concise, descriptive og:title for social sharing previews.',
            instances: [
                { line: headLine, column: headColumn, snippet: headTagExec?.[0]?.trim() || '<head>' }
            ]
        });
    } else {
        const emptyTitles = ogTitleMatches.filter(m => !extractOgMetaContent(m[0]));
        if (emptyTitles.length) {
            issues.push({
                id: 'seo-og-title-empty',
                severity: 'minor',
                description: `${emptyTitles.length} Open Graph title meta tag(s) have empty content.`,
                recommendation:
                    'Ensure og:title content is meaningful, concise, and engaging for social previews.',
                instances: emptyTitles.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
        if (ogTitleMatches.length > 1) {
            issues.push({
                id: 'seo-og-title-multiple',
                severity: 'minor',
                description: `Multiple Open Graph title meta tags found (${ogTitleMatches.length}).`,
                recommendation: 'Keep only one og:title to avoid conflicting social previews.',
                instances: ogTitleMatches.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
    }

    // Open Graph description
    if (ogDescMatches.length === 0) {
        issues.push({
            id: 'seo-og-description-missing',
            severity: 'minor',
            description: 'Missing Open Graph description.',
            recommendation:
                'Add a concise, descriptive og:description to improve social sharing previews.',
            instances: [
                { line: headLine, column: headColumn, snippet: headTagExec?.[0]?.trim() || '<head>' }
            ]
        });
    } else {
        const emptyDescs = ogDescMatches.filter(m => !extractOgMetaContent(m[0]));
        if (emptyDescs.length) {
            issues.push({
                id: 'seo-og-description-empty',
                severity: 'minor',
                description: `${emptyDescs.length} Open Graph description meta tag(s) have empty content.`,
                recommendation:
                    'Ensure og:description content clearly summarizes the page for social previews.',
                instances: emptyDescs.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
        if (ogDescMatches.length > 1) {
            issues.push({
                id: 'seo-og-description-multiple',
                severity: 'minor',
                description: `Multiple Open Graph description meta tags found (${ogDescMatches.length}).`,
                recommendation: 'Keep only one og:description to avoid conflicting social previews.',
                instances: ogDescMatches.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
    }

    // Twitter Card
    if (twCardMatches.length === 0) {
        issues.push({
            id: 'seo-twittercard-missing',
            severity: 'minor',
            description: 'Missing Twitter Card meta tag.',
            recommendation:
                'Add a twitter:card meta tag (e.g., summary or summary_large_image) for proper Twitter previews.',
            instances: [
                { line: headLine, column: headColumn, snippet: headTagExec?.[0]?.trim() || '<head>' }
            ]
        });
    } else {
        const emptyCards = twCardMatches.filter(m => !extractOgMetaContent(m[0]));
        if (emptyCards.length) {
            issues.push({
                id: 'seo-twittercard-empty',
                severity: 'minor',
                description: `${emptyCards.length} Twitter Card meta tag(s) have empty content.`,
                recommendation:
                    'Ensure twitter:card content is valid (summary, summary_large_image, etc.) for proper previews.',
                instances: emptyCards.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
        if (twCardMatches.length > 1) {
            issues.push({
                id: 'seo-twittercard-multiple',
                severity: 'minor',
                description: `Multiple Twitter Card meta tags found (${twCardMatches.length}).`,
                recommendation: 'Keep only one twitter:card to avoid conflicting social previews.',
                instances: twCardMatches.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
    }
    //#endregion    
    return issues;
}

export function formatSeoReport(uri: string, issues: SeoIssue[]): string {
    const lines: string[] = [];
    lines.push('SEO Analysis Report', '===================', `URI: ${uri}`, '');
    lines.push(`Summary: ${issues.length} issues found`, '');
    issues.forEach((iss, i) => {
        lines.push(
            `${i + 1}. [${iss.severity.toUpperCase()}] ${iss.description}`,
            `   - Recommendation: ${iss.recommendation}`
        );
        if (iss.instances?.length) {
            lines.push(
                `   - Instances:`,
                ...iss.instances.map((inst) => {
                    const snippet = inst.snippet.replace(/\s+/g, ' ');
                    return `     • line ${inst.line}, col ${inst.column}: ${snippet}`;
                })
            );
        }
        lines.push('');
    });
    return lines.join('\n');
}

/**
 * GEO analysis
 */
export type GeoSeverity = 'info' | 'minor' | 'moderate' | 'serious';
export type GeoIssue = {
    id: string;
    severity: 'minor' | 'moderate' | 'serious' | 'info';
    description: string;
    recommendation: string;
    instances?: {
        line: number;
        column: number;
        snippet: string;
    }[];
};

export function analyzeHtmlForGeo(html: string): GeoIssue[] {
    const issues: GeoIssue[] = [];
    const lineStarts = computeLineStarts(html);

    // Headings and content chunking (GEO)
    const headingMatches = [
        ...html.matchAll(/<(h[1-6])\b[^>]*>([\s\S]*?)<\/\1>/gi)
    ];

    if (headingMatches.length === 0) {
        issues.push({
            id: 'geo-headings-missing',
            severity: 'moderate',
            description: 'No headings detected for content hierarchy.',
            recommendation:
                'Add descriptive headings (H1–H3) to chunk content into scannable, extractable sections.'
        });
    } else {
        const emptyHeadings = headingMatches.filter(m => !m[2].trim());
        if (emptyHeadings.length > 0) {
            issues.push({
                id: 'geo-headings-empty',
                severity: 'moderate',
                description: `${emptyHeadings.length} heading(s) contain no meaningful text.`,
                recommendation:
                    'Ensure all headings contain clear, descriptive text to define content chunks.',
                instances: emptyHeadings.map(m => {
                    const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }

        const levels = headingMatches.map(m => parseInt(m[1].substring(1), 10));
        for (let i = 1; i < levels.length; i++) {
            if (levels[i] - levels[i - 1] > 1) {
                const m = headingMatches[i];
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                issues.push({
                    id: 'geo-heading-hierarchy-skip',
                    severity: 'minor',
                    description: 'Heading levels skip hierarchy.',
                    recommendation:
                        'Use a logical heading order (H1 → H2 → H3) to improve semantic chunking.',
                    instances: [{ line, column, snippet: m[0].trim() }]
                });
                break;
            }
        }

        const hasSubHeadings = headingMatches.some(m => m[1].toLowerCase() !== 'h1');
        if (!hasSubHeadings) {
            issues.push({
                id: 'geo-headings-no-subsections',
                severity: 'minor',
                description: 'Content uses a top-level heading without subheadings.',
                recommendation:
                    'Add H2/H3 subheadings to break content into smaller, answerable sections.'
            });
        }
    }

    // Paragraph size and chunking (GEO)
    const paragraphMatches = [
        ...html.matchAll(/<p\b[^>]*>([\s\S]*?)<\/p>/gi)
    ];

    const longParagraphs = paragraphMatches
        .map(m => {
            const text = m[1].replace(/<[^>]+>/g, '').trim();
            return { match: m, text, length: text.length };
        })
        .filter(p => p.length > 1200);

    if (longParagraphs.length > 0) {
        issues.push({
            id: 'geo-paragraphs-too-long',
            severity: 'minor',
            description: `${longParagraphs.length} very long paragraph(s) detected (>1200 characters).`,
            recommendation:
                'Split oversized paragraphs into smaller, self-contained chunks. Use subheadings, lists, or definitions to improve answerability.',
            instances: longParagraphs.slice(0, 10).map(p => {
                const { line, column } = positionFromIndex(
                    lineStarts,
                    p.match.index ?? 0
                );
                return {
                    line,
                    column,
                    snippet: p.match[0].trim()
                };
            })
        });
    }

    // Lists and extractable points (GEO)
    const listMatches = [
        ...html.matchAll(/<(ul|ol)\b[^>]*>([\s\S]*?)<\/\1>/gi)
    ];

    if (listMatches.length === 0) {
        issues.push({
            id: 'geo-lists-missing',
            severity: 'info',
            description: 'No lists detected.',
            recommendation:
                'Use bullet or numbered lists to present key points, steps, or summaries for better extractability.'
        });
    } else {
        const weakLists = listMatches.filter(m => {
            const items = [...m[2].matchAll(/<li\b[^>]*>([\s\S]*?)<\/li>/gi)];
            return (
                items.length === 0 ||
                items.every(i => !i[1].replace(/<[^>]+>/g, '').trim())
            );
        });

        if (weakLists.length > 0) {
            issues.push({
                id: 'geo-lists-weak',
                severity: 'minor',
                description: `${weakLists.length} list(s) contain no meaningful list items.`,
                recommendation:
                    'Ensure lists contain multiple, descriptive list items that represent distinct points.',
                instances: weakLists.slice(0, 10).map(m => {
                    const { line, column } = positionFromIndex(
                        lineStarts,
                        m.index ?? 0
                    );
                    return {
                        line,
                        column,
                        snippet: m[0].trim()
                    };
                })
            });
        }
    }

    // Structured data / JSON-LD (GEO)
    const ldJsonMatches = [
        ...html.matchAll(
            /<script\b[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi
        )
    ];

    if (ldJsonMatches.length === 0) {
        issues.push({
            id: 'geo-structured-data-missing',
            severity: 'minor',
            description: 'No schema.org JSON-LD detected.',
            recommendation:
                'Add schema.org JSON-LD (e.g., Article, FAQPage, HowTo) to clarify entities, relationships, and intent.'
        });
    } else {
        const invalidBlocks: typeof ldJsonMatches = [];
        const untypedBlocks: typeof ldJsonMatches = [];

        ldJsonMatches.forEach(m => {
            try {
                const parsed = JSON.parse(m[1].trim());
                const hasType =
                    (Array.isArray(parsed)
                        ? parsed.some(p => p['@type'])
                        : parsed['@type']) ?? false;

                if (!hasType) {
                    untypedBlocks.push(m);
                }
            } catch {
                invalidBlocks.push(m);
            }
        });

        if (invalidBlocks.length > 0) {
            issues.push({
                id: 'geo-structured-data-invalid',
                severity: 'moderate',
                description: `${invalidBlocks.length} JSON-LD block(s) contain invalid JSON.`,
                recommendation:
                    'Ensure all JSON-LD blocks are valid JSON and parseable by search engines and LLMs.',
                instances: invalidBlocks.slice(0, 5).map(m => {
                    const { line, column } = positionFromIndex(
                        lineStarts,
                        m.index ?? 0
                    );
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }

        if (untypedBlocks.length > 0) {
            issues.push({
                id: 'geo-structured-data-missing-type',
                severity: 'minor',
                description: `${untypedBlocks.length} JSON-LD block(s) lack an @type definition.`,
                recommendation:
                    'Include a clear @type (e.g., Article, FAQPage) so entities and intent are explicit.',
                instances: untypedBlocks.slice(0, 5).map(m => {
                    const { line, column } = positionFromIndex(
                        lineStarts,
                        m.index ?? 0
                    );
                    return { line, column, snippet: m[0].trim() };
                })
            });
        }
    }

    // Link text clarity and semantic value (GEO)
    const anchorMatches = [...html.matchAll(/<a\b[^>]*>([\s\S]*?)<\/a>/gi)];

    const vaguePhrases = [
        'click here',
        'go here',
        'read more',
        'learn more',
        'see more',
        'more',
        'here'
    ];

    // Deduplicate by normalized text content
    const seenAnchors = new Set<string>();
    const vagueAnchors = anchorMatches.filter(m => {
        const text = m[1].replace(/<[^>]+>/g, '').trim().toLowerCase();
        if (!text) return true; // empty or icon-only link

        const isVague = vaguePhrases.some(p => text === p || text.startsWith(p + ' '));
        const key = m[0].trim(); // full tag as dedup key
        if (seenAnchors.has(key)) return false; // skip duplicates
        seenAnchors.add(key);
        return isVague;
    });

    if (vagueAnchors.length > 0) {
        issues.push({
            id: 'geo-vague-link-text',
            severity: 'minor',
            description: `${vagueAnchors.length} link(s) use vague or non-descriptive anchor text.`,
            recommendation:
                'Use specific, descriptive link text that reinforces entities, topics, and relationships.',
            instances: vagueAnchors.slice(0, 50).map(m => {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                return {
                    line,
                    column,
                    snippet: m[0].trim()
                };
            })
        });
    }

    // Entity definition detection (GEO)
    const entityPattern = /\b([A-Z][a-zA-Z0-9]*(?:\s+[A-Z][a-zA-Z0-9]*)*)\b/g;
    const definitionPatterns = ['is', 'refers to', 'means', 'stands for', 'represents'];

    const entityParagraphMatches = [...html.matchAll(/<p\b[^>]*>([\s\S]*?)<\/p>/gi)];
    const undefinedEntitiesMap = new Map<string, RegExpMatchArray>();

    entityParagraphMatches.forEach(p => {
        const text = p[1].replace(/<[^>]+>/g, '').trim();
        if (!text) return;

        const entitiesInPara = [...text.matchAll(entityPattern)];
        entitiesInPara.forEach(e => {
            const entity = e[1];
            const lowerText = text.toLowerCase();
            const hasDefinition = definitionPatterns.some(pattern =>
                lowerText.includes(`${entity.toLowerCase()} ${pattern}`)
            );

            if (!hasDefinition && !undefinedEntitiesMap.has(p[0])) {
                undefinedEntitiesMap.set(p[0], p);
            }
        });
    });

    const undefinedEntities = Array.from(undefinedEntitiesMap.values());
    if (undefinedEntities.length > 0) {
        issues.push({
            id: 'geo-entity-undefined',
            severity: 'moderate',
            description: `${undefinedEntities.length} entity(ies) detected without explicit definition.`,
            recommendation:
                'Ensure key entities are defined clearly on first mention (e.g., "dotCMS is a hybrid CMS…").',
            instances: undefinedEntities.slice(0, 10).map(p => {
                const { line, column } = positionFromIndex(lineStarts, p.index ?? 0);
                return {
                    line,
                    column,
                    snippet: p[0].trim()
                };
            })
        });
    }

    // FAQ / Q&A pattern detection (GEO)
    const faqHeadingMatches = [...html.matchAll(/<(h[1-6])\b[^>]*>([\s\S]*?)<\/\1>/gi)];
    const faqQuestions: { match: RegExpMatchArray; question: string }[] = [];

    faqHeadingMatches.forEach(h => {
        const headingText = h[2].replace(/<[^>]+>/g, '').trim();
        if (!headingText) return;

        // Detect headings that are questions (common FAQ style)
        if (headingText.endsWith('?') || /^what|how|why|where|when|which/i.test(headingText)) {
            faqQuestions.push({ match: h, question: headingText });
        }
    });

    // Check that questions have associated answers (paragraphs immediately after)
    const unansweredFaqs: typeof faqQuestions = [];

    faqQuestions.forEach(q => {
        // Find the position immediately after the heading
        const headingEndIndex = q.match.index! + q.match[0].length;
        const nextParagraphMatch = /<p\b[^>]*>([\s\S]*?)<\/p>/i.exec(html.slice(headingEndIndex));

        // Consider it unanswered if no paragraph or empty paragraph
        if (!nextParagraphMatch || !nextParagraphMatch[1].replace(/<[^>]+>/g, '').trim()) {
            unansweredFaqs.push(q);
        }
    });

    if (faqQuestions.length === 0) {
        issues.push({
            id: 'geo-faq-missing',
            severity: 'info',
            description: 'No FAQ-style questions detected.',
            recommendation:
                'Use headings with clear questions (What, How, Why, etc.) followed by concise answers to improve extractability for LLMs.'
        });
    }

    if (unansweredFaqs.length > 0) {
        issues.push({
            id: 'geo-faq-unanswered',
            severity: 'minor',
            description: `${unansweredFaqs.length} FAQ-style question(s) have no answer paragraph.`,
            recommendation:
                'Ensure all FAQ-style headings are followed by concise, informative answers for better LLM comprehension.',
            instances: unansweredFaqs.slice(0, 10).map(q => {
                const { line, column } = positionFromIndex(lineStarts, q.match.index ?? 0);
                return {
                    line,
                    column,
                    snippet: q.match[0].trim()
                };
            })
        });
    }

    // FAQ / Q&A pattern detection with FAQPage JSON-LD (GEO)
    // 1️⃣ Detect question headings in HTML

    const faqQuestions2: { match: RegExpMatchArray; question: string }[] = [];

    headingMatches.forEach(h => {
        const headingText = h[2].replace(/<[^>]+>/g, '').trim();
        if (!headingText) return;

        if (
            headingText.endsWith('?') ||
            /^what|how|why|where|when|which/i.test(headingText)
        ) {
            faqQuestions2.push({ match: h, question: headingText });
        }
    });

    // 2️⃣ Check that questions have associated answer paragraphs
    const unansweredFaqs2: typeof faqQuestions2 = [];

    faqQuestions2.forEach(q => {
        const headingEndIndex = q.match.index! + q.match[0].length;
        const nextParagraphMatch = /<p\b[^>]*>([\s\S]*?)<\/p>/i.exec(html.slice(headingEndIndex));

        if (!nextParagraphMatch || !nextParagraphMatch[1].replace(/<[^>]+>/g, '').trim()) {
            unansweredFaqs2.push(q);
        }
    });

    // 3️⃣ Detect FAQPage JSON-LD
    const faqJsonLdMatches = [
        ...html.matchAll(
            /<script\b[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi
        )
    ];
    const faqJsonQuestions: string[] = [];

    faqJsonLdMatches.forEach(m => {
        try {
            const parsed = JSON.parse(m[1].trim());
            const items = Array.isArray(parsed)
                ? parsed
                : parsed['@type'] === 'FAQPage'
                ? [parsed]
                : [];

            items.forEach(item => {
                if (item['@type'] === 'FAQPage' && Array.isArray(item.mainEntity)) {
                    item.mainEntity.forEach((q: any) => {
                        if (q.name) faqJsonQuestions.push(q.name.toLowerCase());
                    });
                }
            });
        } catch {
            // ignore invalid JSON-LD
        }
    });

    // 4️⃣ Cross-validate headings vs JSON-LD
    const headingFaqTextLower = faqQuestions.map(q => q.question.toLowerCase());
    const missingInJsonLd = headingFaqTextLower.filter(q => !faqJsonQuestions.includes(q));

    if (faqQuestions.length === 0 && faqJsonQuestions.length === 0) {
        issues.push({
            id: 'geo-faq-missing',
            severity: 'info',
            description: 'No FAQ-style questions detected in headings or FAQPage JSON-LD.',
            recommendation:
                'Add headings with clear questions (What, How, Why, etc.) or FAQPage JSON-LD for better LLM extractability.'
        });
    }

    if (unansweredFaqs.length > 0) {
        issues.push({
            id: 'geo-faq-unanswered',
            severity: 'minor',
            description: `${unansweredFaqs.length} FAQ-style question(s) have no answer paragraph.`,
            recommendation:
                'Ensure all FAQ-style headings are followed by concise, informative answers for better LLM comprehension.',
            instances: unansweredFaqs.slice(0, 10).map(q => {
                const { line, column } = positionFromIndex(lineStarts, q.match.index ?? 0);
                return {
                    line,
                    column,
                    snippet: q.match[0].trim()
                };
            })
        });
    }

    if (missingInJsonLd.length > 0) {
        issues.push({
            id: 'geo-faq-missing-jsonld',
            severity: 'minor',
            description: `${missingInJsonLd.length} FAQ headings are not represented in FAQPage JSON-LD.`,
            recommendation:
                'Ensure all FAQ headings are included in FAQPage JSON-LD to improve structured knowledge extraction.',
            instances: faqQuestions
                .filter(q => missingInJsonLd.includes(q.question.toLowerCase()))
                .slice(0, 10)
                .map(q => {
                    const { line, column } = positionFromIndex(lineStarts, q.match.index ?? 0);
                    return {
                        line,
                        column,
                        snippet: q.match[0].trim()
                    };
                })
        });
    }

    // Summary / TL;DR detection (GEO)
    const summaryPatterns = [
        /\bsummary\b/i,
        /\btl;dr\b/i,
        /\bkey takeaways\b/i,
        /\boverview\b/i,
        /\bconclusion\b/i
    ];

    const paragraphMatches2 = [...html.matchAll(/<p\b[^>]*>([\s\S]*?)<\/p>/gi)];
    const summarySections: { match: RegExpMatchArray; pattern: string }[] = [];

    paragraphMatches2.forEach(p => {
        const text = p[1].replace(/<[^>]+>/g, '').trim();
        if (!text) return;

        summaryPatterns.forEach(pattern => {
            if (pattern.test(text)) {
                summarySections.push({ match: p, pattern: pattern.source });
            }
        });
    });

    if (summarySections.length === 0) {
        issues.push({
            id: 'geo-summary-missing',
            severity: 'minor',
            description: 'No summary / TL;DR / key takeaways detected.',
            recommendation:
                'Add a concise summary or TL;DR section at the beginning or end of the content to improve LLM comprehension and extractability.'
        });
    } else {
        // Optional: flag very long summaries
        summarySections.forEach(section => {
            const text = section.match[1].replace(/<[^>]+>/g, '').trim();
            if (text.length > 600) {
                const { line, column } = positionFromIndex(lineStarts, section.match.index ?? 0);
                issues.push({
                    id: 'geo-summary-too-long',
                    severity: 'info',
                    description: `Summary section is very long (${text.length} characters).`,
                    recommendation:
                        'Keep summaries concise (~50–300 words) to improve clarity and LLM extractability.',
                    instances: [
                        {
                            line,
                            column,
                            snippet: section.match[0].trim()
                        }
                    ]
                });
            }
        });
    }

    // Tables / Comparison detection (GEO)
    const tableMatches = [...html.matchAll(/<table\b[^>]*>([\s\S]*?)<\/table>/gi)];

    if (tableMatches.length === 0) {
        issues.push({
            id: 'geo-tables-missing',
            severity: 'info',
            description: 'No HTML tables detected on the page.',
            recommendation:
                'Use tables for comparisons, specifications, or structured data to improve extractability and LLM comprehension.'
        });
    } else {
        tableMatches.forEach((t, index) => {
            const tableContent = t[1];
            const hasHeader = /<th\b[^>]*>/i.test(tableContent);

            if (!hasHeader) {
                const { line, column } = positionFromIndex(lineStarts, t.index ?? 0);
                issues.push({
                    id: 'geo-table-missing-th',
                    severity: 'minor',
                    description: `Table ${index + 1} has no header cells (<th>).`,
                    recommendation:
                        'Add header cells to tables to clearly define rows and columns for better machine understanding and GEO extraction.',
                    instances: [
                        {
                            line,
                            column,
                            snippet: t[0].trim()
                        }
                    ]
                });
            }

            // Optional: flag very large tables
            const numRows = (tableContent.match(/<tr\b[^>]*>/gi) || []).length;
            const numCols = (tableContent.match(/<td\b[^>]*>/gi) || []).length;

            if (numRows > 20 || numCols > 10) {
                const { line, column } = positionFromIndex(lineStarts, t.index ?? 0);
                issues.push({
                    id: 'geo-table-large',
                    severity: 'info',
                    description: `Table ${index + 1} is very large (${numRows} rows, ${numCols} cells).`,
                    recommendation:
                        'Consider splitting large tables or summarizing key points for easier extractability.',
                    instances: [
                        {
                            line,
                            column,
                            snippet: t[0].trim()
                        }
                    ]
                });
            }
        });
    }

    // Entity reinforcement detection (GEO)
    const entityPattern2 = /\b([A-Z][a-zA-Z0-9]*(?:\s+[A-Z][a-zA-Z0-9]*)*)\b/g;
    const paraTexts = [...html.matchAll(/<p\b[^>]*>([\s\S]*?)<\/p>/gi)].map(m =>
        m[1].replace(/<[^>]+>/g, '').trim()
    );

    const entityCounts: Record<string, number> = {};
    paraTexts.forEach(text => {
        const matches = text.matchAll(entityPattern2);
        for (const m of matches) {
            const entity = m[1];
            entityCounts[entity] = (entityCounts[entity] || 0) + 1;
        }
    });

    // Deduplicate under-reinforced entities
    const underReinforcedEntities = Object.entries(entityCounts)
        .filter(([_, count]) => count <= 1)
        .sort((a, b) => a[0].localeCompare(b[0]));

    if (underReinforcedEntities.length > 0) {
        const seen = new Set<string>();
        issues.push({
            id: 'geo-entity-under-reinforced',
            severity: 'minor',
            description: `${underReinforcedEntities.length} key entity(ies) mentioned only once or rarely.`,
            recommendation:
                'Mention key entities multiple times in meaningful contexts throughout the content to improve LLM extractability and comprehension.',
            instances: underReinforcedEntities.slice(0, 10).map(([entity]) => {
                if (seen.has(entity)) return null;
                seen.add(entity);
                const firstMatchIndex = html.search(new RegExp(`\\b${entity}\\b`));
                const { line, column } = positionFromIndex(lineStarts, firstMatchIndex ?? 0);
                return {
                    line,
                    column,
                    snippet: entity
                };
            }).filter(Boolean) as any[]
        });
    }

    // Language / Hreflang Validation (GEO)
    // 1️⃣ Check <html lang="">
    const htmlTagMatch = /<html\b[^>]*>/i.exec(html);
    let langAttr: string | null = null;
    if (htmlTagMatch) {
        const langMatch = /lang=["']([^"']+)["']/i.exec(htmlTagMatch[0]);
        langAttr = langMatch ? langMatch[1].toLowerCase() : null;
        if (!langAttr) {
            const { line, column } = positionFromIndex(lineStarts, htmlTagMatch.index ?? 0);
            issues.push({
                id: 'geo-lang-missing',
                severity: 'minor',
                description: '<html> tag is missing a lang attribute.',
                recommendation:
                    'Add a valid lang attribute (e.g., <html lang="en">) to improve language detection for LLMs and SEO.',
                instances: [{ line, column, snippet: htmlTagMatch[0].trim() }]
            });
        }
    }

    // 2️⃣ Check <link rel="alternate" hreflang="">
    const hreflangMatches = [...html.matchAll(
        /<link\b[^>]*rel=["']alternate["'][^>]*hreflang=["']([^"']+)["'][^>]*href=["']([^"']+)["'][^>]*>/gi
    )];

    if (hreflangMatches.length === 0) {
        issues.push({
            id: 'geo-hreflang-missing',
            severity: 'info',
            description: 'No <link rel="alternate" hreflang="..."> tags detected.',
            recommendation:
                'Add hreflang links for alternate language versions to help LLMs understand multilingual context and improve indexing.'
        });
    } else {
        // Optional: validate href and hreflang formats
        hreflangMatches.forEach((m, index) => {
            const hreflang = m[1].toLowerCase();
            const href = m[2];
            const validPattern = /^[a-z]{2}(-[A-Z]{2})?$/; // e.g., en or en-US

            if (!validPattern.test(hreflang)) {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                issues.push({
                    id: 'geo-hreflang-invalid',
                    severity: 'minor',
                    description: `Hreflang attribute "${hreflang}" in link ${index + 1} is invalid.`,
                    recommendation: 'Use a valid ISO 639-1 language code, optionally with country code (e.g., en, en-US).',
                    instances: [{ line, column, snippet: m[0].trim() }]
                });
            }

            if (!href || href.trim() === '') {
                const { line, column } = positionFromIndex(lineStarts, m.index ?? 0);
                issues.push({
                    id: 'geo-hreflang-empty-href',
                    severity: 'minor',
                    description: `Hreflang link ${index + 1} has an empty href.`,
                    recommendation: 'Ensure href points to the correct alternate page URL.',
                    instances: [{ line, column, snippet: m[0].trim() }]
                });
            }
        });
    }

    return issues;
}

export function formatGeoReport(uri: string, issues: GeoIssue[]): string {
    const lines: string[] = [];
    lines.push('GEO Analysis Report', '===================', `URI: ${uri}`, '');
    lines.push(`Summary: ${issues.length} findings`, '');
    issues.forEach((iss, i) => {
        lines.push(
            `${i + 1}. [${iss.severity.toUpperCase()}] ${iss.description}`,
            `   - Recommendation: ${iss.recommendation}`
        );
        if (iss.instances?.length) {
            lines.push(
                `   - Instances:`,
                ...iss.instances.map((inst) => {
                    const snippet = inst.snippet.replace(/\s+/g, ' ');
                    return `     • line ${inst.line}, col ${inst.column}: ${snippet}`;
                })
            );
        }
        lines.push('');
    });
    return lines.join('\n');
}

function computeLineStarts(text: string): number[] {
    const starts: number[] = [0];
    for (let i = 0; i < text.length; i++) {
        if (text.charCodeAt(i) === 10 /* \n */) {
            starts.push(i + 1);
        }
    }
    return starts;
}

function positionFromIndex(
    lineStarts: number[],
    index: number
): { line: number; column: number } {
    let lo = 0;
    let hi = lineStarts.length - 1;
    while (lo <= hi) {
        const mid = (lo + hi) >> 1;
        if (lineStarts[mid] <= index) {
            lo = mid + 1;
        } else {
            hi = mid - 1;
        }
    }
    const start = lineStarts[hi] ?? 0;
    return {
        line: (hi >= 0 ? hi : 0) + 1,
        column: index - start + 1
    };
}


