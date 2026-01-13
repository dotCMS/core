# dotCMS GitHub Codespaces Configuration

This directory contains the configuration for running dotCMS in GitHub Codespaces, providing a complete development environment in the cloud.

## What's Included

The Codespaces environment includes:

- **dotCMS** (latest) - Latest release
- **PostgreSQL 18** with pgvector extension
- **OpenSearch 1.x** - Search and indexing engine
- **Java 21** - Development runtime
- **Node.js LTS** - Frontend tooling
- **Maven** - Build tool
- **GitHub CLI** - Repository management
- **Docker-in-Docker** - Container management

## Getting Started

### 1. Create a Codespace

There are three ways to create a Codespace:

**Option A: From GitHub Web UI**
1. Navigate to the repository on github.com
2. Click the green "Code" button
3. Select "Codespaces" tab
4. Click "Create codespace on [branch-name]"

**Option B: Using GitHub CLI**
```bash
gh codespace create --repo dotCMS/core
```

**Option C: From VS Code**
1. Install the "GitHub Codespaces" extension
2. Press `Cmd/Ctrl + Shift + P`
3. Select "Codespaces: Create New Codespace"
4. Choose the repository

### 2. Wait for Initialization

The first startup takes **5-10 minutes** as it:
- Downloads Docker images (~2-3 minutes)
- Initializes PostgreSQL database (~2-3 minutes)
- Starts OpenSearch (~1-2 minutes)
- Starts dotCMS and loads initial data (~3-5 minutes)

You'll see a notification when dotCMS is ready!

### 3. Access dotCMS

Once ready, click on the "Ports" tab in VS Code and you'll see:

| Port | Service | URL |
|------|---------|-----|
| 8082 | dotCMS HTTP | `http://localhost:8082` |
| 8443 | dotCMS HTTPS | `https://localhost:8443` |
| 9200 | OpenSearch | `http://localhost:9200` |
| 5432 | PostgreSQL | `localhost:5432` |
| 5005 | Java Debug (JDWP) | For remote debugging |

**Default Admin Credentials:**
- Username: `admin@dotcms.com`
- Password: `admin`

## Development Workflow

### Building the Project

```bash
# Quick build (no tests) - ~2-3 minutes
./mvnw clean install -DskipTests

# Build specific module
./mvnw install -pl :dotcms-core -DskipTests

# Using justfile shortcuts
just build
just build-quicker
```

### Running Tests

```bash
# Run specific test class
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest

# Run Postman tests
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=all

# Using justfile
just test-postman ai
```

### Local Development Workflow

#### Option 1: Develop Against Running Docker Container (Recommended for Quick Iterations)

This approach allows you to make changes to the code and rebuild modules without restarting the entire stack:

1. **Make your code changes** in the Codespace editor

2. **Build only the changed module:**
   ```bash
   # For core changes (fastest)
   ./mvnw install -pl :dotcms-core -DskipTests

   # For core + dependencies
   ./mvnw install -pl :dotcms-core --am -DskipTests
   ```

3. **Copy the updated JAR to the running container:**
   ```bash
   # Find the generated JAR
   JAR_PATH="dotCMS/target/dotcms_*.jar"

   # Copy to container (dotCMS will auto-detect and reload in development mode)
   docker cp $JAR_PATH dotcms:/srv/dotserver/

   # OR restart just the dotCMS container
   docker restart dotcms
   ```

4. **Monitor the logs to see your changes:**
   ```bash
   docker logs -f dotcms
   ```

#### Option 2: Full Local Development with IDE

For more complex development requiring debugging, you can run dotCMS directly from the IDE:

1. **Stop the Docker dotCMS container** (keep DB and OpenSearch running):
   ```bash
   docker stop dotcms
   ```

2. **Set required environment variables:**
   ```bash
   export DB_BASE_URL="jdbc:postgresql://localhost:5432/dotcms"
   export DB_USERNAME="dotcmsdbuser"
   export DB_PASSWORD="password"
   export DOT_ES_ENDPOINTS="https://localhost:9200"
   export DOT_ES_AUTH_BASIC_PASSWORD="admin"
   ```

