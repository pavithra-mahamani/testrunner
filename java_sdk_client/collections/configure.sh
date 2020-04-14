#!/bin/bash
echo hi
java -jar /tmp/javaclient.jar -i $CLUSTER -b $BUCKET -s $SCOPE -c $COLLECTION -o $OP -n $N 
#if var=$(java -jar /tmp/javaclient.jar -i $CLUSTER -b $BUCKET -s $SCOPE -c $COLLECTION -o $OP -n $N)
#then
#  echo "The program exited with success."
#  echo "Here's what it said: $var"
#else
#  echo "The program failed with System.exit($?)"
# echo "Look at the errors above. The failing output was: $var"
#fi

#error=$(java -jar /tmp/javaclient.jar -i $CLUSTER -b $BUCKET -s $SCOPE -c $COLLECTION -o $OP -n $N 2>&1 1>&$out); } {out}>&1
#echo error
