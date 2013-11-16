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

    private static MemcacheService cache = MemcacheServiceFactory.getMemcacheService("cache");
    /*
    private static MemcacheService explainGlobalCache = MemcacheServiceFactory.getMemcacheService("explainGlobal");
    private static MemcacheService transGlobalCache = MemcacheServiceFactory.getMemcacheService("transGlobal");
    private static MemcacheService explainCache = MemcacheServiceFactory.getMemcacheService("explain");
    private static MemcacheService transCache = MemcacheServiceFactory.getMemcacheService("trans");
    */
    public static void clearAllCaches() {
    	cache.clearAll();
    	/*
    	explainGlobalCache.clearAll();
    	transGlobalCache.clearAll();
    	explainCache.clearAll();
    	transCache.clearAll();
    	*/
    }
    
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
				cache.put(name, null);
				return null;
			}
			BufferedReader fileReader = new BufferedReader (new InputStreamReader(fin));
			String line;
			
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