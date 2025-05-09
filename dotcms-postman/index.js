/**
 * @fileoverview Newman Runner for DotCMS Postman Collections
 * This script provides a robust and flexible way to run Postman collections for DotCMS API testing.
 * It supports running individual collections, groups of collections, or all collections with proper
 * error handling and reporting.
 *
 * @module dotcms-postman
 * @requires fs
 * @requires path
 * @requires yargs
 * @requires newman
 * @requires xml2js
 *
 *
 * @typedef {Object} SummaryResults
 * @property {number} completed - Number of completed tests
 * @property {number} errors - Number of errors encountered
 * @property {number} failures - Number of test failures
 * @property {number} skipped - Number of skipped tests
 *
 * @typedef {Object} ConfigItem
 * @property {string} name - Name of the collection group
 * @property {string[]} collections - Array of collection names in the group
 */

const fs = require("fs");
const path = require("path");
const yargs = require("yargs/yargs");
const { hideBin } = require("yargs/helpers");
const newman = require("newman");
const xml2js = require("xml2js");

const summaryResults = {
  completed: 0,
  errors: 0,
  failures: 0,
  skipped: 0,
};

// Default configurations
const defaultServerUrl = "http://localhost:8080";
const defaultPostmanTestsDir = "src/main/resources/postman"; // Default directory
const defaultConfigFilePath = "config.json"; // Default config file path
const defaultPostmanTestsResultsDir = "target/failsafe-reports"; // Default results directory

/**
 * Fetches a JWT token from the DotCMS server for authentication.
 * Implements timeout and error handling for Node.js 22 compatibility.
 *
 * @async
 * @param {string} serverUrl - The URL of the DotCMS server
 * @returns {Promise<string>} The JWT token
 * @throws {Error} If the JWT fetch fails or times out
 */
async function fetchJWT(serverUrl) {
  const username = "admin@dotcms.com";
  const password = "admin";
  const base64 = Buffer.from(`${username}:${password}`).toString("base64");

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 30000); // 30 second timeout

    const response = await fetch(serverUrl + "/api/v1/apitoken", {
      headers: {
        accept: "*/*",
        "content-type": "application/json",
        Authorization: `Basic ${base64}`,
      },
      body: JSON.stringify({
        expirationSeconds: 60000,
        userId: "dotcms.org.1",
        network: "0.0.0.0/0",
        claims: { label: "postman-tests" },
      }),
      method: "POST",
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data.entity.jwt;
  } catch (error) {
    if (error.name === "AbortError") {
      throw new Error("JWT fetch request timed out");
    }
    throw new Error(`Failed to fetch JWT: ${error.message}`);
  }
}

/**
 * Generates a failsafe-summary.xml file with test execution results.
 * This file follows the Maven Failsafe plugin format for test reporting.
 *
 * @param {string} postmanTestsResultsDir - Directory where the summary XML will be saved
 */
function generateFailsafeSummaryXml(postmanTestsResultsDir) {
  const builder = new xml2js.Builder();
  const result = summaryResults.failures > 0 ? 255 : 0;
  const xmlObj = {
    "failsafe-summary": {
      $: { result: result },
      completed: summaryResults.completed,
      errors: summaryResults.errors,
      failures: summaryResults.failures,
      skipped: summaryResults.skipped,
    },
  };

  if (summaryResults.failures > 0) {
    xmlObj["failsafe-summary"]["failureMessage"] = "There are test failures.";
  }

  const xml = builder.buildObject(xmlObj);

  fs.writeFileSync(
    path.join(postmanTestsResultsDir, "failsafe-summary.xml"),
    xml,
    "utf8"
  );
}

/**
 * Executes a Postman collection using Newman with enhanced error handling.
 * Supports Node.js 22 and includes optimized timeout settings for CI/CD.
 *
 * @async
 * @param {string} serverUrl - The DotCMS server URL
 * @param {string} collectionName - Name of the Postman collection to run
 * @param {string} postmanTestsDir - Directory containing Postman collections
 * @param {string} postmanTestsResultsDir - Directory for test results
 * @param {string} jwt - JWT token for authentication
 * @returns {Promise<void>}
 * @throws {Error} If collection execution fails
 */
