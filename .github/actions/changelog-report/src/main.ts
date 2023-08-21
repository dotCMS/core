import * as core from '@actions/core';
import * as md from './md-reporter';

/**
 * Main entry point for this action.
 */
const run = async () => {
    const format = core.getInput('format');
    if (format === 'md') {
        md.report();
    }
};

// Run main function
run();
