#!/usr/bin/env node

/**
 * OpenAPI Documentation Gap Analysis Script
 * 
 * This script analyzes the dotCMS OpenAPI specification to identify endpoints
 * with missing or incomplete documentation. Output is optimized for AI parsing
 * and only includes endpoints that have issues for easy location and fixing.
 * 
 * Features:
 * - Only reports endpoints with issues (excludes perfect endpoints)
 * - AI-friendly structured output with clear file paths and fixes needed
 * - Maps endpoints to source files for direct editing
 * - Prioritizes issues by severity
 * 
 * Usage: node analyze-openapi-gaps.js [--detailed] [--format=json|table|summary|ai]
 * 
 * Prerequisites:
 * - dotCMS server running on localhost:8080
 * - Run from project root directory
 * 
 * Example workflow:
 * 1. curl -s http://localhost:8080/api/openapi.json > openapi.json
 * 2. node tools/dev-scripts/analyze-openapi-gaps.js --format=ai
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Configuration
const OPENAPI_FILE = path.join(process.cwd(), 'openapi.json');
const args = process.argv.slice(2);
const detailed = args.includes('--detailed');
const format = args.find(arg => arg.startsWith('--format='))?.split('=')[1] || 'summary';
const aiOptimized = args.includes('--ai-optimized') || format === 'ai';

// Cache for file mappings to avoid repeated searches
const fileCache = new Map();

// Colors for terminal output
const colors = {
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    magenta: '\x1b[35m',
    cyan: '\x1b[36m',
    white: '\x1b[37m',
    reset: '\x1b[0m',
    bold: '\x1b[1m'
};

function loadOpenAPI() {
    try {
        const data = fs.readFileSync(OPENAPI_FILE, 'utf8');
        return JSON.parse(data);
    } catch (error) {
        if (error.code === 'ENOENT') {
            throw new Error(`OpenAPI file not found at ${OPENAPI_FILE}. Please run: curl -s http://localhost:8080/api/openapi.json > openapi.json`);
        }
        throw new Error(`Failed to load OpenAPI file: ${error.message}`);
    }
}

/**
 * Maps endpoint to source file and method using operationId and path
 */
function findSourceLocation(endpointPath, method, operationId) {
    const cacheKey = `${method}:${endpointPath}:${operationId}`;
    
    if (fileCache.has(cacheKey)) {
        return fileCache.get(cacheKey);
    }
    
    let result = {
        sourceFile: null,
        className: null,
        methodName: null,
        confidence: 'unknown'
    };
    
    // Fast path-based mapping without file system search
    const pathSegments = endpointPath.split('/').filter(segment => segment && !segment.startsWith('{'));
    if (pathSegments.length > 1) {
        const resourceName = pathSegments[1]; // Skip /api, get main segment
        result.className = resourceName.charAt(0).toUpperCase() + resourceName.slice(1) + 'Resource';
        result.sourceFile = `dotCMS/src/main/java/com/dotcms/rest/api/v1/${resourceName}/${result.className}.java`;
        result.confidence = 'estimated';
    }
    
    // Set method name based on operationId if available
    if (operationId && operationId !== 'N/A') {
        result.methodName = operationId;
    }
    
    fileCache.set(cacheKey, result);
    return result;
}

