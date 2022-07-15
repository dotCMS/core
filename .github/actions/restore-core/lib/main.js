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
const core = __importStar(require("@actions/core"));
const restore = __importStar(require("./restore"));
/**
 * Main entry point for this action.
 */
const run = () => {
    // Call module logic to restore cache locations
    const cacheMetadataInput = core.getInput('cache_metadata');
    core.info(`Using cache metadata ${cacheMetadataInput}`);
    const cacheMetadata = JSON.parse(cacheMetadataInput);
    restore.restoreLocations(cacheMetadata).then(cacheLocations => {
        const cacheLocationsOutput = JSON.stringify(cacheLocations, null, 2);
        core.info(`Found these cache locations: ${cacheLocationsOutput}`);
        core.setOutput('cache_locations', cacheLocationsOutput);
    });
};
// Run main function
run();
