package com.parkit.parkingsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    @InjectMocks
    private static ParkingService parkingService;
    
    @Mock
    private InputReaderUtil inputReaderUtil;
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    @Mock
    private TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
    try {
        // Stubbing for inputReaderUtil.readVehicleRegistrationNumber()
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        // Stubbing for ticketDAO.getTicket()
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

        // Stubbing for ticketDAO.updateTicket()
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // Stubbing for parkingSpotDAO.updateParking()
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Failed to set up test mock objects");
    }
}

    @Test
    public void processExitingVehicleTest() throws SQLException, Exception {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(2);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1);
        //WHEN
        parkingService.processExitingVehicle();
        //THEN
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        //WHEN
        parkingService.processIncomingVehicle();
        //THEN
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

   @Test
    public void processExitingVehicleTestUnableUpdate() throws SQLException {
        // GIVEN -- Simulate the situation where updateTicket() returns false
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        
        // WHEN -- Call the processExitingVehicle() method
        parkingService.processExitingVehicle();
        
        // THEN -- Verify that no update was made to the parking spot
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }

   @Test
    public void testGetNextParkingNumberIfAvailable() throws Exception {
        // GIVEN -- Mock the inputReaderUtil and parkingSpotDAO methods
        when(inputReaderUtil.readSelection())
            .thenReturn(1)
            .thenReturn(2);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class)))
            .thenReturn(1)
            .thenReturn(2);
        // WHEN -- Execute the getNextParkingNumberIfAvailable method for car and bike parking spots
        ParkingSpot resultCar = parkingService.getNextParkingNumberIfAvailable();
        ParkingSpot resultBike = parkingService.getNextParkingNumberIfAvailable();

        // THEN -- Verify that the results are not null and have the expected properties
        assertNotNull(resultCar);
        assertEquals(ParkingType.CAR, resultCar.getParkingType());
        assertEquals(1, resultCar.getId());
        assertTrue(resultCar.isAvailable());
        
        assertNotNull(resultBike);
        assertEquals(ParkingType.BIKE, resultBike.getParkingType());
        assertEquals(2, resultBike.getId());
        assertTrue(resultBike.isAvailable());

        // Verify that the getNextAvailableSlot method was called once for each parking type
        verify(parkingSpotDAO, times(2)).getNextAvailableSlot(any(ParkingType.class));    
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() throws Exception {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);
        //WHEN
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
        //THEN
        assertNull(result);

        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
    }
    
   @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3);
        //WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();  
        //THEN
        Assertions.assertNull(parkingSpot);
    
        verify(inputReaderUtil, times(1)).readSelection();
        }
}