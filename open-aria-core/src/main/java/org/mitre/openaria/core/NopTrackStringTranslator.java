
package org.mitre.openaria.core;

import java.util.List;

import org.mitre.caasd.commons.util.Translator;

import com.google.common.collect.Lists;

public class NopTrackStringTranslator implements Translator<Track, String> {

    @Override
    public String to(Track track) {
        return track.asNop();
    }

    @Override
    public Track from(String nopTrackString) {
        return makeTrackFrom(nopTrackString);
    }

    private static Track makeTrackFrom(String string) {
        String[] lines = string.split("\n");
        List<Point> points = Lists.newArrayList();
        for (String line : lines) {
            points.add(NopPoint.from(line));
        }
        return new SimpleTrack(points);
    }
}
