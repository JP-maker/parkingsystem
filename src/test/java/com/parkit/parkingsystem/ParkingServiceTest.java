package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private static ParkingSpot parkingSpot;

    @BeforeEach
    public void setUpPerTest() {
        try {
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

            System.setOut(new PrintStream(outContent));
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void processExitingVehicleTest() {
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(Ticket.class));
    }

    @Test
    public void testProcessIncomingVehicle(){
    	when(inputReaderUtil.readSelection()).thenReturn(1); // 1 = CAR
    	when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        
		parkingService.processIncomingVehicle();
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
		verify(ticketDAO, times(1)).getNbTicket(any(Ticket.class));
	}
    
    @Test
	public void processExitingVehicleTestUnableUpdate() {
    	parkingService.processExitingVehicle();
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        assertFalse(ticketDAO.updateTicket(any(Ticket.class)));
	}
    
    @Test
    public void testGetNextParkingNumberIfAvailable () {
        when(inputReaderUtil.readSelection()).thenReturn(1);
    	when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
		parkingSpot = parkingService.getNextParkingNumberIfAvailable();
		assertNotNull(parkingSpotDAO); 
        assertEquals(1, parkingSpot.getId()); 
        assertTrue(parkingSpot.isAvailable());
	}
	
	@Test
	public void testGetNextParkingNumberIfAvailableParkingNumberNotFound () {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
        assertNull(parkingService.getNextParkingNumberIfAvailable());
	}
	
	@Test
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument () {
		when(inputReaderUtil.readSelection()).thenReturn(3);
		parkingSpot = parkingService.getNextParkingNumberIfAvailable();
		assertNull(parkingSpot, "La méthode doit retourner null en cas d'erreur utilisateur");
		verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class));
	}
	
	@Test
    public void testProcessIncomingVehicleShouldApplyDiscountForReturningUser() {
		when(inputReaderUtil.readSelection()).thenReturn(1); // 1 = CAR
    	when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(ticketDAO.getNbTicket(any(Ticket.class))).thenReturn(1);
        //doNothing().when(ticketDAO).saveTicket(any(Ticket.class));

        parkingService.processIncomingVehicle();

        // Assert
        String consoleOutput = outContent.toString();
        int discountInPercent = (int) (Fare.DISCOUNT * 100);
        String expectedMessage = "Welcome back! As a recurring user of our parking lot, you'll benefit from a " + discountInPercent + "% discount.";

        assertTrue(consoleOutput.contains(expectedMessage), "Le message de discount n'a pas été affiché !");
    }
}
