#!/usr/bin/python
import glob, os, sys, re

numTest = 10;
cnt = 0;

filters = [1037, 1126, 1190, 1227, 1256, 821, 977];

# Regular expressions
title = re.compile("\<title\>\d+\: (.+) \- explain xkcd");
trivia = re.compile("\Explanation.*<\/h2\>(.+)\<h2\>.*Transcript.*\<\/h2\>(.+)\<h2\>.*Trivia");
notrivia = re.compile("\Explanation.*<\/h2\>(.+)\<h2\>.*Transcript.*\<\/h2\>(.+?)\<span.+Discussion");
tag = re.compile("\<[^>]+\>");
incomplete = re.compile("This explanation may be incomplete or incorrect.  If you see a way to improve it,   edit it !  Thanks.");
ws = re.compile("\s");
raw = re.compile("raw\_");

err = open("parse_err.log", "w");
def compare(x, y):
    name1 = raw.sub("", x);
    name2 = raw.sub("", y);
    i1 = int(name1);
    i2 = int(name2);
    #print "Comparing "+str(i1)+" with "+str(i2);
    if(i1 > i2):
        return 1;
    elif(i1 < i2):
        return -1;
    else:
        return 0;

for root, dirs, files in os.walk("raw"):
    files = sorted(files, cmp=compare);
    #files.sort();
    
    for fi in files:
        skip = 0;
        for filtered in filters:
            if(fi == "raw_"+str(filtered)):
                print fi+" matches "+str(filtered)+", skipping...";
                #sys.exit();
                skip = 1;
        if(skip == 1):
            continue;
        filename = os.path.join(root, fi);
        print "Processing " + filename;
        f = open(filename, "r");
        raw = "";
        for line in f:
            raw = raw + line.rstrip().lstrip();
        #print raw;
        #print "-----";
        m = title.search(raw);
        myTitle = "";
        if m:
            print "Title: "+m.group(1);
            myTitle = m.group(1);
        else:
            print "ERROR: TITLE NOT FOUND\n";
            sys.exit(1);
        m = trivia.search(raw);
        if not m:
            #print "No trivia!";
            m = notrivia.search(raw);
            if not m:
                print "ERROR: BOTH FAILED\n";
                err.write(filename+"\n");
                continue;
                #err.close();
                #sys.exit(1);

        #print "-- Explanation --";
        #print m.group(1);
        #print "-- Transcript --";
        #print m.group(2);
        resultE = tag.sub(" ", m.group(1));
        resultT = myTitle+" "+tag.sub(" ", m.group(2));
        
        resultE = ws.sub(" ", resultE);
        resultT = ws.sub(" ", resultT);
        #print "-- Parsed explanation --";
        #print resultE;
        #print "-- Parsed transcript --";
        #print resultT;
        
        # Write to file
        out = open("parse/"+fi, "w");
        out.write(resultE+"\n");
        out.write(resultT);
        out.close();
        #cnt=cnt+1;
        if(cnt >= numTest):
            sys.exit();
        
        #raw_input("Press a key to continue...\n");
        
