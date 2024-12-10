import configparser
import logging
import os
import subprocess
import sys
import time
from configparser import ConfigParser, NoOptionError, NoSectionError

import requests
from requests.auth import HTTPBasicAuth

config = configparser.ConfigParser()
config.read('./properties/config.properties')


# Read properties from the config file
def get_variables(config: ConfigParser, config_section: str):
    def get_config_value(option):
        try:
            return config.get(config_section, option)
        except (NoOptionError, NoSectionError):
            return None

    docker_tag_from = get_config_value('docker_tag_from')
    docker_tag_to = get_config_value('docker_tag_to')
    custom_starter = get_config_value('custom_starter')
    expected_db_version = get_config_value('expected_db_version')
    serverURL = config.get('PLAYWRIGHT', 'BASE_URL')

    return docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL


# Function to set environment variables for docker-compose
def set_env_variables(docker_tag, starter):
    try:
        os.environ.update({'docker_tag': docker_tag, 'custom_starter': starter})
    except Exception as e:
        print(f"Error setting environment variables: {e}")


# Function to start dotCMS using docker-compose
def start_dotcms_with_compose():
    print("- Starting dotCMS using docker-compose...")
    try:
        subprocess.run(["docker-compose", "up", "-d"], check=True)
    except subprocess.CalledProcessError as e:
        print(f"Error starting dotCMS: {e}")
        sys.exit(e.returncode)


# Function to validate if dotCMS is running
def validate_dotcms_isRunning(sleep_seconds):
    try:
        while True:
            output = subprocess.check_output(['docker', 'ps'], text=True)
            if 'upgrade_project-dotcms-1' in output.lower():
                print("dotCMS is running now... Waiting to start dotCMS...")
                time.sleep(sleep_seconds)
                return True
            print("Waiting dotCMS starts...")
            time.sleep(sleep_seconds)
    except subprocess.CalledProcessError as e:
        print(f"Failed to check if dotCMS is running: {e}")
        return False


# Function to  validate if dotCMS is ready to use
def check_system_status(server_url):
    try:
        username = config.get('PLAYWRIGHT', 'username')
        password = config.get('PLAYWRIGHT', 'password')

        if not username or not password:
            logging.error("Username or password not found in configuration.")
            return False

        url = f"{server_url}/api/v1/system-status?extended=true"
        print("- Validating system status... Calling the monitoring API...")
        response = requests.get(url, auth=HTTPBasicAuth(username, password))

        # Check if the status code is 200
        if response.status_code == 200:
            print("System status check successful. Status code:", response.status_code)
            return True
        print(f"Failed to get system status. Status code: "f"{response.status_code}")
        return False
    except requests.exceptions.RequestException as error:
        print(f"Error during API call: {error}")
        return False


# Function to run the Playwright test script in js
def runTests(testFile, tag):
    print(f"- Running Playwright...Validating dotCMS is ready to use...")
    try:
        result = subprocess.run(['npx', 'playwright', 'test', testFile, '--grep', rf'{tag}'],
                                capture_output=True,
                                text=True
                                )

        # Show Playwright test result
        print(result.stdout)
        if result.returncode != 0:
            print("- Playwright test failed.")
            return False
        print("- Playwright test passed successfully.")
        return True
    except subprocess.CalledProcessError as e:
        print(f"Playwright test failed: {e.stderr}")
        sys.exit(e.returncode)
    except Exception as e:
        print(f"- Unexpected error: {str(e)}")
        sys.exit(1)


# Function to get the last dotCMS database version
def get_last_db_version(container):
    try:
        # Step 1: Check if the container is running
        result = subprocess.run(
            ["docker", "ps", "--filter", f"name={container}", "--format", "{{.Status}}"],
            capture_output=True,
            text=True
        )

        if "Up" not in result.stdout:
            raise Exception(f"Container {container} is not running")

        # Step 2: Execute the SQL query 
        query = "SELECT * FROM db_version ORDER BY date_update DESC LIMIT 1;"
        command = (f'PGPASSWORD="password" psql -h db -U dotcmsdbuser -d '
                   f'dotcms -c "{query}"')

        # Run the command in the Docker container
        print("- Getting the db version from the dotCMS instance...")
        result = subprocess.run(
            ["docker", "exec", container, "bash", "-c", command],
            capture_output=True,
            text=True,
            check=True
        )

        lines = result.stdout.strip().split('\n')
        if len(lines) > 2:
            db_version = lines[2].split('|')[0].strip()
            print("dotCMS db Version:", db_version)
            return db_version
        else:
            print("No version found in the database.")
            return None

    except subprocess.CalledProcessError as e:
        print("Error executing SQL query:")
        print(e.stderr)
    except Exception as error:
        print(f"Error: {error}")
        return None


