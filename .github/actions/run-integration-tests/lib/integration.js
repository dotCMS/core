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
Object.defineProperty(exports, "__esModule", { value: true });
exports.runTests = exports.COMMANDS = void 0;
const core = __importStar(require("@actions/core"));
const exec = __importStar(require("@actions/exec"));
const path = __importStar(require("path"));
const setup = __importStar(require("./it-setup"));
// import * as shelljs from 'shelljs'
/**
 * Based on dbType resolves the ci index
 *
 * @returns index based on provided db type
 */
const resolveCiIndex = () => {
    if (dbType === 'postgres') {
        return 0;
    }
    else if (dbType === 'mssql') {
        return 1;
    }
    return -1;
};
const buildEnv = core.getInput('build_env');
const projectRoot = core.getInput('project_root');
const workspaceRoot = path.dirname(projectRoot);
const dotCmsRoot = path.join(projectRoot, 'dotCMS');
const dbType = core.getInput('db_type');
const runtTestsPrefix = 'integration-tests:';
const dockerFolder = `${projectRoot}/cicd/docker`;
const outputDir = `${dotCmsRoot}/build/test-results/integrationTest`;
const reportDir = `${dotCmsRoot}/build/reports/tests/integrationTest`;
const ciIndex = resolveCiIndex();
const DEPS_ENV = {
    postgres: {
        POSTGRES_USER: 'postgres',
        POSTGRES_PASSWORD: 'postgres',
        POSTGRES_DB: 'dotcms'
    }
};
exports.COMMANDS = {
    gradle: {
        cmd: './gradlew',
        args: ['integrationTest', `-PdatabaseType=${dbType}`],
        workingDir: dotCmsRoot,
        outputDir: outputDir,
        reportDir: reportDir,
        ciIndex: ciIndex
    },
    maven: {
        cmd: './mvnw',
        args: ['integrationTest', `-PdatabaseType=${dbType}`],
        workingDir: dotCmsRoot,
        outputDir: outputDir,
        reportDir: reportDir,
        ciIndex: ciIndex
    }
};
const START_DEPENDENCIES_CMD = {
    cmd: 'docker-compose',
    args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'up'],
    workingDir: dockerFolder,
    env: DEPS_ENV[dbType]
};
const STOP_DEPENDENCIES_CMD = {
    cmd: 'docker-compose',
    args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'down'],
    workingDir: dockerFolder,
    env: DEPS_ENV[dbType]
};
/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run integration tests.
 *
 * @param cmd resolved command
 * @returns a number representing the command exit code
 */
const runTests = (cmd) => __awaiter(void 0, void 0, void 0, function* () {
    // Setup ITs
    setup.setupTests(propertyMap());
    // Starting dependencies
    core.info(`
    =======================================
    Starting integration tests dependencies
    =======================================`);
    execCmdAsync(START_DEPENDENCIES_CMD);
    yield waitFor(60, `ES and ${dbType}`);
    // Executes ITs
    resolveParams(cmd);
    core.info(`
    ===========================================
    Running integration tests against ${dbType}
    ===========================================`);
    let itCode;
    try {
        itCode = yield execCmd(cmd);
        core.info(`
      ===========================================
      Integration tests have finished to run
      ===========================================`);
        return itCode;
    }
    catch (err) {
        core.setFailed(`Running integration tests failed due to ${err}`);
        return 127;
    }
    finally {
        stopDeps();
    }
});
exports.runTests = runTests;
/**
 * Stops dependencies.
 */
const stopDeps = () => __awaiter(void 0, void 0, void 0, function* () {
    // Stopping dependencies
    core.info(`
    =======================================
    Stopping integration tests dependencies
    =======================================`);
    try {
        yield execCmd(STOP_DEPENDENCIES_CMD);
    }
    catch (err) {
        console.error(`Error stopping dependencies: ${err}`);
    }
});
/**
 * Creates property map with DotCMS property information to override/append
 *
 * @returns map with property data
 */
