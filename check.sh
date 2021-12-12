#!/bin/sh

rm ticketingsystem/*.class
rm trace 
rm history 

javac -encoding UTF-8 ticketingsystem/Trace.java -d ./bin

result=1

for i in $(seq 1 20); do
    echo -n "testing $i >>>>>>  "
    java -cp bin ticketingsystem/Trace > trace 
    java -jar checker.jar < trace
    if [ $? != 0 ]; then
        echo "Test failed! ::>_<::"
        result=0
        break
    fi
done

if [ $result == 1 ]; then
    echo "All Test passed! Nice Job! :)"
fi
