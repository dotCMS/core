"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.aggregate = void 0;
const core = __importStar(require("@actions/core"));
const inputTypes = ['unit', 'integration_postgres', 'integration_mssql', 'postman'];
const typeLabels = {
    unit: 'Unit Tests',
    integration_postgres: 'Integration Tests [Postgres]',
    integration_mssql: 'Integration Tests [MSSQL]',
    postman: 'Postman Tests'
};
const PASSED = 'PASSED';
const FAILED = 'FAILED';
const aggregate = () => {
    let status = PASSED;
    let color = '#5E7D00';
    const messages = [];
    const slackUser = resolveSlackUser();
    if (slackUser) {
        messages.push(`Hey <@${slackUser}>, your tests have run:`);
    }
    for (const type of inputTypes) {
        const testsStatus = resolveInputs(type);
        if (status === PASSED && (!testsStatus.status || testsStatus.status === FAILED)) {
            status = FAILED;
            color = '#ff2400';
        }
        const emoji = testsStatus.status === PASSED ? ':sunny: ' : ':thunder_cloud_and_rain: ';
        const reportUrls = [
            testsStatus.reportUrl ? `<${testsStatus.reportUrl}|Github>` : '',
            testsStatus.testmoReportUrl ? `<${testsStatus.testmoReportUrl}|Testmo>` : ''
        ].filter(url => !!url);
        const logUrl = testsStatus.logUrl || '';
        const message = `${emoji}*${typeLabels[type]}*`;
        const reportChunk = reportUrls.length > 0 ? `${reportUrls.join(' | ')}` : 'Report unavailable';
        const logChunk = logUrl ? `<${logUrl}|Log>` : 'Log unavailable';
        messages.push(`${message}: ${reportChunk} | ${logChunk}`);
    }
    return {
        status: `Tests status: ${status}`,
        color,
        message: messages.join('\n')
    };
};
exports.aggregate = aggregate;
const resolveInputs = (type) => {
    return {
        status: core.getInput(`${type}_tests_results_status`),
        reportUrl: core.getInput(`${type}_tests_results_report_url`),
        logUrl: core.getInput(`${type}_tests_results_log_url`),
        testmoReportUrl: core.getInput(`testmo_${type}_tests_results_report_url`)
    };
};
const resolveSlackUser = () => {
    const githubUser = core.getInput('github_user') || '';
    core.info(`Detected user ${githubUser}`);
    const confStr = core.getInput('github_slack_conf') || '{}';
    const githubSlackConf = JSON.parse(confStr);
    core.info(`Provided conf:\n${confStr}`);
    const resolved = githubSlackConf[githubUser] || '';
    core.info(`Resolved slack user id ${resolved}`);
    return resolved;
};