# Function to compare the database version with the expected version
def compare_versions(db_version: str, expected_version: str) -> bool:
    print("- Comparing the database version with the expected version.")
    if not db_version:
        print("Cannot compare versions: no database version retrieved.")
        return False

    if db_version == expected_version:
        return True
    else:
        print(" - ** ERROR: DB Version does not match the expected version.")
        print(f"   * Retrieved Version: {db_version}")
        print(f"   * Expected Version: {expected_version}")
        return False


# Function to stop dotCMS using docker-compose
def stop_containers():
    print("- Stopping dotCMS...")
    try:
        # Get the list of running container IDs
        result = subprocess.run(['docker', 'ps', '-q'], check=True, text=True, capture_output=True)
        container_ids = result.stdout.split()

        # Stop each container
        for container_id in container_ids:
            subprocess.run(['docker', 'stop', container_id], check=True)

        print("All containers stopped.")
    except subprocess.CalledProcessError as e:
        print(f"Error stopping containers: {e}")
        sys.exit(1)


# Function to clean dotCMS using docker-compose
def clean_dotcms_with_compose():
    print("- Cleaning dotCMS using docker-compose...")
    try:
        subprocess.run(["docker-compose", "down"], check=True)
        print("dotCMS cleaned successfully.")
    except subprocess.CalledProcessError as e:
        print(f"An error occurred while cleaning dotCMS: {e}")
        sys.exit(e.returncode)
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        sys.exit(1)


# Function to perform the upgrade
def upgrade_dotcms(docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL) -> bool:
    try:
        print(
            f"---------------------------------------------------------------------------------------------------------------")
        print(f"      Upgrading dotCMS from {docker_tag_from} to {docker_tag_to}...   ")
        print(
            f"---------------------------------------------------------------------------------------------------------------")
        print(f" ")

        # Set environment variables for 'from' server
        set_env_variables(docker_tag_from, custom_starter)

        print(f"*************** Starting {docker_tag_from} *************** ")
        print(f" ")

        # Start the 'from' version
        start_dotcms_with_compose()

        # Validate if dotCMS is running
        print("- Is dotCMS running?")
        if not validate_dotcms_isRunning(15):
            raise RuntimeError(f"dotCMS {docker_tag_from} version is not running properly.")

        # Validate system status (monitor API)
        if not check_system_status(serverURL):
            raise RuntimeError(f"System status check failed for {docker_tag_from} version.")
        print("Proceeding with further steps.")

        # Run the Playwright test script
        runTests("./playwrightTests/pw_e2e.spec.ts", "@beforeUpgrade")

        # Stop the 'from' version
        stop_containers()

        # Set environment variables for 'to' release
        set_env_variables(docker_tag_to, custom_starter)
        print(f" ")
        print(f"***************  Upgrading to {docker_tag_to}... *************** ")
        print(f" ")

        # Start the 'to' version
        start_dotcms_with_compose()

        # Validate if dotCMS is running
        print("- Is dotCMS running?")
        if not validate_dotcms_isRunning(15):
            raise RuntimeError(f"dotCMS {docker_tag_from} version is not running properly.")

        # Validate system status (monitor API)
        if not check_system_status(serverURL):
            raise RuntimeError(f"System status check failed for {docker_tag_from} version.")
        print("Proceeding with further steps.")

        # Validate database version
        if not compare_versions(get_last_db_version("upgrade_project-db-1"), expected_db_version):
            raise Exception("Database version is incorrect.")
        print("Database version is correct.")

        # Run the Playwright test script
        runTests("./playwrightTests/pw_e2e.spec.ts", "@afterUpgrade")

        # Stop the 'to' version
        clean_dotcms_with_compose()

        print(f" ")
        print("*************** Upgrade process completed *************** ")
        print(f" ")
        return True

    except Exception as error:
        print(f"*************** ERROR during the upgrade process: {error} *************** ")
        print(f"Rolling back changes and stopping the process.")
        clean_dotcms_with_compose()  # Make sure dotCMS is stopped if an error occurs
        return False



def from_23_01_to_main():
    docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL = get_variables(config, "23_01")
    if upgrade_dotcms(docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL):
        print(f"✅ Upgrade from {docker_tag_from} to {docker_tag_to} completed successfully.")
    else:
        print("❌ Upgrade failed.")


def from_23_10_to_main():
    docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL = get_variables(config, "23_10")
    if upgrade_dotcms(docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL):
        print(f"✅ Upgrade from {docker_tag_from} to {docker_tag_to} completed successfully.")
    else:
        print("❌ Upgrade failed.")


def from_24_04_to_main():
    docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL = get_variables(config, "24_04")
    if upgrade_dotcms(docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL):
        print(f"✅ Upgrade from {docker_tag_from} to {docker_tag_to} completed successfully.")
    else:
        print("❌ Upgrade failed.")


# Main execution flow
if __name__ == "__main__":
    from_23_01_to_main()
    from_23_10_to_main()
    from_24_04_to_main()


