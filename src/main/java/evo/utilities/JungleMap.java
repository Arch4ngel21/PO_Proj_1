package evo.utilities;

import javafx.scene.image.ImageView;

import java.util.HashSet;
import java.util.Random;

public class JungleMap extends AbstractMap {

    public JungleMap(Vector2d lowLeft_, Vector2d highRight_, Thread drawThread_, SimulationEngine engine_)
    {
        lowLeft = lowLeft_;
        highRight = highRight_;
        drawThread = drawThread_;
        engine = engine_;
        eatingPlaces = new HashSet<>();
        copPlaces = new HashSet<>();
    }

    @Override
    public boolean canMoveTo(Vector2d position) {
        // return !isOccupied(position) || elementAt(position) instanceof Grass;
        return lowLeft.x <= position.x && position.x <= highRight.x && lowLeft.y <= position.y && position.y <= highRight.y;
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
        if (elementMap.size() == (highRight.x - lowLeft.x)*(highRight.y- lowLeft.y))
        {
            return;
        }

        Random random = new Random();
        int new_x = random.nextInt(highRight.x+1);
        int new_y = random.nextInt(highRight.y+1);
        Vector2d newPosition = new Vector2d(new_x, new_y);

        while (isOccupied(newPosition) || (new_x < lowLeft.x || new_y < lowLeft.y))
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
}
