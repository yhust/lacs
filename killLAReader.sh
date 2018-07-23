#!/bin/bash
ps ax | grep LoadAwareFileReader |awk -F ' ' '{print $1}' | xargs kill -9
