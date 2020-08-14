
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

print('travis pre_ci.py')
print('')
print('')
print('Repo: %s' % os.environ['TRAVIS_REPO_SLUG'])
print('Branch: %s' % branch)
print('Release?: %s' % str(release_properties_exists))
print('Is Pull Request?: %s' % is_PR)
print('Commit: %s' % commit_message)

# Perform main build
print('Starting build')
# we now have our own version of travis-build.sh
#subprocess.call(['curl', '-fsLO', 'https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh'])
build_var1 = os.environ['encrypted_eb7aa63bf7ac_key']
build_var2 = os.environ['encrypted_eb7aa63bf7ac_iv']
subprocess.call(['bash', 'travis-build.sh', build_var1, build_var2])

# Setup conda environment
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
# build_conda()
