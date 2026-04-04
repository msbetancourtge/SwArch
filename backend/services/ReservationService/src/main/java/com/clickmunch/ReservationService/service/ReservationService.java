package com.clickmunch.ReservationService.service;

import com.clickmunch.ReservationService.dto.*;
import com.clickmunch.ReservationService.entity.Reservation;
import com.clickmunch.ReservationService.entity.ReservationStatus;
import com.clickmunch.ReservationService.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        Reservation reservation = Reservation.builder()
                .customerId(request.customerId())
                .customerName(request.customerName())
                .restaurantId(request.restaurantId())
                .restaurantName(request.restaurantName())
                .reservationDate(request.reservationDate())
                .reservationTime(request.reservationTime())
                .partySize(request.partySize())
                .status(ReservationStatus.Pendiente)
                .notes(request.notes())
                .createdAt(LocalDateTime.now())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return toResponse(saved);
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        return toResponse(reservation);
    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAllOrderedByDate().stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> getReservationsByRestaurant(Long restaurantId) {
        return reservationRepository.findByRestaurantId(restaurantId).stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> getReservationsByCustomer(Long customerId) {
        return reservationRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> getReservationsByStatus(String status) {
        return reservationRepository.findByStatus(status).stream()
                .map(this::toResponse).toList();
    }

    public List<ReservationResponse> getReservationsByRestaurantAndDate(Long restaurantId, LocalDate date) {
        return reservationRepository.findByRestaurantIdAndDate(restaurantId, date).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, UpdateReservationStatusRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        ReservationStatus newStatus = ReservationStatus.valueOf(request.status());
        reservation.setStatus(newStatus);

        Reservation updated = reservationRepository.save(reservation);
        return toResponse(updated);
    }

    @Transactional
    public ReservationResponse linkOrder(Long id, LinkOrderRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));

        reservation.setOrderId(request.orderId());
        Reservation updated = reservationRepository.save(reservation);
        return toResponse(updated);
    }

    @Transactional
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new IllegalArgumentException("Reservation not found: " + id);
        }
        reservationRepository.deleteById(id);
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCustomerId(),
                r.getCustomerName(),
                r.getRestaurantId(),
                r.getRestaurantName(),
                r.getReservationDate(),
                r.getReservationTime(),
                r.getPartySize(),
                r.getStatus().name(),
                r.getNotes(),
                r.getOrderId(),
                r.getCreatedAt()
        );
    }
}
