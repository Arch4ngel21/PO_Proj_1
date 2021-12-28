package evo.utilities;

import javafx.scene.image.ImageView;

import java.util.*;

public abstract class AbstractMap implements IWorldMap{

    protected LinkedHashMap<Vector2d, ArrayList<AbstractMapElement>> elementMap = new LinkedHashMap<>();
    protected HashSet<Vector2d> eatingPlaces;
    protected HashSet<Vector2d> copPlaces;
    protected Vector2d lowLeft;
    protected Vector2d highRight;
    protected SimulationEngine engine;
    protected Thread drawThread;

    @Override
    public boolean place(AbstractMapElement mapElement) {
        Vector2d position = mapElement.getPosition();
        if(canMoveTo(position))
        {
            if (elementMap.containsKey(position))
            {
                this.elementMap.get(position).add(mapElement);
            }
            else
            {
                this.elementMap.put(position, new ArrayList<>(List.of(mapElement)));
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isOccupied(Vector2d position) {

        return this.elementMap.containsKey(position);
    }

    // WARNING - Funkcja używana tylko podczas inicjalizacji mapy, ponieważ wtedy żadne 2 obiekty
    // na siebie nie nachodzą.
    @Override
    public AbstractMapElement elementAt(Vector2d position) {
        if (isOccupied(position))
        {
            return elementMap.get(position).get(0);
        }
        return null;
    }

    @Override
    public boolean doesContainGrass(Vector2d position) {

        ArrayList<AbstractMapElement> givenCell = elementMap.get(position);

        for (int i=0; i < givenCell.size(); i++)
        {
            if (givenCell.get(i).elementIndex == -1)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void positionChanged(Vector2d oldPosition, Vector2d newPosition, AbstractMapElement element) {

        engine.setOldPositionChange(oldPosition);
        engine.setNewPositionChange(newPosition);

        engine.setChangedElement(element);

        // Jeżeli na nowej pozycji znajduje się trawa, to zgłaszamy ją jako nową pozycję, na której odbędzie się jedzenie
        if (!newPosition.equals(new Vector2d(-1, -1)) && !oldPosition.equals(new Vector2d(-1, -1)))
        {
            if (doesContainGrass(newPosition))
            {
                System.out.println("Jedzenie na pozycji " + newPosition.toString());
                eatingPlaces.add(newPosition);
            }

            // Sprawdzenie czy 2 lub więcej zwierząt znajduje się na tym samym polu
            // (drugie zwierzę może w tej samej kolejce odejść z tego pola, ale markujemy w ten sposób potencjalne
            // miejsce, gdzie może zajść rozmnażanie).
            if (isPossibleCopPlace(newPosition))
            {
                copPlaces.add(newPosition);
            }
        }

        drawThread.interrupt();
    }

    @Override
    public void putElement(Vector2d position, AbstractMapElement mapElement) {
        if(isOccupied(position))
        {
            elementMap.get(position).add(mapElement);
        }
        else
        {
            elementMap.put(position, new ArrayList<>(List.of(mapElement)));
        }
    }

    @Override
    public void removeElement(Vector2d position, int removeElementIndex)
    {
        if (elementMap.get(position).size() == 1)
        {
            elementMap.remove(position);
        }
        else
        {
            // Pętla znajdująca element o podanym indeksie w ArrayList i usuwająca go
            for (int i=0; i < elementMap.get(position).size(); i++)
            {
                if (elementMap.get(position).get(i).elementIndex == removeElementIndex)
                {
                    elementMap.get(position).remove(i);
                    break;
                }
            }
        }
    }

    @Override
    public AbstractMapElement getElement(Vector2d position, int elementIndex) {
        if (elementMap.get(position).size() == 1)
        {
            return elementMap.get(position).get(0);
        }
        else
        {
            for (int i=0; i < elementMap.get(position).size(); i++)
            {
                if (elementMap.get(position).get(i).elementIndex == elementIndex)
                {
                    return elementMap.get(position).get(i);
                }
            }
        }

        System.out.println("Exception - no such element in map!");
        return null;
    }

    @Override
    public void eat(Vector2d position) {
        int maxEnergy = -1;
        ArrayList<Integer> maxEnergyIndexes = new ArrayList<>();

        for (int i=0; i < elementMap.get(position).size(); i++)
        {
            if (elementMap.get(position).get(i).elementIndex != -1)
            {
                if (elementMap.get(position).get(i).energy > maxEnergy)
                {
                    maxEnergy = elementMap.get(position).get(i).energy;
                    maxEnergyIndexes.clear();
                    maxEnergyIndexes.add(i);
                }

                else if (elementMap.get(position).get(i).energy == maxEnergy)
                {
                    maxEnergyIndexes.add(i);
                }
            }
        }

        if (maxEnergyIndexes.size() == 1)
        {
            elementMap.get(position).get(maxEnergyIndexes.get(0)).addEnergy(engine.grassEnergy);
            ((Animal) elementMap.get(position).get(maxEnergyIndexes.get(0))).adjustColor();
        }
        else
        {
            int dividedEnergy = engine.grassEnergy / maxEnergyIndexes.size();
            for (int i=0; i < maxEnergyIndexes.size(); i++)
            {
                elementMap.get(position).get(maxEnergyIndexes.get(i)).addEnergy(dividedEnergy);
                ((Animal) elementMap.get(position).get(maxEnergyIndexes.get(i))).adjustColor();
            }
        }
        positionChanged(position, new Vector2d(-1, -1), getElement(position, -1));
        removeElement(position, -1);
        engine.subGrassCounter();
    }

    @Override
    public void copulate(Vector2d position) {
        AbstractMapElement parent1 = null;
        AbstractMapElement parent2 = null;

        // Znalezienie 2 zwierzat o najwiekszej energii
        for (int i=0; i < elementMap.get(position).size(); i++)
        {
            if (elementMap.get(position).get(i).elementIndex != -1 && (parent2 == null || elementMap.get(position).get(i).energy >= parent1.energy))
            {
                parent2 = parent1;
                parent1 = elementMap.get(position).get(i);
            }
        }

        if (parent1.energy < engine.animalStartEnergy / 2 || parent2.energy < engine.animalStartEnergy / 2)
        {
            return;
        }

        Animal newAnimal = new Animal(position, (Animal) parent1, (Animal) parent2, engine.nextAnimalIndex, engine.animalStartEnergy, parent1.map, engine.getAnimalImage(), engine.getOnImageClick());

        newAnimal.energy = (((Animal) parent1).getEnergy() + ((Animal) parent2).getEnergy()) / 2;
        parent1.takeEnergy(((Animal) parent1).getEnergy() / 2);
        parent2.takeEnergy(((Animal) parent2).getEnergy() / 2);

        // Potrzebne do prowadzenia statystyki dzieci dla żyjących zwierząt
        ((Animal) parent1).addChildren();
        ((Animal) parent2).addChildren();

        ((Animal) parent1).adjustColor();
        ((Animal) parent2).adjustColor();

        // Dodanie dziecka / potomka dla obecnie obserwowanego zwierzęcia
        // Dziecko również będzie obserwowane, w celu uwzględnienia jego potencjalnych dzieci
        if(((Animal) parent1).getObserved() || ((Animal) parent2).getObserved())
        {
            newAnimal.setObserved(true);
            engine.addObservedAnimalDescendant();
            engine.addObservedAnimal(newAnimal);

            if (parent1.getElementIndex() == engine.getObservedAnimal().getElementIndex() || parent2.getElementIndex() == engine.getObservedAnimal().getElementIndex())
            {
                engine.addObservedAnimalChild();
            }
        }

        engine.addAnimalIndex();
        engine.addAnimalCounter();
        engine.addAnimal(newAnimal);

        putElement(position, newAnimal);
        positionChanged(new Vector2d(-1, -1), position, newAnimal);
    }

    @Override
    public void removeDeadAnimal(AbstractMapElement mapElement) {
        removeElement(mapElement.getPosition(), mapElement.elementIndex);
        positionChanged(mapElement.getPosition(), new Vector2d(-1, -1), mapElement);
        engine.subAnimalCounter();
        engine.addDeadAnimalCounter();
        engine.addSumLifeSpan(engine.getEra());
    }

    @Override
    public boolean isPossibleCopPlace(Vector2d position) {
        return (elementMap.get(position).size() >= 3 || (elementMap.get(position).size() == 2 && !doesContainGrass(position)));
    }
}
