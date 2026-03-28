/**
 * 
 */
/**
 * @author Admin
 *
 */
module readPrestigeLabels {
	
	exports com.techwerx.labelprocessing.prestige to javafx.graphics;
	
	requires transitive java.desktop;
	requires commons.math3;
	requires transitive lept4j;
	requires jfreechart;
	requires transitive tess4j;
	requires org.slf4j;
	requires com.sun.jna;
	requires org.apache.commons.io;
	requires jboss.vfs;
	requires fuzzywuzzy;
	requires commons.pool2;
	requires jai.imageio.core;
	requires java.string.similarity;
	requires javafx.graphics;
	requires javafx.controls;
	requires jdk.unsupported;
}