# Dotcms Push Publish

Push publish environment where the sender runs on port 8081 and the receiver on port 8082. Database: postgres.

## Usage

### Endpoint Configuration

In the endpoint configuration, it is mandatory to use a local IP address instead of `localhost` or `127.0.0.1`. You need to know the local IP address of the host where the application is running. In a Linux environment, you can find it using the `ifconfig` command, or `ipconfig` if you are in a Windows environment. The result will be something like `192.168.x.x`.

As an alternative to using a local IP address, you can use directly the alias of the receiver node service, for instance:

To reference the receiver service (`dotcms-receiver`), use the `HTTP` protocol with the alias `dotcms-receiver.local` and port `8082`.

### Environment Setup

1. A local path to the license pack must be set here:

   ```yaml
   - {license_local_path}/license.zip:/data/shared/assets/license.zip
   ```

   The license pack must contain at least two licenses (one for each node in the cluster).

2. A local path to access data in the instance can be set by uncommenting this line:

   ```yaml
   #- {local_data_path}:/data/shared
   ```

3. A custom starter can be set through this line (uncomment and change the starter URL accordingly):

   ```yaml
   #"CUSTOM_STARTER_URL": 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20250722/starter-20250722.zip'
   ```

### Deploying Nodes

To deploy the sender and receiver nodes, use the following command:

```bash
docker-compose up
```

### Undeploying Nodes

To undeploy the sender and receiver nodes, use the following command:

```bash
docker-compose down
```

**Important note**: `ctrl+c` does not destroy instances.
