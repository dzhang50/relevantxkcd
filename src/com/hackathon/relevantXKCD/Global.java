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

    private static MemcacheService cache = MemcacheServiceFactory.getMemcacheService("cache");
    
    public static ArrayList<String> urls;
    
    public static void clearAllCaches() {
    	cache.clearAll();
    }
    
    public static void buildURLs() throws IOException {
    	if(urls != null) {
    		return;
    	}
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
    
    public static void buildEDict() throws IOException {
    	if(eGlobalDict == null) {
    		eGlobalDict = buildDict("explainDict");
    		assert(eGlobalDict != null);
    	}
    }
    
    public static void buildTDict() throws IOException {
    	if(tGlobalDict == null) {
    		tGlobalDict = buildDict("transcriptDict");
    		assert(tGlobalDict != null);
    	}
    }
    
    // <dictionary idx, weight>
    @SuppressWarnings("unchecked")
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
    
    

    @SuppressWarnings("unchecked")
	public static HashMap<Integer, Integer> getBlockDict(String name, int number) throws IOException {
    	
    	String cacheKey = name+"_"+Integer.toString(number);
    	//System.out.println("getBlockDict: "+cacheKey);
        byte[] value = (byte[])cache.get(cacheKey); // read from cache

    	
    	if(value != null) {
    		//System.out.println("Hit in cache for "+name);
    		return (HashMap<Integer, Integer>)fromByteArray(value);
    	}
    	else {
    		cacheMisses++;
        	int block = BLOCK_SIZE*(number/BLOCK_SIZE);
    		//System.out.println("Miss in cache for "+name+", building...");
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
			HashMap<Integer, Integer> ret = null;
			HashMap<Integer, Integer> cur = null;
			boolean first = true;
			
			while((line = fileReader.readLine()) != null) {
				if(line.startsWith("-")) {
					//System.out.println("New file: "+myKey);
					myOldKey = myKey;
					myKey = line.replace("-","");
					if(!first) {
						// Write old cache entry
						cache.put(name+"_"+myOldKey, toByteArray(cur));
						//fakeCache.put(Integer.parseInt(myKey), new HashMap<Integer, Integer>(cur));
				    	
						//System.out.println("Putting to cache: "+name+"_"+myKey);
						if(Integer.parseInt(myKey) == number) {
							assert(cur != null);
							ret = cur;
						}
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

			cache.put(name+"_"+myKey, toByteArray(cur));
			/*
			fakeCache.put(Integer.parseInt(myKey), new HashMap<Integer, Integer>(cur));
	    	if(name.equals("explain")) {
	    		fakeECacheOffset = block;
	    	}
	    	else if(name.equals("transcript")) {
	    		fakeTCacheOffset = block;
	    	}
	    	*/
			if(Integer.parseInt(myKey) == number) {
				ret = cur;
			}
			// Add to MemCache
			//cache.put(name, toByteArray(ret));
			
    		return ret;
    	}
    }
    
    /*
    // <dictionary idx, weight>
    @SuppressWarnings("unchecked")
	public static HashMap<Integer, Integer> getDict(String name) throws IOException{
        byte[] value = (byte[])cache.get(name); // read from cache

    	if(value != null) {
    		//System.out.println("Hit in cache for "+name);
    		return (HashMap<Integer, Integer>)fromByteArray(value);
    	}
    	else {
    		HashMap<Integer, Integer> ret = new HashMap<Integer, Integer>();
    		cacheMisses++;
    		//System.out.println("Miss in cache for "+name+", building...");
    		FileInputStream fin = null;
			try {
				fin = new FileInputStream("dicts/"+name);
			} catch (FileNotFoundException e) {
				//cache.put(name, null);
				return null;
			}
			BufferedReader fileReader = new BufferedReader (new InputStreamReader(fin));
			String line;
			
			String myKey = "";
			while((line = fileReader.readLine()) != null) {
				String[] split = line.split(" ");
				if(split.length != 3) {
					System.out.println("WARNING: length != 3, is actually "+split.length);
					continue;
				}
				Integer idx = Integer.parseInt(split[0]);
				Integer weight = Integer.parseInt(split[2]);
				ret.put(idx, weight);
			}
			fileReader.close();
			
			// Add to MemCache
			cache.put(name, toByteArray(ret));
			
    		return ret;
    	}
    }
    
    

    @SuppressWarnings("unchecked")
	public static HashMap<Integer, Integer> getBlockDict(String name, int number) throws IOException {
    	
    	String cacheKey = name+"_"+Integer.toString(number);
    	//System.out.println("getBlockDict: "+cacheKey);
        byte[] value = (byte[])cache.get(cacheKey); // read from cache

    	
    	if(value != null) {
    		//System.out.println("Hit in cache for "+name);
    		return (HashMap<Integer, Integer>)fromByteArray(value);
    	}
    	else {
    		cacheMisses++;
        	int block = BLOCK_SIZE*(number/BLOCK_SIZE);
    		//System.out.println("Miss in cache for "+name+", building...");
    		FileInputStream fin = null;
			try {
				fin = new FileInputStream("dicts/"+name+"_"+Integer.toString(block));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: FILE NOT FOUND!!!");
				return null;
			}
			BufferedReader fileReader = new BufferedReader (new InputStreamReader(fin));
			String line;
			
			String myKey = "";
			HashMap<Integer, Integer> ret = null;
			HashMap<Integer, Integer> cur = null;
			boolean first = true;
			
			while((line = fileReader.readLine()) != null) {
				if(line.startsWith("-")) {
					//System.out.println("New file: "+myKey);
					myKey = line.replace("-","");
					if(!first) {
						// Write old cache entry
						cache.put(name+"_"+myKey, toByteArray(cur));
						//fakeCache.put(Integer.parseInt(myKey), new HashMap<Integer, Integer>(cur));
				    	
						//System.out.println("Putting to cache: "+name+"_"+myKey);
						if(Integer.parseInt(myKey) == number) {
							assert(cur != null);
							ret = cur;
						}
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

			cache.put(name+"_"+myKey, toByteArray(cur));
			/*
			fakeCache.put(Integer.parseInt(myKey), new HashMap<Integer, Integer>(cur));
	    	if(name.equals("explain")) {
	    		fakeECacheOffset = block;
	    	}
	    	else if(name.equals("transcript")) {
	    		fakeTCacheOffset = block;
	    	}
	    	*//*
			if(Integer.parseInt(myKey) == number) {
				ret = cur;
			}
			// Add to MemCache
			//cache.put(name, toByteArray(ret));
			
    		return ret;
    	}
    }
    */
    
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