package org.mtransit.parser.ca_west_coast_express_bus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
// http://ns.translink.ca/gtfs/notifications.zip
// http://ns.translink.ca/gtfs/google_transit.zip
// http://gtfs.translink.ca/static/latest
// SERVICE REPLACED BY VANCOUVER TRANSLINK BUS 701
public class WestCoastExpressBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-west-coast-express-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WestCoastExpressBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating West Coast Express bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating West Coast Express bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String RSN_701 = "701";

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!RSN_701.equals(gRoute.getRouteShortName())) {
			return true; // exclude
		}
		return false; // keep
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return CleanUtils.cleanLabel(gRoute.getRouteLongName().toLowerCase(Locale.ENGLISH));
	}

	private static final String AGENCY_COLOR_VIOLET = "711E8C"; // VIOLET (from PDF map)

	private static final String AGENCY_COLOR = AGENCY_COLOR_VIOLET;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		return null; // use agency color
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final Pattern STARTS_WITH_QUOTE = Pattern.compile("(^\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_QUOTE = Pattern.compile("(\"[;]?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern WCE_LINE_TO = Pattern.compile("(west coast express)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_ROUTE = Pattern.compile("(^[A-Z]{0,1}[0-9]{1,3}[\\s]+{1})", Pattern.CASE_INSENSITIVE);

	private static final String STATION_SHORT = "Sta"; // see @CleanUtils
	private static final Pattern STATION = Pattern.compile("((^|\\W){1}(stn|sta|station)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = "$2" + STATION_SHORT + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = STARTS_WITH_ROUTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = STARTS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = WCE_LINE_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(STATION_REPLACEMENT);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 701L) {
			if (Arrays.asList( //
					"Haney Pl", // <>
					"Maple Rdg E", //
					"Maple Rdg E Via Samuel Robertson", //
					"Maple Rdg East", //
					"Mission City Sta" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Mission City Sta", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Haney Pl", // <>
					"Coq Ctrl Sta" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Coq Ctrl Sta", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected tripts to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern STATION_STN = Pattern.compile("(station|stn|sta)", Pattern.CASE_INSENSITIVE);

	private static final Pattern UNLOADING = Pattern.compile("(unload(ing)?( only)?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern TRAIN_BUS = Pattern.compile("(trainbus)", Pattern.CASE_INSENSITIVE);

	private static final Pattern BOUNDS = Pattern.compile("((^|\\W){1}(eb|eastbound|wb|westbound|sb|southbound|nb|northbound)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	public static final String BOUND_REPLACEMENT = "$2" + "$4";

	private static final Pattern AT_LIKE = Pattern.compile("((^|\\W){1}(fs|ns)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = BOUNDS.matcher(gStopName).replaceAll(BOUND_REPLACEMENT);
		gStopName = CleanUtils.SAINT.matcher(gStopName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		gStopName = AT_LIKE.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = STATION_STN.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = UNLOADING.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = WCE_LINE_TO.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = TRAIN_BUS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		if (!StringUtils.isEmpty(gStop.getStopCode()) && Utils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // using stop code as stop ID
		}
		return 1_000_000 + Integer.parseInt(gStop.getStopId());
	}
}
