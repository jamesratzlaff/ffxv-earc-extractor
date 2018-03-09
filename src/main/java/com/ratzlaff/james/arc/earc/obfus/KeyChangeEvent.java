/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/**
 * @author James Ratzlaff
 *
 */
public class KeyChangeEvent implements Serializable, Timestamped<KeyChangeEvent>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8926139721852891424L;

	public static enum KEYS implements BiFunction<KeyContainer,Long,KeyChangeEvent>{
		MAJOR((kc)->kc.getMajorKey()),
		MINOR((kc)->kc.getMinorKey()),
		PARENT((kc)->kc.getParentKey()),
		TRANSIENT((kc)->kc.getTransientKey()),
		MORPHING((kc)->kc.getMorphingKey()),
		OFFSET((kc)->kc.getOffsetKey()),
		LENGTH((kc)->kc.getLengthKey());

		
		private final ToLongFunction<KeyContainer> toLongFunction;
		private KEYS(ToLongFunction <KeyContainer>toLongFunction) {
			this.toLongFunction=toLongFunction;
		}
		public KeyChangeEvent apply(KeyContainer kc, Long newValue) {
			return new KeyChangeEvent(this, this.toLongFunction.applyAsLong(kc), newValue.longValue());
		}
		
		
	}
	
	
	private final long timestamp = System.currentTimeMillis();
	private final KEYS type;
	private final long oldValue;
	private final long value;
	
	
	public KeyChangeEvent(KEYS key, long oldValue, long newVal) {
		this.type=key;
		this.oldValue=oldValue;
		this.value=newVal;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (value ^ (value >>> 32));
		result = prime * result + (int) (oldValue ^ (oldValue >>> 32));
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof KeyChangeEvent)) {
			return false;
		}
		KeyChangeEvent other = (KeyChangeEvent) obj;
		if (value != other.value) {
			return false;
		}
		if (oldValue != other.oldValue) {
			return false;
		}
		if (timestamp != other.timestamp) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return toString(null);
	}
	public static int getMaxNameLength() {
		return getMaxNameLength(KeyChangeEvent.KEYS.values());
	}
	public static int getMaxNameLength(KeyChangeEvent.KEYS[] keys) {
		int longest = Arrays.stream(keys).map(n->n.name()).max((a,b)->Integer.compare(a.length(),b.length())).get().length();
		return longest;
	}
	
	private static KeyChangeEvent.KEYS[] getAllNamesUsed(Collection<KeyChangeEvent> events){
		KEYS[] ks = events.stream().map(e->e.type).distinct().toArray(size->new KEYS[size]);
		return ks;
	}
	
	public String toString(LongFunction<String> timestampFormatter) {
		int maxNameLength =getMaxNameLength();
		
		long tsValue = getTimestamp();
		String asStr = timestampFormatter==null?String.valueOf(tsValue):timestampFormatter.apply(tsValue);
		char[] padding = new char[maxNameLength-type.name().length()];
		Arrays.fill(padding, ' ');
		StringBuilder sb = new StringBuilder(type.name()).append(padding).append("@").append(asStr).append("\t[").append("0x").append(String.format("%016x",oldValue).toUpperCase()).append("\t->\t").append("0x").append(String.format("0x%016x",value).toUpperCase()).append("]");
		return sb.toString();
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
}
