#!/bin/bash

sed -i '/<\/organization>/i <parent>\n<groupId>org.scijava</groupId>\n<artifactId>pom-scijava</artifactId>\n<version>30.0.0</version>\n<relativePath />\n</parent>' ./build/publications/maven/pom-default.xml 
