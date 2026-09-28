package org.careamics.fiji.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.awt.*;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class LossPlot extends JFrame{
    
    public static void main(final String[] args) {
        Map<String, ArrayList<Float>> losses = new HashMap<>();
        losses.put("epoch", new ArrayList<>(Arrays.asList(0f, 1f, 2f)));
        losses.put("train", new ArrayList<>(Arrays.asList(0.5f, 0.6f, 0.7f)));
        losses.put("val", new ArrayList<>(Arrays.asList(0.4f, 0.5f, 0.6f)));

        final LossPlot plot = new LossPlot(losses);
    }

    public LossPlot(Map<String, ArrayList<Float>> losses) {
        XYSeries train_loss = new XYSeries("Train Loss");
        XYSeries val_loss = new XYSeries("Validation Loss");
        for (int i = 0; i < losses.get("epoch").size(); i++) {
            train_loss.add(losses.get("epoch").get(i), losses.get("train").get(i));
            val_loss.add(losses.get("epoch").get(i), losses.get("val").get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(train_loss);
        dataset.addSeries(val_loss);

        final JFreeChart chart = ChartFactory.createXYLineChart(
            "Losses",
            "Epoch",
            "Loss",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        // get a reference to the plot for further customisation...
        final XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        
        
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesPaint(1, Color.ORANGE);
        plot.setRenderer(renderer);
        
        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getDomainAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        final ChartPanel chartPanel = new ChartPanel(chart);
        
        this.add(chartPanel);
        this.setSize(new Dimension(640, 400));
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setVisible(true);

    }

}
