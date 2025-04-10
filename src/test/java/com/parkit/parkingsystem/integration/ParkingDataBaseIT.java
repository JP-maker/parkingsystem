package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        parkingService.processIncomingVehicle();

        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(savedTicket, "Le ticket n'a pas été créé correctement");
        assertEquals("ABCDEF", savedTicket.getVehicleRegNumber(),"Le numéro de véhicule doit être correct");
    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();

        Date outTime = new Date();
        outTime.setTime( System.currentTimeMillis() + (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        
        parkingService.processExitingVehicle(outTime);

        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(savedTicket.getOutTime());

        savedTicket = ticketDAO.getTicket("ABCDEF");
        double expectedFare = Fare.CAR_RATE_PER_HOUR * 0.75;
        assertEquals( expectedFare, savedTicket.getPrice(), 0.01,"Le tarif n'est pas correctement calculé");
        assertTrue(savedTicket.getOutTime().after(savedTicket.getInTime()),"L'heure de sortie doit être après l'heure d'entrée");
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle(null);
        parkingService.processIncomingVehicle();

        Date outTime = new Date();
        outTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );//60 minutes parking time should give 1 parking fare

        parkingService.processExitingVehicle(outTime);

        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(savedTicket.getOutTime());

        savedTicket = ticketDAO.getTicket("ABCDEF");
        double expectedFare = Fare.CAR_RATE_PER_HOUR * (1.0 - Fare.DISCOUNT);
        assertEquals(expectedFare, savedTicket.getPrice(), 0.01,"Le tarif n'est pas correctement calculé " + expectedFare + " <> " + savedTicket.getPrice());
        assertTrue(savedTicket.getOutTime().after(savedTicket.getInTime()),"L'heure de sortie doit être après l'heure d'entrée");
    }
}
