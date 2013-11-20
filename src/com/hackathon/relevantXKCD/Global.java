package com.hackathon.relevantXKCD;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Global {
	public static HashMap<String, Tuple<Integer, Integer>> globalDict = null;
	public static long cacheMisses = 0;
	
	public final static int BLOCK_SIZE = 500;
	public static HashMap<Integer, Integer> eGlobalDict = null, tGlobalDict = null;
	
	public static HashMap<Integer, HashMap<Integer, Integer>> eDict = null, tDict = null;
	
	public static HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> localCache = new HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>>();
	
    private static MemcacheService cache = MemcacheServiceFactory.getMemcacheService("cache");
    
    public static Classifier<String, String> bayes = new BayesClassifier<String, String>();

    
    public static ArrayList<String> urls;
    
    public static void clearAllCaches() {
    	cache.clearAll();
    }
    
    public static void buildURLs() throws IOException {
    	if(urls != null) {
    		return;
    	}
    	
    	// Should only happen once
        bayes.setMemoryCapacity(1000);
        
    	urls = new ArrayList<String>();
    	
    	FileInputStream fin = null;
		try {
			fin = new FileInputStream("urls");
		} catch (FileNotFoundException e) {
			// cache.put(name, null);
			System.out.println("ERROR: FILE NOT FOUND");
		}
		BufferedReader fileReader = new BufferedReader(new InputStreamReader(
				fin));
		String line;
		
		// Fix off-by-one
		urls.add("");
		while ((line = fileReader.readLine()) != null) {
			urls.add(line.trim());
		}
		System.out.println("URL array size: "+urls.size());
    }
    
    public static void buildEGlobalDict() throws IOException {
    	if(eGlobalDict == null) {
    		eGlobalDict = buildDict("explainDict");
    		assert(eGlobalDict != null);
    	}
    }
    
    public static void buildTGlobalDict() throws IOException {
    	if(tGlobalDict == null) {
    		tGlobalDict = buildDict("transcriptDict");
    		assert(tGlobalDict != null);
    	}
    }
    
    // <dictionary idx, weight>
	public static HashMap<Integer, Integer> buildDict(String name) throws IOException {

		HashMap<Integer, Integer> dict = new HashMap<Integer, Integer>();
		cacheMisses++;
		// System.out.println("Miss in cache for "+name+", building...");
		FileInputStream fin = null;
		try {
			fin = new FileInputStream("dicts/" + name);
		} catch (FileNotFoundException e) {
			// cache.put(name, null);
			System.out.println("ERROR: FILE NOT FOUND");
			return null;
		}
		BufferedReader fileReader = new BufferedReader(new InputStreamReader(
				fin));
		String line;

		while ((line = fileReader.readLine()) != null) {
			String[] split = line.split(" ");
			if (split.length != 3) {
				System.out.println("WARNING: length != 3, is actually "
						+ split.length);
				continue;
			}
			Integer idx = Integer.parseInt(split[0]);
			Integer weight = Integer.parseInt(split[2]);
			dict.put(idx, weight);
		}
		fileReader.close();

		return dict;
	}
    

    public static HashMap<Integer, Integer> getDict(String name, int number) throws IOException {
    	int block = BLOCK_SIZE*(number/BLOCK_SIZE);
    	String key = name+"_"+block;
    	HashMap<Integer, HashMap<Integer, Integer>> entry = null;
    	if(localCache.containsKey(key)) {
    		//System.out.println("  cache hit on key="+key);
    		entry = localCache.get(key);
    	}
    	else {
    		System.out.println("  cache miss on key="+key);
    		entry = getBlockDict(name, block);
    		localCache.put(key, entry);
    	}
    	assert(localCache != null);
    	assert(localCache.containsKey(key));
		return entry.get(number);
    }
    
    @SuppressWarnings("unchecked")
	public static HashMap<Integer, HashMap<Integer, Integer>> getBlockDict(String name, int number) throws IOException {

    	int block = BLOCK_SIZE*(number/BLOCK_SIZE);
    	String cacheKey = name+"_"+Integer.toString(block);
    	//System.out.println("getBlockDict: "+cacheKey);
        byte[] value = (byte[])cache.get(cacheKey); // read from cache

    	
    	if(value != null) {
    		//System.out.println("Hit in cache for "+name);
    		return (HashMap<Integer, HashMap<Integer, Integer>>)fromByteArray(value);
    	}
    	else {
    		cacheMisses++;
    		System.out.println("Miss in cache for "+name+", building...");
    		FileInputStream fin = null;
			try {
				fin = new FileInputStream("dicts/"+name+"_"+Integer.toString(block));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: FILE NOT FOUND!!!");
				return null;
			}
			BufferedReader fileReader = new BufferedReader (new InputStreamReader(fin));
			String line;
			
			String myKey = "", myOldKey = "";
			HashMap<Integer, Integer> cur = null;
			HashMap<Integer, HashMap<Integer, Integer>> build = new HashMap<Integer, HashMap<Integer, Integer>>();
			boolean first = true;
			
			while((line = fileReader.readLine()) != null) {
				if(line.startsWith("-")) {
					//System.out.println("New file: "+myKey);
					myOldKey = myKey;
					myKey = line.replace("-","");
					if(!first) {
						// Write old cache entry
						//cache.put(name+"_"+myOldKey, toByteArray(cur));
						build.put(Integer.parseInt(myOldKey), new HashMap<Integer, Integer>(cur));
				    	//System.out.println("  putting "+Integer.parseInt(myOldKey)+" into build: "+cur);
					}
					first = false;
					// Start of new cache entry
					cur = new HashMap<Integer, Integer>();
					continue;
				}
				String[] split = line.split(" ");
				if(split.length != 3) {
					System.out.println("WARNING: length != 3, is actually "+split.length);
					continue;
				}
				Integer idx = Integer.parseInt(split[0]);
				Integer weight = Integer.parseInt(split[2]);
				cur.put(idx, weight);
			}
			fileReader.close();
			build.put(Integer.parseInt(myOldKey), new HashMap<Integer, Integer>(cur));

			// Add to MemCache
			cache.put(name+"_"+block, toByteArray(build));
			
    		return build;
    	}
    }
    
    public static byte[] toByteArray(Object obj) {
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ObjectOutput out;
    	byte[] b = null; 
		try {
			out = new ObjectOutputStream(bos);
	    	out.writeObject(obj);
	    	b = bos.toByteArray();
	    	out.close();
	    	bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		
    	return b;
    }
    
    public static Object fromByteArray(byte[] b) {
    	ByteArrayInputStream bis = new ByteArrayInputStream(b);
    	ObjectInput in;
    	Object o = null;
		try {
			in = new ObjectInputStream(bis);
	    	o = in.readObject();
	    	bis.close();
	    	in.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	return o;
    }
    
    public static int max(int a, int b) {
    	if(a > b) {
    		return a;
    	}
    	else {
    		return b;
    	}
    }
    
    public static int min(int a, int b) {
    	if(a < b) {
    		return a;
    	}
    	else {
    		return b;
    	}
    }
}