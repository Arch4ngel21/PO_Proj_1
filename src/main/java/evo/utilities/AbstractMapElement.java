package evo.utilities;

import javafx.scene.image.ImageView;

public abstract class AbstractMapElement implements IMapElement{

    protected Vector2d position;
    protected MapDirection orientation;
    protected int elementIndex;
    protected int energy;
    protected IWorldMap map;
    protected ImageView elementImageView;

    public Vector2d getPosition()
    {
        return position;
    }

    public MapDirection getOrientation()
    {
        return orientation;
    }

    public ImageView getElementImageView()
    {
        return elementImageView;
    }

    public void setMap(IWorldMap map_)
    {
        map = map_;
    }

    public void addEnergy(int value){
        energy += value;
    }

    public void takeEnergy(int value){
        energy -= value;
    }

    public int getElementIndex() {
        return elementIndex;
    }
}
