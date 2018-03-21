
/**
 * @author James Ratzlaff
 *
 */
module com.ratzlaff.james.arc.earc {
	requires org.slf4j;
	requires javafx.controls;
	requires javafx.graphics;
	requires java.base;

	exports com.ratzlaff.james.arc;
	exports com.ratzlaff.james.arc.earc;
	exports com.ratzlaff.james.arc.earc.ui;
	exports com.ratzlaff.james.arc.earc.obfus;
	
}