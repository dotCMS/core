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
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const buildEnv = core.getInput('build_env');
const projectRoot = core.getInput('project_root');
const dotCmsRoot = path.join(projectRoot, 'dotCMS');
const testResources = ['log4j2.xml'];
const srcTestResourcesFolder = 'cicd/resources';
const targetTestResourcesFolder = 'dotCMS/src/test/resources';
const runtTestsPrefix = 'unit-tests:';
const outputDir = `${dotCmsRoot}/build/test-results/test`;
const reportDir = `${dotCmsRoot}/build/reports/tests/test`;
exports.COMMANDS = {
    gradle: {
        cmd: './gradlew',
        args: ['test'],
        workingDir: dotCmsRoot,
        outputDir: outputDir,
        reportDir: reportDir
    },
    maven: {
        cmd: './mvnw',
        args: ['test'],
        workingDir: dotCmsRoot,
        outputDir: outputDir,
        reportDir: reportDir
    }
};
/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run unit tests.
 *
 * @param cmd resolved command
 * @returns a number representing the command exit code
 */
const runTests = (cmd) => __awaiter(void 0, void 0, void 0, function* () {
    prepareTests();
    resolveParams(cmd);
    core.info(`
    ==================
    Running unit tests
    ==================`);
    core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`);
    try {
        return yield exec.exec(cmd.cmd, cmd.args, { cwd: cmd.workingDir });
    }
    catch (err) {
        core.setFailed(`Unit tests failed due to: ${err}`);
        return 127;
    }
});
exports.runTests = runTests;
/**
 * Prepares tests by copying necessary files into workspace
 */
const prepareTests = () => {
    core.info('Preparing unit tests');
    testResources.forEach(res => {
        const source = path.join(projectRoot, srcTestResourcesFolder, res);
        const dest = path.join(projectRoot, targetTestResourcesFolder, res);
        core.info(`Copying resource ${source} to ${dest}`);
        fs.copyFileSync(source, dest);
    });
};
/**
 * Add extra parameters to command arguments
 *
 * @param cmd {@link Command} object holding command and arguments
 */
const resolveParams = (cmd) => {
    var _a;
    const tests = (_a = core.getInput('tests')) === null || _a === void 0 ? void 0 : _a.trim();
    if (!tests) {
        core.info('No specific unit tests found');
        return;
    }
    core.info(`Commit message found: "${tests}"`);
    tests.split('\n').forEach(l => {
        const line = l.trim();
        if (!line.toLowerCase().startsWith(runtTestsPrefix)) {
            return;
        }
        const testLine = line.slice(runtTestsPrefix.length).trim();
        if (buildEnv === 'gradle') {
            testLine.split(',').forEach(test => {
                cmd.args.push('--tests');
                cmd.args.push(test.trim());
            });
        }
        else if (buildEnv === 'maven') {
            const normalized = testLine
                .split(',')
                .map(t => t.trim())
                .join(',');
            cmd.args.push(`-Dit.test=${normalized}`);
        }
    });
    core.info(`Resolved params ${cmd.args.join(' ')}`);
};
