/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
public interface Timestamped<T extends Timestamped<T>> extends Comparable<T>{
	
	long getTimestamp();
	
	default int compareTo(T other) {
		if(other==null) {
			return -1;
		} else if(this.equals(other)) {
			return 0;
		}
		return Long.compare(getTimestamp(), other.getTimestamp());
	}
	
	@Override
	boolean equals(Object other);

	@Override
	int hashCode();
}
