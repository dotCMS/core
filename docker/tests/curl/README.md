# Running curl tests

## License set up
The curl tests require a valid license to run.
There are two ways to set up a license.

##### 1. Using an environment variable you can export before to run the script

Example:
```
export LICENSE_KEY=$(<./dotcms/license.txt)
./curl.sh
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
    ./curl.sh
    ```

## How to run curl tests using the run script (curl.sh)

#### Arguments
```
  -d      [OPTIONAL]                   database: (postgres as default) -> One of ["postgres", "mysql", "oracle", "mssql"]
  -b      [OPTIONAL]                   branch: (current branch as default)
  -r      [OPTIONAL][no arguments]     run only: Will not executed a build of the image, use the -r option if an image was already generated
```

#### Examples

```
  ./curl.sh
  ./curl.sh -r
  ./curl.sh -c
  ./curl.sh -d mysql
  ./curl.sh -d mysql -b origin/master
  ./curl.sh -d mysql -b myBranchName
  ./curl.sh -e "--debug-jvm"
  ./curl.sh -e "--tests *HTMLPageAssetRenderedTest"
  ./curl.sh -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
  ./curl.sh -d mysql -b origin/master -e "--debug-jvm --tests *HTMLPageAssetRenderedTest"
```

## Debug

Run:
`./curl.sh -e "--debug-jvm"`

And wait for the curl tests to start running, then you can attach a remote debugger on port `15005`.

# Running individually db images

## How to run using the run script (db.sh)

#### Arguments            
```
  -d      [OPTIONAL]                   database: (postgres as default) -> One of ["postgres", "mysql", "oracle", "mssql"]
```

#### Examples

```
  ../integration/db.sh
  ../integration/db.sh -d postgres
  ../integration/db.sh -d mysql
  ../integration/db.sh -d oracle
  ../integration/db.sh -d mssql
```