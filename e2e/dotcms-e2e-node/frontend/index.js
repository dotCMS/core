const fs = require('fs');
const path = require('path');
const xml2js = require('xml2js');

const summaryResults = {
    completed: 0,
    errors: 0,
    failures: 0,
    skipped: 0
};

// Default configurations
const defaultE2eTestsResultsDir = '../target/failsafe-reports'; // Default results directory

// Function to generate failsafe-summary.xml
const generateFailsafeSummaryXml = (e2eTestsResultsDir) => {
    const builder = new xml2js.Builder();
    const result = summaryResults.failures > 0 ? 255 : 0;
    const xmlObj = {
        'failsafe-summary': {
            $: { result: result },
            completed: summaryResults.completed,
            errors: summaryResults.errors,
            failures: summaryResults.failures,
            skipped: summaryResults.skipped
        }
    };

    if (summaryResults.failures > 0) {
        xmlObj['failsafe-summary']['failureMessage'] = 'There are test failures.';
    }

    const xml = builder.buildObject(xmlObj);

    fs.writeFileSync(path.join(e2eTestsResultsDir, 'failsafe-summary.xml'), xml, 'utf8');
}

const processTestsResults = async () => {
    const parser = new xml2js.Parser();
    const xml = fs.readFileSync(process.env.PLAYWRIGHT_JUNIT_OUTPUT_FILE, 'utf8');
    const xmlDoc = await parser.parseStringPromise(xml);

    xmlDoc.testsuites.testsuite.forEach((ts) => {
        const tests = parseInt(ts.tests || '0');
        const errors = parseInt(ts.errors || '0');
        const failures = parseInt(ts.failures || '0');
        const skipped = parseInt(ts.skipped || '0');
        summaryResults.completed += tests;
        summaryResults.errors += errors;
        summaryResults.failures += failures;
        summaryResults.skipped += skipped;
    });
};

/**
 * Main function to process command line arguments and run the script.
 * Parses command line arguments for named parameters.
 */
async function main() {
    const e2eTestsResultsDir = path.resolve(__dirname, defaultE2eTestsResultsDir);
    // Ensure results directory exists
    if (!fs.existsSync(e2eTestsResultsDir)) {
        fs.mkdirSync(e2eTestsResultsDir, { recursive: true });
    }

    try {
        await processTestsResults();
        generateFailsafeSummaryXml(e2eTestsResultsDir);

        if (summaryResults.failures > 0) {
            console.error('Some E2E tests failed.');
            process.exit(0);
        }
    } catch (error) {
        console.error('An error occurred:', error);
        process.exit(1);
    }
}

// Run the main function and handle any errors
main().catch((err) => {
    console.error('Unhandled error:', err);
    process.exit(1);
});
