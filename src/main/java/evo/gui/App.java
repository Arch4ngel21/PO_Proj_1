package evo.gui;

import com.sun.jdi.Value;
import evo.utilities.AbstractMapElement;
import evo.utilities.Animal;
import evo.utilities.SimulationEngine;
import evo.utilities.Vector2d;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashMap;


public class App extends Application {
    protected int mapHeight;
    protected int mapWidth;

    protected float jungleRatio;

    protected int grassEnergy;
    protected int animalStartEnergy;
    protected int dailyEnergyCost;

    protected int animalsSpawn;

    // protected int grassSpawnPerDay; (== 2 - po jednej w każdej ze stref)

    protected int refreshTime;

    public SimulationEngine engine;

    private boolean isEngineRunning = false;

    public GridPane gridPane;

    public LineChart<Number, Number> statisticsChart;

    public ValueAxis<Number> xAxis;
    public ValueAxis<Number> yAxis;

    public XYChart.Series<Number, Number> animalCount;
    public XYChart.Series<Number, Number> grassCount;
    public XYChart.Series<Number, Number> averageEnergy;
    public XYChart.Series<Number, Number> averageLivingSpan;
    public XYChart.Series<Number, Number> averageChildren;

    public Label selectedAnimalLabel;
    public Label selectedAnimalGenes;
    public Label selectedAnimalNumberOfChildren;
    public Label selectedAnimalDescendants;
    public Label selectedAnimalDeathEra;

    public CheckBox magicCheckBox;

    public Thread engineThread;
    public Thread drawThread;
    public Thread saveStatisticsThread;

    public FileWriter csvWriter;

    public Image animalImage;
    public Image grassImage;

    public EventHandler<MouseEvent> onImageClick;
    public EventHandler<MouseEvent> onEmptyCellClick;


    @Override
    public void start(Stage primaryStage) throws Exception {
        try
        {
            csvWriter = new FileWriter("statistics_data.csv");
        }
        catch (IOException exception)
        {
            File statisticsFile = new File("statistics_data.csv");
            statisticsFile.createNewFile();
            csvWriter = new FileWriter("statistics_data.csv");
        }

        setDrawThread(primaryStage);
        setSaveStatisticsThread();
        drawStartStage(primaryStage);
    }

    @Override
    public void stop()
    {
        System.out.println("Stage is closing");


    }

