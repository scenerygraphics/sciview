
import os
import subprocess

travis_secure = os.environ['TRAVIS_SECURE_ENV_VARS']
is_PR = os.environ['TRAVIS_PULL_REQUEST']
commit_message = os.environ['TRAVIS_COMMIT_MESSAGE']
branch = os.environ['TRAVIS_BRANCH']
release_properties_exists = os.path.exists('release.properties')

print('Branch: %s' % branch)
print('Release?: %s' % str(release_properties_exists))
print('Is Pull Request?: %s' % is_PR)
print('Commit: %s' % commit_message)

# Perform main build
print('Starting build')
subprocess.call(['curl', '-fsLO', 'https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh'])
build_var1 = os.environ['encrypted_eb7aa63bf7ac_key']
build_var2 = os.environ['encrypted_eb7aa63bf7ac_iv']

# Update sites
print('Checking if upload to update site needed')

## Unstable
## Commit message trigger requires one of these conditions:
## - message begin with SV_IJ_DEPLOY_UNSTABLE
## - push/merge to master

if ( branch == 'master' and not is_PR and travis_secure ) or \
    commit_message.startswith('SV_IJ_DEPLOY_UNSTABLE'):
    print('Upload to SciView-Unstable')
    #subprocess.call(['sh', 'sciview_deploy_unstable.sh'])


## Primary
## Commit message trigger requires one of these conditions:
## - message begin with SV_IJ_DEPLOY_PRIMARY
## - release

if ( branch == 'master' and not is_PR and travis_secure and release_properties_exists ) or \
    commit_message.startswith('SV_IJ_DEPLOY_PRIMARY'):
    print('Upload to SciView')
    #subprocess.call(['sh', 'sciview_deploy.sh'])

