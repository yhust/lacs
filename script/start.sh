#!/bin/bash


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py start lacs
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py describe lacs


exit 0
