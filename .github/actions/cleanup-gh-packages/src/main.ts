import * as cleanup from './cleanup-packages'

/**
 * Main entry point for this action.
 */
const run = async () => {
  await cleanup.deletePackages()
}

// Run main function
run()
