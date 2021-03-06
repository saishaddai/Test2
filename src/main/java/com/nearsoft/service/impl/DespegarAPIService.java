package com.nearsoft.service.impl;

import com.nearsoft.bean.Flight;
import com.nearsoft.service.APIService;
import com.nearsoft.utils.APIConnection;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class DespegarAPIService implements APIService {

    private static final int TOMORROW = 1;
    private static final int NEXT_WEEK = 2;
    private static final int NEXT_MONTH = 3;

    private static final int ROUND_TRIP = 2;

    private static Logger logger = Logger.getLogger(DespegarAPIService.class);
    @Autowired
    private APIConnection apiConnection;

    /**
     * Get flights that match with the given options
     *
     * @param otherFilters depending of the content provider it may be helpful
     * @return a list of flight objects that match with the filters
     */
    @Override
    public List<Flight> getFlights(String source, String destiny, String departureDate, String arrivingDate,
                                   int numberOfAdults, int numberOfChildren, int numberOfInfants, int type, String... otherFilters) throws ConnectException {
        //example: http://api.despegar.com/availability/flights/oneWay/HMO/LON/2014-05-25/1/0/0
        //means a flight between HMO and LON in 2014-05-25, one adult, zero children and zero infants
        String query = formatGetFlightsQuery(source, destiny, departureDate,
                arrivingDate, numberOfAdults, numberOfChildren, numberOfInfants, type);
        try {
            return processFlightsAPIResponse(apiConnection.callAPI(query, null));
        } catch (IOException e) {
            logger.error("Error parsing API response");
            throw new ConnectException("API Error");
        }
    }

    /**
     * Process the answer from API. It retrieves all the useful information in order to create Flight objects
     *
     * @param response The response of API as JSON string
     * @return a list of flight objects with information retrieved from API response
     */
    List<Flight> processFlightsAPIResponse(Object response) throws IOException {
        try {
            List<Flight> results = new ArrayList<>();
            Map<String, Object> partialResult;
            ObjectMapper mapper = new ObjectMapper();
            Map apiResponse = mapper.readValue(response.toString(), Map.class);
            if (apiResponse.get("errors") != null) {
                throw new IOException("Error processing despegar.com response");
            }
            List<Map> flights = (List<Map>) apiResponse.get("flights");//first level  of results
            for (Map flight : flights) {
                partialResult = processFlights(flight); //"partial", "outboundRoutes"
                partialResult = processOutboundRoutes((List<Map>) partialResult.get("outboundRoutes"), partialResult);
                partialResult = processSegments((List<Map<String, Map>>) partialResult.get("segments"), partialResult);
                results.add(new Flight(0L, partialResult.get("price").toString(), "one Way", partialResult.get("estimatedDate1").toString(),
                        partialResult.get("estimatedDate2").toString(), partialResult.get("companies").toString(),
                        partialResult.get("estimatedTimeTravel").toString(), partialResult.get("airports").toString(),
                        partialResult.get("stops").toString(), partialResult.get("scales").toString(), false));
                partialResult.clear();
            }
            return results;
        } catch (NullPointerException | IndexOutOfBoundsException | IOException e) {
            throw new IOException("Error processing despegar.com response");
        }
    }

    /**
     * Process the first level of response from despegar.com API
     *
     * @param flight a map of results from despegar,com containing flight information
     * @return the partial result with all the information already processed
     */
    Map<String, Object> processFlights(Map flight) {
        Map<String, Object> result = new HashMap<>();
        result.put("price", ((Map) ((Map) flight.get("priceInfo")).get("total")).get("fare"));
        result.put("outboundRoutes", flight.get("outboundRoutes"));
        return result;
    }

    /**
     * Process the second level of responses of despegar.com API called outboundRoutes
     *
     * @param outboundRoutes a list of results from despegar,com containing outbound routes
     * @param partialResult  the partial result with all the information already processed
     * @return a partial result of outbound routes
     */
    Map<String, Object> processOutboundRoutes(List<Map> outboundRoutes, Map<String, Object> partialResult) {
        for (Map route : outboundRoutes) {
            partialResult.put("estimatedTimeTravel", route.get("duration"));
            partialResult.put("segments", (List<Map<String, Map>>) route.get("segments"));
        }
        partialResult.remove("outboundRoutes");
        return partialResult;
    }

    /**
     * process the segments of a despegar.com API response
     *
     * @param segments      a list of results from despegar,com containing segments
     * @param partialResult the partial result with all the information already processed
     * @return the partial result with the new processed attributes
     */
    Map<String, Object> processSegments(List<Map<String, Map>> segments, Map<String, Object> partialResult) {
        partialResult.put("estimatedDate1", segments.get(0).get("departure").get("date"));
        partialResult.put("estimatedDate2", segments.get(0).get("departure").get("date"));
        partialResult.put("stops", segments.size() == 1 ? "non-stop" : segments.size() + " stops");
        Set<String> companies = new HashSet<>();
        Set<String> airports = new HashSet<>();
        Set<String> scales = new HashSet<>();
        for (Map segment : segments) {
            if (segment.get("marketingCarrierDescription") != null) {
                companies.add((String) ((Map) segment.get("marketingCarrierDescription")).get("description"));
            }
            if (segment.get("arrival") != null) {
                airports.add((String) ((Map) segment.get("arrival")).get("location"));
            }
            if (segment.get("departure") != null) {
                airports.add((String) ((Map) segment.get("departure")).get("location"));
            }
            if (segment.get("arrival") != null) {
                scales.add((String) ((Map) segment.get("arrival")).get("location"));
            }
        }
        partialResult.put("companies", companies);
        partialResult.put("airports", airports);
        partialResult.put("scales", scales.size() > 1 ? scales.toString() : "");
        partialResult.remove("segments");
        return partialResult;
    }


    /**
     * Formats a query for calling getFlights method in API
     *
     * @param source  the source place in IATA code fashion
     * @param destiny the destiny place in IATA code fashion
     * @param type    the type of flight (one way, round trip). Default 'one way'
     */
    String formatGetFlightsQuery(String source, String destiny, String departureDate,
                                 String arrivingDate, int adults, int children,
                                 int infants, int type) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String FLIGHTS_URL = "http://api.despegar.com/availability/flights/";
        String typeString = type == DespegarAPIService.ROUND_TRIP ? "roundTrip" : "oneWay";
        String query = FLIGHTS_URL +
                typeString + "/" + (source.isEmpty() ? "HMO" : source) + "/" + (destiny.isEmpty() ? "MEX" : destiny) + "/" +
                (departureDate == null ? sdf.format(getDate(DespegarAPIService.TOMORROW)) : departureDate) + "/";
        if (type == DespegarAPIService.ROUND_TRIP) {
            query += (arrivingDate == null ? sdf.format(getDate(DespegarAPIService.NEXT_WEEK)) : arrivingDate) + "/";
        }
        query += adults + "/" + children + "/" + infants;
        return query;
    }

    /**
     * Gets a date given the lapse
     *
     * @param lapse the lap of time to consider to create a date; 1 for a day, 2 for a week, 3 for a month
     * @return a date object with tha represents a date in the future
     */
    Date getDate(int lapse) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        switch (lapse) {
            case DespegarAPIService.TOMORROW:
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case DespegarAPIService.NEXT_WEEK:
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case DespegarAPIService.NEXT_MONTH:
                cal.add(Calendar.MONTH, 1);
                break;
        }
        return cal.getTime();
    }

}