function analyzeEndpoint(path, method, operation) {
    const issues = [];
    const warnings = [];
    const fixes = [];
    
    // Find source location
    const sourceLocation = findSourceLocation(path, method, operation.operationId);
    
    // Check for missing tags
    if (!operation.tags || operation.tags.length === 0) {
        issues.push('Missing tags');
        fixes.push({
            type: 'missing_tag',
            priority: 'high',
            annotation: '@Tag(name = "CATEGORY_NAME", description = "CATEGORY_DESCRIPTION")',
            location: 'class_level',
            note: 'Add to class and ensure tag is registered in DotRestApplication.java'
        });
    }
    
    // Check for missing operationId
    if (!operation.operationId) {
        issues.push('Missing operationId');
        fixes.push({
            type: 'missing_operation_id',
            priority: 'high',
            annotation: '@Operation(operationId = "uniqueOperationName")',
            location: 'method_level',
            note: 'Add unique identifier for this operation'
        });
    }
    
    // Check for missing summary
    if (!operation.summary) {
        warnings.push('Missing summary');
        fixes.push({
            type: 'missing_summary',
            priority: 'medium',
            annotation: '@Operation(summary = "Brief description of what this endpoint does")',
            location: 'method_level',
            note: 'Add concise summary describing the operation'
        });
    }
    
    // Check for missing description
    if (!operation.description) {
        warnings.push('Missing description');
        fixes.push({
            type: 'missing_description',
            priority: 'medium',
            annotation: '@Operation(description = "Detailed description of the operation")',
            location: 'method_level',
            note: 'Add detailed description explaining operation behavior'
        });
    }
    
    // Check for missing or generic responses
    if (!operation.responses || Object.keys(operation.responses).length === 0) {
        issues.push('Missing responses');
    } else {
        const responses = operation.responses;
        
        // Check for only default response
        if (Object.keys(responses).length === 1 && responses.default) {
            warnings.push('Only default response defined');
        }
        
        // Check for missing response descriptions
        Object.entries(responses).forEach(([code, response]) => {
            if (!response.description || response.description === 'default response') {
                warnings.push(`Response ${code} has generic/missing description`);
            }
            
            // Check for missing content types in responses
            if (response.content) {
                const contentTypes = Object.keys(response.content);
                if (contentTypes.includes('*/*')) {
                    warnings.push(`Response ${code} uses generic content type */*`);
                }
            }
        });
    }
    
    // Check for missing parameter descriptions
    if (operation.parameters) {
        operation.parameters.forEach((param, index) => {
            if (!param.description) {
                warnings.push(`Parameter ${param.name || index} missing description`);
            }
            if (!param.schema && !param.content) {
                warnings.push(`Parameter ${param.name || index} missing schema`);
            }
        });
    }
    
    // Check for missing requestBody description
    if (operation.requestBody && !operation.requestBody.description) {
        warnings.push('RequestBody missing description');
    }
    
    // Check for missing consumes (requestBody content types)
    if (operation.requestBody && operation.requestBody.content) {
        const contentTypes = Object.keys(operation.requestBody.content);
        if (contentTypes.includes('*/*')) {
            warnings.push('RequestBody uses generic content type */*');
        }
    }
    
    return {
        path,
        method: method.toUpperCase(),
        operationId: operation.operationId || 'N/A',
        tags: operation.tags || [],
        issues,
        warnings,
        fixes,
        hasIssues: issues.length > 0,
        hasWarnings: warnings.length > 0,
        score: calculateScore(operation),
        sourceLocation: sourceLocation,
        hasProblems: issues.length > 0 || warnings.length > 0
    };
}

function calculateScore(operation) {
    let score = 100;
    
    // Critical missing items (-20 points each)
    if (!operation.tags || operation.tags.length === 0) score -= 20;
    if (!operation.operationId) score -= 20;
    if (!operation.responses || Object.keys(operation.responses).length === 0) score -= 20;
    
    // Important missing items (-10 points each)
    if (!operation.summary) score -= 10;
    if (!operation.description) score -= 10;
    
    // Minor issues (-5 points each)
    if (operation.responses && Object.keys(operation.responses).length === 1 && operation.responses.default) {
        score -= 5;
    }
    
    if (operation.parameters) {
        const missingDescriptions = operation.parameters.filter(p => !p.description).length;
        score -= missingDescriptions * 2;
    }
    
    return Math.max(0, score);
}

