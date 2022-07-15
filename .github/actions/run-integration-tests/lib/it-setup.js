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
exports.setupTests = void 0;
const core = __importStar(require("@actions/core"));
const exec = __importStar(require("@actions/exec"));
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const SOURCE_TEST_RESOURCES_FOLDER = 'cicd/resources';
const TARGET_TEST_RESOURCES_FOLDER = 'dotCMS/src/integration-test/resources';
const LICENSE_FOLDER = 'custom/dotsecure/license';
const projectRoot = core.getInput('project_root');
const workspaceRoot = path.dirname(projectRoot);
const IT_FOLDERS = [
    'custom/assets',
    'custom/dotsecure',
    'custom/esdata',
    'custom/output/reports/html',
    'custom/felix',
    LICENSE_FOLDER
];
const TEST_RESOURCES = [path.join(projectRoot, SOURCE_TEST_RESOURCES_FOLDER, 'log4j2.xml')];
/**
 * Setup location folders and files. Override and add properties to config files so ITs can run.
 *
 * @param propertyMap properties vaslues map
 */
const setupTests = (propertyMap) => {
    // prepare folders and copy files
    prepareTests();
    // override existing properties
    overrideProperties(propertyMap);
    // append new properties
    appendProperties(propertyMap);
    // prepare license
    prepareLicense();
};
exports.setupTests = setupTests;
/**
 * Gets the value from the properties map otherwise empty.
 *
 * @param propertyMap properties map
 * @param key key to look for
 * @returns value found in the map
 */
const getValue = (propertyMap, key) => propertyMap.get(key) || '';
/**
 * Prepares by creating necessary folders and copying files.
 */
const prepareTests = () => __awaiter(void 0, void 0, void 0, function* () {
    core.info('Preparing integration tests');
    for (const folder of IT_FOLDERS) {
        const itFolder = path.join(workspaceRoot, folder);
        core.info(`Creating IT folder ${itFolder}`);
        fs.mkdirSync(itFolder, { recursive: true });
    }
    for (const res of TEST_RESOURCES) {
        const dest = path.join(projectRoot, TARGET_TEST_RESOURCES_FOLDER, path.basename(res));
        core.info(`Copying resource ${res} to ${dest}`);
        fs.copyFileSync(res, dest);
    }
});
/**
 * Overrides with provided values the defined config files.
 *
 * @param propertyMap properties map
 */
const overrideProperties = (propertyMap) => __awaiter(void 0, void 0, void 0, function* () {
    core.info('Overriding properties');
    const overrides = getOverrides(propertyMap);
    for (const file of overrides.files) {
        core.info(`Overriding properties at ${file.file}`);
        for (const prop of file.properties) {
            yield exec.exec('sed', ['-i', `s,${prop.original},${prop.replacement},g`, file.file]);
        }
        // core.info(`
        // ##################################
        // Reviewing changes for ${file.file}
        // ##################################`)
        // await exec.exec('cat', [file.file])
    }
});
/**
 * Append provided properties to defined config files.
 *
 * @param propertyMap properties map
 */
const appendProperties = (propertyMap) => {
    core.info('Adding properties');
    const appends = getAppends(propertyMap);
    for (const file of appends.files) {
        core.info(`Appending properties to ${file.file}`);
        const line = file.lines.join('\n');
        core.info(`Appeding properties:\n ${line}`);
        fs.appendFileSync(file.file, `\n${line}`, { encoding: 'utf8', flag: 'a+', mode: 0o666 });
    }
};
/**
 * Get override properties object
 *
 * @param propertyMap properties map
 * @returns {@link OverrideProperties} object
 */
