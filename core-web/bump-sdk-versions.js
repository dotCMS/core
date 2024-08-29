const fs = require('fs');
const path = require('path');

// Function to read and parse JSON
const readJSON = (filePath) => JSON.parse(fs.readFileSync(filePath, 'utf8'));

// Function to write JSON to file
const writeJSON = (filePath, data) =>
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2) + '\n');

// Function to bump the version
const bumpVersion = (version) => {
    const match = version.match(/^(\d+\.\d+\.\d+)(-alpha\.)(\d+)?$/);
    if (!match) {
        throw new Error(`Invalid version format: ${version}`);
    }

    const [, majorMinorPatch, alphaPrefix, buildNumber = 0] = match;
    const newBuildNumber = parseInt(buildNumber) + 1;
    return `${majorMinorPatch}${alphaPrefix}${newBuildNumber}`;
};

// Function to update the version in a package.json file
const updateVersionInPackageJson = (packageJsonPath, newVersion) => {
    const packageJson = readJSON(packageJsonPath);
    packageJson.version = newVersion;
    writeJSON(packageJsonPath, packageJson);
    console.log(`Updated version in ${packageJsonPath} to ${newVersion}`);
};

// Function to update peerDependencies in a package.json file
const updatePeerDependencies = (packageJsonPath, newVersion) => {
    const packageJson = readJSON(packageJsonPath);
    let updated = false;

    if (packageJson.peerDependencies) {
        Object.keys(packageJson.peerDependencies).forEach((dep) => {
            if (dep.startsWith('@dotcms/')) {
                packageJson.peerDependencies[dep] = newVersion;
                updated = true;
            }
        });
    }

    if (updated) {
        writeJSON(packageJsonPath, packageJson);
        console.log(`Updated peerDependencies in ${packageJsonPath} to version ${newVersion}`);
    }
};

// Function to update dependencies in a package.json file for examples
const updateDependenciesInExamples = (packageJsonPath, sdkDependencies) => {
    const packageJson = readJSON(packageJsonPath);
    let updated = false;

    Object.keys(sdkDependencies).forEach((dep) => {
        if (packageJson.dependencies && packageJson.dependencies[dep]) {
            packageJson.dependencies[dep] = sdkDependencies[dep];
            updated = true;
        }
    });

    if (updated) {
        writeJSON(packageJsonPath, packageJson);
        console.log(`Updated dependencies in ${packageJsonPath} to new SDK versions`);
    }
};

// Paths
const sdkDir = path.join(__dirname, 'libs/sdk');
const examplesDir = path.join(__dirname, '../examples');

// Step 1: Bump the version of the client library
const clientPackageJsonPath = path.join(sdkDir, 'client/package.json');
const clientPackageJson = readJSON(clientPackageJsonPath);
const currentVersion = clientPackageJson.version;
const newVersion = bumpVersion(currentVersion);
console.log(`Bumping version of client from ${currentVersion} to ${newVersion}`);

// Step 2: Update the version in all SDK libraries
const sdkLibraries = fs
    .readdirSync(sdkDir)
    .filter((lib) => fs.existsSync(path.join(sdkDir, lib, 'package.json')));
sdkLibraries.forEach((lib) => {
    const packageJsonPath = path.join(sdkDir, lib, 'package.json');
    updateVersionInPackageJson(packageJsonPath, newVersion);
});

// Step 3: Update peerDependencies in other SDK libraries
sdkLibraries.forEach((lib) => {
    if (lib !== 'client') {
        const packageJsonPath = path.join(sdkDir, lib, 'package.json');
        updatePeerDependencies(packageJsonPath, newVersion);
    }
});

// Step 4: Dynamically build the sdkDependencies object
const sdkDependencies = sdkLibraries.reduce((deps, lib) => {
    const packageJsonPath = path.join(sdkDir, lib, 'package.json');
    const packageJson = readJSON(packageJsonPath);
    if (packageJson.name) {
        deps[packageJson.name] = newVersion;
    }
    return deps;
}, {});

// Step 5: Update dependencies in example projects
const exampleProjects = fs
    .readdirSync(examplesDir)
    .filter((proj) => fs.existsSync(path.join(examplesDir, proj, 'package.json')));
exampleProjects.forEach((proj) => {
    const packageJsonPath = path.join(examplesDir, proj, 'package.json');
    updateDependenciesInExamples(packageJsonPath, sdkDependencies);
});

console.log(`All updates complete. New SDK version: ${newVersion}`);
