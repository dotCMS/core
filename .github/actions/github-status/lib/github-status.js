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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.send = void 0;
const core = __importStar(require("@actions/core"));
const node_fetch_1 = __importDefault(require("node-fetch"));
/**
 * Sends the tests results statsus to Github using its API.
 *
 * @param testType test type
 * @param dbType database type
 * @param testResultsStatus test results status (PASSED or FAILED)
 */
const send = (testType, dbType, testResultsStatus) => __awaiter(void 0, void 0, void 0, function* () {
    const pullRequest = core.getInput('pull_request');
    if (!pullRequest) {
        core.warning(`This was not triggered from a pull request, so skipping sending status `);
        return;
    }
    const githubUser = core.getInput('github_user');
    const cicdGithubToken = core.getInput('cicd_github_token');
    const creds = `${githubUser}:${cicdGithubToken}`;
    const pullRequestUrl = `https://api.github.com/repos/dotCMS/core/pulls/${pullRequest}`;
    const prResponse = yield getPullRequest(pullRequestUrl, creds);
    if (!prResponse.ok) {
        core.warning(`Could not get Github pull request ${pullRequest}`);
        return;
    }
    const pr = (yield prResponse.json());
    const testsReportUrl = core.getInput('tests_report_url');
    const status = createStatus(testType, dbType, testResultsStatus, testsReportUrl);
    const statusResponse = yield postStatus(pr._links.statuses.href, creds, status);
    if (!statusResponse.ok) {
        core.warning(`Could not send Github status for ${testType} tests`);
        return;
    }
});
exports.send = send;
/**
 * Resolves what label to use based on the test type.
 *
 * @param testType test type
 * @param dbType database type
 * @returns status label
 */
const resolveStastusLabel = (testType, dbType) => {
    switch (testType) {
        case 'unit':
            return '[Unit tests results]';
        case 'integration':
            return `[Integration tests results] - [${dbType}]`;
        case 'postman':
            return '[Postman tests results]';
        default:
            return '';
    }
};
/**
 * Creates a status object based on the provided params.
 *
 * @param testType test type
 * @param dbType database type
 * @param testResultsStatus test results status
 * @param testsReportUrl report url where tests results are located
 * @returns {@link GithubStatus} object to be used when reporting
 */
const createStatus = (testType, dbType, testResultsStatus, testsReportUrl) => {
    let statusLabel;
    let description;
    if (testResultsStatus === 'PASSED') {
        statusLabel = 'success';
        description = 'Tests executed SUCCESSFULLY';
    }
    else {
        statusLabel = 'failure';
        description = 'Tests FAILED';
    }
    return {
        state: statusLabel,
        description,
        target_url: testsReportUrl,
        context: `Github Actions - ${resolveStastusLabel(testType, dbType)}`
    };
};
/**
 * Creates headers object to be used with Github API.
 *
 * @param creds base 64 encoded username:password credentials
 * @returns headers object
 */
const headers = (creds) => {
    return {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Basic ${Buffer.from(creds).toString('base64')}`
    };
};
/**
 * Uses fetch function to send GET http request to Github API to get pull request data.
 *
 * @param pullRequestUrl pull request API url
 * @param creds base 64 encoded username:password credentials
 * @returns {@link Response} object
 */
const getPullRequest = (pullRequestUrl, creds) => __awaiter(void 0, void 0, void 0, function* () {
    core.info(`Sending GET to ${pullRequestUrl}`);
    const response = yield (0, node_fetch_1.default)(pullRequestUrl, {
        method: 'GET',
        headers: headers(creds)
    });
    return response;
});
/**
 * Uses fetch function to send POST http request to Github API to report status
 *
 * @param statusUrl status report API url
 * @param creds base 64 encoded username:password credentials
 * @param status status object
 * @returns {@link Response} object
 */
const postStatus = (statusUrl, creds, status) => __awaiter(void 0, void 0, void 0, function* () {
    core.info(`Sending POST to ${statusUrl} the payload:\n${JSON.stringify(status, null, 2)}`);
    const response = yield (0, node_fetch_1.default)(statusUrl, {
        method: 'POST',
        headers: headers(creds),
        body: JSON.stringify(status)
    });
    core.info(`Got response:\n${JSON.stringify(response, null, 2)}`);
    return response;
});
