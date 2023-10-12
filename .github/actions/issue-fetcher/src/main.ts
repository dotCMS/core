import * as core from '@actions/core';
import * as fetcher from './issue-fetcher';

/**
 * Main entry point for this action.
 */
const run = async () => {
    const issues = await fetcher.fetchIssues();
    core.setOutput('issues', JSON.stringify(issues, null, 2));
};

// Run main function
run();
