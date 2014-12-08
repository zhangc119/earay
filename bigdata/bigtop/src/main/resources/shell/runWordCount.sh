#!/bin/bash

if [ $# -ne 3 ]
then
    echo "Usage: $0  hadoopFolder inputFile deleteFolderAfterDone"
    echo "       For eg. $0 wordcount input.txt false"
    exit 1
fi

wcFolder=$1
inputFile=$2
deleteFolder=$3

CMDS="hadoop"
for i in $CMDS
do
  command -v $i >/dev/null && continue || { echo "$i command not found on $(hostname)."; exit 1; }
done

hadoop fs -mkdir -p $wcFolder/in
hadoop fs -put $inputFile $wcFolder/in
echo "listing input for wordcount job"
hadoop fs -ls -h $wcFolder/in
echo "Starting wordcount job"
hadoop jar /usr/lib/hadoop-mapreduce/hadoop-mapreduce-examples.jar wordcount $wcFolder/in $wcFolder/out
hadoop fs -cat $wcFolder/out/part-r-* > $wcFolder.out
if [ "$deleteFolder" = true ] ; then
  hadoop fs -rm -r -f -skipTrash $wcFolder
fi