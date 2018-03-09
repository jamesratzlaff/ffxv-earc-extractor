/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
public class MinimalKeygenSaveState extends AbstractKeyContainer implements KeyGenerator, Timestamped<MinimalKeygenSaveState>{
	private final long timestamp;
	private final long morphingKey;
	private final long transientKey;
	private final long parentKey;
	
	public MinimalKeygenSaveState(long morphingKey, long transientKey, long parentKey) {
		this.timestamp=System.currentTimeMillis();
		this.morphingKey=morphingKey;
		this.transientKey=transientKey;
		this.parentKey=parentKey;
	}
	
	public MinimalKeygenSaveState(KeyContainer c) {
		this(c!=null?c.getMorphingKey():0l,c!=null?c.getTransientKey():0l,c!=null?c.getParentKey():0l);
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the morphingKey
	 */
	public long getMorphingKey() {
		return morphingKey;
	}

	/**
	 * @return the transientKey
	 */
	public long getTransientKey() {
		return transientKey;
	}

	/**
	 * @return the parentKey
	 */
	public long getParentKey() {
		return parentKey;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MinimalKeygenSaveState o) {
		if(o==null) {
			return -1;
		} else if(this.equals(o)) {
			return 0;
		}
		return Long.compare(this.timestamp, o.timestamp);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
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
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof MinimalKeygenSaveState)) {
			return false;
		}
		MinimalKeygenSaveState other = (MinimalKeygenSaveState) obj;
		if (timestamp != other.timestamp) {
			return false;
		}
		return true;
	}
	
	public MinimalKeygenSaveState copy() {
		return new MinimalKeygenSaveState(this);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\ntimestamp:    ").append(getTimestamp());
		return sb.toString();
		
		
	}
	

	
	
	
	
}
