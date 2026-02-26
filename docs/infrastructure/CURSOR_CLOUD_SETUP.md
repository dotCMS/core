# Cursor Cloud / Headless Linux VM Setup

Steps to run dotCMS in a Cursor Cloud VM or similar headless Linux environment where Docker is not pre-configured.

## Starting services

```bash
# 1. Start Docker daemon (not needed on Mac or standard Linux with Docker service)
sudo dockerd &>/tmp/dockerd.log &
sudo chmod 666 /var/run/docker.sock

# 2. Build
./mvnw install -DskipTests

# 3. Start stack
just dev-start-on-port 8082
```

Admin UI: `http://localhost:8082/dotAdmin` — `admin@dotcms.com` / `admin`

## Known issues in Cursor Cloud VMs

- **Docker storage driver must be `vfs`** — `fuse-overlayfs` causes dpkg-divert failures.
  Fix: `/etc/docker/daemon.json` → `{"storage-driver": "vfs"}`

- **iptables must be legacy** — default `nftables` breaks Docker networking.
  Fix: `sudo update-alternatives --set iptables /usr/sbin/iptables-legacy`

> These settings are specific to Cursor Cloud VM images. Do not apply them to Mac or standard Linux environments.