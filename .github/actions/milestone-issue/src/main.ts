import * as core from '@actions/core';
import * as m from './milestone';

/**
 * Main entry point for this action.
 */
const run = async () => {
    const confDir = core.getInput('conf_dir');
    const field = core.getInput('field');
    const teams = core.getInput('teams');
    const event = core.getInput('event');

    try {
        await m.assignMilestone(confDir, teams, field, event);
    } catch (error) {
        core.setFailed(`Error detected: ${error}`);
    }
};

// Run main function
run();
