package br.com.notehub.domain.user;

import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.history.UserHistory;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.notification.Notification;
import br.com.notehub.domain.reply.Reply;
import br.com.notehub.domain.token.Token;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Data
@JsonIgnoreProperties({"tokens", "history", "notes", "comments", "replies", "flames", "receivedNotifications", "sentNotifications", "relatedNotifications"})
@ToString(exclude = {"tokens", "history", "notes", "comments", "replies", "flames", "receivedNotifications", "sentNotifications", "relatedNotifications"})
@EqualsAndHashCode(exclude = {"tokens", "history", "notes", "comments", "replies", "flames", "receivedNotifications", "sentNotifications", "relatedNotifications"})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserIdentity> identities = new ArrayList<>();

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String username;

    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String banner;

    private String message;

    private String password;

    private boolean profilePrivate = false;

    private boolean dev = false;

    private boolean sponsor = false;

    private boolean blocked = false;

    private Long score = 0L;

    private Instant createdAt = LocalDateTime.now().toInstant(ZoneOffset.of("-03:00"));

    private boolean active;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "users_subscriptions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "subscription")
    @Convert(converter = SubscriptionConverter.class)
    private Set<Subscription> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private List<Token> tokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserHistory> history = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Note> notes = new ArrayList<>();
    private int notesCount = 0;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Reply> replies = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Flame> flames = new HashSet<>();

    @OneToMany(mappedBy = "to", orphanRemoval = true)
    private List<Notification> receivedNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "from", orphanRemoval = true)
    private List<Notification> sentNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "related")
    private List<Notification> relatedNotifications = new ArrayList<>();

    private int followingCount = 0;

    private int followersCount = 0;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_BASIC"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean wants(Subscription subscription) {
        return subscriptions.contains(subscription);
    }

    public void enable(Subscription subscription) {
        subscriptions.add(subscription);
    }

    public void disable(Subscription subscription) {
        subscriptions.remove(subscription);
    }

    public static User signup(String email, String username, String displayName, String password) {
        User user = new User();
        user.email = email;
        user.username = username;
        user.displayName = displayName;
        user.password = password;
        user.active = false;
        Collections.addAll(user.subscriptions, Subscription.MAINTENANCE, Subscription.RELEASE);
        return user;
    }

    public static User oauthSignup(String email, String username, String displayName, String avatar) {
        User user = new User();
        user.email = email;
        user.username = username.toLowerCase();
        user.displayName = displayName;
        user.avatar = avatar;
        user.active = true;
        Collections.addAll(user.subscriptions, Subscription.MAINTENANCE, Subscription.RELEASE);
        return user;
    }

    public static User update(String username, String displayName, String avatar, String banner, String message, boolean profilePrivate) {
        User user = new User();
        user.username = username;
        user.displayName = displayName;
        user.avatar = avatar;
        user.banner = banner;
        user.message = message;
        user.profilePrivate = profilePrivate;
        return user;
    }

}