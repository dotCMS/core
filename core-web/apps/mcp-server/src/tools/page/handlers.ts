import { Logger } from '../../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';
import { PageService } from '../../services/page';
import type { RenderPageInput } from './index';
import { analyzeHtmlForWcag, formatWcagReport } from './formatters';

const logger = new Logger('PAGE_TOOL');
const pageService = new PageService();

export async function renderPageHtmlHandler(params: RenderPageInput) {
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


