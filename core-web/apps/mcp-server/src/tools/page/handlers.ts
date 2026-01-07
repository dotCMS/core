import { Logger } from '../../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';
import { PageService } from '../../services/page';
import type { AnalyzePageParams, PageUriInput } from './index';
import {
    analyzeHtmlForWcag,
    formatWcagReport,
    analyzeHtmlForSeo,
    formatSeoReport,
    analyzeHtmlForGeo,
    formatGeoReport
} from './formatters';

const logger = new Logger('PAGE_TOOL');
const pageService = new PageService();

/**
 * Render page HTML and return the raw HTML and metadata.
 */
export async function renderPageHtmlHandler(params: PageUriInput) {
    return executeWithErrorHandling(async () => {
        logger.log('Rendering page HTML', { uri: params.uri });

        const { html, contentType } = await pageService.renderHtml({ uri: params.uri });

        logger.log('Page HTML fetched', { contentType, length: html.length });

        const text = [
            'Page Render Result',
            '==================',
            `URI: ${params.uri}`,
            `Content-Type: ${contentType}`,
            `HTML Length: ${html.length}`
        ].join('\n');

        return createSuccessResponse(text, {
            uri: params.uri,
            contentType,
            htmlLength: html.length,
            html
        });
    }, 'Error rendering page HTML');
}

export async function renderPageHtmlAndAnalyzeWcagHandler(params: AnalyzePageParams) {
    return executeWithErrorHandling(async () => {
        const wcagLevel = params.wcag_level ?? 'A';

        logger.log('Rendering page for WCAG analysis', { uri: params.uri, wcagLevel });

        const { html, contentType } = await pageService.renderHtml({ uri: params.uri });
        logger.log('Page rendered', { contentType, length: html.length });

        const findings = analyzeHtmlForWcag(html, wcagLevel);
        const text = formatWcagReport(params.uri, wcagLevel, findings);

        // Build richer structured data summary
        const impactOrder: Record<string, number> = {
            critical: 4,
            serious: 3,
            moderate: 2,
            minor: 1
        };

        const impactCounts = findings.reduce(
            (acc: Record<'minor' | 'moderate' | 'serious' | 'critical', number>, f) => {
                acc[f.impact] = (acc[f.impact] || 0) + 1;
                return acc;
            },
            { minor: 0, moderate: 0, serious: 0, critical: 0 }
        );

        const bySuccessCriterionMap = new Map<
            string,
            { successCriterion: string; guideline: string; count: number; impacts: Record<string, number> }
        >();
        findings.forEach((f) => {
            const key = f.successCriterion;
            if (!bySuccessCriterionMap.has(key)) {
                bySuccessCriterionMap.set(key, {
                    successCriterion: f.successCriterion,
                    guideline: f.guideline,
                    count: 0,
                    impacts: {}
                });
            }
            const entry = bySuccessCriterionMap.get(key)!;
            entry.count += 1;
            entry.impacts[f.impact] = (entry.impacts[f.impact] || 0) + 1;
        });

        const bySuccessCriterion = Array.from(bySuccessCriterionMap.values()).sort(
            (a, b) => b.count - a.count
        );

        const sortedFindings = [...findings].sort((a, b) => {
            const ai = impactOrder[a.impact] || 0;
            const bi = impactOrder[b.impact] || 0;
            if (bi !== ai) return bi - ai;
            return a.successCriterion.localeCompare(b.successCriterion);
        });

        logger.log('WCAG analysis completed', {
            findingCount: findings.length,
            impactCounts
        });

        return createSuccessResponse(text, {
            uri: params.uri,
            wcagLevel,
            contentType,
            htmlLength: html.length,
            summary: {
                totalFindings: findings.length,
                impactCounts
            },
            bySuccessCriterion,
            findings: sortedFindings
        });
    }, 'Error rendering page and analyzing WCAG compliance');
}

/**
 * Render page HTML and analyze basic SEO signals.
 * This uses static HTML heuristics (no JS/CSS execution).
 */
export async function renderPageHtmlAndAnalyzeSeoHandler(params: AnalyzePageParams) {
    return executeWithErrorHandling(async () => {
        logger.log('Rendering page for SEO analysis', { uri: params.uri });

        const { html, contentType } = await pageService.renderHtml({ uri: params.uri });
        logger.log('Page rendered', { contentType, length: html.length });

        const issues = analyzeHtmlForSeo(html);
        const text = formatSeoReport(params.uri, issues);

        logger.log('SEO analysis completed', { issueCount: issues.length });

        return createSuccessResponse(text, {
            uri: params.uri,
            contentType,
            htmlLength: html.length,
            issueCount: issues.length,
            issues
        });
    }, 'Error rendering page and analyzing SEO');
}

/**
 * Render page HTML and analyze GEO (Generative Engine Optimization) heuristics.
 * Checks structure and clarity signals helpful for LLM ingestion.
 */
export async function renderPageHtmlAndAnalyzeGeoHandler(params: AnalyzePageParams) {
    return executeWithErrorHandling(async () => {
        logger.log('Rendering page for GEO analysis', { uri: params.uri });

        const { html, contentType } = await pageService.renderHtml({ uri: params.uri });
        logger.log('Page rendered', { contentType, length: html.length });

        const issues = analyzeHtmlForGeo(html);
        const text = formatGeoReport(params.uri, issues);

        logger.log('GEO analysis completed', { issueCount: issues.length });

        return createSuccessResponse(text, {
            uri: params.uri,
            contentType,
            htmlLength: html.length,
            issueCount: issues.length,
            issues
        });
    }, 'Error rendering page and analyzing GEO');
}


