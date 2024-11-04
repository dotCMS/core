const fs = require('fs');
const path = require('path');
const yargs = require('yargs/yargs');
const { hideBin } = require('yargs/helpers');
const newman = require('newman');
const xml2js = require('xml2js');

const summaryResults = {
    completed: 0,
    errors: 0,
    failures: 0,
    skipped: 0
};

// Default configurations
const defaultServerUrl = 'http://localhost:8080';
const defaultPostmanTestsDir = 'src/main/resources/postman'; // Default directory
const defaultConfigFilePath = 'config.json'; // Default config file path
const defaultPostmanTestsResultsDir = 'target/failsafe-reports'; // Default results directory


// Function to fetch JWT
async function fetchJWT(serverUrl ) {
    const username = 'admin@dotcms.com';
    const password = 'admin';
    const base64 = Buffer.from(`${username}:${password}`).toString('base64');

    const response = await fetch(serverUrl+"/api/v1/apitoken", {
        "headers": {
            "accept": "*/*",
            "content-type": "application/json",
            "Authorization": `Basic ${base64}`
        },
        "body": JSON.stringify({
            "expirationSeconds": 60000,
            "userId": "dotcms.org.1",
            "network": "0.0.0.0/0",
            "claims": {"label": "postman-tests"}
        }),
        "method": "POST"
    });

    const data = await response.json();
    return data.entity.jwt; // Return JWT token
}


// Function to generate failsafe-summary.xml
function generateFailsafeSummaryXml(postmanTestsResultsDir) {
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

    fs.writeFileSync(path.join(postmanTestsResultsDir, 'failsafe-summary.xml'), xml, 'utf8');
}

// Function to run Newman as a Promise
async function runNewman(serverUrl, collectionName, postmanTestsDir, postmanTestsResultsDir, jwt) {
    return new Promise((resolve, reject) => {
        const collectionPath = path.join(postmanTestsDir, `${collectionName}.json`);
        const resultPath = path.join(postmanTestsResultsDir, `TEST-${collectionName}.xml`);

        console.log('Running collection:', collectionName);

        console.log('using jwt:', jwt);
        newman.run(
            {
                collection: require(collectionPath),
                //delayRequest: 2000,
                envVar: [
                    { key: 'serverURL', value: serverUrl },
                    { key: 'jwt', value: jwt }
                ],
                reporters: ['cli', 'junit'],
                reporter: {
                    junit: { export: resultPath }
                }
            },
            function (err, summary) {
                if (err) {
                    console.error(`Error running collection ${collectionName}:`, err);
                    summaryResults.errors++; // Increment errors
                    reject(new Error(`Error running collection ${collectionName}: ${err.message}`));
                    return;
                }

                const failures = summary.run.failures.length;
                summaryResults.failures += failures;
                summaryResults.completed += summary.run.executions.length; // Assuming each execution is a test

                if (failures > 0) {
                    console.error(`Collection ${collectionName} had failures. Details:`);
                    summary.run.failures.forEach((failure, index) => {
                        console.error(`Failure #${index + 1}:`, failure.error || failure);
                    });
                    reject(new Error(`Collection ${collectionName} had failures`));
                    return;
                }

                resolve();
            }
        );
    });
}
// Function to process collections based on groupname
async function processCollections(
    serverUrl,
    groupname,
    postmanTestsDir,
    postmanTestsResultsDir,
    config,
    jwt
) {
    console.log(`Starting collections for groupname: ${groupname}`);

    let collectionsToRun = [];

    const collectionFile = path.join(postmanTestsDir, groupname + '.json');
    if (fs.existsSync(collectionFile)) {
        // If the groupname is actually a collection name
        collectionsToRun.push(groupname);
    } else if (groupname === 'default') {
        // Logic for 'default' group
        const allCollections = fs
            .readdirSync(postmanTestsDir)
            .filter((file) => file.endsWith('.json') && file !== 'postman_environment.json')
            .map((file) => file.replace('.json', ''));

        const specifiedCollections = config.flatMap((item) => item.collections);
        collectionsToRun = allCollections.filter(
            (collection) => !specifiedCollections.includes(collection)
        );
    } else {
        // Logic for other group names
        const configItem = config.find((item) => item.name === groupname);
        if (configItem) {
            collectionsToRun = configItem.collections;
        } else {
            console.error(`Collection or groupname '${groupname}' not found.`);
            process.exit(1);
        }
    }

    // Run Newman for each collection and track failures
    for (let collection of collectionsToRun) {
        try {
            await runNewman(serverUrl, collection, postmanTestsDir, postmanTestsResultsDir, jwt);
            console.log(`Collection ${collection} executed successfully.`);
        } catch (error) {
            console.error('Error in collection:', collection, error.message);
        }
    }

    console.log(`Finished collections for groupname: ${groupname}`);
}