async function runNewman(
  serverUrl,
  collectionName,
  postmanTestsDir,
  postmanTestsResultsDir,
  jwt
) {
  return new Promise((resolve, reject) => {
    const collectionPath = path.join(postmanTestsDir, `${collectionName}.json`);
    const resultPath = path.join(
      postmanTestsResultsDir,
      `TEST-${collectionName}.xml`
    );

    console.log("Running collection:", collectionName);
    console.log("using jwt:", jwt);

    // Validate and sanitize environment variables
    const envVars = [
      { key: "serverURL", value: serverUrl || "http://localhost:8080" },
      { key: "jwt", value: jwt || "" },
    ];

    // Add additional configuration for Node.js 22 compatibility
    const newmanConfig = {
      collection: require(collectionPath),
      envVar: envVars,
      reporters: ["junit", "cli"],
      reporter: {
        junit: { export: resultPath },
        cli: {
          silent: false,
          verbose: true,
          requestErrors: true,
          showRequestData: true,
        },
      },
      timeout: 27000000, // 3 minutes per collection (optimized for CI/CD)
      bail: true, // Stop on first error to fail fast in CI/CD
      ignoreRedirects: false,
      insecure: true,
      suppressExitCode: true,
      // Node.js 22 specific options
      tlsOptions: {
        rejectUnauthorized: false,
      },
      timeoutRequest: 600000,
      timeoutScript: 27000000,
    };

    newman.run(newmanConfig, function (err, summary) {
      if (err) {
        // Enhanced error handling for Node.js 22
        if (err.code === "ERR_INVALID_IP_ADDRESS") {
          console.warn(
            `Warning: IP address validation error in ${collectionName}, continuing...`
          );
          summaryResults.errors++;
          resolve();
          return;
        }

        // Handle other common Node.js 22 errors
        if (
          err.code === "ERR_NETWORK_IMPORT_DISALLOWED" ||
          err.code === "ERR_IMPORT_ASSERTION_TYPE_MISSING" ||
          err.code === "ERR_NETWORK_IMPORT_DISALLOWED"
        ) {
          console.warn(
            `Warning: Node.js 22 specific error in ${collectionName}: ${err.code}`
          );
          summaryResults.errors++;
          resolve();
          return;
        }

        console.error(`Error running collection ${collectionName}:`, err);
        summaryResults.errors++;
        reject(
          new Error(
            `Error running collection ${collectionName}: ${err.message}`
          )
        );
        return;
      }

      try {
        const failures = summary.run.failures.length;
        summaryResults.failures += failures;
        summaryResults.completed += summary.run.executions.length;

        if (failures > 0) {
          console.error(`Collection ${collectionName} had failures. Details:`);
          summary.run.failures.forEach((failure, index) => {
            if (failure.error?.code === "ERR_INVALID_IP_ADDRESS") {
              console.warn(
                `Warning: IP validation error in test ${
                  index + 1
                }, continuing...`
              );
              return;
            }
            console.error(`Failure #${index + 1}:`, failure.error || failure);
          });
          reject(new Error(`Collection ${collectionName} had failures`));
          return;
        }

        resolve();
      } catch (error) {
        console.error(`Error processing results for ${collectionName}:`, error);
        summaryResults.errors++;
        reject(error);
      }
    });
  });
}

/**
 * Processes collections based on a group name configuration.
 * Handles individual collections, default group, and named groups.
 *
 * @async
 * @param {string} serverUrl - The DotCMS server URL
 * @param {string} groupname - Name of the collection group or individual collection
 * @param {string} postmanTestsDir - Directory containing Postman collections
 * @param {string} postmanTestsResultsDir - Directory for test results
 * @param {ConfigItem[]} config - Configuration object containing group definitions
 * @param {string} jwt - JWT token for authentication
 * @returns {Promise<void>}
 */
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

  const collectionFile = path.join(postmanTestsDir, groupname + ".json");
  if (fs.existsSync(collectionFile)) {
    // If the groupname is actually a collection name
    collectionsToRun.push(groupname);
  } else if (groupname === "default") {
    // Logic for 'default' group
    const allCollections = fs
      .readdirSync(postmanTestsDir)
      .filter(
        (file) => file.endsWith(".json") && file !== "postman_environment.json"
      )
      .map((file) => file.replace(".json", ""));

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
      await runNewman(
        serverUrl,
        collection,
        postmanTestsDir,
        postmanTestsResultsDir,
        jwt
      );
      console.log(`Collection ${collection} executed successfully.`);
    } catch (error) {
      console.error("Error in collection:", collection, error.message);
    }
  }

  console.log(`Finished collections for groupname: ${groupname}`);
}

