/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
public class KeyGenSaveState extends MinimalKeygenSaveState {

	private final long lengthKey;
	private final long offsetKey;
	private final long minorKey;
	private final long majorKey;

	/**
	 * @return the lengthKey
	 */
	public long getLengthKey() {
		return lengthKey;
	}

	/**
	 * @return the sizeKey
	 */
	public long getOffsetKey() {
		return offsetKey;
	}

	/**
	 * @return the minorKey
	 */
	public long getMinorKey() {
		return minorKey;
	}

	/**
	 * @return the majorKey
	 */
	public long getMajorKey() {
		return majorKey;
	}

	public KeyGenSaveState(KeyContainer kc) {
		this(kc!=null?kc.getLengthKey():0,kc!=null?kc.getOffsetKey():0,kc!=null?kc.getMorphingKey():0,kc!=null?kc.getTransientKey():0,kc!=null?kc.getParentKey():0,kc!=null?kc.getMinorKey():0, kc!=null?kc.getMajorKey():0);
	}

	public KeyGenSaveState(long lengthKey, long offsetKey, long morphingKey, long transientKey, long parentKey,
			long minorKey, long majorKey) {
		super(morphingKey, transientKey, parentKey);
		this.lengthKey = lengthKey;
		this.offsetKey = offsetKey;
		this.minorKey = minorKey;
		this.majorKey = majorKey;
	}

}
