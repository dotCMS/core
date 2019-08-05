# Running integration tests

## License set up
The integration tests require a valid license to run.
There are two ways to set up a license

##### 1. Using an environment variable you can export before to run the script

Example:
```
export LICENSE_KEY=$(<./dotcms/license.txt)
./run.sh
```

##### 2. Binding a license file (license.dat) to the docker image
1. Inside this folder create a **license** folder (docker/tests/integration/license)
2. Add a valid **license.dat** file inside the created folder
3. Uncomment the **bind** folder instructions in the **\*-docker-compose.yml** files:
    ```
    - type: bind
      source: ./license/license.dat
      target: /custom/dotsecure/license/license.dat
    ```
4. Run the script. Example: 
    ```
    ./run.sh
    ```

## How to run using the run script (run.sh)

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
  ./run.sh
  ./run.sh -r
  ./run.sh -c
  ./run.sh -d mysql
  ./run.sh -d mysql -b origin/master
  ./run.sh -d mysql -b myBranchName
  ./run.sh -e "--debug-jvm"
  ./run.sh -e "--tests *HTMLPageAssetRenderedTest"
  ./run.sh -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
  ./run.sh -d mysql -b origin/master -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
```
## Debug

Run:
`./run.sh -e "--debug-jvm"`

And wait for the integration tests to start running, then you can attach a remote debugger on port `15005`.
