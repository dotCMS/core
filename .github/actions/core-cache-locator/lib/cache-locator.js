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
exports.getCacheLocations = void 0;
const core = __importStar(require("@actions/core"));
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const CACHE_CONFIGURATION = {
    gradle: {
        dependencies: ['~/.gradle/caches', '~/.gradle/wrapper'],
        buildOutput: ['dotCMS/.gradle', 'dotCMS/build/classes', 'dotCMS/build/resources']
    },
    maven: {
        dependencies: ['~/.m2/repository'],
        buildOutput: ['dotCMS/target']
    }
};
const EMPTY_CACHE_LOCATIONS = {
    dependencies: [],
    buildOutput: []
};
const BUILD_OUTPUT = 'buildOutput';
const CACHE_FOLDER = path.join(path.dirname(core.getInput('project_root')), 'cache');
/**
 * Resolves locations to be cached after building core.
 *
 * @returns a {@link CacheLocation} instance with the information of what is going to be cached
 */
const getCacheLocations = () => {
    // Resolves build environment
    const buildEnv = core.getInput('build_env');
    core.info(`Resolving cache locations with buid env ${buildEnv}`);
    const cacheLocations = CACHE_CONFIGURATION[buildEnv];
    if (!cacheLocations) {
        core.warning(`Cannot find cache configuration for build env ${buildEnv}`);
        return EMPTY_CACHE_LOCATIONS;
    }
    // For each cache location resolves the location to be cached
    Object.keys(cacheLocations).forEach(key => resolveLocations(buildEnv, cacheLocations, key, key === BUILD_OUTPUT ? decorateBuildOutput : undefined));
    return cacheLocations;
};
exports.getCacheLocations = getCacheLocations;
/**
 * Adds to a {@link CacheLocation[]} the locations for each lcoation type (key).
 *
 * @param buildEnv build environtment (gradle or maven)
 * @param cacheLocations cache locations based on the build env
 * @param key location type (depedencies or buildOutput)
 * @param decorateFn function to do some extra decoration to locations path

 */
const resolveLocations = (buildEnv, cacheLocations, key, decorateFn) => {
    const cacheEnableKey = key.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`);
    const cacheEnable = core.getBooleanInput(`cache_${cacheEnableKey}`);
    if (!cacheEnable) {
        core.notice(`Core cache is disabled for ${key}`);
        return;
    }
    core.info(`Looking cache configuration for ${key}`);
    const locationKey = key;
    const locations = cacheLocations[locationKey];
    if (!locations) {
        core.warning(`Cannot resolve any ${key} locations for build env ${buildEnv}`);
        return;
    }
    core.info(`Found ${key} locations: ${locations}`);
    const resolvedLocations = locations.map(location => {
        const newLocation = decorateFn ? decorateFn(location) : location;
        if (location !== newLocation) {
            core.info(`Relocating cache from ${location} to ${newLocation}`);
            fs.mkdirSync(path.dirname(newLocation), { recursive: true });
            fs.renameSync(location, newLocation);
        }
        return newLocation;
    });
    core.info(`Resolved ${key} locations: ${resolvedLocations}`);
    cacheLocations[locationKey] = resolvedLocations;
};
/**
 * Decoration function for build output
 *
 * @param location location
 * @returns decorated string
 */
const decorateBuildOutput = (location) => path.join(CACHE_FOLDER, location);
