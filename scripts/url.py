#!/usr/bin/python
import glob, os, sys, re


# Regular expressions
trivia = re.compile("\"(\/wiki\/images[^\"]+)");
raw = re.compile("raw\_");



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

out = open("urls", "w");
for root, dirs, files in os.walk("raw"):
    files = sorted(files, cmp=compare);
    
    for fi in files:
        filename = os.path.join(root, fi);
        print "Processing " + filename;
        f = open(filename, "r");
        raw = "";
        for line in f:
            raw = raw + line.rstrip().lstrip();
        #print raw;
        #print "-----";
        m = trivia.search(raw);
        if m:
            print m.group(1);
            out.write(m.group(1)+"\n");
        else:
            print "ERROR "+fi;
            out.write("\n");
            #sys.exit(1);
       # raw_input("Press a key to continue...\n");
        
