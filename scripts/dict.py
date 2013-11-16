#!/usr/bin/python
import glob, os, sys, re


raw = re.compile("raw\_");

err = open("dict_log", "w");
dicts = [];
dict1 = {};
dict2 = {};
dicts.append(dict1);
dicts.append(dict2);
for root, dirs, files in os.walk("strip"):
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
        
        myDict = {};
        cnt = 0;
        for line in lines:
            words = line.split();
            for word in words:
                # Update local dictionary
                if(myDict.has_key(word)):
                    myDict[word] = myDict[word] + 1;
                else:
                    myDict[word] = 1;
                
                # Update global explanation and transcript 
                # dictionaries for weighting
                if(dicts[cnt].has_key(word)):
                    dicts[cnt][word] = dicts[cnt][word] + 1;
                else:
                    dicts[cnt][word] = 1;
            cnt = cnt + 1;

        # Write Output file
        name = raw.sub("", fi);
        out = open("dicts/"+name, "w");
        sortedDict = [(k, myDict[k]) for k in sorted(myDict, key=myDict.get, reverse=True)];
        for entry in sortedDict:
            out.write(entry[0]+" "+str(entry[1])+"\n");
        out.close();

# Create explanation dictionary
sortedDict = [(k, dicts[0][k]) for k in sorted(dicts[0], key=dicts[0].get, reverse=True)];
out = open("explainDict", "w");
for entry in sortedDict:
    print entry;
    out.write(entry[0]+" "+str(entry[1])+"\n");
out.close();

# Create explanation dictionary
sortedDict = [(k, dicts[1][k]) for k in sorted(dicts[1], key=dicts[1].get, reverse=True)];
out = open("transcriptDict", "w");
for entry in sortedDict:
    print entry;
    out.write(entry[0]+" "+str(entry[1])+"\n");
out.close();
