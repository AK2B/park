package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();
        
        //TODO: Some tests are failing here. Need to check if this logic is correct
        // Calculate duration in minutes
        double duration = (double) (outTime - inTime) / (1000 * 60);

        // Apply free parking for less than 30 minutes
        if (duration < 30.0) {
            ticket.setPrice(0.0);
            return;
        }

        // Calculate duration in hours
        duration /= 60.0;

        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                double hourlyRate = Fare.CAR_RATE_PER_HOUR;
                if (discount) {
                    hourlyRate *= 0.95;
                }
                double price = duration * hourlyRate * 1.0;
                ticket.setPrice(price);
                break;
            }
            case BIKE: {
                double hourlyRate = Fare.BIKE_RATE_PER_HOUR;
                if (discount) {
                    hourlyRate *= 0.95;
                }
                double price = duration * hourlyRate * 1.0;
                ticket.setPrice(price);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}