#!/usr/bin/env node
import { execSync } from 'child_process';

const projects = ['types', 'client', 'uve', 'angular'];
const dependencies = {
  'client': ['types'],
  'uve': ['types'],
  'angular': ['client', 'uve']
};

function setupYalc() {
  console.log('🏗️  Building all projects first...');
  execSync('yarn nx run-many --target=build --projects=sdk-types,sdk-client,sdk-uve,sdk-angular', { stdio: 'inherit' });

  console.log('📦 Publishing to yalc...');
  projects.forEach(project => {
    try {
      execSync(`cd dist/libs/sdk/${project} && yalc publish`, { stdio: 'inherit' });
      console.log(`✅ Published ${project}`);
    } catch (error) {
      console.error(`❌ Failed to publish ${project}`);
    }
  });

  console.log('🔗 Linking dependencies...');
  Object.entries(dependencies).forEach(([project, deps]) => {
    deps.forEach(dep => {
      try {
        execSync(`cd libs/sdk/${project} && yalc add @dotcms/${dep}`, { stdio: 'inherit' });
        console.log(`✅ Linked @dotcms/${dep} to ${project}`);
      } catch (error) {
        console.error(`❌ Failed to link @dotcms/${dep} to ${project}`);
      }
    });
  });

  console.log('🎉 Setup complete!');
}

setupYalc();
