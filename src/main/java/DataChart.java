import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class DataChart extends Application {

    private LineChart<Number, Number> LINE_CHART;
    private Scene SCENE;
    private Stage STAGE;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Line Chart Sample");
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date and Time");
        LINE_CHART = new LineChart<>(xAxis, yAxis);
        LINE_CHART.setTitle("Price Monitoring");
        SCENE = new Scene(LINE_CHART, 800, 600);
        stage.setScene(SCENE);
        stage.show();
        STAGE = stage;
    }

    public DataChart createGraph() {
        launch(DataChart.class);
        return this;
    }

    public XYChart.Series createLine(String lineName) {
        XYChart.Series series = new XYChart.Series();
        series.setName(lineName);
        LINE_CHART.getData().add(series);
        STAGE.setScene(SCENE);
        STAGE.show();
//        series.getData().add(new XYChart.Data(1, 23));
        return series;
    }

    public DataChart addPoint(XYChart.Series series, int time, int value){
        series.getData().add(new XYChart.Data(time, value));
        STAGE.setScene(SCENE);
        STAGE.show();
        return this;
    }
}