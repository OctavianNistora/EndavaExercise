package com.example.carins.service;

import com.example.carins.model.Car;
import com.example.carins.model.InsuranceClaim;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsuranceClaimRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final InsurancePolicyRepository policyRepository;
    private final InsuranceClaimRepository claimRepository;

    public CarService(CarRepository carRepository, InsurancePolicyRepository policyRepository,
                      InsuranceClaimRepository claimRepository) {
        this.carRepository = carRepository;
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;
    }

    public List<Car> listCars() {
        return carRepository.findAll();
    }

    public boolean isInsuranceValid(Long carId, LocalDate date) throws ChangeSetPersister.NotFoundException
    {
        if (carId == null || date == null) return false;
        carRepository.findById(carId).orElseThrow(ChangeSetPersister.NotFoundException::new);
        return policyRepository.existsActiveOnDate(carId, date);
    }

    public long registerClaim(Long carId, LocalDate claimDate, String description, int amount)
    {
        var car = carRepository.findById(carId).orElseThrow();
        var insuranceClaim = new InsuranceClaim();
        insuranceClaim.setCar(car);
        insuranceClaim.setClaimDate(claimDate);
        insuranceClaim.setDescription(description);
        insuranceClaim.setAmount(amount);

        claimRepository.save(insuranceClaim);

        return insuranceClaim.getId();
    }

    public List<String> carHistory(Long carId) {
        carRepository.findById(carId).orElseThrow();
        List<InsurancePolicy> policyList = policyRepository.carPolicyHistory(carId);
        List<InsuranceClaim> claimList = claimRepository.carClaimHistory(carId);
        var result = new ArrayList<String>();
        int claimIndex = 0;

        for (int i = 0; i < policyList.size() - 1; i++) {
            InsurancePolicy policy = policyList.get(i);

            claimIndex = addClaimsToHistory(result, claimList, claimIndex, policy.getStartDate());
            result.add("Policy " + policy.getId() + " for car " + policy.getCar().getId() + " started on "
                    + policy.getStartDate());
            claimIndex = addClaimsToHistory(result, claimList, claimIndex, policy.getEndDate());
            result.add("Policy " + policy.getId() + " for car " + policy.getCar().getId() + " expired on "
                    + policy.getEndDate());
        }
        InsurancePolicy policy = policyList.get(policyList.size() - 1);

        claimIndex = addClaimsToHistory(result, claimList, claimIndex, policy.getStartDate());

        result.add("Policy " + policy.getId() + " for car " + policy.getCar().getId() + " started on "
                + policy.getStartDate());

        claimIndex = addClaimsToHistory(result, claimList, claimIndex, policy.getEndDate());

        if (policy.getEndDate().isBefore(LocalDate.now()))
        {
            result.add("Policy " + policy.getId() + " for car " + policy.getCar().getId() + " expired on "
                    + policy.getEndDate());

            while (claimIndex < claimList.size()) {
                InsuranceClaim claim = claimList.get(claimIndex++);
                result.add("Claim " + claim.getId() + " for car " + claim.getCar().getId() + " on "
                        + claim.getClaimDate() + " with amount " + claim.getAmount() + " and description: "
                        + claim.getDescription());
            }
        }

        return result;
    }

    private int addClaimsToHistory(List<String> result, List<InsuranceClaim> claimList, int claimIndex, LocalDate beforeDate)
    {
        while (claimIndex < claimList.size() &&
                claimList.get(claimIndex).getClaimDate().isBefore(beforeDate)) {
            InsuranceClaim claim = claimList.get(claimIndex++);
            result.add("Claim " + claim.getId() + " for car " + claim.getCar().getId() + " on "
                    + claim.getClaimDate() + " with amount " + claim.getAmount() + " and description: "
                    + claim.getDescription());
        }

        return claimIndex;
    }
}