function analyzeOpenAPI(spec) {
    const results = [];
    const summary = {
        totalEndpoints: 0,
        endpointsWithIssues: 0,
        endpointsWithWarnings: 0,
        missingTags: 0,
        missingOperationId: 0,
        missingSummary: 0,
        missingDescription: 0,
        missingResponses: 0,
        averageScore: 0
    };
    
    if (!spec.paths) {
        throw new Error('No paths found in OpenAPI specification');
    }
    
    Object.entries(spec.paths).forEach(([path, pathItem]) => {
        const methods = ['get', 'post', 'put', 'delete', 'patch', 'head', 'options'];
        
        methods.forEach(method => {
            if (pathItem[method]) {
                summary.totalEndpoints++;
                const analysis = analyzeEndpoint(path, method, pathItem[method]);
                results.push(analysis);
                
                if (analysis.hasIssues) summary.endpointsWithIssues++;
                if (analysis.hasWarnings) summary.endpointsWithWarnings++;
                
                // Count specific issues
                if (analysis.issues.includes('Missing tags')) summary.missingTags++;
                if (analysis.issues.includes('Missing operationId')) summary.missingOperationId++;
                if (analysis.warnings.includes('Missing summary')) summary.missingSummary++;
                if (analysis.warnings.includes('Missing description')) summary.missingDescription++;
                if (analysis.issues.includes('Missing responses')) summary.missingResponses++;
            }
        });
    });
    
    summary.averageScore = results.reduce((sum, r) => sum + r.score, 0) / results.length;
    
    // Filter to only include endpoints with problems for AI optimization
    const problemEndpoints = results.filter(r => r.hasProblems);
    summary.endpointsWithProblems = problemEndpoints.length;
    
    return { 
        results: aiOptimized ? problemEndpoints : results, 
        allResults: results,
        summary 
    };
}

function formatResults(analysis, format) {
    const { results, summary } = analysis;
    
    if (format === 'json') {
        return JSON.stringify(analysis, null, 2);
    }
    
    if (format === 'ai') {
        return formatAIOptimized(results, summary);
    }
    
    if (format === 'table') {
        return formatTable(results, summary);
    }
    
    // Default summary format
    return formatSummary(results, summary);
}

function formatAIOptimized(results, summary) {
    const output = {
        summary: {
            totalEndpoints: summary.totalEndpoints,
            endpointsWithProblems: results.length,
            criticalIssues: summary.endpointsWithIssues,
            warnings: summary.endpointsWithWarnings,
            averageScore: Math.round(summary.averageScore * 10) / 10
        },
        endpointsNeedingFixes: results.map(endpoint => ({
            // Endpoint identification
            path: endpoint.path,
            method: endpoint.method,
            operationId: endpoint.operationId,
            
            // Source location
            sourceFile: endpoint.sourceLocation.sourceFile,
            className: endpoint.sourceLocation.className,
            methodName: endpoint.sourceLocation.methodName,
            confidence: endpoint.sourceLocation.confidence,
            
            // Issues and priority
            score: endpoint.score,
            criticalIssues: endpoint.issues,
            warnings: endpoint.warnings,
            
            // Specific fixes needed
            fixes: endpoint.fixes,
            
            // Quick reference
            needsTag: endpoint.issues.includes('Missing tags'),
            needsSummary: endpoint.warnings.includes('Missing summary'),
            needsDescription: endpoint.warnings.includes('Missing description'),
            needsOperationId: endpoint.issues.includes('Missing operationId'),
            
            // Current state
            currentTags: endpoint.tags,
            hasAnyDocumentation: endpoint.score > 60
        })),
        
        // Aggregate statistics for AI processing
        fixPriority: {
            highPriority: results.filter(r => r.issues.length > 0).length,
            mediumPriority: results.filter(r => r.issues.length === 0 && r.warnings.length > 0).length,
            missingTagsCount: results.filter(r => r.issues.includes('Missing tags')).length,
            missingSummaryCount: results.filter(r => r.warnings.includes('Missing summary')).length
        },
        
        // Files that need updates (grouped for batch processing)
        filesToUpdate: groupEndpointsByFile(results)
    };
    
    return JSON.stringify(output, null, 2);
}

function groupEndpointsByFile(results) {
    const fileGroups = {};
    
    results.forEach(endpoint => {
        if (endpoint.sourceLocation.sourceFile) {
            const file = endpoint.sourceLocation.sourceFile;
            if (!fileGroups[file]) {
                fileGroups[file] = {
                    className: endpoint.sourceLocation.className,
                    confidence: endpoint.sourceLocation.confidence,
                    endpoints: []
                };
            }
            
            fileGroups[file].endpoints.push({
                path: endpoint.path,
                method: endpoint.method,
                operationId: endpoint.operationId,
                methodName: endpoint.sourceLocation.methodName,
                issues: endpoint.issues,
                warnings: endpoint.warnings,
                fixes: endpoint.fixes,
                score: endpoint.score
            });
        }
    });
    
    return fileGroups;
}

