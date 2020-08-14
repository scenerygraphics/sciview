
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

print('travis ci.py')
print('')
print('')
print('Repo: %s' % os.environ['TRAVIS_REPO_SLUG'])
print('Branch: %s' % branch)
print('Release?: %s' % str(release_properties_exists))
print('Is Pull Request?: %s' % is_PR)
print('Commit: %s' % commit_message)

def package_conda():
    subprocess.call(['sh', 'populate_fiji.sh'])
    subprocess.call(['pyinstaller', '--onefile', '--add-data', 'Fiji.app/jars:jars', 'src/main/python/sciview.py'])

    platform = subprocess.check_output(['uname', '-s'])
    arch = subprocess.check_output(['uname', '-m'])

    print(['platform, arch', platform, arch])
    
    if platform == 'Linux' and arch == 'x86_64':
        exe_name = 'sciview-linux64'
    elif platform == 'Linux':
        exe_name = 'sciview-linux32'
    elif platform == 'Darwin':
        exe_name = 'sciview-macos'
    elif platform[:4] == 'MING':
        exe_name = 'sciview-win32'
    elif platform[:7] == 'MSYS_NT':
        exe_name = 'sciview-win32'

    subprocess.call(['mv', 'dist/sciview', exe_name])

package_conda()

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
    ( '[SV_IJ_DEPLOY_UNSTABLE]' in commit_message ):
    
    print('Upload to SciView-Unstable')
    subprocess.call(['sh', 'sciview_deploy_unstable.sh'])


## Primary
## Commit message trigger requires one of these conditions:
## - message begin with SV_IJ_DEPLOY_PRIMARY
## - release

# TODO: check branch == <pom-release-version>
if ( not is_PR and travis_secure and release_properties_exists ) or \
    ( '[SV_IJ_DEPLOY_PRIMARY]' in commit_message ):
    print('Upload to SciView')
    subprocess.call(['sh', 'sciview_deploy.sh'])

