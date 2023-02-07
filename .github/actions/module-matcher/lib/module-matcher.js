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
exports.moduleMatches = void 0;
const core = __importStar(require("@actions/core"));
const node_fetch_1 = __importDefault(require("node-fetch"));
const shelljs = __importStar(require("shelljs"));
/**
 * Resolves which field to use
 *
 * @param module {@link ModuleConf} module to get info from
 * @returns value in resolved field
 */
const location = (module) => module.folder || module.module;
const buildId = core.getInput('build_id');
const current = core.getInput('current');
const modulesConf = JSON.parse(core.getInput('modules'));
const childModules = modulesConf
    .filter(m => m.module !== current && m.parent === current)
    .map(m => location(m));
const pullRequest = core.getInput('pull_request');
const commit = core.getInput('commit');
/**
 * Discover modules that were "touched" by changed contained either in pull request or commit
 *
 * @returns true if module was found
 */
const moduleMatches = () => __awaiter(void 0, void 0, void 0, function* () {
    validateConf();
    core.info(`Provided current module: ${current}`);
    core.info(`Provided modules configuration: ${JSON.stringify(modulesConf, null, 2)}`);
    const currentModule = modulesConf.find(conf => conf.module === current);
    if (!currentModule) {
        core.error(`Module ${current} was not found in configuration`);
        return false;
    }
    if (/^release-\d{2}\.\d{2}(\.\d{1,2})?$|master/.test(buildId)) {
        core.info('Master o release branch detected, allowing workflow to run');
        return true;
    }
    const commits = yield resolveCommits();
    core.info(`Commits found: ${commits.length}`);
    if (commits.length >= 100) {
        // Probably not found bu the amount of commits definitively calls for returning true
        core.info('Commits reached max capacity, allowing workflow to run');
        return true;
    }
    const found = searchInCommits(currentModule, commits);
    found
        ? core.info(`Current module ${current} matched with changes`)
        : core.warning(`Could not match module ${current} with changes, discarding it...`);
    return found;
});
exports.moduleMatches = moduleMatches;
/**
 * Validates provided module configuration
 *
 * @returns {@link ModuleConf} object if found otherwise undefined
 */
const validateConf = () => {
    const main = modulesConf.find(conf => !!conf.main);
    if (!main) {
        throw new Error(`No main module was found at modules configuration: ${JSON.stringify(modulesConf, null, 2)}`);
    }
    return main;
};
/**
 * Searches in given commit list if module appears inside them
 *
 * @param module {@link ModuleConf} instance to search
 * @param commits list of commits
 * @returns true if found, otherwise false
 */
const searchInCommits = (module, commits) => {
    var _a;
    for (const sha of commits) {
        const cmd = `git diff-tree --no-commit-id --name-only -r ${sha}`;
        core.info(`Searching in commit ${sha} by running:\n${cmd}`);
        const output = ((_a = shelljs.exec(cmd)) === null || _a === void 0 ? void 0 : _a.stdout) || '';
        const changed = output.split('\n');
        if (searchInChanges(module, changed)) {
            return true;
        }
    }
    return false;
};
/**
 * Searches in given commit changes if module appears in changes
 *
 * @param module {@link ModuleConf} instance to search
 * @param changed list of changes
 * @returns true if found, otherwise false
 */
const searchInChanges = (module, changed) => {
    for (const change of changed) {
        const normalized = change.trim();
        if (doesCurrentMatch(module, normalized)) {
            core.info(`Found modified module ${JSON.stringify(module, null, 2)} from change at ${normalized}`);
            return true;
        }
    }
    return false;
};
/**
 * If a pull request is detected then return the list of commits included in PR.
 * It a push is detected use actual commit that triggered run.
 *
 * @returns commits as a list of string
 */
const resolveCommits = () => __awaiter(void 0, void 0, void 0, function* () {
    if (pullRequest) {
        core.info(`Provided pull request: ${pullRequest}`);
        const response = yield getPullRequestCommits();
        if (!response.ok) {
            core.warning(`Could not get Github pull request ${pullRequest}`);
            return [];
        }
        const commits = JSON.parse(yield response.text()).map(c => c.sha);
        core.info(`Found pull request ${pullRequest} commits:\n${commits.join(', ')}`);
        return commits;
    }
    else if (commit) {
        core.info(`Found (push) commit: ${commit}`);
        return yield Promise.resolve([commit]);
    }
    else {
        core.warning('No commits found');
        return yield Promise.resolve([]);
    }
});
/**
 * Uses fetch function to send GET http request to Github API to get pull request commits data.
 *
 * @returns {@link Response} object
 */
const getPullRequestCommits = () => __awaiter(void 0, void 0, void 0, function* () {
    const url = `https://api.github.com/repos/dotCMS/core/pulls/${pullRequest}/commits?per_page=250`;
    core.info(`Sending GET to ${url}`);
    const response = yield (0, node_fetch_1.default)(url, { method: 'GET' });
    core.info(`Got response: ${response.status}`);
    return response;
});
/**
 * Evaluates if module is included in change for the cases when the module is configured as main and when is a "child" module.
 *
 * @param module {@link ModuleConf} module instance
 * @param change line of change from commit
 * @returns true it matches, otherwise false
 */
const doesCurrentMatch = (module, change) => {
    if (!!module.main) {
        const folder = module.folder || '.';
        const list = shelljs
            .ls('-A', folder)
            .filter(file => !childModules.find(cm => file.startsWith(cm)));
        return !!list.find(file => change.startsWith(file));
    }
    return change.startsWith(location(module));
};
