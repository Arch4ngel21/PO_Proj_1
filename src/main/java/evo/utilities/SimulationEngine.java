package evo.utilities;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class SimulationEngine implements Runnable{

    private AbstractMap jungleMap;
    private AbstractMap steppeMap;

    protected Vector2d highRight;
    protected Vector2d lowLeftJungle;
    protected Vector2d highRightJungle;

    private Vector2d oldPositionChange;
    private Vector2d newPositionChange;

    private AbstractMapElement changedElement;

    private boolean statisticsToUpdate = false;
    private boolean selectedAnimalStatisticsToUpdate = false;
    private boolean isMagicTacticEnabled = false;

    private int magicTacticUsed = 0;

    protected int mapHeight;
    protected int mapWidth;

    protected float jungleRatio;

    protected int grassEnergy;
    protected int animalStartEnergy;
    protected int dailyEnergyCost;

    protected int animalsSpawn;

    protected int era = 1;

    protected int nextAnimalIndex = 0;
    protected int animalCounter = 0;
    protected int grassCounter = 0;
    protected int deadAnimalsCounter = 0;

    protected long sumLifeSpan = 0;

    private ArrayList<Animal> animalList = new ArrayList<>();
    private ArrayList<Animal> observedAnimals = new ArrayList<>();

    private boolean initialized = false;

    private Thread drawThread;
    private Thread saveStatisticsThread;

    private Image animalImage;
    private Image grassImage;

    private EventHandler<MouseEvent> onImageClick;

    private Animal observedAnimal = null;
    private int observedAnimalDeathEra = -1;
    private int observedAnimalChildren = 0;
    private int observedAnimalDescendants = 0;

    public SimulationEngine(int mapHeight_, int mapWidth_, float jungleRatio_, int grassEnergy_, int animalStartEnergy_,
                            int dailyEnergyCost_, int animalsSpawn_, Thread drawThread_, Thread saveStatisticsThread_,
                            Image animalImage_, Image grassImage_, EventHandler<MouseEvent> onImageClick_)
    {
        mapWidth = mapWidth_;
        mapHeight = mapHeight_;
        jungleRatio = jungleRatio_;
        grassEnergy = grassEnergy_;
        animalStartEnergy = animalStartEnergy_;
        dailyEnergyCost = dailyEnergyCost_;
        animalsSpawn = animalsSpawn_;
        drawThread = drawThread_;
        saveStatisticsThread = saveStatisticsThread_;
        animalImage = animalImage_;
        grassImage = grassImage_;
        onImageClick = onImageClick_;

        highRight = new Vector2d(mapWidth_, mapHeight_);
        int jungleMapHeight = Math.round(mapHeight_ * jungleRatio_);
        int jungleMapWidth = Math.round(mapWidth_ * jungleRatio_);

        lowLeftJungle = new Vector2d((mapWidth_ - jungleMapWidth) / 2, (mapHeight_ - jungleMapHeight) / 2);
        highRightJungle = new Vector2d(lowLeftJungle.x + jungleMapWidth + 1, lowLeftJungle.y + jungleMapHeight + 1);

        jungleMap = new JungleMap(lowLeftJungle, highRightJungle, drawThread, this);
        steppeMap = new SteppeMap(highRight, lowLeftJungle, highRightJungle, drawThread, this);

        Random random = new Random();
        int new_x;
        int new_y;
        Vector2d new_position;

        // Dodanie początkowych zwierząt do mapy
        for (int i=0; i < animalsSpawn; i++)
        {
            new_x = random.nextInt(mapWidth);
            new_y = random.nextInt(mapHeight);
            new_position = new Vector2d(new_x, new_y);

            while (jungleMap.isOccupied(new_position) || steppeMap.isOccupied(new_position))
            {
                new_x = random.nextInt(mapWidth);
                new_y = random.nextInt(mapHeight);
                new_position = new Vector2d(new_x, new_y);
            }

            Animal new_Animal = new Animal(new_position, animalStartEnergy, animalStartEnergy, animalImage, onImageClick);
            new_Animal.elementIndex = i;
            addAnimalIndex();
            addAnimalCounter();
            animalList.add(new_Animal);

            if (lowLeftJungle.x <= new_x && new_x <= highRightJungle.x && lowLeftJungle.y <= new_y && new_y <= highRightJungle.y)
            {
                jungleMap.putElement(new_position, new_Animal);
                new_Animal.setMap(jungleMap);
            }
            else
            {
                steppeMap.putElement(new_position, new_Animal);
                new_Animal.setMap(steppeMap);
            }
        }

        new_x = random.nextInt(mapWidth);
        new_y = random.nextInt(mapHeight);
        new_position = new Vector2d(new_x, new_y);

        while (lowLeftJungle.x > new_x || new_x > highRightJungle.x || lowLeftJungle.y > new_y || new_y > highRightJungle.y || jungleMap.isOccupied(new_position))
        {
            new_x = random.nextInt(mapWidth);
            new_y = random.nextInt(mapHeight);
            new_position = new Vector2d(new_x, new_y);
        }

        Grass new_Grass = new Grass(new_position, grassImage);
        addGrassCounter();
        jungleMap.putElement(new_position, new_Grass);
        new_Grass.setMap(jungleMap);

        new_x = random.nextInt(mapWidth);
        new_y = random.nextInt(mapHeight);
        new_position = new Vector2d(new_x, new_y);

        while ((lowLeftJungle.x <= new_x && new_x <= highRightJungle.x && lowLeftJungle.y <= new_y && new_y <= highRightJungle.y) || steppeMap.isOccupied(new_position))
        {
            new_x = random.nextInt(mapWidth);
            new_y = random.nextInt(mapHeight);
            new_position = new Vector2d(new_x, new_y);
        }

        new_Grass = new Grass(new_position, grassImage);
        addGrassCounter();
        steppeMap.putElement(new_position, new_Grass);
        new_Grass.setMap(steppeMap);

        initialized = true;
    }


    @Override
    public void run() {
        if (!initialized)
        {
            System.out.println("Engine not initialized!");
            return;
        }


        while (true)
        {
            synchronized (this) {
                try {
                    // Jeżeli brak zwierząt, to nie ma sensu kontynuować symulacji
                    if (animalList.size() == 0) {
                        while (true) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException exception) {
                                return;
                            }
                        }
                    }
                    // TODO - Magiczna taktyka
                    // 0. Nowa epoka
                    addEra();

                    // 1. Usunięcie martwych zwierząt
                    int i = 0;
                    int n = animalList.size();
                    while (i < n) {
                        Animal animal = animalList.get(i);
                        animal.takeEnergy(dailyEnergyCost);
                        if (isDead(animal)) {
                            animal.map.removeDeadAnimal(animal);
                            animalList.remove(animal);
                            n--;
                            Thread.sleep(50);
                            if (observedAnimal != null && observedAnimal.elementIndex == animal.elementIndex) {
                                observedAnimalDeathEra = era;
                                setSelectedAnimalStatisticsToUpdate(true);
                                drawThread.interrupt();
                                Thread.sleep(50);
                            }
                        } else {
                            i++;
                        }
                    }

                    // 1.5 Magiczna Taktyka
                    if (isMagicTacticEnabled && animalList.size() == 5 && magicTacticUsed < 3)
                    {
                        System.out.println("Magic Tactic Used!");
                        for (int j=0; j < 5; j++)
                        {
                            useMagicTactic(animalList.get(j));
                        }
                        magicTacticUsed++;
                    }

                    // 2. Przemieszczenie zwierząt
                    for (Animal animal : animalList) {
                        animal.map.move(animal);
                        Thread.sleep(50);
                    }

                    // 3. Jedzenie
                    for (Vector2d eatingPlace : jungleMap.eatingPlaces) {
                        jungleMap.eat(eatingPlace);
                        Thread.sleep(50);
                    }
                    jungleMap.eatingPlaces.clear();

                    for (Vector2d eatingPlace : steppeMap.eatingPlaces) {
                        steppeMap.eat(eatingPlace);
                        Thread.sleep(50);
                    }
                    steppeMap.eatingPlaces.clear();

                    // 4. Rozmnażanie się zwierząt
                    for (Vector2d copPlace : jungleMap.copPlaces) {
                        if (jungleMap.isPossibleCopPlace(copPlace)) {
                            jungleMap.copulate(copPlace);
                            Thread.sleep(50);
                        }
                    }
                    jungleMap.copPlaces.clear();

                    for (Vector2d copPlace : steppeMap.copPlaces) {
                        if (steppeMap.isPossibleCopPlace(copPlace)) {
                            steppeMap.copulate(copPlace);
                            Thread.sleep(50);
                        }
                    }
                    steppeMap.copPlaces.clear();

                    // 5. Nowa trawa
                    jungleMap.growNewGrass();
                    Thread.sleep(50);
                    steppeMap.growNewGrass();
                    Thread.sleep(50);

                    // 6. Aktualizacja statystyk
                    updateStatistics();
                    Thread.sleep(50);

                    // 7. Zapisanie statystyk
                    saveStatistics();

                } catch (InterruptedException exception) {
                    System.out.println("Exception in SimulationEngine Thread");
                }
            }
        }
    }

    public void useMagicTactic(Animal animalCopy)
    {
        Random random = new Random();
        int new_x = random.nextInt(mapWidth);
        int new_y = random.nextInt(mapHeight);
        Vector2d new_position = new Vector2d(new_x, new_y);

        while (jungleMap.isOccupied(new_position) || steppeMap.isOccupied(new_position))
        {
            new_x = random.nextInt(mapWidth);
            new_y = random.nextInt(mapHeight);
            new_position = new Vector2d(new_x, new_y);
        }

        Animal new_Animal = new Animal(new_position, animalStartEnergy, animalStartEnergy, animalImage, onImageClick, animalCopy);
        new_Animal.elementIndex = nextAnimalIndex;
        addAnimalIndex();
        addAnimalCounter();
        animalList.add(new_Animal);

        if (lowLeftJungle.x <= new_x && new_x <= highRightJungle.x && lowLeftJungle.y <= new_y && new_y <= highRightJungle.y)
        {
            jungleMap.putElement(new_position, new_Animal);
            new_Animal.setMap(jungleMap);
        }
        else
        {
            steppeMap.putElement(new_position, new_Animal);
            new_Animal.setMap(steppeMap);
        }
        new_Animal.map.positionChanged(new Vector2d(-1, -1), new_Animal.getPosition(), new_Animal);
    }

    public boolean isDead(Animal animal) {
        return animal.getEnergy() < 0;
    }

    public IWorldMap getJungleMap()
    {
        return jungleMap;
    }

    public IWorldMap getSteppeMap()
    {
        return steppeMap;
    }

    public Vector2d getLowLeftJungle() {
        return lowLeftJungle;
    }

    public Vector2d getHighRightJungle() {
        return highRightJungle;
    }

    public Vector2d getOldPositionChange() {
        return oldPositionChange;
    }

    public Vector2d getNewPositionChange() {
        return newPositionChange;
    }

    public AbstractMapElement getChangedElement() {
        return changedElement;
    }

    public Image getAnimalImage() {
        return animalImage;
    }

    public Image getGrassImage() {
        return grassImage;
    }

    public int getAnimalCounter() {
        return animalCounter;
    }

    public int getGrassCounter() {
        return grassCounter;
    }

    public float getAverageEnergy()
    {
        long sumEnergy = 0;
        for (int i=0; i < animalList.size(); i++)
        {
            sumEnergy += animalList.get(i).getEnergy();
        }

        return sumEnergy / (float) animalList.size();
    }

    public int getEra() {
        return era;
    }

    public float getAverageLifeSpan()
    {
        return (float) sumLifeSpan / (float) era;
    }

    public float getAverageNumberOfChildren()
    {
        int sumChildren = 0;
        for (int i=0; i < animalList.size(); i++)
        {
            sumChildren += animalList.get(i).getNumberOfChildren();
        }

        return (float) sumChildren / (float) animalList.size();
    }

    public boolean getStatisticsToUpdate()
    {
        return statisticsToUpdate;
    }

    public EventHandler<MouseEvent> getOnImageClick()
    {
        return onImageClick;
    }

    public Animal getObservedAnimal() {
        return observedAnimal;
    }

    public boolean getSelectedAnimalStatisticsToUpdate()
    {
        return selectedAnimalStatisticsToUpdate;
    }

    public int getObservedAnimalDeathEra() {
        return observedAnimalDeathEra;
    }

    public int getObservedAnimalChildren() {
        return observedAnimalChildren;
    }

    public int getObservedAnimalDescendants() {
        return observedAnimalDescendants;
    }

    public boolean getMagicTacticEnabled()
    {
        return isMagicTacticEnabled;
    }

    public void updateStatistics()
    {
        statisticsToUpdate = true;
        drawThread.interrupt();
    }

    public void saveStatistics()
    {
        saveStatisticsThread.interrupt();
    }

    public void endOfStatisticsUpdate()
    {
        statisticsToUpdate = false;
    }

    public void setOldPositionChange(Vector2d oldPositionChange) {
        this.oldPositionChange = oldPositionChange;
    }

    public void setNewPositionChange(Vector2d newPositionChange) {
        this.newPositionChange = newPositionChange;
    }

    public void setChangedElement(AbstractMapElement changedElement) {
        this.changedElement = changedElement;
    }

    public void setObservedAnimal(Animal observedAnimal) {
        this.observedAnimal = observedAnimal;
    }

    public void setObservedAnimalDeathEra(int observedAnimalDeathEra) {
        this.observedAnimalDeathEra = observedAnimalDeathEra;
    }

    public void setSelectedAnimalStatisticsToUpdate(boolean selectedAnimalStatisticsToUpdate) {
        this.selectedAnimalStatisticsToUpdate = selectedAnimalStatisticsToUpdate;
    }

    public void setMagicTacticEnabled(boolean value)
    {
        isMagicTacticEnabled = value;
    }

    public void addAnimalIndex()
    {
        nextAnimalIndex++;
    }

    public void addGrassCounter() {
        grassCounter++;
    }

    public void addAnimalCounter()
    {
        animalCounter++;
    }

    public void addDeadAnimalCounter()
    {
        deadAnimalsCounter++;
    }

    public void addEra()
    {
        era++;
    }

    public void addSumLifeSpan(int value)
    {
        sumLifeSpan += value;
    }

    public void addObservedAnimalChild()
    {
        observedAnimalChildren++;
    }

    public void addObservedAnimalDescendant()
    {
        observedAnimalDescendants++;
    }

    public void subGrassCounter()
    {
        grassCounter--;
    }

    public void subAnimalCounter()
    {
        animalCounter--;
    }

    public void addAnimal(Animal animal)
    {
        animalList.add(animal);
    }

    public void addObservedAnimal(Animal animal)
    {
        observedAnimals.add(animal);
    }

    public void clearObservedAnimals()
    {
        for (Animal animal : observedAnimals)
        {
            animal.setObserved(false);
        }

        observedAnimals.clear();
    }

    public void clearObservedAnimalChildren() {
        this.observedAnimalChildren = 0;
    }

    public void clearObservedAnimalDescendants()
    {
        this.observedAnimalDescendants = 0;
    }
}
