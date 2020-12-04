package graphing;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OptimalisationGraphing extends Application {
    public static File csvFile = new File(System.getProperty("user.dir") + "/optimisation.csv");
    public static final String CSV_SEP = ",";

    @Override
    public void start(Stage stage) throws Exception {
      /*  FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")+"/src/graphing" ));
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Input files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }*/
        Scanner sc = new Scanner(csvFile);

        List<Series> series = generateSeriesList();
        String stageTitle = "Cost in function of temperature";
        stage.setTitle(stageTitle);
        final NumberAxis yAxis = new NumberAxis();
        final NumberAxis xAxis = new NumberAxis();

        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        String yAxisLabel = "Cost";
        yAxis.setLabel(yAxisLabel);
        String xAxisLabel = "Temperature";
        xAxis.setLabel(xAxisLabel);
        String lineChartTitle = "Cost in function of temperature";
        lineChart.setTitle(lineChartTitle);



        while (sc.hasNextLine()) {
            String[] nextLine = sc.nextLine().split(CSV_SEP);
            double t = Double.parseDouble(nextLine[0]);
            for (int i = 1; i < 6; i++) {
                double value = Double.parseDouble(nextLine[i]);
                series.get(i).getData().add(new Data(t, value));
            }
        }
        lineChart.getData().add(series.get(1));

        /* for (Series s : series) {
            lineChart.getData().addAll(s);
        }*/

        Scene scene = new Scene(lineChart, 500, 400);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private List<Series> generateSeriesList() {
        List<Series> series = new ArrayList<>(6);

        Series seriesTotal = new Series();
        seriesTotal.setName("Total Cost");
        series.add(seriesTotal);

        Series seriesNS = new Series();
        seriesNS.setName("Cost of Night shifts");
        series.add(seriesNS);

        Series seriesOT = new Series();
        seriesOT.setName("Cost of Overtimes");
        series.add(seriesOT);

        Series seriesUR = new Series();
        seriesUR.setName("Cost of Unscheduled requests");
        series.add(seriesUR);

        Series seriesSL = new Series();
        seriesSL.setName("Cost of items below sotck level");
        series.add(seriesSL);

        Series seriesDP = new Series();
        seriesDP.setName("Cost of Days with parallell shifts");
        series.add(seriesDP);

        return series;
    }

    public static void main(String[] args) {
        launch(args);
    }
}