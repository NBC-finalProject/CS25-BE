package com.example.cs25service.domain.users.service;

import com.example.cs25entity.domain.user.entity.User;
import com.example.cs25entity.domain.user.repository.UserRepository;
import com.example.cs25service.domain.security.dto.AuthUser;
import com.example.cs25service.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void disableUser(AuthUser authUser) {

        User user = userRepository.findBySerialIdOrElseThrow(authUser.getSerialId());
        user.updateDisableUser();

        if (user.getSubscription() != null) {
            subscriptionService.cancelSubscription(user.getSubscription().getSerialId());
        }
    }
}
