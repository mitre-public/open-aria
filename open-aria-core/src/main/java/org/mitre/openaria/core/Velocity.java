package org.mitre.openaria.core;

import static java.util.Objects.requireNonNull;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Speed;

public record Velocity(Speed speed, Course course) {

    public Velocity {
        requireNonNull(speed);
        requireNonNull(course);
    }

}
