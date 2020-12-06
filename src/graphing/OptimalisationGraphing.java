package graphing;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static main.Main.*;

public class OptimalisationGraphing extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(graphingFolder));
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Input files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }
        Scanner sc = new Scanner(selectedFile);

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
                Data data = new Data(t, value);
                series.get(i).getData().add(data);
                System.out.println(data);
            }
        }
//        lineChart.getData().add(series.get(1)); //TOTAL
        lineChart.getData().add(series.get(2));  //costNightShift
        lineChart.getData().add(series.get(3)); //costOverTime
        lineChart.getData().add(series.get(4)); //costUnscheduledRequests
        lineChart.getData().add(series.get(5)); //costStockLevel
        lineChart.getData().add(series.get(6)); //costParallelDays


        /* for (Series s : series) {
            lineChart.getData().addAll(s);
        }*/

        Scene scene = new Scene(lineChart, 500, 400);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public void saveAsPng(LineChart lineChart, String path) {
        WritableImage image = lineChart.snapshot(new SnapshotParameters(), null);
        File file = new File(path);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
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