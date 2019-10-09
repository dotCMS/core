# Running integration tests

## License set up
The integration tests require a valid license to run.
There are two ways to set up a license

##### 1. Using an environment variable you can export before to run the script

Example:
```
export LICENSE_KEY=$(<./dotcms/license.txt)
./integration.sh
```

##### 2. Binding a license file (license.dat) to the docker image
1. Inside this folder create a **license** folder (docker/tests/integration/license)
2. Add a valid **license.dat** file inside the created folder
3. Uncomment the **bind** folder instructions in the **integration-service.yml** files:
    ```
    - type: bind
      source: ./license/license.dat
      target: /custom/dotsecure/license/license.dat
    ```
4. Run the script. Example: 
    ```
    ./integration.sh
    ```

## How to run integration tests using the run script (integration.sh)

#### Arguments
```
  -d      [OPTIONAL]                   database: (postgres as default) -> One of ["postgres", "mysql", "oracle", "mssql"]
  -b      [OPTIONAL]                   branch: (current branch as default)
  -e      [OPTIONAL]                   extra parameters: Must be send inside quotes "
  -r      [OPTIONAL][no arguments]     run only: Will not executed a build of the image, use the -r option if an image was already generated
  -c      [OPTIONAL][no arguments]     cache: allows to use the docker cache otherwhise "--no-cache" will be use when building the image  
```

#### Examples

```
  ./integration.sh
  ./integration.sh -r
  ./integration.sh -c
  ./integration.sh -d mysql
  ./integration.sh -d mysql -b origin/master
  ./integration.sh -d mysql -b myBranchName
  ./integration.sh -e "--debug-jvm"
  ./integration.sh -e "--tests *HTMLPageAssetRenderedTest"
  ./integration.sh -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
  ./integration.sh -d mysql -b origin/master -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
```

## How to run unit tests using the run script (unit.sh) (No license is required)

#### Arguments
```
  -b      [OPTIONAL]                   branch: (current branch as default)
  -e      [OPTIONAL]                   extra parameters: Must be send inside quotes "
  -r      [OPTIONAL][no arguments]     run only: Will not executed a build of the image, use the -r option if an image was already generated
  -c      [OPTIONAL][no arguments]     cache: allows to use the docker cache otherwhise "--no-cache" will be use when building the image  
```

#### Examples

```
  ./unit.sh
  ./unit.sh -r
  ./unit.sh -c
  ./unit.sh -b origin/master
  ./unit.sh -b myBranchName
  ./unit.sh -e "--debug-jvm"
  ./unit.sh -e "--tests *HTMLPageAssetRenderedTest"
  ./unit.sh -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
  ./unit.sh -b origin/master -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
```

## Debug

Run:
`./integration.sh -e "--debug-jvm"`
`./unit.sh -e "--debug-jvm"`

And wait for the integration tests to start running, then you can attach a remote debugger on port `15005`.

# Running individually db images

## How to run using the run script (db.sh)

#### Arguments
```
  -d      [OPTIONAL]                   database: (postgres as default) -> One of ["postgres", "mysql", "oracle", "mssql"]
```

#### Examples

```
  ./db.sh
  ./db.sh -d postgres
  ./db.sh -d mysql
  ./db.sh -d oracle
  ./db.sh -d mssql
```