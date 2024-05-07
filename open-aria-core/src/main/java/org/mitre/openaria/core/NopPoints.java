
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;

import org.mitre.openaria.core.formats.nop.AgwRadarHit;
import org.mitre.openaria.core.formats.nop.CenterRadarHit;
import org.mitre.openaria.core.formats.nop.MeartsRadarHit;
import org.mitre.openaria.core.formats.nop.StarsRadarHit;


public class NopPoints {

    public static class AgwPoint extends NopPoint<AgwRadarHit> {

        public AgwPoint(AgwRadarHit rhMessage) {
            super(rhMessage);
        }

        public AgwPoint(String agwRhMessage) {
            super(agwRhMessage);

            checkArgument(
                this.rhMessage instanceof AgwRadarHit,
                "Cannot create a AgwPoint from a " + rhMessage.getClass().getSimpleName()
            );
        }

        @Override
        public String trackId() {
            return rawMessage().trackNumber();
        }

        @Override
        public String beaconAssigned() {

            return (rawMessage().assignedBeaconCode() == null)
                ? null
                /*
                 * when converting the Integer to a String be sure to intern the resulting String so
                 * that you don't generate hundreds of separate copies of the beacon code
                 */
                : rawMessage().assignedBeaconCode().toString().intern();
        }
    }

    public static class StarsPoint extends NopPoint<StarsRadarHit> {

        public StarsPoint(StarsRadarHit rhMessage) {
            super(rhMessage);
        }

        public StarsPoint(String starsRhMessage) {
            super(starsRhMessage);

            checkArgument(
                this.rhMessage instanceof StarsRadarHit,
                "Cannot create a StarsPoint from a " + rhMessage.getClass().getSimpleName()
            );
        }

        @Override
        public String trackId() {
            return rawMessage().trackNumber();
        }

        @Override
        public String beaconAssigned() {
            return (rawMessage().assignedBeaconCode() == null)
                ? null
                /*
                 * when converting the Integer to a String be sure to intern the resulting String so
                 * that you don't generate hundreds of separate copies of the beacon code
                 */
                : rawMessage().assignedBeaconCode().toString().intern();
        }
    }

    public static class CenterPoint extends NopPoint<CenterRadarHit> {

        public CenterPoint(CenterRadarHit rhMessage) {
            super(rhMessage);
        }

        public CenterPoint(String centerRhMessage) {
            super(centerRhMessage);

            checkArgument(
                this.rhMessage instanceof CenterRadarHit,
                "Cannot create a CenterPoint from a " + rhMessage.getClass().getSimpleName()
            );
        }

        @Override
        public String trackId() {
            return rawMessage().computerId();
        }

        @Override
        public String beaconAssigned() {
            //these Center format does not contain this information
            return null;
        }
    }

    public static class MeartsPoint extends NopPoint<MeartsRadarHit> {

        public MeartsPoint(MeartsRadarHit rhMessage) {
            super(rhMessage);
        }

        public MeartsPoint(String meartsRhMessage) {
            super(meartsRhMessage);

            checkArgument(
                this.rhMessage instanceof MeartsRadarHit,
                "Cannot create a MeartsPoint from a " + rhMessage.getClass().getSimpleName()
            );
        }

        @Override
        public String trackId() {
            //in somewhat rare cases this will return null
            return rawMessage().computerId();
        }

        @Override
        public String beaconAssigned() {
            //these Mearts format does not contain this information
            return null;
        }
    }
}
