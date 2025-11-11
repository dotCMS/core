#!/usr/bin/env node

/**
 * Custom file watcher for SDK libraries using npm pack
 * Watches for changes and triggers rebuild + npm pack + npm install
 */

const { spawn, execSync } = require('child_process');
const path = require('path');
const fs = require('fs');
const {
  watchPaths,
  getAffectedPackages,
  getBuildCommand,
  getAllProjectsForExample,
  getPackagesForExample
} = require('./project-config');

const EXAMPLE = process.argv[2];
if (!EXAMPLE) {
  console.error('❌ Error: No example specified');
  process.exit(1);
}

const coreWebPath = path.join(__dirname, '..');
const examplePath = path.join(__dirname, '..', '..', 'examples', EXAMPLE);
const tempTarballsPath = path.join(__dirname, '..', '.tmp-tarballs');

// Track the dev server process
let devServerProcess = null;

let buildTimeout;
let isBuilding = false;
const debounceMs = 1000;

console.log('👀 Starting file watcher...\n');

// Function to get dev server command
function getDevServerCommand() {
  switch (EXAMPLE) {
    case 'angular':
    case 'angular-ssr':
      return 'npm start';
    case 'nextjs':
      return 'npm run dev';
    case 'astro':
      return 'npm run dev';
    case 'vuejs':
      return 'npm run dev';
    default:
      return 'npm start';
  }
}

// Function to kill dev server
function killDevServer() {
  console.log('\n🛑 Stopping dev server...');

  if (devServerProcess) {
    try {
      // Kill the process and all its children
      process.kill(-devServerProcess.pid, 'SIGTERM');
    } catch (e) {
      // If process group kill fails, try regular kill
      try {
        devServerProcess.kill('SIGTERM');
      } catch (e2) {
        // Already dead
      }
    }
    devServerProcess = null;
    console.log('✅ Dev server stopped!\n');
  }
}

// Function to start dev server
function startDevServer() {
  console.log('🌐 Starting dev server...\n');

  devServerProcess = spawn(getDevServerCommand(), {
    cwd: examplePath,
    stdio: 'inherit',
    shell: true,
    detached: true
  });

  devServerProcess.on('error', (error) => {
    console.error('❌ Dev server error:', error);
  });

  devServerProcess.on('exit', (code) => {
    if (code !== 0) {
      console.error(`⚠️  Dev server exited with code ${code}`);
    }
  });

  console.log('✅ Dev server started!\n');
}

// Function to touch files to force Angular to detect changes
function touchPackageFiles() {
  if (EXAMPLE !== 'angular' && EXAMPLE !== 'angular-ssr') {
    return; // Only needed for Angular
  }

  console.log('🔧 Touching package files to force Angular reload...');
  
  try {
    const allExamplePackages = getPackagesForExample(EXAMPLE);
    
    allExamplePackages.forEach(pkg => {
      const pkgDir = path.join(examplePath, 'node_modules', pkg.name);
      
      if (!fs.existsSync(pkgDir)) {
        console.warn(`  ⚠️  ${pkg.name} not found in node_modules`);
        return;
      }

      // Touch package.json
      const pkgJsonPath = path.join(pkgDir, 'package.json');
      if (fs.existsSync(pkgJsonPath)) {
        const now = new Date();
        fs.utimesSync(pkgJsonPath, now, now);
        console.log(`  ✓ Touched ${pkg.name}/package.json`);
      }

      // Touch main entry point files (common locations)
      const entryPoints = [
        path.join(pkgDir, 'fesm2022', `${pkg.name.replace('@dotcms/', 'dotcms-')}.mjs`),
        path.join(pkgDir, 'fesm2022', `${pkg.name.replace('@dotcms/', 'dotcms-')}.js`),
        path.join(pkgDir, 'index.js'),
        path.join(pkgDir, 'index.mjs'),
        path.join(pkgDir, 'dist', 'index.js'),
        path.join(pkgDir, 'dist', 'index.mjs')
      ];

      entryPoints.forEach(entryPath => {
        if (fs.existsSync(entryPath)) {
          const now = new Date();
          fs.utimesSync(entryPath, now, now);
          console.log(`  ✓ Touched ${path.relative(examplePath, entryPath)}`);
        }
      });
    });
    
    console.log('✅ Files touched!\n');
  } catch (error) {
    console.warn(`  ⚠️  Failed to touch files: ${error.message}`);
  }
}

