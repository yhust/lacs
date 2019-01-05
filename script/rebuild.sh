#!/bin/bash

# set $flintrockPemPath as an environmental variable

# $1 is the number of workers and $2 is the numnber of clients

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo rm -R lacs;git clone https://github.com/yhust/lacs.git"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "cd lacs; mvn clean install -DskipTests=true -Dlicense.skip=true -Dcheckstyle.skip -Dmaven.javadoc.skip=true"


exit 0

