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
const core = __importStar(require("@actions/core"));
const fs = __importStar(require("fs"));
const integration = __importStar(require("./integration"));
const buildEnv = core.getInput('build_env');
const dbType = core.getInput('db_type');
/**
 * Main entry point for this action.
 */
const run = () => __awaiter(void 0, void 0, void 0, function* () {
    core.info("Running Core's integration tests");
    const cmd = integration.COMMANDS[buildEnv];
    if (!cmd) {
        core.error('Cannot resolve build tool, aborting');
        return;
    }
    const exitCode = yield integration.runTests(cmd);
    const skipReport = !(cmd.outputDir && fs.existsSync(cmd.outputDir));
    setOutput('tests_results_location', cmd.outputDir);
    setOutput('tests_results_report_location', cmd.reportDir);
    setOutput('ci_index', cmd.ciIndex);
    setOutput('tests_results_status', exitCode === 0 ? 'PASSED' : 'FAILED');
    setOutput('tests_results_skip_report', skipReport);
    setOutput(`${dbType}_tests_results_status`, exitCode === 0 ? 'PASSED' : 'FAILED');
    setOutput(`${dbType}_tests_results_skip_report`, skipReport);
});
const setOutput = (name, value) => {
    const val = value === undefined ? '' : value;
    core.notice(`Setting output '${name}' with value: '${val}'`);
    core.setOutput(name, value);
};
// Run main function
run();
