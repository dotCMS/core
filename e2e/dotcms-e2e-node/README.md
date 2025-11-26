# Running E2E Tests

If you are reading this it's because somehow you are interested in running dotCMS End-to-End tests locally.
Basically there are two main flavors for achieving this:

## Maven way
dotCMS' core is a Maven multi module project. One submodule is `e2e` which in turn is a parent project for its two submodules: `e2e-dotcms-java` anda `e2e-dotmcs-node`.

Since we are only interested in the last one as we will probably deprecate the Java one, here it's how you call the entire suite:  

### Node.js
```shell
./mvnw -pl :dotcms-e2e-node verify -De2e.test.skip=false
```
As every other test in out projects, if it has one or more dependencies, Maven will take care of their lifecycle.
Hence, you will see Docker containers for database, ElasticSearch, Wiremock and (obviously) dotCMS itself being started up and killed down as the tests run.

BTW, E2E will run against a dotCMS container from the latest locally built image.

To manually kill the E2E dependencies run: 
```shell
./mvnw -pl :dotcms-e2e-node -Pdocker-stop -De2e.test.skip=false
```

To run a specific test file or space-delimited directory list you can run:
```shell
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.specific=login.spec.ts
```
Or
```shell
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.specific="frontend/tests/login/ frontend/tests/contentSearch/"
```

To debug, using `Playwright` UI mode, a specific test:
```shell
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.debug="--ui" \
  -De2e.test.specific=login.spec.ts
```

To debug, using `Playwright` debug inspector, a specific test:
```shell
./mvnw -pl :dotcms-e2e-node verify \
  -De2e.test.skip=false \
  -De2e.test.debug="--debug" \
  -De2e.test.specific=login.spec.ts
```

Two advantages of running E2E tests this way is that:
- Everything is taken care for you
- It runs pretty similar to how it's done in our pipeline

Disadvantages:
- Could take longer if you are adding E2E tests to a feature you are working so probably for that matter the "FrontEnd guy" approach works better for you.

### Using POM pattern 

To create reusable tests, we can use the POM pattern. This pattern allows us to create tests that can be reused across different projects.

There are a short import in typescript that allows us to use the POM pattern:

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@pages": ["./src/pages/index"],
      "@locators/*": ["./src/locators/*"],
      "@utils/*": ["./src/utils/*"],
      "@data/*": ["./src/data/*"],
      "@models/*": ["./src/models/*"]
    }
  }
}
```

And then you can use the pages, locators, utils, data and models in your tests. Pages are the main entry point for each feature and they are the ones that will be used to create the tests.

```typescript
import { LoginPage } from '@pages';

test('should login', async () => {
  await loginPage.login();
});
```




## FrontEnd guys way
E2E tests are implemented using Playwright so you will need the following as pre-requisites:
- Node & NPM
- Yarn
- Playwright

Assuming that you at least have Node, NPM and Yarn installed, to install project in yarn run:
```shell
yarn install --frozen-lockfile
```

To install Playwright and its dependencies:
```shell
yarn global add playwright
yarn playwright install-deps
```

Now that we have these packages installed we need to start dotCMS (with its dependencies). This is usually done by calling on a `docker-compose.yml` file of your preference with all the services needed to have a healthy dotCMS instance.

At `e2e/e2e-dotcms-node/frontend/package.json` you can find the available scripts you can call to execute the tests:

```json
  "scripts": {
    "show-report": "if [[ \"$CURRENT_ENV\" != \"ci\" ]]; then  fi",
    "start": "PLAYWRIGHT_JUNIT_SUITE_ID=nodee2etestsuite PLAYWRIGHT_JUNIT_SUITE_NAME='E2E Node Test Suite' PLAYWRIGHT_JUNIT_OUTPUT_FILE='../target/failsafe-reports/TEST-e2e-node-results.xml' yarn playwright test ${PLAYWRIGHT_SPECIFIC} ${PLAYWRIGHT_DEBUG}; yarn run show-report",
    "start-local": "CURRENT_ENV=local yarn run start",
    "start-dev": "CURRENT_ENV=dev yarn run start",
    "start-ci": "CURRENT_ENV=ci yarn run start",
    "post-testing": "PLAYWRIGHT_JUNIT_OUTPUT_FILE='../target/failsafe-reports/TEST-e2e-node-results.xml' node index.js"
  }
```

All these scripts assume that there is a dotCMS instance running at `8080` port.
- `start-local`: runs E2E tests against http://localhost:8080
- `start-dev`: runs E2E tests against http://localhost:4200, that means it runs a `nx serve dotcms-ui`
command before the tests on top of what is found on http://localhost:8080
- `start-ci`: runs E2E tests against http://localhost:8080 in `headless` mode which is how it's done in the pipeline

So, assuming that you are a frontend developer and you are still implementing a feature using the `start-dev` script will make much sense since it will run `nx` to start the app in the `4200` port.

```shell
yarn run start-dev
```

To run a specific E2E test:
```shell
yarn run start-dev login.spec.ts
```

To run E2E tests located in specific folder(s):
```shell
yarn run start-dev tests/login tests/contentSearch
```

To debug, using `Playwright` UI mode, a specific test:
```shell
yarn run start-dev --ui login.spec.ts
```

To debug, using `Playwright` debug inspector, a specific test:
```shell
yarn run start-dev --debug login.spec.ts
```

When running in an environment other than `CI`, a HTML report will be opened in the browser.
