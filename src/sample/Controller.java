package sample;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    VBox TimeVBox;
    @FXML
    Rectangle r0,r1,r2,r3,r4,r5,r6,r7;
    @FXML
    TextField calenTextField,maxtimeTextField;
    @FXML
    Slider calenSlider, maxtimeSlider, zoomSlider;
    @FXML
    Button CreateButton, StepButton, RunButton;
    @FXML
    ScrollPane RightScrollPane;
    @FXML
    Canvas canvas;

    IntegerProperty maxtime = new SimpleIntegerProperty();
    IntegerProperty calen = new SimpleIntegerProperty();
    boolean[][]ca;
    Rectangle[][] cells;
    BooleanProperty runningProperty = new SimpleBooleanProperty(true);


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MakeBindings();
        List<String> rulesList= new ArrayList<>(256);
        for (int i=0;i<256;i++) rulesList.add(Integer.toString(i));
        calenSlider.setMax(50); calenSlider.setMin(20); maxtimeSlider.setMax(200); maxtimeSlider.setMin(10);
        zoomSlider.setMax(3.125); zoomSlider.setMin(0.125); zoomSlider.setValue(1);
        calenSlider.setValue(30); maxtimeSlider.setValue(100);
        CreateButton.setOnAction(event -> create());
        StepButton.setOnAction(event -> step());
        RunButton.setOnAction(event -> run());
    }

    private void create() {
        canvas.setVisible(false);
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.setHeight(0);
        canvas.setWidth(0);
        TimeVBox.setVisible(true);
        cells = new Rectangle[calen.get()][];
        ca = new boolean[calen.get()][];

        for (int i=0;i<cells.length;i++) {
            ca[i] = new boolean[ca.length];
            cells[i] = new Rectangle[cells.length];
            for (int j=0;j<cells.length;j++) {
                final int ifi = i;
                final int jfi = j;
                final Rectangle rect = new Rectangle(16, 16);
                rect.widthProperty().bind(zoomSlider.valueProperty().multiply(16));
                rect.heightProperty().bind(zoomSlider.valueProperty().multiply(16));
                rect.setFill(Color.WHITE);
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(1);
                ca[i][j] = false;
                rect.fillProperty().addListener((observable, oldValue, newValue) -> {
                    if (((Color) newValue).getRed() == 0) ca[ifi][jfi] = true;
                    else ca[ifi][jfi] = false;
                });
                rect.setOnMouseClicked(event1 -> {
                    Rectangle source = (Rectangle) event1.getSource();
                    Color fill = (Color) source.getFill();
                    if (fill.getRed() == 0) source.setFill(Color.WHITE);
                    else source.setFill(Color.BLACK);
                });
                cells[i][j] = rect;
            }
        }
        TimeVBox.getChildren().clear();
        HBox cabox;
        for (Rectangle[] cr : cells) {
            cabox=new HBox();
            cabox.getChildren().addAll(cr);
            TimeVBox.getChildren().add(cabox);
        }
        runningProperty.set(false);
    }


    private void step() {
        int n = ca.length;
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        if (canvas.getWidth() == 0) {
            TimeVBox.setVisible(false);
            canvas.setVisible(true);
            canvas.setWidth(16 * n);
            canvas.setHeight(16 * n);
            for (int i = 0; i < n; i++) {
                for (int j=0;j<n;j++) {
                    gc.setFill(cells[i][j].getFill());
                    //gc.setFill(Color.BLACK);
                    gc.fillRect(16 * i, 0, 16, 16);
                }
            }
        }
        canvas.setHeight(canvas.getHeight() + 16);
        runningProperty.set(true);

        Task<boolean[][]> task = new Task<boolean[][]>() {
            @Override
            protected boolean[][] call() throws Exception {
                boolean[][] newca = new boolean[n][];
                for (int i = 0; i < n; i++) {
                    newca[i]=new boolean[n];
                    for (int j=0;j<n;j++) {
                        big:
                        for (int ii=i-1;ii<=i+1;ii++)
                            for (int jj=j-1;jj<=j+1;jj++) {
                                if (ii<0||jj<0||ii>=n||jj>=n||ca[ii][jj]) {
                                    newca[i][j]=true;
                                    break big;
                                }
                            }
                    }
                }
                return newca;
            }
        };
        task.setOnSucceeded(event -> {
            boolean[][] newca = (boolean[][]) event.getSource().getValue();
            ca=newca;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    gc.setFill(newca[i][j] ? Color.BLACK : Color.WHITE);
                    gc.fillRect(16 * j, 16 * i, 16, 16);
                }
            }
            runningProperty.set(false);
        });
        Thread t = new Thread(task);
        t.start();
    }

    private void run() {
        int n = ca.length;
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        if (canvas.getWidth() == 0) {
            TimeVBox.setVisible(false);
            canvas.setVisible(true);
            canvas.setWidth(16 * n);
            canvas.setHeight(16 * n);
            for (int i = 0; i < n; i++) {
                for (int j=0;j<n;j++) {
                    gc.setFill(cells[i][j].getFill());
                    gc.fillRect(16 * i, 0, 16, 16);
                }
            }
        }
        runningProperty.set(true);

        Task<boolean[][]> task = new Task<boolean[][]>() {
            @Override
            protected boolean[][] call() throws Exception {
                int maxtime = (int) maxtimeSlider.getValue();
                boolean[][] newca = new boolean[n][];
                for (int time=0;time<maxtime;time++) {
                    for (int i = 0; i < n; i++) {
                        newca[i]=new boolean[n];
                        for (int j=0;j<n;j++) {
                            big:
                            for (int ii = i - 1; ii <= i + 1; ii++)
                                for (int jj = j - 1; jj <= j + 1; jj++) {
                                    if (ii < 0 || jj < 0 || ii >= n || jj >= n || ca[ii][jj]) {
                                        newca[i][j] = true;
                                        break big;
                                    }
                                }
                            }
                        ca=newca;
                        }
                    }
                return newca;
            }
        };
        task.setOnSucceeded(event -> {
            boolean[][] newca = (boolean[][]) event.getSource().getValue();
            for (int i = 0; i < n; i++) {
                for (int j=0;j<n;j++) {
                    //ca[i][j] = newca[i][j];
                    gc.setFill(newca[i][j] ? Color.BLACK : Color.WHITE);
                    gc.fillRect(16 * j, 16 * i, 16, 16);
                }
            }
            runningProperty.set(false);
        });
        Thread t = new Thread(task);
        t.start();
    }

    private void MakeBindings() {
        maxtimeTextField.textProperty().bind(Bindings.createStringBinding(() -> Integer.toString((int) maxtimeSlider.getValue()), maxtimeSlider.valueProperty()));
        calenTextField.textProperty().bind(Bindings.createStringBinding(()->Integer.toString((int) calenSlider.getValue()), calenSlider.valueProperty()));
        maxtime.bind(Bindings.createIntegerBinding(() -> (int) maxtimeSlider.getValue(), maxtimeSlider.valueProperty()));
        calen.bind(Bindings.createObjectBinding(() -> (int) calenSlider.getValue(), calenSlider.valueProperty()));
        Scale scale=new Scale(1,1);
        scale.xProperty().bind(zoomSlider.valueProperty());
        scale.yProperty().bind(zoomSlider.valueProperty());
        canvas.getTransforms().add(scale);
        StepButton.disableProperty().bind(runningProperty);
        RunButton.disableProperty().bind(runningProperty);
    }
}
