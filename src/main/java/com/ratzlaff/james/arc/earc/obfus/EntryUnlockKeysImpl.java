/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * @author James Ratzlaff
 *
 */
public class EntryUnlockKeysImpl implements EntryUnlockKeys {
	

	private final long fileLengthUnlockKey;
	private final long dataOffsetUnlockKey;
	private LongBinaryOperator longOperator;
	private IntBinaryOperator intOperator;

	EntryUnlockKeysImpl() {
		this(0,0l);
	}
	
	EntryUnlockKeysImpl(long fileLengthUnlockKey, long offsetUnlockKey) {
		this.fileLengthUnlockKey = fileLengthUnlockKey;
		this.dataOffsetUnlockKey = offsetUnlockKey;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.UnlockKeys#getDataOffsetFrom(long)
	 */
	@Override
	public long getDataOffsetFrom(long obfuscatedOffset) {
		long offset = getLongOperator().applyAsLong(getDataOffsetUnlockKey(), obfuscatedOffset);
		return offset;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.UnlockKeys#getEntryLengthFrom(int)
	 */
	@Override
	public int getEntryLengthFrom(int obfuscatedEntryLength) {
		int lengthInArchive = getIntOperator().applyAsInt((int)getLengthInArchiveUnlockKey(), obfuscatedEntryLength);
		return lengthInArchive;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.UnlockKeys#getSizeOnDiskFrom(int)
	 */
	@Override
	public int getSizeOnDiskFrom(int obfuscatedSizeOnDisk) {
		int sizeOnDisk = getIntOperator().applyAsInt(getSizeOnDiskUnlockKey(), obfuscatedSizeOnDisk);
		return sizeOnDisk;
	}

	void setIntBinaryOperator(IntBinaryOperator intOp) {
		this.intOperator = intOp;
	}

	void setLongBinaryOperator(LongBinaryOperator longOp) {
		this.longOperator = longOp;
	}

	private IntBinaryOperator getIntOperator() {
		return intOperator == null ? EntryUnlockKeys.DefaultOps.DEFAULT_INT_OPERATOR : intOperator;
	}

	private LongBinaryOperator getLongOperator() {
		return longOperator == null ? EntryUnlockKeys.DefaultOps.DEFAULT_LONG_OPERATOR : longOperator;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.UnlockKeys#getLengthInArchiveUnlockKey()
	 */
	@Override
	public long getLengthInArchiveUnlockKey() {
		return fileLengthUnlockKey;
	}

	/* (non-Javadoc)
	 * @see com.ratzlaff.james.arc.earc.obfus.UnlockKeys#getDataOffsetUnlockKey()
	 */
	@Override
	public long getDataOffsetUnlockKey() {
		return dataOffsetUnlockKey;
	}

	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (dataOffsetUnlockKey ^ (dataOffsetUnlockKey >>> 32));
		result = prime * result + (int) (fileLengthUnlockKey ^ (fileLengthUnlockKey >>> 32));
		result = prime * result + ((intOperator == null) ? 0 : intOperator.hashCode());
		result = prime * result + ((longOperator == null) ? 0 : longOperator.hashCode());
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
		if (!(obj instanceof EntryUnlockKeysImpl)) {
			return false;
		}
		EntryUnlockKeysImpl other = (EntryUnlockKeysImpl) obj;
		if (dataOffsetUnlockKey != other.dataOffsetUnlockKey) {
			return false;
		}
		if (fileLengthUnlockKey != other.fileLengthUnlockKey) {
			return false;
		}
		if (intOperator == null) {
			if (other.intOperator != null) {
				return false;
			}
		} else if (!intOperator.equals(other.intOperator)) {
			return false;
		}
		if (longOperator == null) {
			if (other.longOperator != null) {
				return false;
			}
		} else if (!longOperator.equals(other.longOperator)) {
			return false;
		}
		return true;
	}

	public String toString() {
		String longFormat = "0x%016x";
		String intFormat = "0x%08x";
		StringBuilder sb = new StringBuilder();
		String offsetKey = String.format(longFormat, getDataOffsetUnlockKey());
		String lenKey = String.format(intFormat, getLengthInArchiveUnlockKey());
		String sizeKey = String.format(intFormat, getSizeOnDiskUnlockKey());
		sb.append("Offset unlock key=").append(offsetKey)
		.append(", length in archive unlock key=").append(lenKey)
		.append(", size on disk unlock key=").append(sizeKey);
		return sb.toString();
	}

}
