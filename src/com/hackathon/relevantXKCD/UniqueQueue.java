package com.hackathon.relevantXKCD;

import java.util.ArrayList;
import java.util.List;

public class UniqueQueue<Type> {
	List<Type> list = new ArrayList<Type>();
	
	public void enq(Type entry) {
		if(!list.contains(entry))
			list.add(entry);
	}
	
	public Type deq() {
		Type e = list.remove(0);
		return e;
	}
}