// Common function to execute the build flow (same as dev-pack.js)
function executeBuildFlow() {
  // Step 1: Delete dist/libs/sdk (all content inside)
  console.log('🗑️  Deleting dist/libs/sdk...\n');
  const distSdkPath = path.join(coreWebPath, 'dist', 'libs', 'sdk');
  try {
    if (fs.existsSync(distSdkPath)) {
      execSync(`rm -rf ${distSdkPath}/*`, {
        cwd: coreWebPath,
        stdio: 'pipe'
      });
      console.log('✅ dist/libs/sdk cleaned!\n');
    } else {
      console.log('⚠️  dist/libs/sdk not found, skipping...\n');
    }
  } catch (error) {
    console.error('❌ Failed to delete dist/libs/sdk');
    console.error('Error:', error.message);
    throw error;
  }

  // Step 2: Delete .tarballs folder and node_modules
  console.log('🗑️  Deleting .tarballs and node_modules...\n');
  const exampleTarballsPath = path.join(examplePath, '.tarballs');
  const nodeModulesPath = path.join(examplePath, 'node_modules');
  const packageLockPath = path.join(examplePath, 'package-lock.json');
  const npmCachePath = path.join(examplePath, '.npm-cache');
  const falseFolderPath = path.join(examplePath, 'false');

  try {
    if (fs.existsSync(exampleTarballsPath)) {
      execSync(`rm -rf ${exampleTarballsPath}`, {
        cwd: examplePath,
        stdio: 'pipe'
      });
      console.log('  ✓ .tarballs deleted');
    }

    if (fs.existsSync(nodeModulesPath)) {
      execSync(`rm -rf ${nodeModulesPath}`, {
        cwd: examplePath,
        stdio: 'pipe'
      });
      console.log('  ✓ node_modules deleted');
    }

    if (fs.existsSync(packageLockPath)) {
      fs.unlinkSync(packageLockPath);
      console.log('  ✓ package-lock.json deleted');
    }

    // Clean up any unwanted folders
    if (fs.existsSync(npmCachePath)) {
      execSync(`rm -rf ${npmCachePath}`, { cwd: examplePath, stdio: 'pipe' });
    }
    if (fs.existsSync(falseFolderPath)) {
      execSync(`rm -rf ${falseFolderPath}`, { cwd: examplePath, stdio: 'pipe' });
    }

    console.log('✅ Cleanup complete!\n');
  } catch (error) {
    console.error('❌ Failed to delete directories');
    console.error('Error:', error.message);
    throw error;
  }

  // Step 3: Run build to the libs
  console.log('📦 Building libraries...\n');
  try {
    const allProjectsForExample = getAllProjectsForExample(EXAMPLE);
    const buildCmd = getBuildCommand(allProjectsForExample);
    execSync(buildCmd, {
      stdio: 'inherit',
      cwd: coreWebPath
    });
    console.log('\n✅ Build complete!\n');
  } catch (error) {
    console.error('❌ Build failed');
    console.error('Error:', error.message);
    throw error;
  }

  // Step 4: Run npm pack
  console.log('📦 Packing libraries...\n');
  try {
    // Create .tarballs directory
    fs.mkdirSync(exampleTarballsPath, { recursive: true });

    const allExamplePackages = getPackagesForExample(EXAMPLE);
    allExamplePackages.forEach(pkg => {
      const pkgPath = path.join(coreWebPath, pkg.path);
      console.log(`  Packing ${pkg.name}...`);

      // Pack in the package directory
      const output = execSync('npm pack', {
        cwd: pkgPath,
        encoding: 'utf8'
      }).trim();

      const sourceTarball = path.join(pkgPath, output);
      const destTarball = path.join(exampleTarballsPath, output);

      // Copy tarball to .tarballs directory
      execSync(`cp "${sourceTarball}" "${destTarball}"`, {
        stdio: 'pipe'
      });

      // Remove source tarball from package directory
      if (fs.existsSync(sourceTarball)) {
        fs.unlinkSync(sourceTarball);
      }

      // Verify tarball was copied
      if (!fs.existsSync(destTarball)) {
        throw new Error(`Tarball not found after copy: ${destTarball}`);
      }

      const stats = fs.statSync(destTarball);
      console.log(`    ✓ ${output} (${(stats.size / 1024).toFixed(2)} KB)`);
    });
    console.log('\n✅ All libraries packed!\n');
  } catch (error) {
    console.error('❌ Failed to pack libraries');
    console.error('Error:', error.message);
    throw error;
  }

  // Step 5: Override package.json (from the project)
  console.log('📝 Overriding package.json with local tarballs...\n');
  try {
    const packageJsonPath = path.join(examplePath, 'package.json');
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

    const allExamplePackages = getPackagesForExample(EXAMPLE);
    // Update to use relative file: references
    allExamplePackages.forEach(pkg => {
      // Find the tarball filename for this package
      const tarballFiles = fs.readdirSync(exampleTarballsPath).filter(f => 
        f.includes(pkg.name.replace('@dotcms/', 'dotcms-'))
      );
      
      if (tarballFiles.length > 0 && packageJson.dependencies[pkg.name]) {
        const filename = tarballFiles[0];
        packageJson.dependencies[pkg.name] = `file:.tarballs/${filename}`;
        console.log(`  ✓ ${pkg.name} → .tarballs/${filename}`);
      }
    });

    fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
    console.log('✅ package.json updated!\n');
  } catch (error) {
    console.error('❌ Failed to update package.json');
    console.error('Error:', error.message);
    throw error;
  }

  // Step 6: Run npm i (without creating .npm-cache)
  console.log('📥 Installing all dependencies (including local tarballs)...\n');
  try {
    execSync(`npm install --legacy-peer-deps`, {
      cwd: examplePath,
      stdio: 'inherit'
    });
    console.log('\n✅ All dependencies installed!\n');
    
    // Touch files to force Angular to detect changes
    touchPackageFiles();
  } catch (error) {
    console.error('❌ Failed to install dependencies');
    console.error('Error:', error.message);
    throw error;
  }
}

