package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
	
	public void calculateFare(Ticket ticket) {
		calculateFare(ticket, false);
	}

	// function to calculate the fare of a ticket based on the parking type and the duration of parking and if the user is a recurring user
    public void calculateFare(Ticket ticket, boolean isRecurringUser) {
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        
        //Check if the user is a recurring user and apply the discount if necessary
        double discount = 1.0;
        if (isRecurringUser) {
			discount = 1.0 - Fare.DISCOUNT;
        }

        //Convert the time to milliseconds
        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        float duration = (outHour - inHour);
        //Convert the time to hours
        duration = duration / (1000 * 60 * 60);
        
        if (duration > 0.5) {
        	switch (ticket.getParkingSpot().getParkingType()){
	            case CAR: {
	                ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR * discount);
	                break;
	            }
	            case BIKE: {
	                ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR * discount);
	                break;
	            }
	            default: throw new IllegalArgumentException("Unkown Parking Type");
        	}
		} else {
			ticket.setPrice(0);
		}
    }
}