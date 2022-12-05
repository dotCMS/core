# Demo Reference Implementation

This reference implementation demonstrates how the dotCMS provided containers can be configured to function together in a simple demo configuration.  This configuration only
has two services - one service for dotCMS itself and one for the DB server.

A valid license is NOT required for this configuration to work properly; however, if you want to mount a valid license pack into the dotCMS image, you can do that by replacing this line: 
```#- [serverpath]/license.zip:/data/shared/assets/license.zip```

with a line like:
```
- ./license.zip:/data/shared/assets/license.zip
```
where the path before the colon points to the license pack on the host filesystem.

You should have at least 2GB of RAM dedicated to Docker for both containers to run.
This configuration is not recommended for production use. 

## docker-compose
### startup
1. If desired, ensure license pack is mounted properly into the dotCMS image as discussed above.
2. In the same directory as the docker-compose.yml file, run:
```docker-compose up```  
3. Wait for dotCMS to finish starting up.  The inital startup takes an extra amount of time as it has to create the schema and data for the database.  You know that it is finished starting and intializing when you see ```Deployment of web application directory [/srv/dotserver/tomcat-8.5.32/webapps/ROOT] has finished in```...
4. Now you can access dotCMS via http://localhost:8080/ (or by other relevant IP or DNS entry)

### cleanup
1.  In terminal window where docker-compose was run, hit ```<Ctrl-C>```  This will causing the docker services to stop. 
2. To ensure the networks are stopped and all containers have been stopped cleanly, run ```docker-compose down```
3. These commands will stop all containers and docker networks that were started; however, the data has been persisted in named volumes.
4. The command ```docker volume ls``` will list all of the docker volumes.  If you wish to remove volumes, you can use the ```docker volume rm ... ``` syntax.
