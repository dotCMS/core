const fs = require('fs');
const path = require('path');

// Using absolute paths directly without __dirname
const buildOutputPath = '/Users/fmontes/Developer/@deleteme/dotcli-localhost/files/live/en-us/demo.dotcms.com/application/angular-app';
const destinationPath = '/Users/fmontes/Developer/@deleteme/dotcli-localhost/files/live/en-us/demo.dotcms.com/application/themes/angular';

// Ensure destination directory exists
if (!fs.existsSync(destinationPath)) {
  fs.mkdirSync(destinationPath, { recursive: true });
}

// Copy index.html
try {
  const sourceFile = path.join(buildOutputPath, 'index.html');
  const destFile = path.join(destinationPath, 'template.vtl');

  // Verify file exists before copying
  if (fs.existsSync(sourceFile)) {
    fs.copyFileSync(sourceFile, destFile);
    console.log(`Successfully copied index.html to ${destinationPath}/template.vtl`);
  } else {
    console.error(`Source file does not exist: ${sourceFile}`);
    process.exit(1);
  }
} catch (error) {
  console.error('Error copying index.html:', error);
  process.exit(1);
}