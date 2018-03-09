/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ratzlaff.james.arc.earc.ui.Configuration;
import com.ratzlaff.james.util.io.BigHex;

/**
 * Deobfuscates the size information for zip entries for the windows version of FFXV.  Note: I do not provide these key values. Use your internet, or rev.eng. skills to find them yourself.
 * @author James Ratzlaff
 *
 */
public class DeflateDeobfuscator {

	
	public static final Pattern hex = Pattern.compile("(?:0[xX])*([0-9a-fA-F]+)[lLhH]?");
	private static final Pattern regularNumber = Pattern.compile("([\\-]?[0-9]+)");
	public static final long DEFAULT_GREATER_KEY = (long) Configuration.get().getOrDefault(DeflateDeobfuscator.class, "DEFAULT_GREATER_KEY", DeflateDeobfuscator::getNumber, 0l, Long::toHexString);
	public static final long DEFAULT_LESSER_KEY = (long) Configuration.get().getOrDefault(DeflateDeobfuscator.class, "DEFAULT_LESSER_KEY", DeflateDeobfuscator::getNumber, 0l, Long::toHexString);

	private long greaterKey;
	private long lesserKey;

	public DeflateDeobfuscator(long greaterKey, long lesserKey) {
		this.greaterKey = greaterKey;
		this.lesserKey = lesserKey;
	}

	public DeflateDeobfuscator() {
		this(DEFAULT_GREATER_KEY, DEFAULT_LESSER_KEY);
	}

	/**
	 * @return the greaterKey
	 */
	public long getGreaterKey() {
		return greaterKey;
	}

	/**
	 * @param greaterKey
	 *            the greaterKey to set
	 */
	public void setGreaterKey(long greaterKey) {
		this.greaterKey = greaterKey;
	}

	/**
	 * @return the lesserKey
	 */
	public long getLesserKey() {
		return lesserKey;
	}

	/**
	 * @param lesserKey
	 *            the lesserKey to set
	 */
	public void setLesserKey(long lesserKey) {
		this.lesserKey = lesserKey;
	}
	
	/**
	 * 
	 * @param individualKey
	 * @param leftValue
	 * @return return the deobfuscated value of {@code rightValue} if {@code rightValue} is obfuscated and vice-versa
	 */
	public int getToggledObfuscationForLeftValue(short individualKey, int leftValue) {
		long intermediate=generateIntermediateKey(individualKey);
		return (int) (leftValue^(intermediate>>0x20));
		
	}
	
	/**
	 * 
	 * @param individualKey
	 * @param leftValue
	 * @return return the deobfuscated value of {@code leftValue} if {@code leftValue} is obfuscated and vice-versa
	 */
	public int getToggledObfuscationForRightValue(short individualKey, int rightValue) {
		long intermediate=generateIntermediateKey(individualKey);
		return (int) (rightValue^(intermediate));
	}

	/**
	 * 
	 * @param archiveKey
	 *            the 2-byte (short) value that is generally found as the last data field for an earc entry
	 * @param obfuscated
	 *            the obfuscated value to be deobfuscated
	 * @param composited
	 *            if set to true, the {@code obfuscated} value will be treated as is other wise the value will be
	 *            transformed by swapping the heigher and lower 4 bytes
	 * @return a long composed of the compressed value in the upper 4 bytes and the decompressed size in the lower 4 bytes
	 */
	public long apply(short archiveKey, long obfuscated, boolean composited) {
		long intermediateKey = generateIntermediateKey(archiveKey);
		if(!composited) {
			obfuscated=swapInts(obfuscated);
		}
		long result = (int) calculateRight4Bytes(intermediateKey, obfuscated);
		int left = ((calculateLeft4ytes(intermediateKey, obfuscated)));
		result |= (((long) left) << Integer.SIZE);
		return result;
	}
	
	public long apply(short archiveKey, long obfuscated) {
		return apply( archiveKey,  obfuscated,false);
	}
	
