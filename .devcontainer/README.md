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

**From GitHub Web UI**
1. Navigate to the repository on github.com
2. Click the green "Code" button
3. Select "Codespaces" tab
4. Click "Create codespace on [branch-name]"

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

**Default Admin Credentials:**
- Username: `admin@dotcms.com`
- Password: `admin`

### Debugging in Codespaces

GitHub Codespaces fully supports debugging Java applications with breakpoints using VS Code's built-in debugger.

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
   - Select **"Debug dotCMS (Docker)"**
   - Click the green play button

5. **Set breakpoints** in your Java source code
6. **Trigger the code path** by accessing dotCMS in the browser
7. **Debugger pauses** at your breakpoints!

### Working with the Database

```bash
# Connect to PostgreSQL
docker exec -it db psql -U dotcmsdbuser -d dotcms
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

## Advanced Configuration

### Changing the dotCMS Docker Image

The dotCMS Docker image is configured in the docker-compose file:

**Location:** `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`

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
