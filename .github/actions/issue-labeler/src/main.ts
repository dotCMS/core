import * as labeler from './issue-labeler';

/**
 * Main entry point for this action.
 */
const run = async () => {
    labeler.labelIssues();
};

// Run main function
run();