const propertyMap = () => {
    const properties = new Map();
    properties.set('dotSecureFolder', appendToWorkspace('custom/dotsecure'));
    properties.set('dotCmsFolder', dotCmsRoot);
    properties.set('felixFolder', appendToWorkspace('custom/felix'));
    properties.set('assetsFolder', appendToWorkspace('custom/assets'));
    properties.set('esDataFolder', appendToWorkspace('custom/esdata'));
    properties.set('logsFolder', dotCmsRoot);
    properties.set('dbType', dbType);
    return properties;
};
/**
 * Append folder to workspace
 *
 * @param folder folder to append to workspace
 * @returns workspace + folder
 */
const appendToWorkspace = (folder) => path.join(workspaceRoot, folder);
/**
 * Resolve paramateres to produce command arguments
 *
 * @param cmd {@link Command} object holding command and arguments
 */
const resolveParams = (cmd) => {
    var _a;
    const tests = (_a = core.getInput('tests')) === null || _a === void 0 ? void 0 : _a.trim();
    if (!tests) {
        addFallbackTest(cmd.args);
        return;
    }
    core.info(`Commit message found: "${tests}"`);
    const resolved = [];
    for (const l of tests.split('\n')) {
        const line = l.trim();
        if (!line.toLowerCase().startsWith(runtTestsPrefix)) {
            continue;
        }
        const testLine = line.slice(runtTestsPrefix.length).trim();
        if (buildEnv === 'gradle') {
            for (const test of testLine.split(',')) {
                resolved.push('--tests');
                resolved.push(test.trim());
            }
        }
        else if (buildEnv === 'maven') {
            const normalized = testLine
                .split(',')
                .map(t => t.trim())
                .join(',');
            resolved.push(`-Dit.test=${normalized}`);
        }
    }
    if (resolved.length === 0) {
        addFallbackTest(resolved);
    }
    cmd.args.push(...resolved);
    core.info(`Resolved params ${cmd.args.join(' ')}`);
};
const addFallbackTest = (tests) => {
    core.info('No specific integration tests found using MainSuite');
    if (buildEnv === 'gradle') {
        tests.push('-Dtest.single=com.dotcms.MainSuite');
    }
    else if (buildEnv === 'maven') {
        tests.push('-Dit.test=com.dotcms.MainSuite');
    }
};
/**
 * Delays the resolve part of a promise to simulate a sleep
 *
 * @param seconds number of seconds
 * @returns void promise
 */
const delay = (seconds) => new Promise(resolve => setTimeout(resolve, seconds * 1000));
/**
 * Waits for specific time with corresponding messages.
 *
 * @param wait time to wait
 * @param startLabel start label
 * @param endLabel endlabel
 */
const waitFor = (wait, startLabel, endLabel) => __awaiter(void 0, void 0, void 0, function* () {
    core.info(`Waiting ${wait} seconds for ${startLabel}`);
    yield delay(wait);
    const finalLabel = endLabel || startLabel;
    core.info(`Waiting on ${finalLabel} loading has ended`);
});
const printCmd = (cmd) => {
    let message = `Executing cmd: ${cmd.cmd} ${cmd.args.join(' ')}`;
    if (cmd.workingDir) {
        message += `\ncwd: ${cmd.workingDir}`;
    }
    if (cmd.env) {
        message += `\nenv: ${JSON.stringify(cmd.env, null, 2)}`;
    }
    core.info(message);
};
const execCmd = (cmd) => __awaiter(void 0, void 0, void 0, function* () {
    printCmd(cmd);
    return yield exec.exec(cmd.cmd, cmd.args, { cwd: cmd.workingDir, env: cmd.env });
});
const execCmdAsync = (cmd) => {
    printCmd(cmd);
    //shelljs.exec([cmd.cmd, ...cmd.args].join(' '), {async: true})
    exec.exec(cmd.cmd, cmd.args, { cwd: cmd.workingDir, env: cmd.env });
};