// Function to build, pack, and install (only affected packages)
async function buildPackAndInstall(changedProject) {
  if (isBuilding) {
    console.log('⏳ Build already in progress, skipping...');
    return;
  }

  isBuilding = true;

  try {
    // Get only affected packages for this example
    const affectedPackages = getAffectedPackages(changedProject, EXAMPLE);

    if (affectedPackages.length === 0) {
      console.log(`\n💡 ${changedProject} changed, but not used by ${EXAMPLE} - skipping\n`);
      isBuilding = false;
      return;
    }

    console.log(`\n📦 Change detected in ${changedProject}!`);
    console.log(`   Affected: ${affectedPackages.map(p => p.name).join(', ')}\n`);

    // Step 1: Kill dev server
    killDevServer();

    // Execute the same build flow
    executeBuildFlow();

    // Step 7: Restart dev server
    startDevServer();

    console.log('👀 Watching for changes...\n');
  } catch (error) {
    console.error('❌ Build, pack, or install failed:', error.message);
  } finally {
    isBuilding = false;
  }
}

// Watch each library directory
const exampleProjects = getAllProjectsForExample(EXAMPLE);
let lastChangedProject = null;

Object.entries(watchPaths).forEach(([dir, project]) => {
  const fullPath = path.join(coreWebPath, dir);

  if (!fs.existsSync(fullPath)) {
    console.warn(`⚠️  Directory not found: ${dir}`);
    return;
  }

  // Only watch if this project is used by the example
  if (!exampleProjects.includes(project)) {
    console.log(`  ⊘  Skipping ${project} (not used by ${EXAMPLE})`);
    return;
  }

  console.log(`  👁️  Watching ${project} (${dir})`);

  fs.watch(fullPath, { recursive: true }, (eventType, filename) => {
    if (!filename || !filename.match(/\.(ts|tsx|html|css|scss)$/)) {
      return;
    }

    console.log(`📝 File changed: ${dir}/${filename}`);
    lastChangedProject = project;

    // Debounce multiple rapid changes
    if (buildTimeout) {
      clearTimeout(buildTimeout);
    }

    buildTimeout = setTimeout(() => {
      buildPackAndInstall(lastChangedProject);
    }, debounceMs);
  });
});

// Cleanup function to restore package.json and clean up
function cleanup() {
  console.log('\n\n🧹 Cleaning up...');

  // Kill dev server
  if (devServerProcess) {
    try {
      process.kill(-devServerProcess.pid, 'SIGTERM');
      console.log('  ✓ Stopped dev server');
    } catch (e) {
      try {
        devServerProcess.kill('SIGTERM');
      } catch (e2) {
        // Already dead
      }
    }
  }

  // Restore package.json to use "latest" versions
  try {
    console.log('  ✓ Restoring package.json to latest versions...');
    const packageJsonPath = path.join(examplePath, 'package.json');
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

    // Reset all @dotcms packages to "latest"
    const packages = getPackagesForExample(EXAMPLE);
    packages.forEach(pkg => {
      if (packageJson.dependencies[pkg.name]) {
        packageJson.dependencies[pkg.name] = 'latest';
        console.log(`    ✓ ${pkg.name} → latest`);
      }
    });

    fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
    console.log('  ✓ package.json restored');
  } catch (e) {
    console.error('  ⚠️  Warning: Could not restore package.json:', e.message);
  }

  // Remove .tarballs directory
  try {
    const exampleTarballsPath = path.join(examplePath, '.tarballs');
    if (fs.existsSync(exampleTarballsPath)) {
      execSync(`rm -rf ${exampleTarballsPath}`, { cwd: examplePath, stdio: 'pipe' });
      console.log('  ✓ .tarballs directory removed');
    }
  } catch (e) {
    console.error('  ⚠️  Warning: Could not remove .tarballs directory');
  }

  console.log('\n👋 Development environment cleaned and restored!\n');
  process.exit(0);
}

console.log('\n✅ File watcher is active!');
console.log(`💡 Watching only libraries used by ${EXAMPLE}\n`);
console.log('   Change a file to trigger: kill → delete dist/libs/sdk → delete .tarballs/node_modules → build → pack → override package.json → npm install → touch files (Angular) → restart\n');

// Start the dev server initially
startDevServer();

// Cleanup dev server on exit
process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);

// Keep process alive
process.stdin.resume();
