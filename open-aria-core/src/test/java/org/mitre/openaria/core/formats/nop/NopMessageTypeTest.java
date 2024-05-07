package org.mitre.openaria.core.formats.nop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.openaria.core.formats.nop.NopMessageType.isNopRadarHit;

import org.junit.jupiter.api.Test;

public class NopMessageTypeTest {

    public static final String STARS_1 = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-083.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
    public static final String STARS_2 = "[RH],STARS,BIL,10/18/2016,00:06:18.097,,,,1200,000,000,xxx,047.00894,-109.34417,2640,0000,-32.9031,72.0203,,,,BIL,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";

    public static final String CENTER_1 = "[RH],Center,ZLA_B,07-10-2016,06:16:23.000,AAL350,B738,L,7305,176,381,319,34.0453,-119.1269,350,,,,,ZLA/15,,ZLA_B,,,,E0613,SFO,,IFR,,350,1396392188,LAX,0704,176//280,,L,1,,,{RH}";
    public static final String CENTER_2 = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";

    public static final String MEARTS_1 = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,257,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";
    public static final String MEARTS_2 = "[RH],MEARTS,ZUA_B,11-05-2019,15:30:02.020,,,,2000,390,455,142,016.1706,149.0943,,,499.43,417.5876,,/,,ZUA_B,,,,,,,,,,,,,390//,,,1,{RH}";

    public static final String AGW_1 = "[RH],AGW,ABI_B,07/12/2016,19:21:08.848,N832AT,PA44,,5136,101,144,251,032.62683,-099.43983,088,5136,9.69,15.09,1,B,0,ABI,MAF,MWL,BGS,,MAF,,IFR,,39,39,TKI,,00,,S,0,,0,,94.59,96.59,{RH}";
    public static final String AGW_2 = "[RH],AGW,ABI_B,07/12/2016,19:21:19.384,N2233W,PA28,,6276,066,96,266,032.31720,-098.82792,209,6276,42.77,0.59,1,B,2,ABI,L,MWL,ABIA,,ABI,,IFR,,188,69,JEN276015,,00,,S,0,V,0,,125.69,78.2,{RH}";

    @Test
    public void isNopRadarHit_acceptsAllKnownFormats() {
        assertThat(isNopRadarHit(STARS_1), is(true));
        assertThat(isNopRadarHit(STARS_2), is(true));
        assertThat(isNopRadarHit(CENTER_1), is(true));
        assertThat(isNopRadarHit(CENTER_2), is(true));
        assertThat(isNopRadarHit(MEARTS_1), is(true));
        assertThat(isNopRadarHit(MEARTS_2), is(true));
        assertThat(isNopRadarHit(AGW_1), is(true));
        assertThat(isNopRadarHit(AGW_2), is(true));
    }
}
