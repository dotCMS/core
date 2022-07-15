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
exports.build = void 0;
const core = __importStar(require("@actions/core"));
const exec = __importStar(require("@actions/exec"));
const path = __importStar(require("path"));
const gradleCmd = './gradlew';
const mavenCmd = './mvnw';
const projectRoot = core.getInput('project_root');
const dotCmsRoot = path.join(projectRoot, 'dotCMS');
const COMMANDS = {
    gradle: [
        {
            cmd: gradleCmd,
            args: ['createDistPrep', 'compileIntegrationTestJava', 'prepareIntegrationTests'],
            workingDir: dotCmsRoot
        }
    ],
    maven: [
        {
            cmd: mavenCmd,
            args: ['package', '-DskipTests'],
            workingDir: dotCmsRoot
        }
    ]
};
/**
 * Based on a detected build environment, that is gradle or maven, this resolves the command to run in order to build core.
 *
 * @param buildEnv build environment
 * @returns a number representing the command exit code
 */
const build = (buildEnv) => __awaiter(void 0, void 0, void 0, function* () {
    const cmds = COMMANDS[buildEnv];
    if (!cmds || cmds.length === 0) {
        core.error('Cannot resolve build tool, aborting');
        return Promise.resolve(127);
    }
    let rc = 0;
    for (const cmd of cmds) {
        core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`);
        rc = yield exec.exec(cmd.cmd, cmd.args, { cwd: cmd.workingDir });
        if (rc !== 0) {
            break;
        }
    }
    return rc;
});
exports.build = build;
