#!/usr/bin/env node

/**
 * Development script using npm pack (more reliable than yalc)
 * Watches for changes, rebuilds, packs, and reinstalls in example
 */

const { spawn, execSync } = require('child_process');
const path = require('path');
const fs = require('fs');
const {
  getPackagesForExample,
  getAllProjectsForExample,
  getBuildCommand
} = require('./project-config');

const EXAMPLE = process.argv[2];
const VALID_EXAMPLES = ['angular', 'angular-ssr', 'nextjs', 'astro', 'vuejs'];

// Validate example argument
if (!EXAMPLE) {
  console.error('❌ Error: Please specify an example to run\n');
  console.log('Usage: npm run pack:angular | pack:nextjs | pack:astro | pack:vuejs | pack:angular-ssr');
  process.exit(1);
}

if (!VALID_EXAMPLES.includes(EXAMPLE)) {
  console.error(`❌ Error: Invalid example "${EXAMPLE}"`);
  console.log(`Valid examples: ${VALID_EXAMPLES.join(', ')}`);
  process.exit(1);
}

console.log(`\n🚀 Starting npm pack development for ${EXAMPLE} example...\n`);

const coreWebPath = path.join(__dirname, '..');
const examplePath = path.join(__dirname, '..', '..', 'examples', EXAMPLE);
const tempTarballsPath = path.join(__dirname, '..', '.tmp-tarballs');

let watchProcess;
let isCleaningUp = false;

// Check if example exists
if (!fs.existsSync(examplePath)) {
  console.error(`❌ Error: Example directory not found at ${examplePath}`);
  process.exit(1);
}

// Cleanup function
function cleanup() {
  if (isCleaningUp) return;
  isCleaningUp = true;

  console.log('\n\n🧹 Cleaning up...');

  // Kill watch process (which also manages the dev server)
  if (watchProcess) {
    try {
      watchProcess.kill('SIGTERM');
      console.log('  ✓ Stopped watch process and dev server');
    } catch (e) {
      // Already dead
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

  // Remove node_modules and package-lock
  try {
    console.log('  ✓ Removing node_modules and package-lock.json...');
    const nodeModulesPath = path.join(examplePath, 'node_modules');
    const packageLockPath = path.join(examplePath, 'package-lock.json');
    const npmCachePath = path.join(examplePath, '.npm-cache');
    const falseFolderPath = path.join(examplePath, 'false');

    if (fs.existsSync(nodeModulesPath)) {
      execSync(`rm -rf ${nodeModulesPath}`, { cwd: examplePath, stdio: 'pipe' });
    }
    if (fs.existsSync(packageLockPath)) {
      fs.unlinkSync(packageLockPath);
    }
    if (fs.existsSync(npmCachePath)) {
      execSync(`rm -rf ${npmCachePath}`, { cwd: examplePath, stdio: 'pipe' });
    }
    if (fs.existsSync(falseFolderPath)) {
      execSync(`rm -rf ${falseFolderPath}`, { cwd: examplePath, stdio: 'pipe' });
    }
  } catch (e) {
    console.error('  ⚠️  Warning: Could not clean files');
  }

  // Reinstall with latest versions
  try {
    console.log('  ✓ Installing latest versions...');
    execSync('npm install --legacy-peer-deps', {
      cwd: examplePath,
      stdio: 'inherit'
    });
  } catch (e) {
    console.error('  ⚠️  Warning: Could not restore dependencies');
  }

  // Clean temporary tarballs
  try {
    if (fs.existsSync(tempTarballsPath)) {
      execSync(`rm -rf ${tempTarballsPath}`, { cwd: coreWebPath, stdio: 'pipe' });
      console.log('  ✓ Temporary tarballs cleaned');
    }
  } catch (e) {
    // Ignore
  }

  console.log('\n👋 Development environment cleaned and restored!\n');
  process.exit(0);
}

// Handle termination signals
process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);

// Get only the packages needed for this example
const packages = getPackagesForExample(EXAMPLE);
const projectNames = getAllProjectsForExample(EXAMPLE);

console.log(`📦 Preparing ${EXAMPLE} for local development...\n`);
console.log(`   Libraries: ${packages.map(p => p.name).join(', ')}\n`);

// Common function to execute the build flow
function executeBuildFlow() {
  // Step 1: Delete dist/libs/sdk (all content inside)
  console.log('🗑️  Deleting dist/libs/sdk...\n');
  const distSdkPath = path.join(coreWebPath, 'dist', 'libs', 'sdk');
  try {
    if (fs.existsSync(distSdkPath)) {
      execSync(`rm -rf ${distSdkPath}/*`, {
        cwd: coreWebPath,
        stdio: 'inherit'
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
    const buildCmd = getBuildCommand(projectNames);
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

    packages.forEach(pkg => {
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

    // Update to use relative file: references
    packages.forEach(pkg => {
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
  } catch (error) {
    console.error('❌ Failed to install dependencies');
    console.error('Error:', error.message);
    throw error;
  }
}

// Execute the build flow
try {
  executeBuildFlow();
} catch (error) {
  cleanup();
}

// Step 7: Run the project (start watch mode and dev server)
console.log('👀 Starting watch mode and dev server...\n');

const watchPackScript = path.join(__dirname, 'watch-pack.js');

// Pass the dev server management to the watcher
watchProcess = spawn('node', [watchPackScript, EXAMPLE], {
  cwd: coreWebPath,
  stdio: 'inherit'
});

watchProcess.on('error', (error) => {
  console.error('❌ Watch process error:', error);
  cleanup();
});

watchProcess.on('exit', (code) => {
  if (code !== 0 && !isCleaningUp) {
    console.log('\n⚠️  Watch process stopped unexpectedly');
    cleanup();
  }
});

// Success message
setTimeout(() => {
  console.log('\n');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('✅ Development environment is ready!');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('');
  console.log('💡 How it works:');
  console.log('   1. Edit any file in core-web/libs/sdk/*');
  console.log('   2. File watcher detects the change');
  console.log('   3. Nx rebuilds the changed library');
  console.log('   4. npm pack creates a tarball');
  console.log('   5. npm install updates the example');
  console.log('   6. Dev server hot-reloads the changes');
  console.log('');
  console.log('🛑 To stop: Press Ctrl+C (cleanup will restore everything)');
  console.log('═══════════════════════════════════════════════════════════');
  console.log('');
}, 3000);