    public void drawStartStage(Stage primaryStage) {
        primaryStage.setTitle("Settings");

        try
        {
            animalImage = new Image(new FileInputStream("src/main/resources/gui/Animal.png"));
            grassImage = new Image(new FileInputStream("src/main/resources/gui/Grass.png"));
        }
        catch (FileNotFoundException exception)
        {
            System.out.println("Nie mozna znalezc podanego pliku");
            System.exit(-1);
        }

        createEventHandlerOnImageClick();
        createEventHandlerOnEmptyCellClick();

        VBox mainBox = new VBox();

        // MapHeight
        Label mapHLabel = new Label("Map height: ");
        TextField mapHField = new TextField("50");
        HBox mapHBox = new HBox();
        mapHBox.getChildren().addAll(mapHLabel, mapHField);
        mapHBox.setAlignment(Pos.CENTER);

        // Map Width
        Label mapWLabel = new Label("Map Width: ");
        TextField mapWField = new TextField("50");
        HBox mapWBox = new HBox();
        mapWBox.getChildren().addAll(mapWLabel, mapWField);
        mapWBox.setAlignment(Pos.CENTER);

        // Jungle Ratio
        Label jungleRatioLabel = new Label("Jungle Ratio: ");
        TextField jungleRatioField = new TextField("0.2");
        HBox jungleRatioBox = new HBox();
        jungleRatioBox.getChildren().addAll(jungleRatioLabel, jungleRatioField);
        jungleRatioBox.setAlignment(Pos.CENTER);

        // Grass Energy
        Label grassLabel = new Label("Grass energy: ");
        TextField grassField = new TextField("1000");
        HBox grassEnergyBox = new HBox();
        grassEnergyBox.getChildren().addAll(grassLabel, grassField);
        grassEnergyBox.setAlignment(Pos.CENTER);

        // Animal Start Energy
        Label startEnergyLabel = new Label("Animal's start energy: ");
        TextField startEnergyField = new TextField("200");
        HBox startEnergyBox = new HBox();
        startEnergyBox.getChildren().addAll(startEnergyLabel, startEnergyField);
        startEnergyBox.setAlignment(Pos.CENTER);

        // Daily energy Cost
        Label dailyEnergyLabel = new Label("Daily energy cost: ");
        TextField dailyEnergyField = new TextField("10");
        HBox dailyEnergyBox = new HBox();
        dailyEnergyBox.getChildren().addAll(dailyEnergyLabel, dailyEnergyField);
        dailyEnergyBox.setAlignment(Pos.CENTER);

        // Animals Spawn
        Label animalsSpawnLabel = new Label("Animals spawn: ");
        TextField animalsSpawnField = new TextField("100");
        HBox animalsSpawnBox = new HBox();
        animalsSpawnBox.getChildren().addAll(animalsSpawnLabel, animalsSpawnField);
        animalsSpawnBox.setAlignment(Pos.CENTER);

        // Refresh Time
        Label refreshTimeLabel = new Label("Refresh Time: ");
        TextField refreshTimeField = new TextField("10");
        HBox refreshTimeBox = new HBox();
        refreshTimeBox.getChildren().addAll(refreshTimeLabel, refreshTimeField);
        refreshTimeBox.setAlignment(Pos.CENTER);

        // Error message Text
        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setAlignment(Pos.CENTER);

        Button startButton = new Button("Start");
        startButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try
                {
                    mapHeight = Integer.parseInt(mapHField.getText());
                    mapWidth = Integer.parseInt(mapWField.getText());
                    jungleRatio = Float.parseFloat(jungleRatioField.getText());
                    grassEnergy = Integer.parseInt(grassField.getText());
                    animalStartEnergy = Integer.parseInt(startEnergyField.getText());
                    dailyEnergyCost = Integer.parseInt(dailyEnergyField.getText());
                    animalsSpawn = Integer.parseInt(animalsSpawnField.getText());
                    refreshTime = Integer.parseInt(refreshTimeField.getText());

                    if (jungleRatio >= 1 || grassEnergy < 0 || animalStartEnergy < 0 || dailyEnergyCost < 0 ||
                            animalsSpawn < 10 || refreshTime < 0)
                    {
                        throw new NumberFormatException();
                    }

                    errorLabel.setText("");

                    engine = new SimulationEngine(mapHeight, mapWidth, jungleRatio, grassEnergy, animalStartEnergy,
                            dailyEnergyCost, animalsSpawn, drawThread, saveStatisticsThread, animalImage, grassImage, onImageClick);

                    engineThread = new Thread(engine);
                    engineThread.setDaemon(true);

                    drawFirstStage(primaryStage);

                    setEngineRunning(true);
                    drawThread.start();
                    saveStatisticsThread.start();
                    engineThread.start();

                }
                catch (NumberFormatException exception)
                {
                    errorLabel.setText("Incorrect value provided");
                }

            }
        });


        mainBox.getChildren().addAll(mapHBox, mapWBox, jungleRatioBox, grassEnergyBox, startEnergyBox, dailyEnergyBox,
                animalsSpawnBox, refreshTimeBox, errorLabel, startButton);

        mainBox.setAlignment(Pos.CENTER);
        mainBox.setSpacing(10);

        primaryStage.setScene(new Scene(mainBox, 500, 500));
        primaryStage.show();
    }

    public void setDrawThread(Stage primaryStage)
    {
        drawThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                {
                    synchronized (this)
                    {
                        try
                        {
                            Thread.sleep(200);
                        }
                        catch (InterruptedException exception)
                        {
                            if (engine.getSelectedAnimalStatisticsToUpdate())
                            {
                                engine.setSelectedAnimalStatisticsToUpdate(false);
                                Platform.runLater(() -> updateSelectedAnimalStatistics(primaryStage));
                            }

                            else if (engine.getStatisticsToUpdate())
                            {
                                engine.endOfStatisticsUpdate();
                                Platform.runLater(() -> updateStatistics(primaryStage));
                            }
                            else
                            {
                                Vector2d oldPosition = engine.getOldPositionChange();
                                Vector2d newPosition = engine.getNewPositionChange();
                                AbstractMapElement changedElement = engine.getChangedElement();

                                Platform.runLater(() -> updateStage(primaryStage, oldPosition, newPosition, changedElement));
                            }
                        }
                    }
                }
            }
        });
    }

    public void setSaveStatisticsThread()
    {
        saveStatisticsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                {
                    synchronized (this)
                    {
                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException exception)
                        {
                            try {
                                csvWriter.append("Number of animals: ");
                                csvWriter.append(String.valueOf(engine.getAnimalCounter()));
                                csvWriter.append(" Number of grass: ");
                                csvWriter.append(String.valueOf(engine.getGrassCounter()));
                                csvWriter.append(" Average energy for animal: ");
                                csvWriter.append(String.valueOf(engine.getAverageEnergy()));
                                csvWriter.append(" Average living span for animal: ");
                                csvWriter.append(String.valueOf(engine.getAverageLifeSpan()));
                                csvWriter.append(" Average number of children for living animals: ");
                                csvWriter.append(String.valueOf(engine.getAverageNumberOfChildren()));
                                csvWriter.append("\n");
                                csvWriter.flush();

                            } catch (IOException e) {
                                System.out.println("IOException przy zapisywaniu danych!");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
    }

    public void drawFirstStage(Stage primaryStage)
    {
        primaryStage.setTitle("Simulation");

        HBox mainHBox = new HBox();

        // ---------------- Mapa ----------------
        VBox mapVBox = new VBox();

        gridPane = new GridPane();
        gridPane.setGridLinesVisible(true);
        gridPane.setAlignment(Pos.CENTER);

        Label xy_label = new Label("y/x");
        gridPane.add(xy_label, 0, 0);
        gridPane.getRowConstraints().add(new RowConstraints(15));
        gridPane.getColumnConstraints().add(new ColumnConstraints(15));
        gridPane.setHalignment(xy_label, HPos.CENTER);

        for (int y=0; y < mapHeight; y++)
        {
            Label label_y = new Label(String.valueOf(mapHeight - y - 1));
            gridPane.add(label_y, 0, y+1);
            gridPane.getRowConstraints().add(new RowConstraints(15));
            gridPane.setHalignment(xy_label, HPos.CENTER);
        }

        for (int x=0; x < mapWidth; x++)
        {
            Label label_x = new Label(String.valueOf(x));
            gridPane.add(label_x, x+1, 0);
            gridPane.getColumnConstraints().add(new ColumnConstraints(15));
            gridPane.setHalignment(xy_label, HPos.CENTER);
        }

        ImageView element;
        StackPane pane;
        for (int y=0; y < mapHeight; y++)
        {
            for (int x=0; x < mapWidth; x++)
            {
                if(engine.getJungleMap().isOccupied(new Vector2d(x, y)))
                {

                    element = engine.getJungleMap().elementAt(new Vector2d(x, y)).getElementImageView();

                    gridPane.add(element, x+1, mapHeight - y);
                    element.setFitWidth(15);
                    element.setFitHeight(15);
                    gridPane.setHalignment(element, HPos.CENTER);
                    gridPane.setValignment(element, VPos.CENTER);
                }
                else if (engine.getSteppeMap().isOccupied(new Vector2d(x, y)))
                {

                    element = engine.getSteppeMap().elementAt(new Vector2d(x, y)).getElementImageView();

                    gridPane.add(element, x+1, mapHeight - y);
                    element.setFitWidth(15);
                    element.setFitHeight(15);
                    gridPane.setHalignment(element, HPos.CENTER);
                    gridPane.setValignment(element, VPos.CENTER);
                }
                else
                {
                    pane = new StackPane();
                    pane.setOnMouseClicked(onEmptyCellClick);

                    if (engine.getLowLeftJungle().x <= x && x <= engine.getHighRightJungle().x && engine.getLowLeftJungle().y <= y && y <= engine.getHighRightJungle().y)
                    {
                        pane.setStyle("-fx-background-color: green; -fx-border-color: black");
                    }

                    gridPane.add(pane, x+1, mapHeight - y);
                    GridPane.setFillHeight(pane, true);
                    GridPane.setFillWidth(pane, true);
                }
            }
        }

        HBox buttonsHBox = new HBox();
        Button resumeSimulationButton = new Button("Resume");
        Button stopSimulationButton = new Button("Stop");

        resumeSimulationButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (getEngineRunning())
                {
                    return;
                }

                setEngineRunning(true);
                engineThread.resume();
            }
        });

        stopSimulationButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!getEngineRunning())
                {
                    return;
                }

                setEngineRunning(false);
                engineThread.suspend();
            }
        });

        Label magicCheckBoxLabel = new Label("Enable Magic Tactic:");
        magicCheckBox = new CheckBox();
        magicCheckBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(magicCheckBox.isSelected())
                {
                    engine.setMagicTacticEnabled(true);
                }
                else
                {
                    engine.setMagicTacticEnabled(false);
                }
            }
        });

        buttonsHBox.getChildren().addAll(resumeSimulationButton, stopSimulationButton, magicCheckBoxLabel, magicCheckBox);
        buttonsHBox.setSpacing(35);
        buttonsHBox.setAlignment(Pos.CENTER);

        mapVBox.getChildren().addAll(gridPane, buttonsHBox);
        mapVBox.setSpacing(20);
        // ---------------- Mapa ----------------

        // ------------- Statystyki -------------
        VBox statisticsVBox = new VBox();
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        xAxis.setLabel("Era");
        xAxis.setAutoRanging(false);
        xAxis.setUpperBound(20.0);

        statisticsChart = new LineChart<Number, Number>(xAxis, yAxis);
        statisticsChart.setTitle("Simulation's statistics");

        animalCount = new XYChart.Series<Number, Number>();
        grassCount = new XYChart.Series<Number, Number>();
        averageEnergy = new XYChart.Series<Number, Number>();
        averageLivingSpan = new XYChart.Series<Number, Number>();
        averageChildren = new XYChart.Series<Number, Number>();

        animalCount.setName("Number of animals");
        grassCount.setName("Number of grass");
        averageEnergy.setName("Average energy for animal");
        averageLivingSpan.setName("Average living span for animal");
        averageChildren.setName("Average number of children for living animals");

        animalCount.getData().add(new XYChart.Data<>(1, engine.getAnimalCounter()));
        grassCount.getData().add(new XYChart.Data<>(1, engine.getGrassCounter()));
        averageEnergy.getData().add(new XYChart.Data<>(1, engine.getAverageEnergy()));
        averageLivingSpan.getData().add(new XYChart.Data<>(1, engine.getAverageLifeSpan()));
        averageChildren.getData().add(new XYChart.Data<>(1, engine.getAverageNumberOfChildren()));

        statisticsChart.getData().addAll(animalCount, grassCount, averageEnergy, averageLivingSpan, averageChildren);

        statisticsVBox.getChildren().add(statisticsChart);
        statisticsVBox.setAlignment(Pos.CENTER);
        statisticsVBox.setSpacing(30);
        // ------------- Statystyki -------------

        // --- Statystyki zaznaczonego zwierzęcia ---
        VBox selectedAnimaVBox = new VBox();
        selectedAnimaVBox.setMaxSize(400, 1200);
        selectedAnimaVBox.setPrefSize(300, 500);
        selectedAnimaVBox.setStyle("-fx-border-color: black; -fx-border-image-insets: 5");
        selectedAnimaVBox.setAlignment(Pos.CENTER);
        selectedAnimaVBox.setSpacing(30);

        selectedAnimalLabel = new Label("Selected animal: ---");
        selectedAnimalLabel.setStyle("-fx-font-weight: bold");
        selectedAnimalLabel.setFont(Font.font("Arial", 16));

        selectedAnimalGenes = new Label("Genes: -----------");
        selectedAnimalGenes.setAlignment(Pos.CENTER);

        selectedAnimalNumberOfChildren = new Label("Number of Children: ---");
        selectedAnimalNumberOfChildren.setAlignment(Pos.CENTER);

        selectedAnimalDescendants = new Label("Number of Descendants: ---");
        selectedAnimalDescendants.setAlignment(Pos.CENTER);

        selectedAnimalDeathEra = new Label("Era of Animal's death: ---");
        selectedAnimalDeathEra.setAlignment(Pos.CENTER);

        selectedAnimaVBox.getChildren().addAll(selectedAnimalLabel, selectedAnimalGenes, selectedAnimalNumberOfChildren,
                selectedAnimalDescendants, selectedAnimalDeathEra);
        statisticsVBox.getChildren().add(selectedAnimaVBox);

        // --- Statystyki zaznaczonego zwierzęcia ---

        mainHBox.getChildren().addAll(statisticsVBox, mapVBox);
        mainHBox.setSpacing(50);

        Scene firstScene = new Scene(mainHBox, 500, 500);
        primaryStage.setScene(firstScene);
        primaryStage.show();
    }

    public void updateStage(Stage primaryStage, Vector2d oldPosition, Vector2d newPosition, AbstractMapElement mapElement)
    {

        // Jeżeli oldPosition -> (-1, -1), to dodajemy nowy element do mapy
        if (!oldPosition.equals(new Vector2d(-1, -1)))
        {
            gridPane.getChildren().remove(mapElement.getElementImageView());

            if (!engine.getJungleMap().isOccupied(oldPosition) && !engine.getSteppeMap().isOccupied(oldPosition))
            {
                StackPane pane = new StackPane();
                if (engine.getLowLeftJungle().x <= oldPosition.x && oldPosition.x <= engine.getHighRightJungle().x && engine.getLowLeftJungle().y <= oldPosition.y && oldPosition.y <= engine.getHighRightJungle().y)
                {
                    pane.setStyle("-fx-background-color: green; -fx-border-color: black");
                }

                gridPane.add(pane, oldPosition.x + 1, mapHeight - oldPosition.y);
                GridPane.setFillHeight(pane, true);
                GridPane.setFillWidth(pane, true);
            }
            else
            {
                if (engine.getJungleMap().isOccupied(oldPosition))
                {
                    gridPane.add(engine.getJungleMap().elementAt(oldPosition).getElementImageView(), oldPosition.x + 1, mapHeight - oldPosition.y);
                }
                else
                {
                    gridPane.add(engine.getSteppeMap().elementAt(oldPosition).getElementImageView(), oldPosition.x + 1, mapHeight - oldPosition.y);
                }
            }

        }

        // Nowy obiekt na mapie
        // Jeżeli newPosition -> (-1, -1), to oznacza, że zwierzak zginął i go właśnie usuwamy
        if (!newPosition.equals(new Vector2d(-1, -1)))
        {
            gridPane.add(mapElement.getElementImageView(), newPosition.x + 1, mapHeight - newPosition.y);
            gridPane.setHalignment(mapElement.getElementImageView(), HPos.CENTER);
            gridPane.setValignment(mapElement.getElementImageView(), VPos.CENTER);
        }

        primaryStage.show();
    }

    public void updateStatistics(Stage primaryStage)
    {
        if (engine.getEra() % 20 == 0)
        {
            animalCount.getData().clear();
            grassCount.getData().clear();
            averageEnergy.getData().clear();
            averageLivingSpan.getData().clear();
            averageChildren.getData().clear();

            xAxis.setLowerBound(engine.getEra());
            xAxis.setUpperBound(engine.getEra() + 20);
        }

        animalCount.getData().add(new XYChart.Data<>(engine.getEra(), engine.getAnimalCounter()));
        grassCount.getData().add(new XYChart.Data<>(engine.getEra(), engine.getGrassCounter()));
        averageEnergy.getData().add(new XYChart.Data<>(engine.getEra(), engine.getAverageEnergy()));
        averageLivingSpan.getData().add(new XYChart.Data<>(engine.getEra(), engine.getAverageLifeSpan()));
        averageChildren.getData().add(new XYChart.Data<>(engine.getEra(), engine.getAverageNumberOfChildren()));

        primaryStage.show();
    }

    public void updateSelectedAnimalStatistics(Stage primaryStage)
    {
        selectedAnimalLabel.setText("Selected Animal: " + String.valueOf(engine.getObservedAnimal().getElementIndex()));
        selectedAnimalGenes.setText("Genes: " + Arrays.toString(getFirstHalfGenesArray(engine.getObservedAnimal().getGenes())) +
                "\n" + Arrays.toString(getSecondHalfGenesArray(engine.getObservedAnimal().getGenes())));
        selectedAnimalNumberOfChildren.setText("Number of Children: " + String.valueOf(engine.getObservedAnimalChildren()));
        selectedAnimalDescendants.setText("Number of Descendants: " + String.valueOf(engine.getObservedAnimalDescendants()));

        if (engine.getObservedAnimalDeathEra() != -1)
        {
            selectedAnimalDeathEra.setText("Era of Animal's death: " + String.valueOf(engine.getObservedAnimalDeathEra()));
        }

        primaryStage.show();
    }

    public void setEngineRunning(boolean value)
    {
        isEngineRunning = value;
    }

    public boolean getEngineRunning()
    {
        return isEngineRunning;
    }

    public void createEventHandlerOnImageClick()
    {
        onImageClick = new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (isEngineRunning)
                {
                    return;
                }

                ImageView elementImageView = (ImageView) event.getSource();
                int element_x = GridPane.getColumnIndex(elementImageView);
                int element_y = GridPane.getRowIndex(elementImageView);

                AbstractMapElement animal;
                if (engine.getJungleMap().isOccupied(new Vector2d(element_x-1, mapHeight - element_y)))
                {
                    animal = engine.getJungleMap().elementAt(new Vector2d(element_x-1, mapHeight - element_y));
                }
                else
                {
                    animal = engine.getSteppeMap().elementAt(new Vector2d(element_x-1, mapHeight - element_y));
                }

                engine.setObservedAnimal((Animal) animal);
                engine.addObservedAnimal((Animal) animal);
                ((Animal) animal).setObserved(true);

                selectedAnimalLabel.setText("Selected Animal: " + String.valueOf(animal.getElementIndex()));
                selectedAnimalGenes.setText("Genes: " + Arrays.toString(getFirstHalfGenesArray(((Animal) animal).getGenes())) +
                        "\n" + Arrays.toString(getSecondHalfGenesArray(((Animal) animal).getGenes())));
                selectedAnimalNumberOfChildren.setText("Number of Children: 0");
                selectedAnimalDescendants.setText("Number of Descendants: 0");
            }
        };
    }

    public void createEventHandlerOnEmptyCellClick()
    {
        onEmptyCellClick = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (isEngineRunning)
                {
                    return;
                }

                if (engine.getObservedAnimal() == null)
                {
                    return;
                }

                selectedAnimalLabel.setText("Selected animal: ---");
                selectedAnimalGenes.setText("Genes: -----------");
                selectedAnimalNumberOfChildren.setText("Number of Children: ---");
                selectedAnimalDescendants.setText("Number of Descendants: ---");
                selectedAnimalDeathEra.setText("Era of Animal's death: ---");

                engine.setObservedAnimal(null);
                engine.setObservedAnimalDeathEra(-1);

                engine.clearObservedAnimals();
                engine.clearObservedAnimalChildren();
                engine.clearObservedAnimalDescendants();
            }
        };
    }

    public int[] getFirstHalfGenesArray(int[] genes)
    {
        int[] res = new int[16];
        for (int i=0; i < 16; i++)
        {
            res[i] = genes[i];
        }
        return res;
    }

    public int[] getSecondHalfGenesArray(int[] genes)
    {
        int[] res = new int[16];
        for (int i=16; i < 32; i++)
        {
            res[i-16] = genes[i];
        }
        return res;
    }
}
