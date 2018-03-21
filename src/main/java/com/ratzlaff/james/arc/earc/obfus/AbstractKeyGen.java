/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
public abstract class AbstractKeyGen extends BaseKeyGen implements KeyGenerator{
	

	private long morphingKey;
	protected long transientKey = 0l;
	protected long offsetKey = 0l;
	protected long lengthKey = 0l;
	

	protected AbstractKeyGen(long minorKey,long majorKey,long parentKey) {
		super(minorKey, majorKey, parentKey);
	}
	
	protected AbstractKeyGen(long minorKey, long majorKey, long parentKey, long transientKey) {
		this(minorKey,majorKey,parentKey);
		setTransientKey(transientKey);
	}
	
	

	protected AbstractKeyGen(long parentKey) {
		this(parentKey, DEFAULT_MAJOR_KEY, DEFAULT_MINOR_KEY);
	}
	
	protected AbstractKeyGen(long parentKey, long transientKey) {
		this(parentKey, DEFAULT_MAJOR_KEY, DEFAULT_MINOR_KEY, transientKey);
	}

	public void setTransientKey(long transientKey) {
		beforeSettingTransientKey(transientKey);
		this.transientKey = transientKey;
		afterSettingTransientKey();
	}
	
	public EntryUnlockKeys setTransientKeyAndGetEntryUnlockKey(long transientKey) {
		setTransientKey(transientKey);
		return getEntryUnlockKeys();
	} 
	
	public EntryUnlockKeys getEntryUnlockKeys() {
		return new EntryUnlockKeysImpl(getLengthKey(), getOffsetKey());
	}

	public long getOffsetKey() {
		return offsetKey;
	}

	public long getLengthKey() {
		return lengthKey;
	}

	public long getMorphingKey() {
		return morphingKey;
	}

	public long getTransientKey() {
		return transientKey;
	}

	protected void beforeSettingTransientKey(long newVal) {
		
	}
	protected void beforeSettingOffsetKey(long newVal) {
		
	}
	protected void beforeSettingLengthKey(long newVal) {
		
	}
	protected void beforeSettingMorphingKey(long newValue) {
		// does nothing by default but you can override this to do things such as saving propreties to a collection for
		// tracking
	}
	
	
	
	protected void afterSettingTransientKey() {
		long newLenKey =generateLengthKey();
		setLengthKey(newLenKey);
	}

	protected void setLengthKey(long lengthKey) {
		beforeSettingLengthKey(lengthKey);
		this.lengthKey = lengthKey;
		afterSettingLengthKey();
	}

	protected void afterSettingLengthKey() {
		long newOffsetKey = generateOffsetKey();
		setOffsetKey(newOffsetKey);
	}

	protected void setOffsetKey(long offsetKey) {
		beforeSettingOffsetKey(offsetKey);
		this.offsetKey = offsetKey;
		afterSettingOffsetKey();

	}

	protected void afterSettingOffsetKey() {
		setMorphingKey(getOffsetKey());
	}

	private void setMorphingKey(long value) {
		if(value!=morphingKey) {
		beforeSettingMorphingKey(value);
		morphingKey = value;
		}
	}


	public void reset() {
		setParentKey(getParentKey());
	}

	protected void resetMorphingKeyToOriginalValue() {
		setMorphingKey(getMajorKey() ^ getParentKey());
	}

	@Override
	protected void afterSettingParentKey() {
		resetMorphingKeyToOriginalValue();
		resetTransientLengthAndOffsetKeysToOriginalValue();
	}

	protected void resetTransientLengthAndOffsetKeysToOriginalValue() {
		transientKey = 0l;
		lengthKey = 0l;
		offsetKey = 0l;
	}

	

	
	
}
