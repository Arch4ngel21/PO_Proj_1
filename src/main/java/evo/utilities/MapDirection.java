package evo.utilities;

public enum MapDirection {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST;

    public String toString()
    {
        return switch (this)
                {
                    case NORTH -> "Polnoc";
                    case NORTHEAST -> "Polnoc-Wschod";
                    case SOUTH -> "Poludnie";
                    case SOUTHEAST -> "Poludnie-Wschod";
                    case WEST -> "Zachod";
                    case SOUTHWEST -> "Poludnie-Zachod";
                    case EAST -> "Wschod";
                    case NORTHWEST -> "Polnoc-Zachod";
                };
    }

    public MapDirection next()
    {
        return switch (this) {
            case NORTH -> NORTHEAST;
            case NORTHEAST -> EAST;
            case EAST -> SOUTHEAST;
            case SOUTHEAST -> SOUTH;
            case SOUTH -> SOUTHWEST;
            case SOUTHWEST -> WEST;
            case WEST -> NORTHWEST;
            case NORTHWEST -> NORTH;
        };
    }

    public MapDirection previous()
    {
        return switch (this) {
            case NORTH -> NORTHWEST;
            case NORTHEAST -> NORTH;
            case EAST -> NORTHEAST;
            case SOUTHEAST -> EAST;
            case SOUTH -> SOUTHEAST;
            case SOUTHWEST -> SOUTH;
            case WEST -> SOUTHWEST;
            case NORTHWEST -> WEST;
        };
    }

    public Vector2d toUnitVector()
    {
        return switch (this) {
            case NORTH -> new Vector2d(0, 1);
            case NORTHEAST -> new Vector2d(1, 1);
            case SOUTH -> new Vector2d(0, -1);
            case SOUTHEAST -> new Vector2d(1, -1);
            case WEST -> new Vector2d(-1, 0);
            case SOUTHWEST -> new Vector2d(-1, -1);
            case EAST -> new Vector2d(1, 0);
            case NORTHWEST -> new Vector2d(-1, 1);
        };
    }

    public MapDirection rotate(MapDirection orientation, int rotations)
    {
        MapDirection newOrientation = orientation;

        // Jeżeli > 4, to obracamy w drugą stronę
        if (rotations > 4)
        {
            rotations -= 8;
        }

        if (rotations > 0)
        {
            for (int i=0; i < rotations; i++)
            {
                newOrientation = newOrientation.next();
            }
        }

        if (rotations < 0)
        {
            for (int i=0; i < -rotations; i++)
            {
                newOrientation = newOrientation.previous();
            }
        }

        return newOrientation;
    }
}
