#!/bin/bash
if [ ! "$1" ]; then
    echo "First argument has to be the file that we analyze"
    exit
fi
Ultimate_OUTPUT=`./Ultimate --console BuchiAutomizerCWithBlockEncoding.xml "$1" --settings settings.epf`

RESULT_PROVEN_TERMINATION=`echo "$Ultimate_OUTPUT" | grep "Buchi Automizer proved that your program is terminating"`
RESULT_UNKNOWN_TERMINATION=`echo "$Ultimate_OUTPUT" | grep "Buchi Automizer was unable to decide termination"`
RESULT_FALSE_TERMINATION=`echo "$Ultimate_OUTPUT" | grep "Nonterminating execution"`

if [ "$RESULT_PROVEN_TERMINATION" ]; then
	echo "YES"
fi
    
if [ "$RESULT_FALSE_TERMINATION" ]; then
	echo "NO"
fi
    
if [ "$RESULT_UNKNOWN_TERMINATION" ]; then
	echo "MAYBE"
fi
echo ""
echo "$Ultimate_OUTPUT"