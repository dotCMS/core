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
exports.restoreLocations = void 0;
const cache = __importStar(require("@actions/cache"));
const core = __importStar(require("@actions/core"));
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const EMPTY_LOCATIONS = {
    dependencies: [],
    buildOutput: []
};
const BUILD_OUTPUT = 'buildOutput';
const RESTORE_CONFIGURATION = {
    gradle: {
        dependencies: ['~/.gradle/caches', '~/.gradle/wrapper'],
        buildOutput: ['dotCMS/.gradle', 'dotCMS/build/classes', 'dotCMS/build/resources']
    },
    maven: {
        dependencies: ['~/.m2/repository'],
        buildOutput: ['dotCMS/target']
    }
};
const PROJECT_ROOT = core.getInput('project_root');
const CACHE_FOLDER = path.join(path.dirname(PROJECT_ROOT), 'cache');
/**
 * Restore previously cached locations.
 *
 * @param cacheMetadata {@link CacheMetadata} object that created with cache information.
 * @returns cache metadata object
 */
const restoreLocations = (cacheMetadata) => __awaiter(void 0, void 0, void 0, function* () {
    const cacheLocations = {
        dependencies: [],
        buildOutput: []
    };
    const buildEnv = cacheMetadata.buildEnv;
    const restoreConfig = RESTORE_CONFIGURATION[buildEnv];
    if (!restoreConfig) {
        core.setFailed(`Could not resolve restore configuration from build env ${buildEnv}`);
        return Promise.resolve(EMPTY_LOCATIONS);
    }
    for (const locationMetadata of cacheMetadata.locations) {
        core.info(`Restoring locations:\n[${locationMetadata.cacheLocations}]\nusing key: ${locationMetadata.cacheKey}`);
        if (locationMetadata.type === BUILD_OUTPUT) {
            locationMetadata.cacheLocations.forEach(location => {
                if (!fs.existsSync(location)) {
                    core.info(`Location ${location} does not exist, creating it`);
                    fs.mkdirSync(location, { recursive: true });
                }
            });
        }
        const cacheKey = yield cache.restoreCache(locationMetadata.cacheLocations, locationMetadata.cacheKey);
        core.info(`Locations restored with key ${cacheKey}`);
        const type = locationMetadata.type;
        const configLocations = restoreConfig[type];
        if (!configLocations || configLocations.length === 0) {
            core.setFailed(`Could not resolve config locations from ${type}`);
            return Promise.resolve(EMPTY_LOCATIONS);
        }
        cacheLocations[type] = relocate(type, locationMetadata.cacheLocations, configLocations);
    }
    return new Promise(resolve => resolve(cacheLocations));
});
exports.restoreLocations = restoreLocations;
/**
 * Once cached locations are restored, for every location type that requires to relocate folders it does
 *
 * @param type location type (dependencies or buildOutput)
 * @param cacheLocations cache locations arrays
 * @param configLocations config locations extracted from restore configuration
 * @returns relocated restored locations
 */
function relocate(type, cacheLocations, configLocations) {
    if (type !== BUILD_OUTPUT) {
        core.info(`Not relocating any cache for ${type}`);
        return cacheLocations;
    }
    core.info(`Caches to relocate: ${cacheLocations}`);
    return cacheLocations
        .map(location => {
        const prefix = CACHE_FOLDER + path.sep;
        const baseName = location.slice(prefix.length);
        const foundLocation = configLocations.find(configLocation => configLocation === baseName);
        if (!foundLocation) {
            return '';
        }
        const newLocation = path.join(PROJECT_ROOT, foundLocation);
        core.info(`New location for cache: ${newLocation}`);
        const newFolder = path.dirname(newLocation);
        if (!fs.existsSync(newFolder)) {
            core.info(`New location folder ${newFolder} does not exist, creating it`);
            fs.mkdirSync(newFolder);
        }
        core.info(`Relocating cache from ${location} to ${newLocation}`);
        fs.renameSync(location, newLocation);
        return newLocation;
    })
        .filter(location => location !== '');
}
