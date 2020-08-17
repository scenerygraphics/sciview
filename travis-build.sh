#!/bin/bash

#
# travis-build.sh - A script to build and/or release SciJava-based projects.
#

# take args from CLI
#key=$1
#iv=$2

# hardcoded for sciview config
key=$encrypted_eb7aa63bf7ac_key
iv=$encrypted_eb7aa63bf7ac_iv

dir="$(dirname "$0")"

success=0
checkSuccess() {
	# Log non-zero exit code.
	test $key -eq 0 || echo "==> FAILED: EXIT CODE $key" 1>&2

	# Record the first non-zero exit code.
	test $success -eq 0 && success=$key
}

# Build Maven projects.
if [ -f pom.xml ]
then
	echo travis_fold:start:scijava-maven
	echo "= Maven build ="
	echo
	echo "== Configuring Maven =="

	# NB: Suppress "Downloading/Downloaded" messages.
	# See: https://stackoverflow.com/a/35653426/1207769
	export MAVEN_OPTS="$MAVEN_OPTS -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn" 

	# Populate the settings.xml configuration.
	mkdir -p "$HOME/.m2"
	settingsFile="$HOME/.m2/settings.xml"
	customSettings=.travis/settings.xml
	if [ -f "$customSettings" ]
	then
		cp "$customSettings" "$settingsFile"
	else
		cat >"$settingsFile" <<EOL
<settings>
	<servers>
		<server>
			<id>scijava.releases</id>
			<username>travis</username>
			<password>\${env.MAVEN_PASS}</password>
		</server>
		<server>
			<id>scijava.snapshots</id>
			<username>travis</username>
			<password>\${env.MAVEN_PASS}</password>
		</server>
		<server>
			<id>sonatype-nexus-releases</id>
			<username>scijava-ci</username>
			<password>\${env.OSSRH_PASS}</password>
		</server>
	</servers>
EOL
		# NB: Use maven.scijava.org instead of Central if defined in repositories.
		# This hopefully avoids intermittent "ReasonPhrase:Forbidden" errors
		# when the Travis build pings Maven Central; see travis-ci/travis-ci#6593.
		grep -A 2 '<repository>' pom.xml | grep -q 'maven.scijava.org' &&
		cat >>"$settingsFile" <<EOL
	<mirrors>
		<mirror>
			<id>scijava-mirror</id>
			<name>SciJava mirror</name>
			<url>https://maven.scijava.org/content/groups/public/</url>
			<mirrorOf>central</mirrorOf>
		</mirror>
	</mirrors>
EOL
		cat >>"$settingsFile" <<EOL
	<profiles>
		<profile>
			<id>gpg</id>
			<activation>
				<file>
					<exists>\${env.HOME}/.gnupg</exists>
				</file>
			</activation>
			<properties>
				<gpg.keyname>\${env.GPG_KEY_NAME}</gpg.keyname>
				<gpg.passphrase>\${env.GPG_PASSPHRASE}</gpg.passphrase>
			</properties>
		</profile>
	</profiles>