3. **Run dotCMS from Maven:**
   ```bash
   cd dotCMS
   ../mvnw tomcat7:run-war -Dtomcat.port=8080
   ```

4. **Access locally running dotCMS:**
   - URL: http://localhost:8080
   - Admin: admin@dotcms.com / admin

#### Hot Reload for Frontend (Angular) Development

For frontend development in `core-web/`:

```bash
# Terminal 1: Keep backend services running (Docker)
docker ps  # Verify dotcms, db, opensearch are running

# Terminal 2: Start frontend dev server with hot reload
cd core-web
npm install  # First time only
nx run dotcms-ui:serve

# Frontend will be available at http://localhost:4200
# Changes auto-reload without rebuilding backend
```

### Debugging in Codespaces

GitHub Codespaces fully supports debugging Java applications with breakpoints using VS Code's built-in debugger.

#### Setting Up Debug Configuration

1. **Create or update `.vscode/launch.json`** in the workspace root:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug dotCMS Integration Test",
      "request": "launch",
      "mainClass": "",
      "projectName": "dotcms-integration",
      "vmArgs": "-Xmx2g -Dcoreit.test.skip=false",
      "args": "",
      "console": "integratedTerminal"
    },
    {
      "type": "java",
      "name": "Debug Single Test Method",
      "request": "launch",
      "mainClass": "",
      "projectName": "dotcms-integration",
      "vmArgs": "-Xmx2g",
      "console": "integratedTerminal"
    },
    {
      "type": "java",
      "name": "Attach to dotCMS (Remote Debug)",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005,
      "projectName": "dotcms-core"
    },
    {
      "type": "java",
      "name": "Debug Tomcat",
      "request": "launch",
      "mainClass": "org.apache.catalina.startup.Bootstrap",
      "args": "start",
      "vmArgs": "-Xmx2g -Dcatalina.home=${workspaceFolder}/dotCMS/target/tomcat",
      "projectName": "dotcms-core"
    }
  ]
}
```

2. **Save the file** - VS Code will automatically detect it

#### Debugging Unit Tests

**Method 1: Using VS Code Test Explorer (Easiest)**

1. Open a test file (e.g., `ContentTypeAPIImplTest.java`)
2. Click the **Run/Debug** icon next to any test method or class
3. Select **"Debug Test"**
4. Set breakpoints by clicking left of line numbers
5. Test will pause at breakpoints

**Method 2: Using Maven with Debug Port**

```bash
# Run test with debug enabled
./mvnw test -pl :dotcms-integration \
  -Dtest=ContentTypeAPIImplTest \
  -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

Then attach debugger using "Attach to dotCMS (Remote Debug)" configuration.

#### Debugging Integration Tests

1. **Open the test file** you want to debug
2. **Set breakpoints** by clicking left of the line numbers (red dot appears)
3. **Right-click the test class or method** → **Debug Test**
4. **Debugger will start** and pause at your breakpoints

Common test locations:
- Integration tests: `dotcms-integration/src/test/java/`
- Core tests: `dotCMS/src/test/java/`

#### Debugging Running dotCMS Container

To debug the live dotCMS application running in Docker:

1. **Enable remote debugging** in docker-compose.yml:

   Edit `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`:
   ```yaml
   dotcms:
     image: dotcms/dotcms:latest
     environment:
       CMS_JAVA_OPTS: '-Xmx1g -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
     ports:
       - "8082:8082"
       - "8443:8443"
       - "5005:5005"  # Debug port
   ```

2. **Restart dotCMS to apply changes:**
   ```bash
   cd docker/docker-compose-examples/single-node-demo-site
   docker-compose down
   docker-compose up -d
   ```

3. **Forward the debug port** (Codespaces does this automatically, but verify):
   - Check the **Ports** tab in VS Code
   - Ensure port **5005** is forwarded

4. **Attach debugger:**
   - Open **Run and Debug** panel (Cmd/Ctrl + Shift + D)
   - Select **"Attach to dotCMS (Remote Debug)"**
   - Click the green play button

5. **Set breakpoints** in your Java source code
6. **Trigger the code path** by accessing dotCMS in the browser
7. **Debugger pauses** at your breakpoints!

