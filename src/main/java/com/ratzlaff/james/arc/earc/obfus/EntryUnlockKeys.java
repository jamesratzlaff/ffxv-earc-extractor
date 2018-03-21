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
public interface EntryUnlockKeys {

	static final  class DefaultOps {
		protected static final LongBinaryOperator DEFAULT_LONG_OPERATOR = (a, b) -> a ^ b;
		protected static final IntBinaryOperator DEFAULT_INT_OPERATOR = (a, b) -> a ^ b;
	}
	
	long getDataOffsetFrom(long obfuscatedOffset);

	int getEntryLengthFrom(int obfuscatedEntryLength);

	int getSizeOnDiskFrom(int obfuscatedSizeOnDisk);

	/**
	 * @return the fileLengthXORKey
	 */
	default long getLengthInArchiveUnlockKey() {
		return 0l;
	}

	/**
	 * 
	 * @return the size on disk XOR Key
	 */
	default int getSizeOnDiskUnlockKey() {
		return (int)(getLengthInArchiveUnlockKey()>>0x20);
	}

	/**
	 * @return the dataOffsetXORKey
	 */
	default long getDataOffsetUnlockKey() {
		return 0l;
	}
	
	

}