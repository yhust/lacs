#!/bin/bash

# set $flintrockPemPath as an environmental variable

# $1 is the number of workers and $2 is the numnber of clients


#launch
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py launch lacs --num-slaves $(($1+$2))

#set up
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo yum update -y"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo yum install java-1.8.0-openjdk* -y"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs 'export JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk"'

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs 'sudo sed -i s/\$releasever/7/g /etc/yum.repos.d/epel-apache-maven.repo'
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo yum -y install apache-maven"

#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py describe lacs

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo yum -y install git"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "git clone https://github.com/yhust/lacs.git"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo yum -y install python2-pip"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo yum -y install gcc-c++"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo pip install numpy==1.14.4"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo pip install cvxpy==0.4.9"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "cd alluxio-la;mvn clean install -DskipTests=true -Dlicense.skip=true -Dcheckstyle.skip -Dmaven.javadoc.skip=true"

exit 0

