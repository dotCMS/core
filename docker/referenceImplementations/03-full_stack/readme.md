# Full Stack Reference Implementation

This reference implementation demonstrates how all of the dotCMS provided containers can be configured to function together.  These containers can be run on single or multiple Docker nodes.

For this configuration to work properly, you need to have a valid license pack mounted into the dotCMS image.  You can do that by replacing this line: 
```#- [serverpath]/license.zip:/data/shared/assets/license.zip```

with a line like:
```
- ./license.zip:/data/shared/assets/license.zip
```
where the path before the colon points to the license pack on the host filesystem.

If you are planning on running this complete stack on a single docker host, you must have at
least 6GB of RAM dedicated to Docker for all of the containers to run.  If you are running
this stack for a production system, normal capacity planning is needed to determine the
amount of resources needed to effeciently handle system load.

## docker-compose
### startup
1. Ensure license pack is mounted properly into the dotCMS image as discussed above.
2. In the same directory as the docker-compose.yml file, run:
```docker-compose up```  
3. Wait for dotCMS to finish starting up.  The inital startup takes an extra amount of time as it has to create the schema and data for the database.  You know that it is finished starting and intializing when you see ```Deployment of web application directory [/srv/dotserver/tomcat-8.5.32/webapps/ROOT] has finished in```...
4. Now you can access dotCMS via http://localhost/ (or by other relevant IP or DNS entry)

### cleanup
1.  In terminal window where docker-compose was run, hit ```<Ctrl-C>```  This will causing the docker services to stop. 
2. To ensure the networks are stopped and all containers have been stopped cleanly, run ```docker-compose down```
3. These commands will stop all containers and docker networks that were started; however, the data has been persisted in named volumes.
4. The command ```docker volume ls``` will list all of the docker volumes.  If you wish to remove volumes, you can use the ```docker volume rm ... ``` syntax.

## kubernetes
For a complete kubernetes example, please refer to demo located here:  [https://github.com/brentgriffin/2018BootcampK8sDemo](https://github.com/brentgriffin/2018BootcampK8sDemo)

