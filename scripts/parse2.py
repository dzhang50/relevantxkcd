#!/usr/bin/python
import glob, os, sys, re
import nltk

numTest = 10;
cnt = 0;

# Regular expressions
code = re.compile("\&.+?\;");
alphanum = re.compile("[^0-9A-Za-z ]");
ws = re.compile("\s+");
numbers = re.compile(" [0-9]+ ");
numbersBegin = re.compile("^[0-9]+ ");
numbersEnd = re.compile(" [0-9]+$");
incomplete = re.compile("this explanation may be incomplete or incorrect if you see a way to improve it edit it thanks");

err = open("errstrip_log", "w");
for root, dirs, files in os.walk("parse"):
    files.sort();
    
    for fi in files:
        filename = os.path.join(root, fi);
        print "Processing " + filename;
        f = open(filename, "r");
        lines = [];
        for line in f:
            lines.append(line);
        #print "Explanation:\n"+lines[0];
        #print "Transcript:\n"+lines[1];
        
        if len(lines) != 2:
            print "ERROR: LINES NOT 2";
            print lines;
            sys.exit(1);
            
        results = [];
        for line in lines:
            result = code.sub(" ", line);
            result = alphanum.sub("", result);
            result = numbers.sub(" ", result);
            result = numbersBegin.sub(" ", result);
            result = numbersEnd.sub(" ", result);
            result = ws.sub(" ", result);
            result = result.lower();
            result = incomplete.sub("", result);
            result = ws.sub(" ", result);
            print "-- Parsed --";
            print result;
            results.append(result);

        # Write to file
        out = open("parse2/"+fi, "w");
        out.write(results[0]+"\n");
        out.write(results[1]);
        out.close();
        
        #raw_input("Press a key to continue...\n");
        
