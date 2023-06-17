package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {

    }

    @Test
    public void testParkingACar() throws ClassNotFoundException {
        //GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        //WHEN
        parkingService.processIncomingVehicle();
        //THEN
        // TODO: check that a ticket is actually saved in DB
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        // TODO: check that Parking table is updated with availability
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        // TODO: check that the next available parking spot is updated
        ParkingType parkingType = parkingSpot.getParkingType();
        int nextAvailableSpotId = parkingSpotDAO.getNextAvailableSlot(parkingType);
        ParkingSpot nextAvailableSpot = new ParkingSpot(nextAvailableSpotId, parkingType, false);
        assertFalse(nextAvailableSpot.isAvailable());
    }


    @Test
    public void testParkingLotExit() throws ClassNotFoundException {
        // Given
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
        // When
        // The car leaves the parking lot
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
       
        //TODO: check that the fare generated and out time are populated correctly in the database
        // Then
        assertNotNull(ticket);
        assertNotNull(ticket.getOutTime());
        assertNotNull(ticket.getPrice());
    }

    @Test
    public void testParkingLotExitRecurringUser() throws ClassNotFoundException {
        // Given
        // A recurring user with a 5% discount is parked in the parking lot
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        double initialPrice = ticket.getPrice();
        double discountedPrice = initialPrice * 0.95; // Apply a 5% discount
        
        // When
        // The recurring user leaves the parking lot
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF");
        
        // Then
        // Check that the out time and discounted price are populated correctly in the database
        assertNotNull(ticket);
        assertNotNull(ticket.getOutTime());
        assertNotNull(ticket.getPrice());
        assertEquals(discountedPrice, ticket.getPrice(), 0.001);
    }

}
