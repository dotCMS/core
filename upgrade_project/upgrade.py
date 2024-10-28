import os
import subprocess
import configparser
import time  
import requests
from requests.auth import HTTPBasicAuth
import sys 

config = configparser.ConfigParser()
config.read('./properties/config.properties')

# Read properties from the config file
def get_variables(config_section):
    docker_tag_from = config.get(config_section, 'docker_tag_from')
    docker_tag_to = config.get(config_section, 'docker_tag_to')
    custom_starter = config.get(config_section, 'custom_starter')
    expected_db_version = config.get(config_section, 'expected_db_version')
    serverURL = config.get('PLAYWRIGHT', 'BASE_URL')

    return docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL

# Function to set environment variables for docker-compose
def set_env_variables(docker_tag, starter):
    os.environ['docker_tag'] = docker_tag
    os.environ['custom_starter'] = starter


# Function to start dotCMS using docker-compose
def start_dotcms_with_compose():
    print("- Starting dotCMS using docker-compose...")
    subprocess.run(["docker-compose", "up", "-d"], check=True)


# Function to validate if dotCMS is running
def validate_dotcms_isRunning(sleep_seconds):
    try:
        while True:
            output = subprocess.check_output(['docker', 'ps'], text=True)         
            if 'upgrade_project-dotcms-1' in output.lower():
                print("dotCMS is running now... Waiting to start dotCMS...")
                time.sleep(sleep_seconds)
                return True
            else:
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

        # Construct the full URL
        url = f"{server_url}/api/v1/system-status?extended=true"
        
        # Make the request
        response = requests.get(url, auth=HTTPBasicAuth(username, password))
        
        # Check if the status code is 200
        if response.status_code == 200:
            print("System status check successful. Status code:",
                  response.status_code)
            return True
        else:
            print(f"Failed to get system status. Status code: "
                  f"{response.status_code}")
            return False
    except requests.exceptions.RequestException as error:
        print(f"Error during API call: {error}")
        return False