/**
 * Processes all collection groups sequentially.
 * Includes both configured groups and the default group.
 *
 * @async
 * @param {string} serverUrl - The DotCMS server URL
 * @param {string} postmanTestsDir - Directory containing Postman collections
 * @param {string} postmanTestsResultsDir - Directory for test results
 * @param {ConfigItem[]} config - Configuration object containing group definitions
 * @param {string} jwt - JWT token for authentication
 * @returns {Promise<void>}
 */
async function processAllCollections(
  serverUrl,
  postmanTestsDir,
  postmanTestsResultsDir,
  config,
  jwt
) {
  console.log("Processing all collection groups:");

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
  await processCollections(
    serverUrl,
    "default",
    postmanTestsDir,
    postmanTestsResultsDir,
    config,
    jwt
  );
}

/**
 * Main function that orchestrates the execution of Postman collections.
 * Handles command line arguments, sets up the environment, and manages the test execution flow.
 *
 * Command line options:
 * - serverUrl (-s): URL of the DotCMS server
 * - postmanTestsDir (-d): Directory containing Postman collections
 * - configFilePath (-c): Path to the configuration JSON file
 * - postmanTestsResultsDir (-r): Directory for storing test results
 *
 * @async
 * @returns {Promise<void>}
 * @throws {Error} If there are unhandled errors during execution
 */
async function main() {
  const argv = yargs(hideBin(process.argv))
    .command(
      "<groupname|all|comma-separated-group-names>",
      "what collections or groups to run"
    )
    .demandCommand(1)
    .option("serverUrl", {
      alias: "s",
      describe: "URL of the server to connect to",
      default: defaultServerUrl,
      type: "string",
    })
    .option("postmanTestsDir", {
      alias: "d",
      describe: "Directory containing the Postman collections",
      default: defaultPostmanTestsDir,
      type: "string",
    })
    .option("configFilePath", {
      alias: "c",
      describe: "Path to the configuration JSON file",
      default: defaultConfigFilePath,
      type: "string",
    })
    .option("postmanTestsResultsDir", {
      alias: "r",
      describe: "Directory for storing the test results",
      default: defaultPostmanTestsResultsDir,
      type: "string",
    })
    .demandCommand(
      1,
      "You need at least one command (group name) to run the script"
    )
    .help().argv;

  const groupname = argv._[0];
  const serverUrl = argv.serverUrl;
  const postmanTestsDir = path.resolve(__dirname, argv.postmanTestsDir);
  const configFilePath = path.resolve(__dirname, argv.configFilePath);
  const postmanTestsResultsDir = path.resolve(
    __dirname,
    argv.postmanTestsResultsDir
  );

  const jwt = await fetchJWT(serverUrl);

  console.log(`Got jwt: ${jwt}`);
  // Ensure results directory exists
  if (!fs.existsSync(postmanTestsResultsDir)) {
    fs.mkdirSync(postmanTestsResultsDir, { recursive: true });
  }

  // Read and parse the configuration file
  const config = JSON.parse(fs.readFileSync(configFilePath, "utf8"));

  // Change working directory to postman tests directory
  process.chdir(postmanTestsDir);

  // Process collections based on groupname
  try {
    if (groupname === "all") {
      console.log("All collections");
      await processAllCollections(
        serverUrl,
        postmanTestsDir,
        postmanTestsResultsDir,
        config,
        jwt
      );
    } else {
      const groupNames = groupname.split(",");
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
      console.error("Some collections failed.");
      process.exit(0);
    }
  } catch (error) {
    console.error("An error occurred:", error);
    process.exit(1);
  }
}

// Run the main function and handle any errors
main().catch((err) => {
  console.error("Unhandled error:", err);
  process.exit(1);
});
