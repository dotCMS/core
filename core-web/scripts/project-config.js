#!/usr/bin/env node

/**
 * Centralized configuration for which libraries each example project uses
 */

// Map of watch directories to their Nx project names
const watchPaths = {
  'libs/sdk/angular/src': 'sdk-angular',
  'libs/sdk/client/src': 'sdk-client',
  'libs/sdk/uve/src': 'sdk-uve',
  'libs/sdk/types/src': 'sdk-types',
  'libs/sdk/react/src': 'sdk-react',
  'libs/sdk/experiments/src': 'sdk-experiments'
};

// Map of Nx project names to package info
const allPackages = {
  'sdk-angular': { name: '@dotcms/angular', path: 'dist/libs/sdk/angular' },
  'sdk-client': { name: '@dotcms/client', path: 'dist/libs/sdk/client' },
  'sdk-uve': { name: '@dotcms/uve', path: 'dist/libs/sdk/uve' },
  'sdk-types': { name: '@dotcms/types', path: 'dist/libs/sdk/types' },
  'sdk-react': { name: '@dotcms/react', path: 'dist/libs/sdk/react' },
  'sdk-experiments': { name: '@dotcms/experiments', path: 'dist/libs/sdk/experiments' }
};

// Map of example projects to the SDK libraries they use
const projectDependencies = {
  'angular': ['sdk-angular', 'sdk-client', 'sdk-uve', 'sdk-types'],
  'angular-ssr': ['sdk-angular', 'sdk-client', 'sdk-uve', 'sdk-types'],
  'nextjs': ['sdk-react', 'sdk-client', 'sdk-uve', 'sdk-types', 'sdk-experiments'],
  'astro': ['sdk-react', 'sdk-client', 'sdk-uve', 'sdk-types'],
  'vuejs': ['sdk-client', 'sdk-uve', 'sdk-types']
};

// Dependency graph: which packages depend on which
const packageDependencies = {
  'sdk-angular': ['sdk-client', 'sdk-uve', 'sdk-types'],
  'sdk-react': ['sdk-client', 'sdk-uve', 'sdk-types'],
  'sdk-client': ['sdk-types'],
  'sdk-uve': ['sdk-client', 'sdk-types'],
  'sdk-types': [],
  'sdk-experiments': ['sdk-types']
};

/**
 * Get all packages needed for a specific example
 */
function getPackagesForExample(example) {
  const deps = projectDependencies[example] || [];
  return deps.map(dep => allPackages[dep]);
}

/**
 * Get affected packages when a specific package changes
 * (includes the changed package and anything that depends on it)
 */
function getAffectedPackages(changedProject, targetExample) {
  const exampleDeps = projectDependencies[targetExample] || [];
  const affected = new Set();

  // Add the changed package if used by example
  if (exampleDeps.includes(changedProject)) {
    affected.add(changedProject);
  }

  // Add packages that depend on the changed package
  Object.entries(packageDependencies).forEach(([pkg, deps]) => {
    if (deps.includes(changedProject) && exampleDeps.includes(pkg)) {
      affected.add(pkg);
    }
  });

  return Array.from(affected).map(dep => allPackages[dep]);
}

/**
 * Get Nx build command for specific projects
 */
function getBuildCommand(projects) {
  const projectList = Array.isArray(projects) ? projects : [projects];
  return `npx nx run-many -t build --projects=${projectList.join(',')} --configuration=development --skip-nx-cache`;
}

/**
 * Get all projects that should be built for an example
 */
function getAllProjectsForExample(example) {
  return projectDependencies[example] || [];
}

module.exports = {
  watchPaths,
  allPackages,
  projectDependencies,
  packageDependencies,
  getPackagesForExample,
  getAffectedPackages,
  getBuildCommand,
  getAllProjectsForExample
};
