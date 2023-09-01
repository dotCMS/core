import * as core from '@actions/core';
import * as m from './members';

/**
 * Main entry point for this action.
 */
const run = async () => {
    const confDir = core.getInput('conf_dir');
    const member = core.getInput('member');
    const get = core.getInput('get');

    const metadata = m.getMetadata(confDir, member, get);

    core.setOutput('metadata', metadata);
    core.info(`Metadata: ${metadata}`);
};

// Run main function
run();
