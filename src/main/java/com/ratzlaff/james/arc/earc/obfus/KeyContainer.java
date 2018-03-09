/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
interface KeyContainer {
	
	
	
	long getParentKey();
	long getTransientKey();
	long getLengthKey();
	long getOffsetKey();
	long getMorphingKey();
	
	default int getDeobfuscatedLengthInArchive(int obfuscated) {
		return (int) ((int) (getLengthKey()) ^ obfuscated);
	}

	default int getDeobfuscatedSizeOnDisk(int obfuscated) {
		return (int) ((int) (getLengthKey() >> 0x20)) ^ obfuscated;
	}
	
	default long getDeobfiscatedLengthInArchiveAndSizeOnDisk(long bigEndianLongOfBothValuesReadAtTheSameTime) {
		int leftMostBytes = (int) (bigEndianLongOfBothValuesReadAtTheSameTime >> 0x20);
		int rightMostBytes = (int) bigEndianLongOfBothValuesReadAtTheSameTime;
		long combinedAnswerWithLengthAsTheMSBAndSizeAsTheLSB = getDeobfiscatedLengthInArchiveAndSizeOnDisk(
				leftMostBytes, rightMostBytes);
		return combinedAnswerWithLengthAsTheMSBAndSizeAsTheLSB;
	}

	default long getDeobfiscatedLengthInArchiveAndSizeOnDisk(int bytesWithSmallerOffset,
			int foruBytesAfterBytesWithSmallerOffset) {
		int length =getDeobfuscatedLengthInArchive(foruBytesAfterBytesWithSmallerOffset);
		int sizeOnDisk =(getDeobfuscatedSizeOnDisk(bytesWithSmallerOffset));
		long reso  =(((long)length) << Integer.SIZE) | ((long)(( sizeOnDisk)));
		return reso;
	}

	default long getDeobfuscatedOffset(long obfuscated) {
		return getOffsetKey() ^ obfuscated;
	}
	
	
	default long getMajorKey() {
		return AbstractKeyContainer.DEFAULT_MAJOR_KEY;
	}
	
	
	default long getMinorKey() {
		return AbstractKeyContainer.DEFAULT_MINOR_KEY;
	}
	
	
	
	
}
