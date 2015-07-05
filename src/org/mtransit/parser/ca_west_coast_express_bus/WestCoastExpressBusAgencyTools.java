package org.mtransit.parser.ca_west_coast_express_bus;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
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
		System.out.printf("Generating West Coast Express bus data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating West Coast Express bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!RSN_WCE.equals(gRoute.route_short_name)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	private static final String TRAINBUS_THS_LC = "trainbus";

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (!gTrip.trip_headsign.toLowerCase(Locale.ENGLISH).contains(TRAINBUS_THS_LC)) {
			return true; // TrainBus is a bus, not a train
		}
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
		return Long.parseLong(gRoute.route_short_name); // use route short name as route ID
	}

	private static final String RSN_WCE = "997";
	private static final String WCE_SHORT_NAME = "WCE";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (RSN_WCE.equals(gRoute.route_short_name)) {
			return WCE_SHORT_NAME;
		}
		System.out.println("Unexpected route short name " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final String TRAIN_BUS_LONG_NAME = "West Coast Express TrainBus";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return TRAIN_BUS_LONG_NAME;
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

	private static final long RID_WCE = 997l;

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == RID_WCE) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		}
		System.out.printf("Unexpected trip (unexpected route ID: %s): %s\n", mRoute.id, gTrip);
		System.exit(-1);
	}

	private static final Pattern STARTS_WITH_QUOTE = Pattern.compile("(^\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_QUOTE = Pattern.compile("(\"[;]?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern WCE_LINE_TO = Pattern.compile("(west coast express)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = STARTS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = WCE_LINE_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_BOUND = Pattern.compile("((east|west|north|south)bound$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STATION_STN = Pattern.compile("(station|stn)", Pattern.CASE_INSENSITIVE);

	private static final Pattern UNLOADING = Pattern.compile("(unload(ing)?( only)?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern TRAIN_BUS = Pattern.compile("(trainbus)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = STATION_STN.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = UNLOADING.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = WCE_LINE_TO.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = ENDS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = TRAIN_BUS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = MSpec.cleanStreetTypes(gStopName);
		return MSpec.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		if (!StringUtils.isEmpty(gStop.stop_code) && Utils.isDigitsOnly(gStop.stop_code)) {
			return Integer.parseInt(gStop.stop_code); // using stop code as stop ID
		}
		return 1000000 + Integer.parseInt(gStop.stop_id);
	}
}
