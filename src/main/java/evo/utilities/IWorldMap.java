package evo.utilities;

/**
 * The interface responsible for storing information about the map and handling
 * events that occur during process of animal evolution
 *
 **/
public interface IWorldMap {

    boolean canMoveTo(Vector2d position);

    boolean place(AbstractMapElement mapElement);

    boolean isOccupied(Vector2d position);

    boolean doesContainGrass(Vector2d position);

    boolean isPossibleCopPlace(Vector2d position);

    void move(Animal animal);

    void eat(Vector2d position);

    void copulate(Vector2d position);

    void removeDeadAnimal(AbstractMapElement mapElement);

    void positionChanged(Vector2d oldPosition, Vector2d newPosition, AbstractMapElement element);

    void putElement(Vector2d position, AbstractMapElement mapElement);

    void removeElement(Vector2d position, int elementIndex);

    void growNewGrass();

    AbstractMapElement getElement(Vector2d position, int elementIndex);

    AbstractMapElement elementAt(Vector2d position);

}
