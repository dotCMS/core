import * as changelog from './changelog';

/**
 * Main entry point for this action.
 */
const run = async () => {
    changelog.generateChangeLog();
};

// Run main function
run();
