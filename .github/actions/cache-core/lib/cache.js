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
exports.cacheCore = void 0;
const cache = __importStar(require("@actions/cache"));
const core = __importStar(require("@actions/core"));
const exec = __importStar(require("@actions/exec"));
const cache_1 = require("@actions/cache");
const EMPTY_CACHE_RESULT = {
    cacheKey: '',
    type: '',
    cacheLocations: []
};
/**
 * Uses cache library to cache a provided collection of locations.
 *
 * @returns a {@link Promise<CacheMetadata>} with data about the cache operation: key, locations and cache id
 */
const cacheCore = () => __awaiter(void 0, void 0, void 0, function* () {
    const buildEnv = core.getInput('build_env');
    core.info(`Resolving cache locations with buid env ${buildEnv}`);
    const cacheLocations = JSON.parse(core.getInput('cache_locations'));
    core.info(`Attempting to cache core using these locations:\n ${JSON.stringify(cacheLocations, null, 2)}`);
    const availableCacheKeysStr = core.getInput('available_cache_keys');
    core.info(`Available cache keys: ${availableCacheKeysStr}`);
    const availableCacheKeys = JSON.parse(core.getInput('available_cache_keys'));
    const cacheKeys = availableCacheKeys[buildEnv];
    core.info(`Cache keys: ${JSON.stringify(cacheKeys, null, 2)}`);
    const cacheMetadata = {
        buildEnv: buildEnv,
        locations: []
    };
    const locationTypes = Object.keys(cacheLocations);
    core.info(`Caching these locations: ${locationTypes.join(', ')}`);
    for (const locationType of locationTypes) {
        const cacheLocationMetadata = yield cacheLocation(cacheLocations, cacheKeys, locationType);
        if (cacheLocationMetadata !== EMPTY_CACHE_RESULT) {
            cacheMetadata.locations.push(cacheLocationMetadata);
        }
    }
    return new Promise(resolve => resolve(cacheMetadata));
});
exports.cacheCore = cacheCore;
/**
 * Do the actual caching of the provided locations
 *
 * @param cacheLocations provided locations
 * @param resolvedKeys keys to used for caching
 * @param locationType location type
 * @returns a {@link Promise<CacheResult>} with the caching operation data
 */
const cacheLocation = (cacheLocations, resolvedKeys, locationType) => __awaiter(void 0, void 0, void 0, function* () {
    const cacheKey = resolvedKeys[locationType];
    const resolvedLocations = cacheLocations[locationType];
    core.info(`Caching locations:\n  [${resolvedLocations.join(', ')}]\n  with key: ${cacheKey}`);
    let cacheResult = EMPTY_CACHE_RESULT;
    try {
        const cacheId = yield cache.saveCache(resolvedLocations, cacheKey);
        core.info('Location contents');
        for (const location of resolvedLocations) {
            ls(location);
        }
        core.info(`Cache id found: ${cacheId}`);
        cacheResult = {
            cacheKey,
            type: locationType,
            cacheLocations: resolvedLocations,
            cacheId
        };
        core.info(`Resolved cache result ${JSON.stringify(cacheResult)}`);
    }
    catch (err) {
        if (err instanceof cache_1.ReserveCacheError) {
            core.info(`${err}, so still considering for cache`);
            cacheResult = {
                cacheKey,
                type: locationType,
                cacheLocations: resolvedLocations
            };
        }
        else {
            core.warning(`Could not cache using ${cacheKey} due to ${err}`);
        }
    }
    return Promise.resolve(cacheResult);
});
const ls = (location) => __awaiter(void 0, void 0, void 0, function* () {
    core.info(`Listing folder ${location}`);
    try {
        yield exec.exec('ls', ['-las', location]);
    }
    catch (err) {
        core.info(`Cannot list folder ${location}`);
    }
});
