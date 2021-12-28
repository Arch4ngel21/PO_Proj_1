package evo.utilities;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Random;

public class Animal extends AbstractMapElement{

    private int[] genes;
    private int numberOfChildren = 0;
    private int animalStartEnergy;
    private boolean isObserved = false;
    private ColorAdjust monochrome;
    private Blend blendedColor;
    private Color color;
    Random random = new Random();

    // Konstruktor dla tworzenia początkowych zwierząt
    public Animal (Vector2d position_, int energy_, int animalStartEnergy_, Image animalImage_, EventHandler<MouseEvent> onImageClick)
    {
        position = position_;
        orientation = MapDirection.values()[random.nextInt(MapDirection.values().length)];
        energy = energy_;
        animalStartEnergy = animalStartEnergy_;

        genes = new int[32];
        for (int i=0; i < 32; i++)
        {
            genes[i] = random.nextInt(8);
        }
        Arrays.sort(genes);

        elementImageView = new ImageView(animalImage_);
        elementImageView.setFitHeight(15);
        elementImageView.setFitWidth(15);
        elementImageView.setOnMouseClicked(onImageClick);
        createBlendedColor();
    }

    // Konstruktor dla tworzenia zwierząt przy taktyce Magicznej
    public Animal (Vector2d position_, int energy_, int animalStartEnergy_, Image animalImage_, EventHandler<MouseEvent> onImageClick, Animal animalCopy)
    {
        position = position_;
        orientation = MapDirection.values()[random.nextInt(MapDirection.values().length)];
        energy = energy_;
        animalStartEnergy = animalStartEnergy_;
        genes = animalCopy.getGenes().clone();

        elementImageView = new ImageView(animalImage_);
        elementImageView.setFitHeight(15);
        elementImageView.setFitWidth(15);
        elementImageView.setOnMouseClicked(onImageClick);
        createBlendedColor();
    }

    // Konstruktor dla tworzenia dzieci zwierząt
    public Animal(Vector2d position_, Animal parent1, Animal parent2, int elementIndex_, int animalStartEnergy_, IWorldMap map_, Image animalImage_, EventHandler<MouseEvent> onImageClick)
    {
        position = position_;
        orientation = MapDirection.values()[random.nextInt(MapDirection.values().length)];
        elementIndex = elementIndex_;
        map = map_;
        animalStartEnergy = animalStartEnergy_;

        float energyRatio = (float) parent1.energy / ((float) parent2.energy + (float) parent1.energy);

        genes = new int[32];

        // Ile genomów weźmiemy od pierwszego rodzica
        int parent1Genes = Math.round(32 * energyRatio);

        int i = 0;
        int j;

        // tablica do sprawdzania czy wzięliśmy już ten genom
        boolean[] checkList = new boolean[32];
        while (i < parent1Genes)
        {
            j = random.nextInt(32);
            while (checkList[j])
            {
                j = random.nextInt(32);
            }

            genes[i] = parent1.getGenes()[j];
            checkList[j] = true;
            i++;
        }

        checkList = new boolean[32];
        while (i < 32)
        {
            j = random.nextInt(32);
            while (checkList[j])
            {
                j = random.nextInt(32);
            }

            genes[i] = parent2.getGenes()[j];
            checkList[j] = true;
            i++;
        }
        Arrays.sort(genes);

        elementImageView = new ImageView(animalImage_);
        elementImageView.setFitHeight(15);
        elementImageView.setFitWidth(15);
        elementImageView.setOnMouseClicked(onImageClick);
        createBlendedColor();
        adjustColor();
    }

    @Override
    public String getImageName() {
        return null;
    }

    public int[] getGenes() {
        return genes;
    }

    public int getEnergy() {
        return energy;
    }

    public String toString()
    {
        return String.valueOf(elementIndex);
    }

    @Override
    public void positionChanged() {
        // TODO
    }

    public void createBlendedColor()
    {
        monochrome = new ColorAdjust();
        monochrome.setSaturation(-1.0);
        color = Color.rgb(127, 127, 0, 0.25);
        blendedColor = new Blend(BlendMode.MULTIPLY, monochrome, new ColorInput(0, 0,
                15, 15, color));

        elementImageView.setEffect(blendedColor);
        elementImageView.setCache(true);
        elementImageView.setCacheHint(CacheHint.SPEED);
    }

    /**
     * Dla energy == animalStartEnergy      => R: 127, G: 127, B: 0
     * Dla energy == 0                      => R: 255, G: 0, B: 0
     * Dla energy >= animalStartEnergy * 2  => R: 0, G: 255, B: 0
     **/
    public void adjustColor()
    {
        Platform.runLater(() -> {
            if (energy >= animalStartEnergy * 2)
            {
                color = Color.rgb(0, 255, 0, 0.25);
            }
            else if (energy < 0)
            {
                color = Color.rgb(255, 0, 0, 0.25);
            }
            else
            {
                float colorRatio = (animalStartEnergy - energy) / (float) animalStartEnergy;
                color = Color.rgb(Math.round(127*(1+colorRatio)), Math.round(127*(1-colorRatio)), 0, 0.25);
            }
            blendedColor = new Blend(BlendMode.MULTIPLY, monochrome, new ColorInput(0, 0,
                    15, 15, color));
            elementImageView.setEffect(blendedColor);
        });
    }

    public void addChildren()
    {
        numberOfChildren++;
    }

    public int getNumberOfChildren() {
        return numberOfChildren;
    }

    public boolean getObserved()
    {
        return isObserved;
    }

    public void setObserved(boolean observed) {
        isObserved = observed;
    }
}
