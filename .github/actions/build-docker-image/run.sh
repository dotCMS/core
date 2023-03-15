function buildAndPush {
  executeCmd "docker build
    --no-cache
    -t ${INPUT_IMAGE_NAME}
    --build-arg BUILD_FROM=COMMIT
    --build-arg BUILD_ID=${INPUT_BUILD_ID}
    ."
  [[ ${cmd_result} != 0 ]] \
      && echo "Error building docker image" \
      && return 1
  setOutput built_image_name ${INPUT_IMAGE_NAME}
  
  if [[ "${INPUT_PUSH}" == 'true' ]]; then
    [[ -z "${INPUT_GITHUB_USER}" ]] \
      && echo "No Github user was provided, ignoring pushing" \
      && return 2
    [[ -z "${INPUT_GHCR_TOKEN}" ]] \
      && echo "No GHRC Token was provided, ignoring pushing" \
      && return 3
    
    executeCmd "echo ${INPUT_GHCR_TOKEN} | docker login ghcr.io -u ${INPUT_GITHUB_USER} --password-stdin"
    [[ ${cmd_result} != 0 ]] \
      && echo "Error when docker login" \
      && return 4
    
    executeCmd "docker ${INPUT_IMAGE_NAME}"
    return ${cmd_result}
  fi

  return ${cmd_result}
}

pushd ${INPUT_DOCKER_PATH}
buildAndPush
rc=$?
popd

exit ${rc}
