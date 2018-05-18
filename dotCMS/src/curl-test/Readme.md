

# How to run the REST endpoints tests 

This is a set of automated tests to validate every workflow use case. This will validate the response, headers, and return codes for all endpoints. 


### *__Requirements__*
1- To run this set locally,  you need to have the npn installed in your computer, if you don't have this installed, follow this guide to install via homebrew. 
```
- brew update
- brew install node
- brew upgrade node
```
2- Now, make sure you have installed **"node"** and **"npm"** by running those commands in your terminal 
```
- node -v
- npm -v
```

3- Once you have npm, you should be able to install **"newman"**. **This is the library to interact with postman.** Run this command to install via npm.
```
- npm install -g newman
```
4- Make sure you have installed newman
```
- newman -v 
```
5- Finally,  you have all the required libraries to run test from terminal. 

### *__Running tests__*

To run this you only need to download the postman collection with all the test cases, this is a json file. 

**Make sure you are running dotCMS in the folllow url:** "http://localhost:8080"; if not you need to change the value of the {{serverURL}} variable in the last lines of the json file.  

To execute this test use: 
``` 
- newman run ($postmanCollectionPath)
```

**Also you can import that collection to postman and run each of them individually.** 

### *__How to import collection on postman__*
1- Once you have installe postman locally, open the app and click to import button that is on the top of the window. [Image](https://screencast.com/t/KO9OM9YaeQ)

2- Now select the downloaded file of the collection. [Image](https://screencast.com/t/pCO3B1Fkk)

3- To run those test you need a dotCMS instance running locally in the port 8080, **If you are running dotCMS in a different port or URL, please follow those steps to edit the collection variable in the properties** 
##### *__To edit:__*
1- Rigth click in the collection folder, and go to edit
2- Go to the variables tab and change the value [Image](https://screencast.com/t/zZPEKAuI8ra)
3- Click to update

Here a doc to see how works each endpoint: 
>Workflow Resource: https://goo.gl/JKrWr4
>Pages Resource: https://goo.gl/ExjFGM
