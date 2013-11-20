#!/usr/bin/python
import urllib2, cookielib
import time
import re
import random
import os

baseurl = "http://www.explainxkcd.com/wiki/index.php";
hdr = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11',
       'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
       'Accept-Charset': 'ISO-8859-1,utf-8;q=0.7,*;q=0.3',
       'Accept-Encoding': 'none',
       'Accept-Language': 'en-US,en;q=0.8',
       'Connection': 'keep-alive'};

urls = [];

for i in range(1,1293):
    if(os.path.isfile("raw/raw_"+str(i))):
        print "File "+str(i)+" exists, skipping!";
        continue;

    url = baseurl+"?title="+str(i);

    print "Downloading " + url + "...\n";
    req = urllib2.Request(url, headers=hdr);
    try:
        page = urllib2.urlopen(req);
    except urllib2.HTTPError, e:
        print e.fp.read();
    content = ""
    content = page.read();
    
    out = open("raw/raw_"+str(i), "w");
    out.write(content);
    wait = random.randrange(1,5);
    print "Sleeping for " + str(wait) + " seconds...\n";
    time.sleep(wait);