// Function to process all collections sequentially
async function processAllCollections(serverUrl, postmanTestsDir, postmanTestsResultsDir, config, jwt) {
    console.log('Processing all collection groups:');

    for (const item of config) {
        console.log(`- Group name: ${item.name}`);
    }
    console.log(`- Group name: default`); // Log for the "default" group

    for (const item of config) {
        await processCollections(
            serverUrl,
            item.name,
            postmanTestsDir,
            postmanTestsResultsDir,
            config,
            jwt
        );
    }
    await processCollections(serverUrl, 'default', postmanTestsDir, postmanTestsResultsDir, config, jwt);
}
/**
 * Parses command line arguments for named parameters.
 */
/**
 * Main function to process command line arguments and run the script.
 */
async function main() {
    const argv = yargs(hideBin(process.argv))
        .command('<groupname|all|comma-separated-group-names>', 'what collections or groups to run')
        .demandCommand(1)
        .option('serverUrl', {
            alias: 's',
            describe: 'URL of the server to connect to',
            default: defaultServerUrl,
            type: 'string'
        })
        .option('postmanTestsDir', {
            alias: 'd',
            describe: 'Directory containing the Postman collections',
            default: defaultPostmanTestsDir,
            type: 'string'
        })
        .option('configFilePath', {
            alias: 'c',
            describe: 'Path to the configuration JSON file',
            default: defaultConfigFilePath,
            type: 'string'
        })
        .option('postmanTestsResultsDir', {
            alias: 'r',
            describe: 'Directory for storing the test results',
            default: defaultPostmanTestsResultsDir,
            type: 'string'
        })
        .demandCommand(1, 'You need at least one command (group name) to run the script')
        .help().argv;

    const groupname = argv._[0];
    const serverUrl = argv.serverUrl;
    const postmanTestsDir = path.resolve(__dirname, argv.postmanTestsDir);
    const configFilePath = path.resolve(__dirname, argv.configFilePath);
    const postmanTestsResultsDir = path.resolve(__dirname, argv.postmanTestsResultsDir);

    const jwt = await fetchJWT(serverUrl);

    console.log(`Got jwt: ${jwt}`);
    // Ensure results directory exists
    if (!fs.existsSync(postmanTestsResultsDir)) {
        fs.mkdirSync(postmanTestsResultsDir, { recursive: true });
    }

    // Read and parse the configuration file
    const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

    // Change working directory to postman tests directory
    process.chdir(postmanTestsDir);

    // Process collections based on groupname
    try {
        if (groupname === 'all') {
            console.log('All collections');
            await processAllCollections(serverUrl, postmanTestsDir, postmanTestsResultsDir, config, jwt);
        } else {
            const groupNames = groupname.split(',');
            for (const name of groupNames) {
                await processCollections(
                    serverUrl,
                    name.trim(),
                    postmanTestsDir,
                    postmanTestsResultsDir,
                    config,
                    jwt
                );
            }
        }

        generateFailsafeSummaryXml(postmanTestsResultsDir);

        // Check if any collections failed and exit with a non-zero status code
        if (summaryResults.failures > 0) {
            console.error('Some collections failed.');
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
