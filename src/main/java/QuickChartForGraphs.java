import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.util.ArrayList;

public class QuickChartForGraphs {

    private QuickChartForGraphs() {
    }

    public static XYChart getChart(String chartTitle, String xTitle, String yTitle, String[] seriesNames, double[] xData,
                                   ArrayList<double[]> yDatas) {
        XYChart chart = new XYChart(600, 400);
        chart.setTitle(chartTitle);
        chart.setXAxisTitle(xTitle);
        chart.setYAxisTitle(yTitle);
        for (int i = 0; i < yDatas.size(); i++) {
            XYSeries series;
            if (seriesNames[i] != null) {
                series = chart.addSeries(seriesNames[i], xData, yDatas.get(i));
            } else {
                chart.getStyler().setLegendVisible(false);
                series = chart.addSeries(" " + i, xData, yDatas.get(i));
            }

            series.setMarker(SeriesMarkers.NONE);

        }
        return chart;
    }
}
