#!/usr/bin/python
import glob, os, sys, re


raw = re.compile("raw\_");

err = open("dict_log", "w");
globalDict = {};
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
        
        for line in lines:
            words = line.split();
            for word in words:
                # Update local dictionary
                if(globalDict.has_key(word)):
                    globalDict[word] = globalDict[word] + 1;
                else:
                    globalDict[word] = 1;

sortedGlobalDict = [(k, globalDict[k]) for k in sorted(globalDict, key=globalDict.get, reverse=True)];
globalDictList = [];
out = open("globalDict", "w");
for entry in sortedGlobalDict:
    #print entry;
    out.write(entry[0]+" "+str(entry[1])+"\n");
    globalDictList.append(entry[0]);
out.close();


#--------------------------------------

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
        
        myDicts = [];
        myDicts.append({});
        myDicts.append({});
        cnt = 0;
        for line in lines:
            words = line.split();
            for word in words:
                # Update local explanation dictionary
                if(myDicts[cnt].has_key(word)):
                    myDicts[cnt][word] = myDicts[cnt][word] + 1;
                else:
                    myDicts[cnt][word] = 1;
                
                # Update global explanation and transcript 
                # dictionaries for weighting
                if(dicts[cnt].has_key(word)):
                    dicts[cnt][word] = dicts[cnt][word] + 1;
                else:
                    dicts[cnt][word] = 1;
            cnt = cnt + 1;

        # Write Output file
        for i in range(2):
            name = raw.sub("", fi);
            f = "";
            if i == 0:
                f = "dicts/explain_"+name;
            elif i == 1:
                f = "dicts/transcript_"+name;
            out = open(f, "w");
            sortedDict = [(k, myDicts[i][k]) for k in sorted(myDicts[i], key=myDicts[i].get, reverse=True)];
            for entry in sortedDict:
                # Find dimension corresponding to word
                idx = globalDictList.index(entry[0]);
                if(idx == -1):
                    print "ERROR: NOT FOUND IN GLOBALDICTLIST";
                    print entry[0];
                    sys.exit(1);
                #print entry[0]+" found at "+str(idx);
                out.write(str(idx)+" "+entry[0]+" "+str(entry[1])+"\n");
            out.close();



# Create explanation dictionary
print "Creating global explanation dictionary";
sortedDict = [(k, dicts[0][k]) for k in sorted(dicts[0], key=dicts[0].get, reverse=True)];
out = open("explainDict", "w");
for entry in sortedDict:
    #print entry;
    # Find dimension corresponding to word
    idx = globalDictList.index(entry[0]);
    if(idx == -1):
        print "ERROR: NOT FOUND IN GLOBALDICTLIST";
        print entry[0];
        sys.exit(1);
    #print entry[0]+" found at "+str(idx);
    out.write(str(idx)+" "+entry[0]+" "+str(entry[1])+"\n");
out.close();

# Create transcript dictionary
print "Creating transcript dictionary";
sortedDict = [(k, dicts[1][k]) for k in sorted(dicts[1], key=dicts[1].get, reverse=True)];
out = open("transcriptDict", "w");
for entry in sortedDict:
    #print entry;
    # Find dimension corresponding to word
    idx = globalDictList.index(entry[0]);
    if(idx == -1):
        print "ERROR: NOT FOUND IN GLOBALDICTLIST";
        print entry[0];
        sys.exit(1);
    #print entry[0]+" found at "+str(idx);
    #out.write(str(idx)+" "+entry[0]+" "+str(entry[1])+"\n");
    out.write(str(idx)+" "+entry[0]+" "+str(entry[1])+"\n");
out.close();
