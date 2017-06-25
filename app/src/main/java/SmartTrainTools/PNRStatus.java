package SmartTrainTools;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import commons.Config;

/**
 * Created by root on 10/6/17.
 */

public class PNRStatus implements Serializable {
    private String PNR;
    private boolean chartPrepared;
    private TravelClass travelClass;
    private MyDate dateOfJourney;
    private Station boardingPoint, from, to, reservationUpto;
    private String trainNo, trainName;
    private ArrayList<Passenger> passengers;

    public PNRStatus(String PNR) throws IOException, ParseException {
        this.PNR = PNR;
        Document document = Jsoup.connect("https://www.api.railrider.in/ajax_pnr_check.php")
                .data("pnr_post", PNR)
                .timeout(30000)
                .post();
        JSONParser parser = new JSONParser();
        JSONObject pnrStatus = (JSONObject) parser.parse(document.body().text());
        System.out.println(document.body().text());

        this.chartPrepared = !(pnrStatus.get("chart_prepared").equals("N"));
        this.travelClass = new TravelClass(pnrStatus.get("class1").toString());
        String dateOfJourney = pnrStatus.get("doj").toString();
        try {
            this.dateOfJourney = MyDate.parseMyDate(dateOfJourney, "dd-MM-yyyy");
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            this.dateOfJourney = new MyDate(1, 1, 1970);
        }
        this.boardingPoint = parseStation((JSONObject) pnrStatus.get("boarding_point"));
        this.from = parseStation((JSONObject) pnrStatus.get("from_station"));
        this.to = parseStation((JSONObject) pnrStatus.get("to_station"));
        this.reservationUpto = parseStation((JSONObject) pnrStatus.get("reservation_upto"));
        this.trainNo = pnrStatus.get("train_num").toString();
        this.trainName = Config.rc.getTrainName(trainNo);
        this.passengers = new ArrayList<>(6);
        for (Object passenger : (JSONArray) pnrStatus.get("passengers")) {
            passengers.add(this.parsePassenger((JSONObject) passenger));
        }

    }

    public String getPNR() {
        return PNR;
    }

    public boolean isChartPrepared() {
        return chartPrepared;
    }

    public TravelClass getTravelClass() {
        return travelClass;
    }

    public MyDate getDateOfJourney() {
        return dateOfJourney;
    }

    public Station getBoardingPoint() {
        return boardingPoint;
    }

    public Station getFrom() {
        return from;
    }

    public Station getTo() {
        return to;
    }

    public Station getReservationUpto() {
        return reservationUpto;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public String getTrainName() {
        return trainName;
    }

    public ArrayList<Passenger> getPassengers() {
        return passengers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PNRStatus pnrStatus = (PNRStatus) o;

        return PNR != null ? PNR.equals(pnrStatus.PNR) : pnrStatus.PNR == null;

    }

    @Override
    public int hashCode() {
        return PNR != null ? PNR.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PNRStatus{" +
                "PNR='" + PNR + '\'' +
                ", travelClass=" + travelClass +
                ", dateOfJourney=" + dateOfJourney +
                ", from=" + from +
                ", to=" + to +
                ", trainNo='" + trainNo + '\'' +
                '}';
    }

    private Station parseStation(JSONObject station) {
        return new Station(station.get("code").toString(), station.get("name").toString());
    }

    public Passenger parsePassenger(JSONObject passenger) {
        return new Passenger(passenger.get("booking_status").toString(), passenger.get("current_status").toString());
    }

    public class Passenger implements Serializable {
        private String bookingStatus, currentStatus;

        public Passenger(String bookingStatus, String currentStatus) {
            this.bookingStatus = bookingStatus;
            this.currentStatus = currentStatus;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public String getBookingStatus() {
            return bookingStatus;
        }
    }
}