# Function to run the Playwright test script in js
def runTests(testFile):
    js_test_path = testFile
    print(f"- Running Playwright...Validating dotCMS is ready to use...")
    try:
        result = subprocess.run(['npx', 'playwright', 'test',
                                 js_test_path],
                                capture_output=True, text=True)
        print(result.stdout)  # Show Playwright test result
        if result.returncode != 0:
            print(f"Playwright test failed: {result.stderr}")
        else:
            print("- Playwright test passed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"- Error running the Playwright test: {e.stderr}") 


# Function to get the last dotCMS database version
def get_last_db_version():
    # Define the container name
    container_name = "upgrade_project-db-1"

    try:        
        # Step 1: Check if the container is running
        result = subprocess.run(
            ["docker", "ps", "--filter", f"name={container_name}",
             "--format", "{{.Status}}"],
            capture_output=True, 
            text=True
            )
        
        if "Up" not in result.stdout:
            raise Exception(f"Container {container_name} is not running")
        
        # Step 2: Execute the SQL query 
        query = "SELECT * FROM db_version ORDER BY date_update DESC LIMIT 1;"
        command = (f'PGPASSWORD="password" psql -h db -U dotcmsdbuser -d '
                   f'dotcms -c "{query}"')
        
        # Run the command in the Docker container
        print("- Getting the db version from the dotCMS instance...")
        result = subprocess.run(
            ["docker", "exec", container_name, "bash", "-c", command],
            capture_output=True,
            text=True,
            check=True
        )
        
        #print("Command output:")
        #print(result.stdout)  

        lines = result.stdout.strip().split('\n')
        if len(lines) > 2:  # Check if we have results
            db_version_line = lines[2]  # First data line
            # Get the first field and strip whitespace
            db_version = db_version_line.split('|')[0].strip()
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


# Function to compare the database version with the expected version
def compare_versions(db_version, expected_version):
    print("- Comparing the database version with the expected version.")
    if db_version is None:
        print("Cannot compare versions: no database version retrieved.")
        return None

    # Compare and print result
    if db_version == expected_version:
        print("DB Version matches the expected version.")
        return True
    else:
        print(" - ** ERROR: DB Version does not match the expected version.")
        print(f"   * Retrieved Version: {db_version}")
        print(f"   * Expected Version: {expected_version}")
        return False


# Function to stop dotCMS using docker-compose
def stop_dotcms_with_compose():
    print("- Stopping dotCMS using docker-compose...")
    subprocess.run(['docker', 'kill', '$(docker ps -q)'], shell=True,
                   check=True, text=True, capture_output=True)


# Function to clean dotCMS using docker-compose
def clean_dotcms_with_compose():
    print("- Cleaning dotCMS using docker-compose...")
    subprocess.run(["docker-compose", "down"], check=True)

# Function to cleanup Docker containers and volumes
def cleanup():
    try:
        # Remove all stopped containers
        subprocess.run("docker rm -f $(docker ps -a -q)",
                       shell=True,
                       check=True)
        print("- Successfully removed all stopped containers.")

        # Remove all Docker volumes
        subprocess.run("docker volume rm $(docker volume ls -q)",
                       shell=True,
                       check=True)
        print("- Successfully removed all Docker volumes.")
        
    except subprocess.CalledProcessError as e:
        print(f"An error occurred while cleaning up: {e}")


# Function to perform the upgrade
def upgrade_dotcms():
    try:
        # Get variables from the config file
        docker_tag_from, docker_tag_to, custom_starter, expected_db_version, serverURL = get_variables("TEST1")

        print(f"---------------------------------------------------------------------------------------------------------------")
        print(f"      Upgrading dotCMS from {docker_tag_from} to {docker_tag_to}...   ")
        print(f"---------------------------------------------------------------------------------------------------------------")
        print(f" ")

        # Set environment variables for 'from' server
        set_env_variables(docker_tag_from, custom_starter)

        print(f"*************** Starting {docker_tag_from} *************** ")
        print(f" ")
        
        # Start the 'from' version
        start_dotcms_with_compose()

        # Function to validate if dotCMS is running
        is_running = validate_dotcms_isRunning(15)
        print("- Is dotCMS running?", is_running)
        if not is_running:
            raise Exception(f"dotCMS {docker_tag_from} version is not running properly.")

        # Validate if dotCMS is ready to use according the monitoring API
        is_status_ok = check_system_status(serverURL)

        if not is_status_ok:
            raise Exception(f"System status check failed for {docker_tag_from} version.")
        else:
            print("Proceeding with further steps.")

        # Run the Playwright test script
        runTests("./playwrightTests/validateStatus.spec.js")

        # Stop the 'from' version
        stop_dotcms_with_compose()

        # Set environment variables for 'to' release
        set_env_variables(docker_tag_to, custom_starter)
        print(f" ")
        print(f"***************  Upgrading to {docker_tag_to}... *************** ")
        print(f" ")

        # Start the 'to' version
        start_dotcms_with_compose()

        # Function to validate if dotCMS is running
        is_running = validate_dotcms_isRunning(100)
        print("- Is dotCMS running?", is_running)
        if not is_running:
            raise Exception(f"dotCMS {docker_tag_to} version is not running properly.")

        # Validate if dotCMS is ready to use according the monitoring API
        is_status_ok = check_system_status(serverURL)

        if not is_status_ok:
            raise Exception(f"System status check failed for {docker_tag_to} version.")
        else:
            print("Proceeding with further steps.")

        # Validate if the database version is in correct version
        if not compare_versions(get_last_db_version(), expected_db_version):
            raise Exception("Database version is incorrect.")

        # Run the Playwright test script
        runTests("./playwrightTests/upgradeStatus.spec.js")

        # Stop the 'to' version
        clean_dotcms_with_compose()

        print(f" ")
        print("*************** Upgrade process completed *************** ")  
        print(f" ")
      
    except Exception as error:
        print(f"*************** ERROR during the upgrade process: {error} *************** ")
        print(f"Rolling back changes and stopping the process.")
        clean_dotcms_with_compose()  # Make sure dotCMS is stopped if an error occurs
        #cleanup()  # Clean up Docker containers and volumes
        sys.exit(1)  # Exit the function early if any step fails

# Main execution flow
if __name__ == "__main__":
    upgrade_dotcms()




    