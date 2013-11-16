package com.hackathon.relevantXKCD;

public class Tuple<X, Y> {
	public X first;
	public Y second;

	public Tuple(X x, Y y) {
		this.first = x;
		this.second = y;
	}
	public Tuple() {
		
	}
	
	@Override
	public String toString() {
		return "["+first+", "+second+"]";
	}
}
