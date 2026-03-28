/**
 *
 */
package com.techwerx.pdf;

import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author Admin
 *
 */
public class PDFHandlerChartFactory {

	private static PDFHandlerChartFactory phcf = new PDFHandlerChartFactory();

	public static PDFHandlerChartFactory getPDFHandlerChartFactory() {
		return phcf;
	}

	/*
	 * public JFreeChart createXYSeries(int[][] input, String seriesTitle, String
	 * chartTitle, String xAxisTitle, String yAxisTitle) throws IOException {
	 *
	 * XYSeries series1 = new XYSeries(seriesTitle); for (Integer key : keys) {
	 * series1.add(key, inputMap.get(key)); }
	 *
	 * XYSeriesCollection dataset = new XYSeriesCollection();
	 * dataset.addSeries(series1);
	 *
	 * JFreeChart chart = ChartFactory.createXYLineChart(chartTitle, xAxisTitle,
	 * yAxisTitle, dataset, PlotOrientation.VERTICAL, true, true, false);
	 *
	 * return chart; }
	 */

	public static JFreeChart createBarChart(int[] input, String valueDescription, String chartTitle, String xAxisTitle,
			String yAxisTitle) throws IOException {

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		int noOfInputs = input.length;
		for (int i = 0; i < noOfInputs; i++) {
			dataset.setValue(input[i], valueDescription, Integer.toString(i));
		}

		JFreeChart chart = ChartFactory.createBarChart(chartTitle, xAxisTitle, yAxisTitle, dataset,
				PlotOrientation.VERTICAL, false, true, false);

		return chart;
	}

	public static boolean drawBarChart(int[] input, String fileName, int width, int height, String valueDescription,
			String chartTitle, String xAxisTitle, String yAxisTitle) throws IOException {
		JFreeChart jfc = createBarChart(input, valueDescription, chartTitle, xAxisTitle, yAxisTitle);
		return drawChart(jfc, fileName, width, height);
	}

	public static boolean drawBarChart(int[] input, int verticalLinePosition, String fileName, int width, int height,
			String valueDescription, String chartTitle, String xAxisTitle, String yAxisTitle) throws IOException {
		JFreeChart jfc = createBarChart(input, valueDescription, chartTitle, xAxisTitle, yAxisTitle);
		// IntervalMarker im = new IntervalMarker(2050.5,2153.1);
		// ValueMarker marker = new ValueMarker(verticalLinePosition); // position is
		// the value on the axis
		// marker.setPaint(Color.black);
		// marker.setLabel("here"); // see JavaDoc for labels, colors, strokes
		// CategoryPlot plot = jfc.getCategoryPlot();
		// plot.addRangeMarker(marker);
		// CategoryMarker cm = new
		// CategoryMarker(Integer.valueOf(verticalLinePosition));
		// cm.setDrawAsLine(true);
		// cm.setPaint(Color.LIGHT_GRAY);
		// plot.addDomainMarker(cm);
		return drawChart(jfc, fileName, width, height);
	}

	public static boolean drawChart(JFreeChart chart, String fileName, int width, int height) throws IOException {
		ChartUtils.saveChartAsPNG(new File(fileName), chart, width, height);
		return true;
	}

	/*
	 * public boolean drawXYSeries(HashMap<Integer, Integer> inputMap, String
	 * fileName, int width, int height, String valueDescription, String chartTitle,
	 * String xAxisTitle, String yAxisTitle) throws IOException { JFreeChart jfc =
	 * this.createXYSeries(inputMap, valueDescription, chartTitle, xAxisTitle,
	 * yAxisTitle); return this.drawChart(jfc, fileName, width, height); }
	 */
}