	private static long swapInts(long nonComposite) {
		long result = 0l;
		int leftInt = getLeftIntValueFromRawLong(nonComposite);
		int rightInt = getRightIntValueFromRawLong(nonComposite);
		result|=(((long)rightInt)<<Integer.SIZE);
		result|=leftInt>>>Integer.SIZE;
		return result;
	}

	private long generateIntermediateKey(short archiveKey) {
		long intermediateKey = ((((long) archiveKey) & 0xFFFF)) * greaterKey;
		intermediateKey += lesserKey;
		return intermediateKey;
	}

	private int calculateRight4Bytes(long intermediateKey, long obfuscated) {
		return calculateRight4Bytes(intermediateKey, (int) obfuscated);
	}

	private int calculateLeft4ytes(long intermediateKey, long obfuscated) {
		int leftEight = (int) (obfuscated >> Integer.SIZE);
		return calculateLeft4Bytes(intermediateKey, leftEight);
	}

	private int calculateLeft4Bytes(long intermediateKey, int left4Bytes) {
		return calculateRight4Bytes(intermediateKey >> 0x20, left4Bytes);
	}

	private int calculateRight4Bytes(long intermediateKey, int right4Bytes) {
		return (int) (right4Bytes ^ intermediateKey);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (greaterKey ^ (greaterKey >>> 32));
		result = prime * result + (int) (lesserKey ^ (lesserKey >>> 32));
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
		if (!(obj instanceof DeflateDeobfuscator)) {
			return false;
		}
		DeflateDeobfuscator other = (DeflateDeobfuscator) obj;
		if (greaterKey != other.greaterKey) {
			return false;
		}
		if (lesserKey != other.lesserKey) {
			return false;
		}
		return true;
	}


	

	public static void main(String[] args) {
		String archiveKey = args[0];
		short aKey = (short) getNumber(archiveKey);
		String deobfuscateLeft = args[1];
		String deobfuscateRight = null;
		if (args.length > 2) {
			deobfuscateRight = args[2];
		}

		long asLong = deobfuscateRight == null ? (getNumber(deobfuscateLeft))
				: (getNumber(deobfuscateLeft)) << 32 | getNumber(deobfuscateRight);
		DeflateDeobfuscator dd = new DeflateDeobfuscator();
		long deobfuscated = dd.apply(aKey, asLong);
		int left = getLeftIntValueFromRawLong(deobfuscated);
		int right =getRightIntValueFromRawLong(deobfuscated);
		
		System.out.println(String.format("obfusc val %016x", asLong));
		System.out.println(String.format("obfs L val %016x", getLeftIntValueFromRawLong(asLong)));
		System.out.println(String.format("obfs R val %016x", getRightIntValueFromRawLong(asLong)));
		System.out.println(String.format("deobfs val %016x", deobfuscated));
		System.out.println(String.format("deob L val %08x",left));
		System.out.println(String.format("deob R val %08x",right));
		System.out.println(String.format("reob L val %08x", dd.getToggledObfuscationForLeftValue(aKey, left)));
		System.out.println(String.format("reob R val %08x", dd.getToggledObfuscationForRightValue(aKey, right)));
		

	}
	
	public static int getRightIntValueFromRawLong(long value) {
		long rightMask = 0xFFFFFFFFl;
		return (int)(value&rightMask);
	}
	
	public static int getLeftIntValueFromRawLong(long value) {
		long leftMask = 0xFFFFFFFF00000000l;
		long leftOrig = value&leftMask;
		return (int)(leftOrig>>>Integer.SIZE);
	}

	public static long getNumber(String str) {
		Matcher m = regularNumber.matcher(str);
		int base = 10;

		if (!m.matches()) {
			m = hex.matcher(str);
			base = 16;
		}
		if (m.matches()) {
			str = m.group(1);
		}
		if(base==16) {
			return BigHex.parseHexAsLong(str);
		} else {
		
			return Long.parseLong(str, base);
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("/*Deflate deobfuscator*/{'greaterKey':%x,'lesserKey':%x}",greaterKey,lesserKey));
		return sb.toString();
	}

}
