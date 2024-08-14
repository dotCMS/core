const fs = require('fs');
const path = require('path');

// Function to read and parse JSON
const readJSON = (filePath) => JSON.parse(fs.readFileSync(filePath, 'utf8'));

// Function to write JSON to file
const writeJSON = (filePath, data) =>
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2) + '\n');

// Function to bump the version
const bumpVersion = (version) => {
    const match = version.match(/^(\d+\.\d+\.\d+-alpha\.)(\d+)$/);
    if (!match) {
        throw new Error(`Invalid version format: ${version}`);
    }

    const prefix = match[1];
    const buildNumber = parseInt(match[2]);

    const newBuildNumber = buildNumber + 1;
    return `${prefix}${newBuildNumber}`;
};

// Function to update dependencies in a package.json file
const updatePackageDependencies = (
    packageJsonPath,
    newVersion,
    dependenciesMap,
    isPeerDependency = false
) => {
    const packageJson = readJSON(packageJsonPath);
    let updated = false;

    // Determine whether to update dependencies or peerDependencies
    const depsKey = isPeerDependency ? 'peerDependencies' : 'dependencies';
    const dependencies = packageJson[depsKey] || {};

    // Update dependencies/peerDependencies to the new version based on the dependenciesMap
    Object.keys(dependenciesMap).forEach((dep) => {
        if (dependencies[dep]) {
            console.log(
                `Updating ${depsKey.slice(0, -1)} ${dep} in ${packageJsonPath} to version ${newVersion}`
            );
            dependencies[dep] = newVersion;
            updated = true;
        }
    });

    if (updated) {
        packageJson[depsKey] = dependencies;
        writeJSON(packageJsonPath, packageJson);
        console.log(`Updated ${depsKey} in ${packageJsonPath} to version ${newVersion}`);
    } else {
        console.log(`No relevant ${depsKey} to update in ${packageJsonPath}`);
    }
};

// Function to update the version of the SDK packages themselves
const updateSdkPackageVersion = (packageJsonPath, newVersion, dependenciesMap) => {
    const packageJson = readJSON(packageJsonPath);
    packageJson.version = newVersion;

    // Update the internal peerDependencies within SDKs
    updatePackageDependencies(packageJsonPath, newVersion, dependenciesMap, true);

    writeJSON(packageJsonPath, packageJson);
    console.log(`Updated ${packageJsonPath} to version ${newVersion}`);
};

// Function to find all package.json files in a directory recursively
const findPackageJsonFilesRecursively = (dir) => {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach((file) => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);
        if (stat && stat.isDirectory()) {
            results = results.concat(findPackageJsonFilesRecursively(filePath));
        } else if (file === 'package.json') {
            results.push(filePath);
        }
    });
    return results;
};

// Define the root directories to search
const sdkRoot = path.join(__dirname, 'libs/sdk');
const examplesRoot = path.join(__dirname, '../examples');

// Find all package.json files in the SDK and examples directories
const sdkPackages = findPackageJsonFilesRecursively(sdkRoot);
const examplePackages = findPackageJsonFilesRecursively(examplesRoot);

// Log the found files for verification
console.log(`Found SDK packages: ${sdkPackages.join(', ')}`);
console.log(`Found Example packages: ${examplePackages.join(', ')}`);

// Get the current version from sdk-client
const sdkClientPackageJson = readJSON(sdkPackages.find((pkg) => pkg.includes('client')));
const currentVersion = sdkClientPackageJson.version;

// Bump the version
const newVersion = bumpVersion(currentVersion);

// Define the dependency mappings
const sdkDependencies = {
    '@dotcms/client': newVersion,
    '@dotcms/angular': newVersion,
    '@dotcms/react': newVersion,
    '@dotcms/experiments': newVersion
};

const exampleDependencies = {
    '@dotcms/client': newVersion,
    '@dotcms/angular': newVersion,
    '@dotcms/react': newVersion,
    '@dotcms/experiments': newVersion
};

// Update all SDK package.json files (versions and peerDependencies)
sdkPackages.forEach((pkg) => updateSdkPackageVersion(pkg, newVersion, sdkDependencies));

// Update all example package.json files (dependencies only)
examplePackages.forEach((pkg) => updatePackageDependencies(pkg, newVersion, exampleDependencies));

console.log(`All relevant package.json files updated to version ${newVersion}`);
