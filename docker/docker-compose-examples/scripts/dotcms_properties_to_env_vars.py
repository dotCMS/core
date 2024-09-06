#!/usr/bin/env python3

import argparse
from pathlib import Path

epilog = """
Retrieve settings from .properties files from "binary install" of dotCMS,
and print them out in docker or k8s yaml environment variable format 

see https://www.dotcms.com/docs/latest/configuration-properties#EnvironmentVariables
"""


def list_properties_files(
    search_path="/home/dotcms/wwwroot/current/plugins/com.dotcms.config/conf",
):
    """
    returns a list of possible dotcms properties files
    """
    properties_files = []
    conf_path = Path(search_path)
    if conf_path.is_file():
        properties_files = [conf_path]
    elif conf_path.is_dir():
        for base in [
            "dotmarketing-config",
            "dotcms-config-cluster",
            "portal",
            "system",
        ]:
            for ext in ["", "-ext"]:
                for ini in ["", ".ini"]:
                    properties_files += list(Path(conf_path).glob(f"**/{base}{ext}.properties{ini}"))
    else:
        print(f"specified path '{conf_path}' is not a file or directory")
    if not properties_files:
        print(f"No properties files found on path: {conf_path}")
    return properties_files


def get_property_value(line):
    """
    parse a single line from a config file
    return a dict e.g.
    {"property" : "CMS_HEAP_SIZE", "value": "10g"}
    """
    ignored_properties = [
        "DOT_ASSET_REAL_PATH",
        "DOT_DOTCMS_LOGGING_HOME",
        "DOT_DYNAMIC_CONTENT_PATH",
        "DOT_TAIL_LOG_LOG_FOLDER",
        "DOT_FELIX_BASE_DIR",
        "DOT_FELIX_FELIX_FILEINSTALL_DIR",
        "DOT_FELIX_FELIX_UNDEPLOYED_DIR",
        "DOT_ES_PATH_DATA",
        "DOT_ES_PATH_WORK",
        "DOT_ES_HTTP_ENABLED",
        "DOT_ES_HTTP_CORS_ENABLED",
        "DOT_ES_HTTP_PORT",
        "DOT_ES_HTTP_HOST",
    ]
    line = line.strip()
    (prop, value) = line.split("=", 1)
    prop = "DOT_" + prop.strip().upper().replace(".", "_")
    value = value.strip()
    if not value:
        value = ""
    # quote yaml literals and int's
    value = yaml_safe_value(value)
    env_var = {}
    if prop not in ignored_properties:
        env_var = {"property": prop, "value": value}
    return env_var


def parse_properties_file(properties_files):
    output_lines = []
    for file in properties_files:
        try:
            with open(file, "r") as f:
                properties_contents = f.readlines()
        except FileNotFoundError:
            continue
        for line in properties_contents:
            line = line.strip()
            if not line:
                pass
            elif "=" in line and not line.startswith("#"):
                parsed_line = get_property_value(line)
                if parsed_line:
                    output_lines.append(parsed_line)
    return output_lines


def print_properties_docker_yml(properties):
    """
    each element in "properties" should either be a dict with a "comment" key, or with both "property" and "value" keys set
    """
    prepend = 6 * " "
    for line in properties:
        print(f'{prepend}{line["property"]}:  {line["value"]}')


def print_properties_k8s(properties):
    """
    each element in "properties" should either be a dict with a "comment" key, or with both "property" and "value" keys set
    """
    prepend = 8 * " "
    print(f"{prepend}env:")
    for line in properties:
        print(f'{prepend}  - name: {line["property"]}')
        print(f"{prepend}    value: {line['value']}")


def yaml_safe_value(value):
    """
    env var values which are ints or literals must be quoted in docker/k8s yaml config files else yaml processor
    does not pass them to docker/k8s as strings

    we also convert yaml null to empty string, though we should not encounter nulls in this script
    """
    yaml_nulls = (
        "null",
        "Null",
        "NULL",
    )

    yaml_bools = (
        "y",
        "Y",
        "yes",
        "Yes",
        "YES",
        "n",
        "N",
        "no",
        "No",
        "NO",
        "true",
        "True",
        "TRUE",
        "false",
        "False",
        "FALSE",
        "on",
        "On",
        "ON",
        "off",
        "Off",
        "OFF",
    )
    try:
        value = int(value)
    except (TypeError, ValueError):
        pass
    if isinstance(value, int) or value in yaml_bools:
        value = f'"{value}"'
    elif value in yaml_nulls:
        value = ""
    return value


if __name__ == "__main__":
    output_choices = ["docker", "k8s"]
    parser = argparse.ArgumentParser(
        description="Convert dotCMS config plugin properties to ENV vars",
        epilog=epilog,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--format",
        choices=output_choices,
        default="docker",
        help="output formats: " + ",".join(output_choices),
    )
    parser.add_argument(
        "propfile",
        help="Filesystem path to a properties file, or a conf directory containing dotcms properties files. These files will reside in plugins/com.dotcms.config/conf if a ROOT config plugin was used"
        )
    args = parser.parse_args()

    properties_files = list_properties_files(args.propfile)
    properties = parse_properties_file(properties_files)

    print()
    if args.format == "docker":
        print_properties_docker_yml(properties)
    elif args.format == "k8s":
        print_properties_k8s(properties)
    print()
    print("""
## Be sure to verify these values before running in production!
## Remember integers and booleans should be quoted in yaml for docker compose and k8s config.
## Source properties files:
""")
    for file in properties_files:
        print(f"## {file}")
    print()

