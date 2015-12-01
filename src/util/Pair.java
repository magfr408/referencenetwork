package util;

public class Pair<F,T> {
	private F f;
	private T t;
	
	public Pair(F f, T t) {
		this.f = f;
		this.t= t;
	}
	
	public F getF() { return f;}
	public T getT() { return t;}
}