/**
 * 
 */
package com.ratzlaff.james.arc.earc.obfus;

/**
 * @author James Ratzlaff
 *
 */
public interface KeyGenerator extends KeyContainer {

	
	
	@Override
	default long getOffsetKey() {
		return generateOffsetKey();
	}
	
	
	@Override
	default long getLengthKey() {
		return generateLengthKey();
	}
	
	default long generateOffsetKey() {
		long generatedOffsetKey =  (getLengthKey() * getMinorKey()) ^ ~(getTransientKey());
		return generatedOffsetKey;
	}
	
	default long generateLengthKey() {
		long generatedLengthKey = (getMorphingKey() * getMinorKey()) ^ getTransientKey();
		return generatedLengthKey;
	}
	
	
	
}
