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
@JsonIgnoreProperties({"tokens", "history", "notes", "comments", "replies", "flames", "followers", "following", "receivedNotifications", "sentNotifications", "relatedNotifications"})
@ToString(exclude = {"tokens", "history", "notes", "comments", "replies", "flames", "followers", "following", "receivedNotifications", "sentNotifications", "relatedNotifications"})
@EqualsAndHashCode(exclude = {"tokens", "history", "notes", "comments", "replies", "flames", "followers", "following", "receivedNotifications", "sentNotifications", "relatedNotifications"})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String providerId;

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

    @Convert(converter = HostConverter.class)
    private Host host;

    private boolean profilePrivate = false;

    private boolean sponsor = false;

    private boolean blocked = false;

    private Long score = 0L;

    private Instant createdAt = LocalDateTime.now().toInstant(ZoneOffset.of("-03:00"));

    private boolean active;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_follows",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private Set<User> following = new HashSet<>();
    private int followingCount = 0;

    @ManyToMany(mappedBy = "following", fetch = FetchType.LAZY)
    private Set<User> followers = new HashSet<>();
    private int followersCount = 0;

    public User(String email, String username, String displayName, String password) {
        this.host = Host.NOTEHUB;
        this.email = email;
        this.displayName = displayName;
        this.username = username;
        this.password = password;
        this.active = false;
    }

    public User(String id, Host host, String email, String username, String displayName, String avatar) {
        this.providerId = id;
        this.host = host;
        this.email = email;
        this.username = username.toLowerCase();
        this.displayName = displayName;
        this.avatar = avatar;
        this.active = true;
    }

    public User(String username, String displayName, String avatar, String banner, String message, boolean profilePrivate) {
        this.username = username;
        this.displayName = displayName;
        this.avatar = avatar;
        this.banner = banner;
        this.message = message;
        this.profilePrivate = profilePrivate;
    }

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

}