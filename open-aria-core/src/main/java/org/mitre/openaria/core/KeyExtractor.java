package org.mitre.openaria.core;

/**
 * KeyExtractor is an important public interface that enables ARIA to process unknown streams of
 * aircraft surveillance data (eg NOP, ASDEX, ADSB, ATC simulation data etc)
 * <p>
 * KeyExtractors extract a "unique linking key" from an individual data records. These "linking
 * keys" are then used to link related data records into a single sequence.
 * <p>
 * ARIA uses a KeyExtractor to organize a continuous Stream of Point data records into a Stream of
 * tracks (where each track contains Points that describe exactly one aircraft).
 * <p>
 * For example, A KeyExtractor can help convert a {@code List<RadarHit>} where each RadarHit
 * describes a potentially different aircraft into a {@code List<RadarHit>} where the
 * entries in each inner list describe a single aircraft.
 *
 * @param <P> The Point class
 */
@FunctionalInterface
public interface KeyExtractor<P> {

    /**
     * @param point A single Point record (e.g. a NOP Radar Hit, ASDEX Point, etc.)
     *
     * @return A "unique linking key" that will be used to link individual Points into Tracks. These
     *     linking keys should be unique for the duration of the output Tracks existence.
     */
    String joinKeyFor(P point);
}