function formatTable(results, summary) {
    let output = '';
    
    output += `${colors.bold}${colors.cyan}OpenAPI Documentation Gap Analysis - Detailed Table${colors.reset}\n`;
    output += `${'='.repeat(120)}\n\n`;
    
    // Table header
    output += `${colors.bold}`;
    output += `${'Path'.padEnd(40)} `;
    output += `${'Method'.padEnd(8)} `;
    output += `${'Tags'.padEnd(15)} `;
    output += `${'Score'.padEnd(7)} `;
    output += `${'Issues'.padEnd(50)}`;
    output += `${colors.reset}\n`;
    output += `${'-'.repeat(120)}\n`;
    
    // Sort by score (lowest first to highlight worst endpoints)
    const sortedResults = results.sort((a, b) => a.score - b.score);
    
    sortedResults.forEach(result => {
        const color = result.score < 50 ? colors.red : 
                     result.score < 70 ? colors.yellow : 
                     result.score < 90 ? colors.blue : colors.green;
        
        const issues = [...result.issues, ...result.warnings].join(', ') || 'None';
        const tags = result.tags.join(', ') || 'None';
        
        output += `${color}`;
        output += `${result.path.substring(0, 39).padEnd(40)} `;
        output += `${result.method.padEnd(8)} `;
        output += `${tags.substring(0, 14).padEnd(15)} `;
        output += `${result.score.toString().padEnd(7)} `;
        output += `${issues.substring(0, 49)}`;
        output += `${colors.reset}\n`;
    });
    
    output += `\n${formatSummary(results, summary)}`;
    
    return output;
}

function formatSummary(results, summary) {
    let output = '';
    
    output += `${colors.bold}${colors.cyan}OpenAPI Documentation Gap Analysis Summary${colors.reset}\n`;
    output += `${'='.repeat(60)}\n\n`;
    
    // Overall statistics
    output += `${colors.bold}Overall Statistics:${colors.reset}\n`;
    output += `  Total Endpoints: ${colors.yellow}${summary.totalEndpoints}${colors.reset}\n`;
    output += `  Endpoints with Critical Issues: ${colors.red}${summary.endpointsWithIssues}${colors.reset} (${(summary.endpointsWithIssues/summary.totalEndpoints*100).toFixed(1)}%)\n`;
    output += `  Endpoints with Warnings: ${colors.yellow}${summary.endpointsWithWarnings}${colors.reset} (${(summary.endpointsWithWarnings/summary.totalEndpoints*100).toFixed(1)}%)\n`;
    output += `  Average Documentation Score: ${getScoreColor(summary.averageScore)}${summary.averageScore.toFixed(1)}/100${colors.reset}\n\n`;
    
    // Specific issues breakdown
    output += `${colors.bold}Critical Issues Breakdown:${colors.reset}\n`;
    output += `  Missing Tags: ${colors.red}${summary.missingTags}${colors.reset}\n`;
    output += `  Missing Operation ID: ${colors.red}${summary.missingOperationId}${colors.reset}\n`;
    output += `  Missing Responses: ${colors.red}${summary.missingResponses}${colors.reset}\n\n`;
    
    output += `${colors.bold}Warning Issues Breakdown:${colors.reset}\n`;
    output += `  Missing Summary: ${colors.yellow}${summary.missingSummary}${colors.reset}\n`;
    output += `  Missing Description: ${colors.yellow}${summary.missingDescription}${colors.reset}\n\n`;
    
    // Worst endpoints
    const worstEndpoints = results
        .filter(r => r.score < 70)
        .sort((a, b) => a.score - b.score)
        .slice(0, 10);
    
    if (worstEndpoints.length > 0) {
        output += `${colors.bold}Top ${Math.min(10, worstEndpoints.length)} Endpoints Needing Attention:${colors.reset}\n`;
        worstEndpoints.forEach((endpoint, index) => {
            const color = endpoint.score < 50 ? colors.red : colors.yellow;
            output += `  ${index + 1}. ${color}${endpoint.method} ${endpoint.path}${colors.reset} `;
            output += `(Score: ${getScoreColor(endpoint.score)}${endpoint.score}${colors.reset})\n`;
            
            if (detailed) {
                endpoint.issues.forEach(issue => {
                    output += `     ${colors.red}• ${issue}${colors.reset}\n`;
                });
                endpoint.warnings.forEach(warning => {
                    output += `     ${colors.yellow}• ${warning}${colors.reset}\n`;
                });
            }
        });
        output += '\n';
    }
    
    // Recommendations
    output += `${colors.bold}Recommendations:${colors.reset}\n`;
    
    if (summary.missingTags > 0) {
        output += `  ${colors.red}• Add @Tag annotations to ${summary.missingTags} endpoints${colors.reset}\n`;
    }
    
    if (summary.missingOperationId > 0) {
        output += `  ${colors.red}• Add operationId to ${summary.missingOperationId} endpoints${colors.reset}\n`;
    }
    
    if (summary.missingSummary > 10) {
        output += `  ${colors.yellow}• Add @Operation(summary="...") to ${summary.missingSummary} endpoints${colors.reset}\n`;
    }
    
    if (summary.missingDescription > 10) {
        output += `  ${colors.yellow}• Add detailed descriptions to ${summary.missingDescription} endpoints${colors.reset}\n`;
    }
    
    if (summary.averageScore < 80) {
        output += `  ${colors.blue}• Focus on improving documentation quality (current average: ${summary.averageScore.toFixed(1)}/100)${colors.reset}\n`;
    }
    
    return output;
}

