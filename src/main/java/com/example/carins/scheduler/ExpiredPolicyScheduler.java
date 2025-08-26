package com.example.carins.scheduler;

import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.InsurancePolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ExpiredPolicyScheduler
{
    @Autowired
    InsurancePolicyRepository insurancePolicyRepository;

    Logger logger = LoggerFactory.getLogger(ExpiredPolicyScheduler.class);

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleTask()
    {
        List<InsurancePolicy> expiredPolicies = insurancePolicyRepository.findByEndDate(LocalDate.now());
        for (InsurancePolicy policy : expiredPolicies)
        {
            logger.info("Policy {} for car {} expired on {}", policy.getId(), policy.getCar().getId(),
                    policy.getEndDate());
        }
    }
}
