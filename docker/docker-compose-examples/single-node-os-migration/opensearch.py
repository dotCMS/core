#!/usr/bin/env -S uv run --python 3.14 --python-preference managed
# /// script
# requires-python = ">=3.14"
# dependencies = [
#     "requests",
#     "urllib3",
# ]
# ///
import argparse
import json
import logging
import secrets
import string
from datetime import datetime, timedelta

import requests
import urllib3
from requests.auth import HTTPBasicAuth

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(name="opensearch-py")

# suppress https warnings for self-signed certs on ES server
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

es_names_min_length = 3
es_names_max_length = 32


class OpenSearchClientException(Exception):
    """received error response from OpenSearch server"""


class IntrnalUserException(OpenSearchClientException):
    """user unexpectedly present or absent"""


def generate_opensearch_password(length=24):
    alphabet = string.ascii_letters + string.digits + "!@#$%^&*"
    return "".join(secrets.choice(alphabet) for _ in range(length))


class OpenSearchClient:
    """
    sends HTTP requests to OpenSearch server

    scheme str: http or https
    host str: server ip or hostname
    port int: server port
    username str: admin username
    password str: admin password

    usage:
        os_client = OpenSearchClient("admin", "pass!", host="es1.example.com")
        os_client.create_all
    """

    def __init__(
        self,
        username,
        password,
        scheme="https",
        host="127.0.0.1",
        port=9200,
        debug=False,
    ):
        self.server = f"{scheme}://{host}:{port}"
        if username and password:
            self.auth = HTTPBasicAuth(username, password)
        else:
            self.auth = None
        self.debug = debug
        # see https://github.com/dotCMS/infrastructure-as-code/tree/master/kubernetes/guides/10.%20Managed%20Elasticsearch#kibana---permissions
        self.index_permissions = ["indices_all", "indices_monitor"]
        self.cluster_permissions = [
            "cluster:monitor/health",
            "indices:data/write/bulk",
            "cluster:monitor/state",
            "cluster:monitor/nodes/stats",
            "indices:data/read/scroll",
            "indices:data/read/scroll/clear",
        ]
        self.all_index_permissions = [
            "indices:monitor/stats",
            "indices:monitor/settings/get",
            "indices:admin/aliases/get",
        ]

    def create_all(self, customer_name, password=None):
        """create new ES user, roles, mapping"""
        customer_name = customer_name.strip()
        username = self.username_from_customer(customer_name)
        if password:
            password = password.strip()
        else:
            password = generate_opensearch_password()
        assert len(customer_name) >= es_names_min_length
        assert len(customer_name) <= es_names_max_length
        assert len(username) >= es_names_min_length
        assert len(username) <= es_names_max_length
        self.create_user(username, password, customer_name)
        self.create_action_groups(customer_name)
        self.create_role(customer_name)
        self.create_role_mapping(customer_name, username)
        return {"username": username, "password": password}

    def delete_all(self, customer_name):
        """create ES user, roles, mapping"""
        customer_name = customer_name.strip()
        username = self.username_from_customer(customer_name)
        self.delete_role_mapping(customer_name)
        self.delete_action_groups(customer_name)
        self.delete_role(customer_name)
        self.delete_user(username)
        if self.get_users(username) is not None:
            raise IntrnalUserException(f"User '{username}' still exists")

    def http_request(self, verb, uri, payload=None, headers=None, timeout=20):
        """
        requests wrapper
        """
        verb = verb.lower()
        url = self.urljoin(self.server, uri)
        response_error = False
        if verb in ["get", "delete"]:
            response = requests.request(
                verb,
                url,
                auth=self.auth,
                verify=False,
                timeout=timeout,
            )
        elif verb == "put":
            response = requests.request(
                verb,
                url,
                data=payload,
                headers=headers,
                auth=self.auth,
                verify=False,
                timeout=timeout,
            )
            if int(response.status_code) not in (200, 201):
                response_error = True
        if self.debug or response_error:
            print("### OPENSEARCH DEBUG ###")
            print("# REQUEST:")
            print(verb.upper())
            print(url)
            if payload:
                print("data:")
                print(json.dumps(json.loads(payload), indent=2))
            print("# RESPONSE:")
            if response_error:
                logger.error("status: %s", response.status_code)
            else:
                print(f"status: {response.status_code}")
            print(json.dumps(json.loads(response.content.decode()), indent=2))
            print()
        if response_error:
            raise OpenSearchClientException("Unexpected response to OpenSearch request")
        return response

    def urljoin(self, *args):
        """Joins given arguments into an url, removing extra slashes"""
        return "/".join(
            (x.rstrip("/") if x.startswith("//") else x.strip("/") for x in args),
        )

    def username_from_customer(self, customer):
        return f"{customer}-es-user"

    def get_users(self, username):
        URI = f"_plugins/_security/api/internalusers/{username}"
        r = self.http_request("get", URI)
        response_body = str((r.content.decode()))
        if "NOT_FOUND" in response_body:
            return None
        return json.loads(response_body)

    def create_user(self, username, password, customer_name):
        assert username
        if self.get_users(username):
            logger.info("User '%s' already exists, skipping creation", username)
            return
        URI = f"_plugins/_security/api/internalusers/{username}"
        headers = {"Content-type": "application/json"}
        payload = {
            "password": f"{password}",
            "attributes": {"dotcms.client.name.short": f"{customer_name}"},
        }
        payload = json.dumps(payload)
        self.http_request("put", URI, payload, headers)

    def delete_user(self, username):
        URI = f"_plugins/_security/api/internalusers/{username}"
        self.http_request("delete", URI)

    def create_role(self, customer_name):
        URI = f"_plugins/_security/api/roles/{customer_name}-role"
        headers = {"Content-type": "application/json"}
        payload = {
            "cluster_permissions": [f"{customer_name}-cluster"],
            "index_permissions": [
                {
                    "index_patterns": [f"cluster_{customer_name}*"],
                    "allowed_actions": [f"{customer_name}-index"],
                },
                {
                    "index_patterns": ["*"],
                    "allowed_actions": [f"{customer_name}-all-indices"],
                },
            ],
        }
        payload = json.dumps(payload)
        self.http_request("put", URI, payload, headers)

    def delete_role(self, customer_name):
        URI = f"_plugins/_security/api/roles/{customer_name}-role"
        self.http_request("delete", URI)

    def create_action_groups(self, customer_name):
        # Cluster permissions
        URI = f"_plugins/_security/api/actiongroups/{customer_name}-cluster"
        headers = {"Content-type": "application/json"}
        payload = {"allowed_actions": self.cluster_permissions}
        payload = json.dumps(payload)
        self.http_request("put", URI, payload, headers)

        # Index permissions
        URI = f"_plugins/_security/api/actiongroups/{customer_name}-index"
        payload = {"allowed_actions": self.index_permissions}
        payload = json.dumps(payload)
        self.http_request("put", URI, payload, headers)

        # All indices permissions
        URI = f"_plugins/_security/api/actiongroups/{customer_name}-all-indices"
        payload = {"allowed_actions": self.all_index_permissions}
        payload = json.dumps(payload)
        self.http_request("put", URI, payload, headers)

    def delete_action_groups(self, customer_name):
        # Delete cluster permissions/action group
        URI = f"_plugins/_security/api/actiongroups/{customer_name}-cluster"
        self.http_request("delete", URI)

        # Delete index permissions/action group
        URI = f"_plugins/_security/api/actiongroups/{customer_name}-index"
        self.http_request("delete", URI)

        # Delete all indices permissions/action group
        URI = f"_plugins/_security/api/actiongroups/{customer_name}-all-indices"
        self.http_request("delete", URI)

    def create_role_mapping(self, customer_name, username):
        URI = f"_plugins/_security/api/rolesmapping/{customer_name}-role"
        headers = {"Content-type": "application/json"}
        payload = {"users": [f"{username}"]}
        payload = json.dumps(payload)
        self.http_request("put", URI, payload, headers)

    def delete_role_mapping(self, customer_name):
        URI = f"_plugins/_security/api/rolesmapping/{customer_name}-role"
        self.http_request("delete", URI)

    def get_dotcms_indices(self):
        URI = "_cluster/health?level=indices"
        r = self.http_request("get", URI)
        return sorted(
            [index for index in r.json()["indices"] if index.startswith("cluster_")]
        )

    def get_all_aliases(self):
        URI = "_alias"
        r = self.http_request("get", URI)
        return r.json()

    def get_sitesearch_without_aliases(self, exclude_recent=True):
        """
        Used to find old sitesearch indices that have no aliases and can therefore be deleted
        '_alias' returns
        {
            'cluster_ppf-bulgaria-prod.sitesearch_20240321230000_bf022347-e7d6-11ee-9e41-3a83af152fac': {
                'aliases': {}
            },
            'cluster_arqiva-uat-2310.live_20240212205204': {'aliases': {}},
            'cluster_worldline-dev.sitesearch_20240322042500_25ec4c32-e804-11ee-a536-12b1dcc3cd97': {
                'aliases': {'cluster_worldline-dev.support-viveum_ecom-psp_com_NL': {}}
            },
        older_than_days: int - only return indices older than this many days, for cleanup tasks
        """
        without_aliases = set()
        ss = ".sitesearch_"
        today_ss = ss + datetime.now().strftime("%Y%m%d")
        yesterday_ss = ss + (datetime.now() - timedelta(days=1)).strftime("%Y%m%d")
        for index, data in self.get_all_aliases().items():
            if ss not in index:
                continue
            if exclude_recent and (today_ss in index or yesterday_ss in index):
                continue
            if not data["aliases"]:
                without_aliases.add(index)
        return without_aliases

    def parse_index_name(self, full_name):
        """
        index name format:
          cluster_worldline-prod.sitesearch_20240322052500_87b9dbdb-e80c-11ee-8f1c-06e1bcb1d229
        return (customer, env, is_sitesearch)
        """
        try:
            owner, index = full_name.split(".")
            owner = owner.split("_")[1]
            customer, env = owner.split("-", 1)
            logger.debug("%s, %s, %s", customer, env, index.startswith("sitesearch."))
            return [customer, env, index.startswith("sitesearch_")]
        except ValueError:
            logger.debug("Unable to unpack index name '%s', skipping", full_name)

    def filter_dotcms_indices_by_customer(self):
        """
        return a dict
        { customer_name:
           env_name:
             indices: []
             site_search_indices: [],

        """
        customers = dict()
        for index in self.get_dotcms_indices():
            try:
                (customer, env, is_site_search) = self.parse_index_name(index)
            except TypeError:
                continue
            if customer not in customers:
                customers[customer] = {"count": 0}
            if env not in customers[customer]:
                customers[customer][env] = {
                    "indices": [],
                    "site_search_indices": [],
                }
            customers[customer]["count"] += 1
            if is_site_search:
                customers[customer][env]["site_search_indices"].append(index)
            else:
                customers[customer][env]["indices"].append(index)
        return sorted(
            customers.items(),
            key=lambda item: item[1]["count"],
        )

    def get_sitesearch_indices(self):
        [index for index in self.get_dotcms_indices() if ".sitesearch_" in index]

    def delete_index(self, index):
        if not index.startswith("cluster_"):
            msg = f"Index name must start with 'cluster_', received '{index}'"
            logger.error(msg)
            raise ValueError(msg)
        URI = f"{index}"
        self.http_request("delete", URI)


