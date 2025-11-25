Sample kubernetes manifests for dotCMS on [AWS EKS](https://aws.amazon.com/eks/) using AWS services:
- [AWS Load Balancer Controller](https://docs.aws.amazon.com/eks/latest/userguide/aws-load-balancer-controller.html) - load balancer sticky sessions are required only for dotCMS backend access on multi-node/HA deployments
- [AWS EFS](https://aws.amazon.com/efs/) for shared file assets
- [AWS Simple Email Service](https://aws.amazon.com/ses/) SMTP service
- [AWS WAF](https://aws.amazon.com/waf/)