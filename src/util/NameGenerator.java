package util;

/**
 * Holder of an incrementing int value which can be used for naming.
 * @author Magnus Fransson, magnus.fransson@sweco.se
 *
 */
public class NameGenerator {
	
	private Integer u;
	
	public NameGenerator() {
		this.u = 0;
	}
	
	public String newName() {
		this.u++;
		
		return Integer.toString(u);
	}
}
