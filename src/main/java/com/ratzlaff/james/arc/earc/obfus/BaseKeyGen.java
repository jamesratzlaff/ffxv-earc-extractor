/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
public abstract class BaseKeyGen extends AbstractKeyContainer implements KeyGenerator {

	protected final long minorKey;
	protected final long majorKey;
	protected long parentKey;

	protected BaseKeyGen(long minorKey, long majorKey, long parentKey) {
		beforeSettingMinorKey(minorKey);
		this.minorKey = minorKey;
		beforeSettingMajorKey(majorKey);
		this.majorKey = majorKey;
		setParentKey(parentKey);
	}

	protected BaseKeyGen(long parentKey) {
		this(DEFAULT_MINOR_KEY, DEFAULT_MAJOR_KEY, parentKey);
	}

	protected void setParentKey(long parentKey) {
		beforeSettingParentKey(parentKey);
		this.parentKey = parentKey;
		afterSettingParentKey();
	}

	protected void afterSettingParentKey() {
	};

	protected void beforeSettingMinorKey(long newValue) {
	}

	protected void beforeSettingMajorKey(long newValue) {
	}

	protected void beforeSettingParentKey(long newValue) {
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.KeyContainer#getParentKey()
	 */
	@Override
	public long getParentKey() {
		return parentKey;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.KeyContainer#getMajorKey()
	 */
	@Override
	public long getMajorKey() {
		return majorKey;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.KeyContainer#getMinorKey()
	 */
	@Override
	public long getMinorKey() {
		return minorKey;
	}

}
