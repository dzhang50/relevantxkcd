package com.hackathon.relevantXKCD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class Global {
    private static MemcacheService explainGlobalCache = MemcacheServiceFactory.getMemcacheService("explainGlobal");
    private static MemcacheService transGlobalCache = MemcacheServiceFactory.getMemcacheService("transGlobal");
    private static MemcacheService explainCache = MemcacheServiceFactory.getMemcacheService("explain");
    private static MemcacheService transCache = MemcacheServiceFactory.getMemcacheService("trans");
    
    public static void clearAllCaches() {
    	explainGlobalCache.clearAll();
    	transGlobalCache.clearAll();
    	explainCache.clearAll();
    	transCache.clearAll();
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