const getOverrides = (propertyMap) => {
    const dotSecureFolder = getValue(propertyMap, 'dotSecureFolder');
    const dotCmsFolder = getValue(propertyMap, 'dotCmsFolder');
    const felixFolder = getValue(propertyMap, 'felixFolder');
    const assetsFolder = getValue(propertyMap, 'assetsFolder');
    const resourcesFolder = path.join(dotCmsFolder, 'src/main/resources');
    const itResourcesFolder = path.join(dotCmsFolder, 'src/integration-test/resources');
    const dbType = getValue(propertyMap, 'dbType');
    return {
        files: [
            {
                file: `${dotCmsFolder}/gradle.properties`,
                properties: [
                    {
                        original: '^# integrationTestFelixFolder=.*$',
                        replacement: `integrationTestFelixFolder=${felixFolder}`
                    }
                ]
            },
            {
                file: `${resourcesFolder}/dotmarketing-config.properties`,
                properties: [
                    {
                        original: '^#DYNAMIC_CONTENT_PATH=.*$',
                        replacement: `DYNAMIC_CONTENT_PATH=${dotSecureFolder}`
                    }
                ]
            },
            {
                file: `${itResourcesFolder}/${dbType}-db-config.properties`,
                properties: [
                    {
                        original: '://database',
                        replacement: '://localhost'
                    }
                ]
            },
            {
                file: `${itResourcesFolder}/it-dotcms-config-cluster.properties`,
                properties: [
                    {
                        original: '^es.path.home=.*$',
                        replacement: `es.path.home=${dotCmsFolder}/src/main/webapp/WEB-INF/elasticsearch`
                    },
                    {
                        original: '^ES_HOSTNAME=.*$',
                        replacement: 'ES_HOSTNAME=localhost'
                    }
                ]
            },
            {
                file: `${itResourcesFolder}/it-dotmarketing-config.properties`,
                properties: [
                    {
                        original: '^DYNAMIC_CONTENT_PATH=.*$',
                        replacement: `DYNAMIC_CONTENT_PATH=${dotSecureFolder}`
                    },
                    {
                        original: '^TAIL_LOG_LOG_FOLDER=.*$',
                        replacement: `TAIL_LOG_LOG_FOLDER=${dotSecureFolder}/logs/`
                    },
                    {
                        original: '^ASSET_REAL_PATH =.*$',
                        replacement: `ASSET_REAL_PATH=${assetsFolder}`
                    },
                    {
                        original: '^#TOOLBOX_MANAGER_PATH=.*$',
                        replacement: `TOOLBOX_MANAGER_PATH=${dotCmsFolder}/src/main/webapp/WEB-INF/toolbox.xml`
                    },
                    {
                        original: '^VELOCITY_ROOT =.*$',
                        replacement: `VELOCITY_ROOT=${dotCmsFolder}/src/main/webapp/WEB-INF/velocity`
                    },
                    {
                        original: '^GEOIP2_CITY_DATABASE_PATH_OVERRIDE=.*$',
                        replacement: `GEOIP2_CITY_DATABASE_PATH_OVERRIDE=${dotCmsFolder}/src/main/webapp/WEB-INF/geoip2/GeoLite2-City.mmdb`
                    },
                    {
                        original: '^felix.base.dir=.*$',
                        replacement: `felix.base.dir=${felixFolder}`
                    }
                ]
            }
        ]
    };
};
/**
 * Get apeend properties object.
 *
 * @param propertyMap properties map
 * @returns {@link AppendProperties} object
 */
const getAppends = (propertyMap) => {
    const felixFolder = getValue(propertyMap, 'felixFolder');
    const esDataFolder = getValue(propertyMap, 'esDataFolder');
    const logsFolder = getValue(propertyMap, 'logsFolder');
    const dotCmsFolder = getValue(propertyMap, 'dotCmsFolder');
    const itResourcesFolder = path.join(dotCmsFolder, 'src/integration-test/resources');
    return {
        files: [
            {
                file: `${itResourcesFolder}/it-dotmarketing-config.properties`,
                lines: [
                    `felix.felix.fileinstall.dir=${felixFolder}/load`,
                    `felix.felix.undeployed.dir=${felixFolder}/undeploy`,
                    'dotcms.concurrent.locks.disable=false'
                ]
            },
            // {
            //   file: `${itResourcesFolder}/it-dotcms-config-cluster.properties`,
            //   lines: ['ES_ENDPOINTS=http://localhost:9200', 'ES_PROTOCOL=http', 'ES_HOSTNAME=localhost', 'ES_PORT=9200']
            // },
            {
                file: `${dotCmsFolder}/src/main/webapp/WEB-INF/elasticsearch/config/elasticsearch-override.yml`,
                lines: [
                    'cluster.name: dotCMSContentIndex_docker',
                    `path.data: ${esDataFolder}`,
                    `path.repo: ${esDataFolder}/essnapshot/snapshots`,
                    `path.logs: ${logsFolder}`,
                    '# http.port: 9200',
                    '# transport.tcp.port: 9309',
                    'http.enabled: false',
                    'http.cors.enabled: false',
                    '# http.host: localhost',
                    'cluster.routing.allocation.disk.threshold_enabled: false'
                ]
            }
        ]
    };
};
/**
 * Creates license folder and file with appropiate key.
 */
const prepareLicense = () => __awaiter(void 0, void 0, void 0, function* () {
    const licensePath = path.join(workspaceRoot, LICENSE_FOLDER);
    const licenseKey = core.getInput('license_key');
    const licenseFile = path.join(licensePath, 'license.dat');
    core.info(`Adding license to ${licenseFile}`);
    fs.writeFileSync(licenseFile, licenseKey, { encoding: 'utf8', flag: 'a+', mode: 0o777 });
    yield exec.exec('ls', ['-las', licenseFile]);
});
