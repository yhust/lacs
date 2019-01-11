#!/bin/bash

# set $flintrockPemPath as an environmental variable

# $1 is the number of workers and $2 is the numnber of clients

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "sudo rm -R lacs;git clone https://github.com/yhust/lacs.git"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "cd lacs; mvn clean install -DskipTests=true -Dlicense.skip=true -Dcheckstyle.skip -Dmaven.javadoc.skip=true"


# start alluxio
#read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "~/lacs/bin/alluxio format;~/lacs/bin/alluxio-start.sh all SudoMount"


# configuration
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "cp ~/lacs/conf/alluxio-site.properties.template ~/lacs/conf/alluxio-site.properties"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "cp ~/lacs/conf/alluxio-env.sh.template ~/lacs/conf/alluxio-env.sh"
#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "echo   'alluxio.worker.memory.size=32GB' >> ~/lacs/conf/alluxio-site.properties"
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "echo 'alluxio.master.hostname=${line:9}' >> ~/lacs/conf/alluxio-site.properties"
#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "echo 'alluxio.underfs.address=hdfs://${line:9}:9000'>> ~/lacs/conf/alluxio-site.properties"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "mkdir /tmp/alluxio"

python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "echo -e 'alluxio.worker.tieredstore.levels=2\nalluxio.worker.tieredstore.level0.alias=MEM\nalluxio.worker.tieredstore.level1.alias=HDD\nalluxio.worker.tieredstore.level1.dirs.path=/tmp/alluxio\nalluxio.worker.tieredstore.level1.dirs.quota=10GB\nalluxio.lacs.mode=Test' >> ~/lacs/conf/alluxio-site.properties" 


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "echo ${line:9} > ~/lacs/conf/masters"


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "rm  ~/lacs/conf/workers;touch ~/lacs/conf/workers"

i=1
while read -r line
do
    test $i -lt 2 && let "i++" && continue
    test $i -gt $[1+$1] && let "i++" && continue
    python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "echo '${line:9}' >> ~/lacs/conf/workers"
    let "i++"
done < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "mkdir ~/lacs/underFSStorage"

# start alluxio
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "~/lacs/bin/alluxio format;~/lacs/bin/alluxio-start.sh all SudoMount"

#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py copy-file lacs $(cd `dirname $0`; cd ..; pwd)/python/slow_interval.txt /home/ec2-user/lacs/
#python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py copy-file lacs $(cd `dirname $0`; cd ..; pwd)/python/fast_interval.txt /home/ec2-user/lacs/

exit 0