if __name__ == "__main__":
    import os

    parser = argparse.ArgumentParser(description="OpenSearch user provisioning")
    parser.add_argument("--scheme", default=os.environ.get("OS_SCHEME", "https"))
    parser.add_argument("--host", default=os.environ.get("OS_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("OS_PORT", "9200")))
    parser.add_argument("--admin-user", default=os.environ.get("OS_ADMIN_USER"))
    parser.add_argument("--admin-pass", default=os.environ.get("OS_ADMIN_PASS"))
    parser.add_argument("--password", default=os.environ.get("OS_PASSWORD"))
    parser.add_argument("--customer", default=os.environ.get("OS_CUSTOMER"))
    parser.add_argument("--debug", action="store_true",
                        default=os.environ.get("OS_DEBUG", "").lower() in ("1", "true"))
    args = parser.parse_args()

    for field in ("admin_user", "admin_pass", "password", "customer"):
        if not getattr(args, field):
            parser.error(f"--{field.replace('_', '-')} or {('OS_' + field).upper()} required")

    client = OpenSearchClient(
        username=args.admin_user,
        password=args.admin_pass,
        scheme=args.scheme,
        host=args.host,
        port=args.port,
        debug=args.debug,
    )
    result = client.create_all(args.customer, password=args.password)
    logger.info("Created user: %s", result["username"])