#### Debugging Tips

**Working with Breakpoints:**
- **Set breakpoint:** Click left of line number
- **Conditional breakpoint:** Right-click breakpoint → Edit Breakpoint → Add condition
- **Logpoint:** Right-click line → Add Logpoint (logs without stopping)
- **Exception breakpoints:** Debug panel → Breakpoints → Add Exception Breakpoint

**Debug Actions:**
- **Continue (F5):** Resume execution until next breakpoint
- **Step Over (F10):** Execute current line, move to next
- **Step Into (F11):** Enter method call
- **Step Out (Shift+F11):** Exit current method
- **Restart (Cmd/Ctrl+Shift+F5):** Restart debugging session

**Viewing Variables:**
- **Variables panel:** Shows all local variables and their values
- **Watch panel:** Add expressions to monitor (e.g., `user.getUserId()`)
- **Hover over variables:** See values inline in code
- **Debug Console:** Execute expressions while paused

**Performance Tips:**
- Disable unused breakpoints instead of deleting them
- Use conditional breakpoints to filter noise
- Use logpoints for non-invasive debugging
- Limit breakpoints in hot paths (loops, frequent methods)

#### Example: Debugging a REST Endpoint

Let's debug the ContentType REST endpoint:

1. **Open the file:**
   ```bash
   code dotCMS/src/main/java/com/dotcms/rest/api/v1/contenttype/ContentTypeResource.java
   ```

2. **Set a breakpoint** in the `getContentType` method (around line 150)

3. **Start remote debugging:**
   - Enable debug port in docker-compose.yml (see above)
   - Restart dotCMS
   - Attach debugger using "Attach to dotCMS (Remote Debug)"

4. **Trigger the endpoint:**
   ```bash
   # In a terminal or use the browser
   curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8082/api/v1/contenttype/id/YOUR_CONTENT_TYPE_ID
   ```

5. **Debugger pauses** at your breakpoint - inspect variables, step through code

#### Troubleshooting Debug Issues

**Debugger won't connect:**
```bash
# Check if debug port is open
docker exec dotcms netstat -tuln | grep 5005

# Check Java process has debug enabled
docker exec dotcms ps aux | grep jdwp

# Verify port is forwarded in Codespaces
curl http://localhost:5005  # Should connect or get response
```

**Breakpoints not hitting:**
- Ensure you're debugging the same code version that's running
- Rebuild and redeploy after code changes
- Check if code path is actually executed (add logging)
- Verify source code matches compiled bytecode

**Slow debugging:**
- Reduce heap size if memory constrained
- Disable unnecessary breakpoints
- Use conditional breakpoints instead of stopping on every hit
- Consider using logpoints instead of breakpoints for non-critical paths

### Managing Services

```bash
# Check service status
docker ps

# View dotCMS logs
docker logs -f dotcms

# Restart dotCMS (if needed)
docker restart dotcms

# Stop all services
docker-compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml down

# Start all services again
docker-compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml up -d
```

### Working with the Database

```bash
# Connect to PostgreSQL
docker exec -it db psql -U dotcmsdbuser -d dotcms

# Backup database
docker exec db pg_dump -U dotcmsdbuser dotcms > backup.sql

# Restore database
docker exec -i db psql -U dotcmsdbuser dotcms < backup.sql
```

### Working with OpenSearch

```bash
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# View indices
curl http://localhost:9200/_cat/indices?v

# Search content
curl -X GET "http://localhost:9200/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match_all": {}
  }
}
'
```

## Troubleshooting

### dotCMS won't start

**Check if services are running:**
```bash
docker ps
```

You should see three containers: `dotcms`, `db`, and `opensearch`.

**Check logs for errors:**
```bash
docker logs dotcms
docker logs db
docker logs opensearch
```

**Restart services:**
```bash
cd /workspace
docker-compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml restart
```

### Out of memory errors

Codespaces have limited resources. Try:

```bash
# Check resource usage
docker stats

# Reduce Java heap size (edit docker-compose.yml)
# Change CMS_JAVA_OPTS: '-Xmx1g' to '-Xmx512m'
```

### Port already in use

