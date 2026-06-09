package br.com.notehub.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public interface UserService {

    User create(User user);

    String generateActivationToken(User user);

    void activate(UUID idFromToken);

    void promote(UUID idFromToken);

    void changePassword(String email, String newPassword);

    void changeEmail(String oldEmail, String newEmail);

    User edit(UUID idFromToken, User user);

    void changeProfileVisibility(UUID idFromToken);

    void changeUsername(UUID idFromToken, String username);

    void changeDisplayName(UUID idFromToken, String displayName);

    void changeAvatar(UUID idFromToken, String avatar);

    void changeBanner(UUID idFromToken, String banner);

    void changeMessage(UUID idFromToken, String message);

    void allowSubscription(UUID idFromToken, String subscriptionStr);

    void disallowSubscription(UUID idFromToken, String subscriptionStr);

    void delete(UUID idFromToken, String password);

    User getUser(String username);

    List<User> getAllActiveUsers();

    Page<User> findAll(Pageable pageable, String q);

    List<String> getUserDisplayNameHistory(String username);

    Set<Subscription> getUserSubscriptions(UUID idFromToken);

    void cleanUsersWithExpiredActivationTime();

}