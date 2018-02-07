/**
 * 
 */
package com.ratzlaff.james.arc.earc;

import java.lang.System.Logger;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author James Ratzlaff
 *
 */
class Logging {

	public static Logger getLogger(String name, ResourceBundle rb) {
		Logger l = null;
		if(rb!=null) {
			l=System.getLogger(name, rb);
		} else {
			l=System.getLogger(name);
		}
		return l;
	}
	
	public static Logger getLogger(String name) {
		return getLogger(name, null);
	}
	
	public static Logger getLogger(Class<?> clazz) {
		String name = clazz!=null?clazz.getName():null;
		Logger l = getLogger(name);
		return l;
	}
	
	
}
