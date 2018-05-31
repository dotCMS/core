

# How to run the REST endpoints tests 

The intention of these tests is to validate the response, headers, and return codes for all endpoints. 

### *__Requirements__*
1. To run this set of tests locally you need to have **npn** installed in your computer, if you don't have it installed, follow this guide to install it via _homebrew_. 
    ```
    brew update
    brew install node
    brew upgrade node
    ```
2. Now, make sure you have installed **node** and **npm** by running these commands in your terminal 
    ```
    node -v
    npm -v
    ```
3. Once you have **npm**, you should be able to install **newman**. **This is the library to interact with postman.** Run this command to install via **npm**.
    ```
    npm install -g newman
    ```
4. Make sure you have installed newman
    ```
    newman -v 
    ```

Now you have all the required libraries to run these tests from the terminal. 

### *__Running tests__*

To run the tests you need the postman collection json file with all the test cases found in this folder. 

**Make sure you are running dotCMS in the folllow url:** *http://localhost:8080* if not you need to change the value of the **{{serverURL}}** variable in the last lines of each json tests file.  

To execute a collection of tests use: 
``` 
newman run ($postmanCollectionJSONPath)
```

**Also you can import the collections to postman and run each of them individually.** 

### *__How to import collection on postman__*
1. Once you have installed postman locally, open the app and click to the import button (it is on the top of the window. [Image](https://screencast.com/t/KO9OM9YaeQ))
2. Select the collection json file ([Image](https://screencast.com/t/pCO3B1Fkk))
3. To run those test you need a dotCMS instance running locally in the port *8080*, **If you are running dotCMS in a different port or URL, please follow those steps to edit the collection variable in the properties**
 
##### *__To edit:__*
1. Right click in the collection folder, and go to edit
2. Go to the variables tab and change the value [Image](https://screencast.com/t/zZPEKAuI8ra)
3. Click to update

## Documentation: 
* Workflow Resource: https://goo.gl/JKrWr4
* Pages Resource: https://goo.gl/ExjFGM
* Nav Resource: https://goo.gl/xfSfRA