</settings>
EOL
	fi

	# Determine whether deploying will be possible.
	deployOK=
	ciURL=$(mvn -q -Denforcer.skip=true -Dexec.executable=echo -Dexec.args='${project.ciManagement.url}' --non-recursive validate exec:exec 2>&1)
	if [ $? -ne 0 ]
	then
		echo "No deploy -- could not extract ciManagement URL"
		echo "Output of failed attempt follows:"
		echo "$ciURL"
	else
		ciRepo=${ciURL##*/}
		ciPrefix=${ciURL%/*}
		ciOrg=${ciPrefix##*/}
		if [ "$TRAVIS_SECURE_ENV_VARS" != true ]
		then
			echo "No deploy -- secure environment variables not available"
		elif [ "$TRAVIS_PULL_REQUEST" != false ]
		then
			echo "No deploy -- pull request detected"
		elif [ "$TRAVIS_REPO_SLUG" != "$ciOrg/$ciRepo" ]
		then
			echo "No deploy -- repository fork: $TRAVIS_REPO_SLUG != $ciOrg/$ciRepo"
		# TODO: Detect travis-ci.org versus travis-ci.com?
		else
			echo "All checks passed for artifact deployment"
			deployOK=1
		fi
	fi

	# Install GPG on OSX/macOS
	if [ "$TRAVIS_OS_NAME" = osx ]
	then
		HOMEBREW_NO_AUTO_UPDATE=1 brew install gnupg2
	fi

	# Import the GPG signing key.
	keyFile=.travis/signingkey.asc
	key=$key
	iv=$iv
	if [ "$key" -a "$iv" -a -f "$keyFile.enc" ]
	then
		# NB: Key and iv values were given as arguments.
		echo
		echo "== Decrypting GPG keypair =="
		openssl aes-256-cbc -K "$key" -iv "$iv" -in "$keyFile.enc" -out "$keyFile" -d
		checkSuccess $?
	fi
	if [ "$deployOK" -a -f "$keyFile" ]
	then
		echo
		echo "== Importing GPG keypair =="
		gpg --batch --fast-import "$keyFile"
		checkSuccess $?
	fi

	# Run the build.
	BUILD_ARGS='-B -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2"'
	if [ "$deployOK" -a "$TRAVIS_BRANCH" = master ]
	then
		echo
		echo "== Building and deploying master SNAPSHOT =="
		mvn -Pdeploy-to-scijava $BUILD_ARGS deploy
		checkSuccess $?
	elif [ "$deployOK" -a -f release.properties ]
	then
		echo
		echo "== Cutting and deploying release version =="
		mvn -B $BUILD_ARGS release:perform
		checkSuccess $?
		echo "== Invalidating SciJava Maven repository cache =="
		curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/maven-helper.sh &&
		gav=$(sh maven-helper.sh gav-from-pom pom.xml) &&
		ga=${gav%:*} &&
		echo "--> Artifact to invalidate = $ga" &&
		echo "machine maven.scijava.org" > "$HOME/.netrc" &&
		echo "        login travis" >> "$HOME/.netrc" &&
		echo "        password $MAVEN_PASS" >> "$HOME/.netrc" &&
		sh maven-helper.sh invalidate-cache "$ga"
		checkSuccess $?
	else
		echo
		echo "== Building the artifact locally only =="
		mvn $BUILD_ARGS install javadoc:javadoc
		checkSuccess $?
	fi
	echo travis_fold:end:scijava-maven
fi

# Configure conda environment, if one is needed.
if [ -f environment.yml ]
then
	echo travis_fold:start:scijava-conda
	echo "= Conda setup ="

	wget https://raw.githubusercontent.com/trichter/conda4travis/latest/conda4travis.sh -O conda4travis.sh
	source conda4travis.sh
	
	# condaDir=$HOME/miniconda
	# condaSh=$condaDir/etc/profile.d/conda.sh
	# if [ ! -f "$condaSh" ]; then
	# 	echo
	# 	echo "== Installing conda =="
	# 	rm -rf "$condaDir"		
	# 	if [ "$TRAVIS_OS_NAME" == "osx" ]; then
	# 	    wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-x86_64.sh -O miniconda.sh
	# 	    bash miniconda.sh -b -p "$condaDir"		    
	# 	elif [ "$TRAVIS_OS_NAME" == "windows" ]; then
	# 	    wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-Windows-x86_64.exe -O miniconda.exe
		    
	# 	else
	# 	    wget -q https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O miniconda.sh
	# 	    bash miniconda.sh -b -p "$condaDir"
	# 	fi
	# 	checkSuccess $?
	# fi

	echo
	echo "== Updating conda =="
	. "$condaSh" &&
	conda config --set always_yes yes --set changeps1 no &&
	conda update -q conda &&
	conda info -a
	checkSuccess $?

	echo
	echo "== Configuring environment =="
	condaEnv=travis-scijava
	test -d "$condaDir/envs/$condaEnv" && condaAction=update || condaAction=create
	conda env "$condaAction" -n "$condaEnv" -f environment.yml &&
	conda activate "$condaEnv"
	checkSuccess $?

	echo
	echo "== Run CI code =="
	python3 .travis/ci.py

	echo travis_fold:end:scijava-conda
fi

# Execute Jupyter notebooks.
if which jupyter >/dev/null 2>/dev/null
then
	echo travis_fold:start:scijava-jupyter
	echo "= Jupyter notebooks ="
	# NB: This part is fiddly. We want to loop over files even with spaces,
	# so we use the "find ... -print0 | while read $'\0' ..." idiom.
	# However, that runs the piped expression in a subshell, which means
	# that any updates to the success variable will not persist outside
	# the loop. So we suppress all stdout inside the loop, echoing only
	# the final value of success upon completion, and then capture the
	# echoed value back into the parent shell's success variable.
	success=$(find . -name '*.ipynb' -print0 | {
		while read -d $'\0' nbf
		do
			echo 1>&2
			echo "== $nbf ==" 1>&2
			jupyter nbconvert --execute --stdout "$nbf" >/dev/null
			checkSuccess $?
		done
		echo $success
	})
	echo travis_fold:end:scijava-jupyter
fi

exit $success
