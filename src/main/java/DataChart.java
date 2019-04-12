import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;

import java.util.ArrayList;

public class DataChart {
    private final XYChart chart;
    private final SwingWrapper sw;

    public DataChart() {
        String[] seriesNames = {"aine", "pine2", "line3", "dine4"};
        double[] data = {50, 51, 52, 53, 54, 55};
        ArrayList<double[]> data2 = new ArrayList<>();
        data2.add(new double[]{50.0, 51.0, 52.0, 53.0, 54.0, 55.0});
        data2.add(new double[]{55.0, 54.0, 53.0, 52.0, 51.0, 50.0});
        data2.add(new double[]{50.0, 50.5, 51.0, 51.5, 52.0, 52.5});
        data2.add(new double[]{55.0, 54.5, 54.0, 53.5, 53.0, 52.5});
        chart = QuickChartForGraphs.getChart("Simple XChart Real-time Demo",
                "Radians", "Sine", seriesNames, data, data2);
        sw = new SwingWrapper<>(chart);
        sw.displayChart();
    }

    public DataChart updateGraph(DataChart dataChart, String lineName, double xData,
                                 double yData) {
        double[] xxData = {xData};
        double[] yyData = {yData};
        dataChart.getChart().updateXYSeries(lineName, xxData, yyData, null);
        dataChart.getSw().repaintChart();
        return this;
    }

    public XYChart getChart(){
        return chart;
    }

    public SwingWrapper getSw(){
        return sw;
    }

}