package com.example.carins.web;

import com.example.carins.model.Car;
import com.example.carins.service.CarService;
import com.example.carins.web.dto.CarDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class CarController
{

    private final CarService service;

    public CarController(CarService service)
    {
        this.service = service;
    }

    @GetMapping("/cars")
    public List<CarDto> getCars()
    {
        return service.listCars().stream().map(this::toDto).toList();
    }

    @GetMapping("/cars/{carId}/insurance-valid")
    public ResponseEntity<?> isInsuranceValid(@NotNull @PathVariable Long carId, @NotEmpty @RequestParam String date)
    {
        try
        {
            LocalDate.parse(date);
        }
        catch (DateTimeParseException e)
        {
            return ResponseEntity.badRequest().body("Date must be in a valid YYYY-MM-DD format");
        }

        LocalDate d = LocalDate.parse(date);
        try
        {
            boolean valid = service.isInsuranceValid(carId, d);
            return ResponseEntity.ok(new InsuranceValidityResponse(carId, d.toString(), valid));
        }
        catch (ChangeSetPersister.NotFoundException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cars/{carId}/claims")
    public ResponseEntity<?> registerClaim(@NotNull @PathVariable Long carId, @Valid @RequestBody ClaimRequest request)
    {
        try
        {
            var claimId = service.registerClaim(carId, request.claimDate, request.description, request.amount);
            var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                    .replacePath("/api/claims/" + claimId)
                    .build()
                    .toUri();
            return ResponseEntity.created(uri).build();
        }
        catch (NoSuchElementException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cars/{carId}/history")
    public ResponseEntity<?> getCarHistory(@NotNull @PathVariable Long carId)
    {
        try
        {
            var history = service.carHistory(carId);
            return ResponseEntity.ok(history);
        }
        catch (NoSuchElementException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    private CarDto toDto(Car c)
    {
        var o = c.getOwner();
        return new CarDto(c.getId(), c.getVin(), c.getMake(), c.getModel(), c.getYearOfManufacture(),
                o != null ? o.getId() : null,
                o != null ? o.getName() : null,
                o != null ? o.getEmail() : null);
    }

    public record InsuranceValidityResponse(Long carId, String date, boolean valid)
    {
    }

    public record ClaimRequest(LocalDate claimDate, String description, int amount)
    {
    }
}