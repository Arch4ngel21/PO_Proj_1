package evo.utilities;

import javafx.scene.image.ImageView;

import java.util.HashSet;
import java.util.Random;

public class SteppeMap extends AbstractMap{

    protected Vector2d jungleLowLeft;
    protected Vector2d jungleHighRight;

    public SteppeMap(Vector2d highRight_, Vector2d jungleLowLeft_, Vector2d jungleHighRight_, Thread drawThread_, SimulationEngine engine_)
    {
        lowLeft = new Vector2d(0, 0);
        highRight = highRight_;
        jungleLowLeft = jungleLowLeft_;
        jungleHighRight = jungleHighRight_;
        drawThread = drawThread_;
        engine = engine_;
        eatingPlaces = new HashSet<>();
        copPlaces = new HashSet<>();
    }

    @Override
    public boolean canMoveTo(Vector2d position) {

        // return !isOccupied(position) || elementAt(position) instanceof Grass;
        return position.x < jungleLowLeft.x || position.x > jungleHighRight.x || position.y < jungleLowLeft.y || position.y > jungleHighRight.y;
    }

    @Override
    public void move(Animal animal) {
        Random random = new Random();

        int next_move = animal.getGenes()[random.nextInt(32)];

        if (next_move == 0 || next_move == 4)
        {
            Vector2d oldPosition = animal.getPosition();
            Vector2d newPosition;

            if (next_move == 0)
            {
                newPosition = oldPosition.add(animal.getOrientation().toUnitVector());
            }
            else
            {
                newPosition = oldPosition.subtract(animal.getOrientation().toUnitVector());
            }

            AbstractMapElement temp;
            newPosition = getPositionInsideMap(newPosition);

            if (canMoveTo(newPosition))
            {
                temp = getElement(oldPosition, animal.elementIndex);
                removeElement(oldPosition, temp.elementIndex);


                temp.position = newPosition;
                putElement(newPosition, temp);

                positionChanged(oldPosition, newPosition, temp);
            }
        }
        else
        {
            animal.orientation = animal.getOrientation().rotate(animal.getOrientation(), next_move);
        }
    }

    @Override
    public void growNewGrass() {
        Random random = new Random();
        int new_x = random.nextInt(highRight.x);
        int new_y = random.nextInt(highRight.y);
        Vector2d newPosition = new Vector2d(new_x, new_y);

        while (isOccupied(newPosition) || (jungleLowLeft.x <= new_x && new_x <= jungleHighRight.x && jungleLowLeft.y <= new_y && new_y <= jungleHighRight.y))
        {
            new_x = random.nextInt(highRight.x);
            new_y = random.nextInt(highRight.y);
            newPosition = new Vector2d(new_x, new_y);
        }

        Grass newGrass = new Grass(newPosition, engine.getGrassImage());
        putElement(newPosition, newGrass);
        positionChanged(new Vector2d(-1 ,-1), newPosition, newGrass);
        engine.addGrassCounter();
    }

    public Vector2d getPositionInsideMap(Vector2d position)
    {
        if (position.x >= highRight.x || position.y >= highRight.y)
        {
            return new Vector2d(position.x % highRight.x, position.y % highRight.y);
        }
        else if (position.x < 0)
        {
            return new Vector2d(position.x + highRight.x, position.y);
        }
        else if (position.y < 0)
        {
            return new Vector2d(position.x, position.y + highRight.y);
        }
        return position;
    }
}
