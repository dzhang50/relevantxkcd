#!/usr/bin/python
import glob, os, sys, re

numTest = 10;
cnt = 0;

err = open("strip_log", "w");
rem = open("remove.list", "r");

remove = rem.readlines();
cnt = 0;
for line in remove:
    remove[cnt] = line.rstrip();
    cnt = cnt + 1;
print remove;

for root, dirs, files in os.walk("parse2"):
    files.sort();
    
    for fi in files:
        filename = os.path.join(root, fi);
        print "Processing " + filename;
        f = open(filename, "r");
        lines = [];
        for line in f:
            lines.append(line);
        
        if len(lines) != 2:
            print "ERROR: LINES NOT 2";
            print lines;
            sys.exit(1);
        
        results = [];
        for line in lines:
            for word in remove:
                line = line.replace(" "+word+" ", " ");
            #print "-- Parsed --";
            #print line;
            results.append(line);
        
        # Write to file
        out = open("strip/"+fi, "w");
        out.write(results[0]);
        out.write(results[1]);
        out.close();
        
        #raw_input("Press a key to continue...\n");
        
