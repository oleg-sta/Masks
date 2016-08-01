package ru.flightlabs.masks.totriangle;

import ru.flightlabs.masks.model.primitives.Line;
import ru.flightlabs.masks.model.primitives.Point;

public interface Triangulation {
    
    public Line[] convertToTriangle(Point[] points, Line[] lines);

}
