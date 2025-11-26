Sample kubernetes manifests for dotCMS on [AWS EKS](https://aws.amazon.com/eks/) using AWS services:

* [AWS Load Balancer Controller](https://docs.aws.amazon.com/eks/latest/userguide/aws-load-balancer-controller.html) - load balancer sticky sessions are required only for dotCMS backend access on multi-node/HA deployments
* [AWS EFS](https://aws.amazon.com/efs/) for shared file assets
* [AWS Simple Email Service](https://aws.amazon.com/ses/) SMTP service
* [AWS WAF](https://aws.amazon.com/waf/)

## Healthcheck Notes
The healthchecks are rough guides only. You will need to monitor and tune for your specific implimentation and infrastructure. A couple notes:

* startupProbe must be long enough to allow dotCMS upgrade tasks to run without interruption when upgrading the dotCMS version. The time required for this depends on factors like database size, Postgres db resources, and dotCMS app resources. 
* readinessProbe and ALB healthcheck probes provide similar functionality so be mindful of how they interplay. This is true of any k8s Ingress. 