function getScoreColor(score) {
    if (score >= 90) return colors.green;
    if (score >= 70) return colors.blue;
    if (score >= 50) return colors.yellow;
    return colors.red;
}

function main() {
    try {
        if (format !== 'ai' && format !== 'json') {
            console.log(`${colors.cyan}Loading OpenAPI specification from ${OPENAPI_FILE}...${colors.reset}`);
        }
        
        const spec = loadOpenAPI();
        
        if (format !== 'ai' && format !== 'json') {
            console.log(`${colors.green}✓ Successfully loaded OpenAPI specification${colors.reset}\n`);
        }
        
        const analysis = analyzeOpenAPI(spec);
        const output = formatResults(analysis, format);
        
        console.log(output);
        
        // Exit code based on critical issues (only for non-AI formats)
        if (format !== 'ai' && format !== 'json' && analysis.summary.endpointsWithIssues > 0) {
            process.exit(1);
        }
        
    } catch (error) {
        console.error(`${colors.red}Error: ${error.message}${colors.reset}`);
        process.exit(1);
    }
}

// Help text
if (args.includes('--help') || args.includes('-h')) {
    console.log(`
${colors.bold}OpenAPI Documentation Gap Analysis Script${colors.reset}

${colors.bold}Usage:${colors.reset}
  node tools/dev-scripts/analyze-openapi-gaps.js [options]

${colors.bold}Options:${colors.reset}
  --detailed              Show detailed issues for each endpoint
  --format=<type>         Output format: summary (default), table, json, ai
  --ai-optimized          Enable AI-optimized output (same as --format=ai)
  --help, -h              Show this help message

${colors.bold}Output Formats:${colors.reset}
  summary                 Human-readable summary with recommendations
  table                   Detailed table view of all endpoints
  json                    Complete raw analysis data
  ai                      AI-optimized structured data with source file mapping

${colors.bold}Examples:${colors.reset}
  # Basic analysis
  node tools/dev-scripts/analyze-openapi-gaps.js
  
  # Detailed view
  node tools/dev-scripts/analyze-openapi-gaps.js --detailed
  
  # Table format
  node tools/dev-scripts/analyze-openapi-gaps.js --format=table
  
  # Export JSON report
  node tools/dev-scripts/analyze-openapi-gaps.js --format=json > gaps-report.json
  
  # AI-optimized output for automated processing
  node tools/dev-scripts/analyze-openapi-gaps.js --format=ai > ai-analysis.json

${colors.bold}Prerequisites:${colors.reset}
  1. dotCMS server running on localhost:8080
  2. Run from project root directory
  3. Download OpenAPI spec first:
     curl -s http://localhost:8080/api/openapi.json > openapi.json

${colors.bold}Exit Codes:${colors.reset}
  0 - No critical issues found
  1 - Critical issues found (missing tags, operationId, or responses)
`);
    process.exit(0);
}

main();