If ports are already in use:

```bash
# Find process using port
lsof -i :8082

# Kill the process
kill -9 <PID>
```

### Database connection issues

```bash
# Check PostgreSQL is ready
docker exec db pg_isready -U dotcmsdbuser -d dotcms

# Reset database (⚠️ deletes all data)
docker-compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml down -v
docker-compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml up -d
```

## Resource Management

### Codespace Machine Types

GitHub Codespaces offers different machine types:

- **2-core** (8GB RAM, 32GB storage) - Minimum, may be slow
- **4-core** (16GB RAM, 32GB storage) - **Recommended** for dotCMS
- **8-core** (32GB RAM, 64GB storage) - Best performance
- **16-core** (64GB RAM, 128GB storage) - Overkill for most tasks

### Cost Optimization

- **Stop codespace** when not in use (saves compute hours)
- **Delete codespace** when done (saves storage)
- Use **prebuilds** for faster startup (repository setting)

```bash
# Stop codespace (preserves state)
gh codespace stop

# Delete codespace (removes everything)
gh codespace delete
```

## Advanced Configuration

### Changing the dotCMS Docker Image

The dotCMS Docker image is configured in the docker-compose file:

**Location:** `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`

**Current image (line 48):**
```yaml
dotcms:
  image: dotcms/dotcms:latest
```

#### Available Image Options

You can use different dotCMS images depending on your needs:

**1. Specific trunk build:**
```yaml
image: dotcms/dotcms:trunk_322d922
```

**2. Latest trunk build:**
```yaml
image: dotcms/dotcms:trunk
```

**3. Specific release version:**
```yaml
image: dotcms/dotcms:25.01.0
image: dotcms/dotcms:24.10.3
image: dotcms/dotcms:23.10.12
```

#### How to Change the Image

1. **Edit the docker-compose file:**
   ```bash
   # Open in your editor
   code docker/docker-compose-examples/single-node-demo-site/docker-compose.yml

   # Or use vim/nano
   vim docker/docker-compose-examples/single-node-demo-site/docker-compose.yml
   ```

2. **Locate line 48 and change the image:**
   ```yaml
   dotcms:
     image: dotcms/dotcms:YOUR_DESIRED_VERSION
   ```

3. **Apply the changes:**
   ```bash
   cd docker/docker-compose-examples/single-node-demo-site

   # Stop current services
   docker-compose down

   # Pull the new image
   docker-compose pull dotcms

   # Start with new image
   docker-compose up -d
   ```

4. **Verify the new image is running:**
   ```bash
   docker ps | grep dotcms
   docker logs -f dotcms
   ```

#### Finding Available Images

To see all available dotCMS images on Docker Hub:
```bash
# Search Docker Hub
docker search dotcms/dotcms

# View tags on Docker Hub website
# https://hub.docker.com/r/dotcms/dotcms/tags
```

#### Important Notes

- **Data persistence:** Your database and shared data are stored in Docker volumes and will persist across image changes
- **Breaking changes:** Major version upgrades may require database migrations
- **Testing:** Always test image changes in a development environment first
- **Rollback:** If something goes wrong, change back to the previous image and restart

### Customizing Environment Variables

In the same docker-compose.yml file, you can modify dotCMS settings (starting at line 49):

```yaml
dotcms:
  environment:
    CMS_JAVA_OPTS: '-Xmx1g'                    # Java heap size
    DOT_INITIAL_ADMIN_PASSWORD: 'admin'        # Admin password
    CUSTOM_STARTER_URL: 'https://...'          # Starter site URL
    # Add more environment variables as needed
```

Common environment variables:
- `CMS_JAVA_OPTS`: JVM options (memory, GC settings)
- `DOT_INITIAL_ADMIN_PASSWORD`: Initial admin password
- `CUSTOM_STARTER_URL`: Custom starter site ZIP URL
- `DOT_DOTCMS_CLUSTER_ID`: Cluster identifier
- `DOT_ES_ENDPOINTS`: OpenSearch endpoint URL

After making any changes, restart the services:
```bash
cd docker/docker-compose-examples/single-node-demo-site
docker-compose down
docker-compose up -d
```
