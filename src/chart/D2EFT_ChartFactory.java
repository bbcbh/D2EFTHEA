package chart;

import java.util.Arrays;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

public class D2EFT_ChartFactory {
	
	
	
	
	

	public static JFreeChart generateSingleFreqencyBarChart(int[] data, String title, String xlabel, String ylabel) {

		JFreeChart chart;

		Arrays.sort(data);
		int[] stat = new int[data[data.length - 1] + 1];

		for (int i = 0; i < data.length; i++) {
			stat[data[i]]++;
		}

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int i = 1; i < stat.length; i++) {
			dataset.addValue(stat[i], ylabel, Integer.toString(i));
		}

		chart = ChartFactory.createBarChart(title, xlabel, ylabel, dataset);	
		chart.removeLegend();
		java.awt.Color barColor = new java.awt.Color(0,0,255,25);
		
		
		((BarRenderer)  chart.getCategoryPlot().getRenderer()).setBarPainter(new StandardBarPainter());
		((BarRenderer)  chart.getCategoryPlot().getRenderer()).setSeriesPaint(0,barColor);
		chart.getCategoryPlot().getDomainAxis(0).setCategoryMargin(0);
		
		return chart;

		
	}

	public static void main(String[] arg) {
		int[] test_data = new int[] { 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 5, 5, 5 };
		final JFreeChart c = D2EFT_ChartFactory.generateSingleFreqencyBarChart(test_data, "Test", "Test data", "Freq");

		showJFreeChart(c);
	}

	public static void showJFreeChart(final JFreeChart c) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					org.jfree.chart.ChartFrame frame = new ChartFrame("Test Frame", c);
					frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
					frame.setBounds(100, 100, 500, 350);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
