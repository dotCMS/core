## One time setup instructions for single ubuntu testing server:
### Follow instructions here:
https://medium.com/@maheshacharya_44641/install-openshift-origin-on-ubuntu-18-04-7b98773c2ee6

### As root run (optional but makes it easier to interact with oc):
```
sudo -i
oc completion bash >>/etc/bash_completion.d/oc_completion
exit
```

### Run this command so that elasticsearch nodes will startup without error:
```
echo 'vm.max_map_count=262144' | sudo tee -a /etc/sysctl.conf > /dev/null
sudo sysctl -p
```

### If AWS (or other headless) Ubuntu instance, should do the following to address entropy problems:
```
sudo apt-get install rng-tools
sudo vi /etc/default/rng-tools
Add line “HRNGDEVICE=/dev/urandom” and save
sudo /etc/init.d/rng-tools restart
```
Source:  https://www.howtoforge.com/helping-the-random-number-generator-to-gain-enough-entropy-with-rng-tools-debian-lenny



## Single server persistent storage:
Verify persistent storage setup:
On openshift host, create directory structure for named, persistent storage.
Needs to be owned by root:root
Group and other need to have write permissions
E.g. - chmod -R g+w,o+w .
Structure should look like this:
```
.
├── dotassets
├── database
├── es-static
└── plugins
    └── static
With permissions and ownership that look like this:
drwxrwxrwx  2 root root 4096 Jul 29 01:01 dotassets
drwxrwxrwx  2 root root 4096 Jul 29 01:02 database
drwxrwxrwx  2 root root 4096 Jul 29 01:02 es-static
drwxrwxrwx  3 root root 4096 Jul 25 20:19 plugins
```

## Bring up cluster:
```
oc cluster up --routing-suffix=[PUBLICIP].xip.io --public-hostname=[PUBLICHOSTNAME]
```

e.g. :
```
oc cluster up --routing-suffix=3.130.126.14.xip.io --public-hostname=ec2-3-130-126-14.us-east-2.compute.amazonaws.com
```

### Login:
```
oc login -u system:admin
```

### Deploy:
Make sure you have a license.zip file located at the location specified in the setup-01-create-prereq.sh file, then execute:
```
./setup.sh
```

### To create external route to service:
```
oc expose svc haproxy-svc
```

### To get externally available host and port:
```
oc get route
```

## To undeploy:
```
oc delete route haproxy-svc
./cleanup.sh
```

## Take down cluster:
```
oc cluster down
```

&nbsp;
&nbsp;

## Other helpful commands
### To scale up/down:
```
oc scale deployment dotcms --replicas=2
```

###To view logs:
```
oc logs [CONTAINER_NAME] -f
```

