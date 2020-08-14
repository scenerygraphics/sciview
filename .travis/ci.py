
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

# Function for building executable with conda
# def build_conda():
#     from pathlib import Path
#     home = str(Path.home())

#     print('------ BUILD CONDA -----')
    
#     script_name = 'Miniconda3-latest-Linux-x86_64.sh'
#     miniconda_dir = '%s/miniconda' % home
#     subprocess.call(['curl', '-fsLO', 'https://repo.continuum.io/miniconda/%s' % script_name])
#     subprocess.call(['bash', script_name, '-b', '-p', miniconda_dir])
#     subprocess.call(['source', '%s/etc/profile.d/conda.sh' % miniconda_dir])
#     subprocess.call(['hash', '-r'])
#     subprocess.call(['conda', 'config', '--set', 'always_yes', 'yes', '--set', 'changeps1', 'no'])
#     subprocess.call(['conda', 'update', '-q', 'conda'])
#     # Useful for debugging any issues with conda
#     subprocess.call(['conda', 'info', '-a'])

#     # Replace dep1 dep2 ... with your dependencies
#     subprocess.call(['conda', 'env', 'create', '-f', 'environment.yml'])
#     subprocess.call(['conda', 'activate', 'sciview'])
#     subprocess.call(['pyinstaller src/main/python/sciview.py'])
# build_conda()

def package_conda():
    subprocess.call(['sh', 'populate_fiji.sh'])
    subprocess.call(['pyinstaller', '--onefile', '--add-data', 'Fiji.app/jars:jars', 'src/main/python/sciview.py'])

    platform = subprocess.check_output(['uname', '-s'])
    arch = subprocess.check_output(['uname', '-m'])

    if platform == 'Linux' and arch == 'x86_64':
        exe_name = 'sciview-linux64'
    elif platform == 'Linux':
        exe_name = 'sciview-linux32'
    elif platform == 'Darwin':
        exe_name = 'sciview-macos'
    elif platform.startswith('MING'):
        exe_name = 'sciview-win32'
    elif platform.startswith('MSYS_NT'):
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

