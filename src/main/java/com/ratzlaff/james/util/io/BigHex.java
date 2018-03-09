/**
 * 
 */
package com.ratzlaff.james.util.io;

import java.util.Objects;
import java.util.regex.Matcher;

import com.ratzlaff.james.arc.earc.obfus.DeflateDeobfuscator;

/**
 * @author James Ratzlaff
 *Created due to Long.parseLong has a silly character safety limit
 */
public class BigHex {
	
	
	public static long parseHexAsLong(String l) {
		Objects.requireNonNull(l);
		String toParse=null;
		Matcher m=DeflateDeobfuscator.hex.matcher(l);
		if(m.matches()) {
			toParse=m.group(1);
		}
		Objects.requireNonNull(toParse, "the string "+l+" is not parseable as a hex encoded long");
		toParse=toParse.toUpperCase();
		long result=0;
		for(int i=toParse.length()-1;i>-1;i--) {
			int currentChar = toParse.codePointAt(i);
			boolean isNumber = (48&currentChar)==48;
			int nibbleIndex=(toParse.length()-1)-i;
			long asByteVal = isNumber?currentChar-48:(currentChar&7)+9;
			result|=asByteVal<<(nibbleIndex<<2);
		}
		return result;
	}

}
