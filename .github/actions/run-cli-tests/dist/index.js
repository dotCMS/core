const core = require('@actions/core');
const github = require('@actions/github');

try {
  const dockerImage = core.getInput('docker-image');
  const time = new Date().toTimeString();

  console.log(`Hello ${dockerImage}!`);
  core.setOutput('time', time);
} catch (error) {
  core.setFailed(error.message);
}
