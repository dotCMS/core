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


