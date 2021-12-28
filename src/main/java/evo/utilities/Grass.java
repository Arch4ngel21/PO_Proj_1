package evo.utilities;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Grass extends AbstractMapElement{


    public Grass(Vector2d pos, Image grassImage_)
    {
        this.position = pos;
        elementIndex = -1;
        elementImageView = new ImageView(grassImage_);
        elementImageView.setFitHeight(15);
        elementImageView.setFitWidth(15);
    }

    public String toString()
    {
        return "*";
    }

    @Override
    public String getImageName() {
        return "src/main/resources/Grass.png";
    }

    @Override
    public void positionChanged() {}

}
