/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

import com.ratzlaff.james.arc.earc.ui.Configuration;

/**
 * @author James Ratzlaff
 *
 */
public abstract class AbstractKeyContainer implements KeyContainer {
	/**
	 * You'll have to obtain these keys yourself.  With some good ol hacker-sleuthing you should be able to find them in no time.
	 * Hint:  With a semi-decent debugger or dissasembler, and if really use your head, you'll find that 4-bytes is all it takes to find what you seek. 
	 */
	protected static final long DEFAULT_MAJOR_KEY = (long) Configuration.get().getOrDefault(AbstractKeyContainer.class,
			"DEFAULT_MAJOR_KEY", DeflateDeobfuscator::getNumber, 0x0l, Long::toHexString);
	protected  static final long DEFAULT_MINOR_KEY = (long) Configuration.get().getOrDefault(AbstractKeyContainer.class,
			"DEFAULT_MINOR_KEY", DeflateDeobfuscator::getNumber, 0x0l, Long::toHexString);
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (getLengthKey() ^ (getLengthKey() >>> 32));
		result = prime * result + (int) (getMajorKey() ^ (getMajorKey() >>> 32));
		result = prime * result + (int) (getMinorKey() ^ (getMinorKey() >>> 32));
		result = prime * result + (int) (getMorphingKey() ^ (getMorphingKey() >>> 32));
		result = prime * result + (int) (getOffsetKey() ^ (getOffsetKey() >>> 32));
		result = prime * result + (int) (getParentKey() ^ (getParentKey() >>> 32));
		result = prime * result + (int) (getTransientKey() ^ (getTransientKey() >>> 32));
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
		if (!(obj instanceof KeyGen)) {
			return false;
		}
		AbstractKeyGen other = (AbstractKeyGen) obj;
		if (getLengthKey() != other.getLengthKey()) {
			return false;
		}
		if (getMajorKey() != other.getMajorKey()) {
			return false;
		}
		if (getMinorKey() != other.getMinorKey()) {
			return false;
		}
		if (getMorphingKey() != other.getMorphingKey()) {
			return false;
		}
		if (getOffsetKey() != other.getOffsetKey()) {
			return false;
		}
		if (getParentKey() != other.getParentKey()) {
			return false;
		}
		if (getTransientKey() != other.getTransientKey()) {
			return false;
		}
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nMajorKey:     ").append(Long.toHexString(getMajorKey()));
		sb.append("\nMinorKey:     ").append(Long.toHexString(getMinorKey()));
		sb.append("\nParentKey:    ").append(Long.toHexString(getParentKey()));
		sb.append("\nTransientKey: ").append(Long.toHexString(getTransientKey()));
		sb.append("\nMorphingKey:  ").append(Long.toHexString(getMorphingKey()));
		sb.append("\nlengthKey:    ").append(Long.toHexString(getLengthKey()));
		sb.append("\noffsetKey:    ").append(Long.toHexString(getOffsetKey()));
		return sb.toString();
	}
	
}
