
import os
import subprocess

travis_secure = os.environ['TRAVIS_SECURE_ENV_VARS']
PR = os.environ['TRAVIS_PULL_REQUEST']
is_PR = ( PR != 'false' )

# If this is a PR we use the source branch name, and last commit message
if is_PR:
    print('Fetching PR information')
    branch = os.environ['TRAVIS_PULL_REQUEST_BRANCH']

    import requests
    import json
    r = requests.get('https://api.github.com/repos/scenerygraphics/sciview/pulls/%d/commits' % int(PR))

    if r.ok:
        commits = json.loads(r.text or r.content)
        commit_message = commits[-1]['commit']['message']
        print('Commit message: %s' % commit_message)
else:
    branch = os.environ['TRAVIS_BRANCH']
    commit_message = os.environ['TRAVIS_COMMIT_MESSAGE']

release_properties_exists = os.path.exists('release.properties')

print('')
print('')
print('')
print('Repo: %s' % os.environ['TRAVIS_REPO_SLUG'])
print('Branch: %s' % branch)
print('Release?: %s' % str(release_properties_exists))
print('Is Pull Request?: %s' % is_PR)
print('Commit: %s' % commit_message)

# Perform main build
print('Starting build')
subprocess.call(['curl', '-fsLO', 'https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh'])
build_var1 = os.environ['encrypted_eb7aa63bf7ac_key']
build_var2 = os.environ['encrypted_eb7aa63bf7ac_iv']
subprocess.call(['sh', 'travis-build.sh', build_var1, build_var2])

# Update sites
print('')
print('')
print('')
print('Checking if upload to update site needed')

## Unstable
## Commit message trigger requires one of these conditions:
## - message begin with SV_IJ_DEPLOY_UNSTABLE
## - push/merge to master

if ( branch == 'master' and not is_PR and travis_secure ) or \
    commit_message.startswith('SV_IJ_DEPLOY_UNSTABLE'):
    print('Upload to SciView-Unstable')
    subprocess.call(['sh', 'sciview_deploy_unstable.sh'])


## Primary
## Commit message trigger requires one of these conditions:
## - message begin with SV_IJ_DEPLOY_PRIMARY
## - release

if ( branch == 'master' and not is_PR and travis_secure and release_properties_exists ) or \
    commit_message.startswith('SV_IJ_DEPLOY_PRIMARY'):
    print('Upload to SciView')
    subprocess.call(['sh', 'sciview_deploy.sh'])

