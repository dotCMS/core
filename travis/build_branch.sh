 #!/bin/bash
echo $TRAVIS_BRANCH
echo $TRAVIS_COMMIT
gcloud builds submit --config=travis/cloudbuild.yaml --substitutions=BRANCH_NAME=$TRAVIS_BRANCH  --substitutions=COMMIT_SHA=$TRAVIS_COMMIT .