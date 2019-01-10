#!/bin/bash


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py start lacs
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py describe lacs

# start alluxio
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "~/lacs/bin/alluxio format;~/lacs/bin/alluxio-start.sh all SudoMount"

exit 0
