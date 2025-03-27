package org.mitre.openaria.core.formats.swim;

import static java.util.Optional.ofNullable;
import static org.mitre.openaria.core.formats.swim.SwimTaisPointFlightplanFuser.arrivalAirport;
import static org.mitre.openaria.core.formats.swim.SwimTaisPointFlightplanFuser.departureAirport;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.Pair;
import org.mitre.swim.jaxb.tais.r4.*;
import org.mitre.swim.parse.JaxbSwimMessage;

/**
 * Supports the R 4.0 schemas.
 */
class SwimTaisR40PointParser implements Function<JaxbSwimMessage, List<TaisPoint>> {

  /**
   * resolution of xpos, ypos int value
   */
  static final double POSITION_RESOLUTION = 1 / 256.0;
  /**
   * Functional class for looking up a {@link Tracon} based on the source of the incoming SWIM message.
   * <br>
   * This tracon is used to convert the X/Y positions reported by TAIS for track locations to Latitude/Longitude.
   */
  private static final SwimTaisTraconResolver traconResolver = new SwimTaisTraconResolver();
  private static final SwimTaisPointFlightplanFuser pointFlightplanFuser = new SwimTaisPointFlightplanFuser();

  @Override
  public List<TaisPoint> apply(JaxbSwimMessage jaxbSwimMessage) {
    Object messageObject = jaxbSwimMessage.getObject();

    if (messageObject instanceof TATrackAndFlightPlan) {
      TATrackAndFlightPlan taisMessage = (TATrackAndFlightPlan) messageObject;

      Optional<Tracon> tracon = traconResolver.apply(taisMessage.getSrc());

      return tracon
          .map(t -> taisMessage.getRecord().stream()
              .filter(record -> record.getTrack() != null)
              .filter(record -> record.getTrack().getMrtTime() != null)
              .map(record -> parseMessage(t, record).build())
              .collect(Collectors.toList()))
          .orElseGet(Collections::emptyList);
    }

    return Collections.emptyList();
  }

  /**
   * Parse an individual composite track/flightplan message from TAIS.
   */
  private TaisPoint.Builder parseMessage(Tracon tracon, TrackAndFlightPlanRecordType trackAndFlightplanMessage) {

    TrackRecordType trackMessage = trackAndFlightplanMessage.getTrack();

    long mrtTime = trackMessage.getMrtTime().toGregorianCalendar().getTimeInMillis();

    double x = trackMessage.getXPos() * POSITION_RESOLUTION;
    double y = trackMessage.getYPos() * POSITION_RESOLUTION;

//    Pair<Double, Double> latLon = StarsProjection.from(tracon, mrtTime).systemCoordinates(x, y);

    TaisPoint.Builder builder = new TaisPoint.Builder()
        .tracon(tracon.name())
        .trackNum(trackMessage.getTrackNum())
        .time(mrtTime)
        .eramGufi(ofNullable(trackAndFlightplanMessage.getEnhancedData()).map(EnhancedData::getEramGufi).orElse(null))
        .sfdpsGufi(ofNullable(trackAndFlightplanMessage.getEnhancedData()).map(EnhancedData::getSfdpsGufi).orElse(null))
//        .latitude(ofNullable(trackAndFlightplanMessage.getTrack()).map(TrackRecordType::getLat).orElse(latLon.first()))
//        .longitude(ofNullable(trackAndFlightplanMessage.getTrack()).map(TrackRecordType::getLon).orElse(latLon.second()))
        .latitude(ofNullable(trackAndFlightplanMessage.getTrack()).map(TrackRecordType::getLat).orElseThrow())
        .longitude(ofNullable(trackAndFlightplanMessage.getTrack()).map(TrackRecordType::getLon).orElseThrow())
        .xpos(x)
        .ypos(y)
        .acAddress(trackMessage.getAcAddress())
        .adsb(trackMessage.getAdsb())
        .frozen(trackMessage.getFrozen())
        .newProp(trackMessage.getNew())
        .pseudo(trackMessage.getPseudo())
        .reportedAltitude(integerToDouble(trackMessage.getReportedAltitude()))
        .reportedBeaconCode(trackMessage.getReportedBeaconCode())
        .status(ofNullable(trackMessage.getStatus()).map(TrackStatusType::name).orElse(null))
        .trackNum(trackMessage.getTrackNum())
        .vvert(integerToDouble(trackMessage.getVVert()))
        .vx(integerToDouble(trackMessage.getVx()))
        .vy(integerToDouble(trackMessage.getVy()));

    return ofNullable(trackAndFlightplanMessage.getFlightPlan())
        .map(this::parseFlightplan)
        .map(f -> pointFlightplanFuser.apply(builder, f)
            // Use the provided enhanced data versions of these fields if populated - otherwise resolve it from the flight plan entry/exit fixes as normal
            .arrAirport(ofNullable(trackAndFlightplanMessage.getEnhancedData()).map(EnhancedData::getDestinationAirport).orElse(arrivalAirport(f)))
            .depAirport(ofNullable(trackAndFlightplanMessage.getEnhancedData()).map(EnhancedData::getDepartureAirport).orElse(departureAirport(f))))
        .orElse(builder);
  }

  /**
   * Parse an individual composite track/flightplan message from TAIS.
   */
  private TaisFlightplan parseFlightplan(FlightPlanRecordType flightplan) {

    Pair<String, String> keyboardAndPosition = ofNullable(flightplan.getCps())
        .filter(s -> s.length() == 2)
        .map(s -> Pair.of(s.substring(0, 1), s.substring(1, 2)))
        .orElse(Pair.of(null, null));

    return new TaisFlightplan.Builder()
        .acid(flightplan.getAcid())
        .acType(flightplan.getAcType())
        .assignedBeaconCode(flightplan.getAssignedBeaconCode())
        .airport(flightplan.getAirport())
        .category(flightplan.getCategory())
        .dbi(flightplan.getDbi())
        .ecid(flightplan.getECID())
        .entryFix(flightplan.getEntryFix())
        .exitFix(flightplan.getExitFix())
        .flightRules(Optional.of(flightplan.getFlightRules()).map(FlightRulesType::name).orElse(null))
        .lld(ofNullable(flightplan.getLld()).map(LeaderLineDirectionType::name).orElse(null))
        .ocr(ofNullable(flightplan.getOcr()).map(OwnershipChangeReasonType::name).orElse(null))
        .ptdTime(flightplan.getPtdTime())
        .requestedAltitude(ofNullable(flightplan.getRequestedAltitude()).map(Integer::longValue).orElse(null))
        .rnav(flightplan.getRnav())
        .runway(flightplan.getRunway())
        .scratchPad1(flightplan.getScratchPad1())
        .scratchPad2(flightplan.getScratchPad2())
        .sfpn(flightplan.getSfpn())
        .status(ofNullable(flightplan.getStatus()).map(FlightPlanStatusType::name).orElse(null))
        .suspended(flightplan.getSuspended())
        .type(flightplan.getType().toString())
        .keyboard(keyboardAndPosition.first())
        .positionSymbol(keyboardAndPosition.second())
        .build();
  }

  private Double integerToDouble(Integer i) {
    return ofNullable(i).map(Integer::doubleValue).orElse(null);
  }
}