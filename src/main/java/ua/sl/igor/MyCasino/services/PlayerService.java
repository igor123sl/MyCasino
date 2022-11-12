package ua.sl.igor.MyCasino.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ua.sl.igor.MyCasino.domain.Player;
import ua.sl.igor.MyCasino.domain.enums.Role;
import ua.sl.igor.MyCasino.repositories.PlayerRepository;
import ua.sl.igor.MyCasino.util.exceptions.NotEnoughMoneyException;
import ua.sl.igor.MyCasino.util.exceptions.PlayerNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService implements UserDetailsService, Validator {

    private final PlayerRepository playerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, JdbcTemplate jdbcTemplate, @Lazy PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public void decreasePlayerBalance(long id, long amount) {
        Player player = findById(id).orElseThrow(PlayerNotFoundException::new);
        if (player.getBalance() < amount) {
            throw new NotEnoughMoneyException();
        }
        player.setBalance(player.getBalance() - amount);
        save(player);
    }

    public void increasePlayerBalance(long id, long amount) {
        Player player = findById(id).orElseThrow(PlayerNotFoundException::new);
        player.setBalance(player.getBalance() + amount);
        save(player);
    }

    public boolean changePassword(Player player, String oldPassword, String newPassword) {
        Player playerDB = findById(player.getId()).orElseThrow(PlayerNotFoundException::new);
        if (passwordEncoder.matches(oldPassword, playerDB.getPassword())) {
            playerDB.setPassword(passwordEncoder.encode(newPassword));
            save(playerDB);
            return true;
        }
        return false;
    }

    public void ban(long id) {
        Player player = findById(id).orElseThrow(PlayerNotFoundException::new);
        jdbcTemplate.update("DELETE FROM spring_session WHERE principal_name=?", player.getEmail());
        player.setAccountNonLocked(false);
        save(player);
    }

    public void unBan(long id) {
        Player player = findById(id).orElseThrow(PlayerNotFoundException::new);
        player.setAccountNonLocked(true);
        save(player);
    }

    public void register(Player player) {
        player.setPassword(passwordEncoder.encode(player.getPassword()));
        player.setRole(Role.ROLE_USER);
        player.setAccountNonLocked(true);
        save(player);
    }

    public Player save(Player player) {
        return playerRepository.save(player);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return playerRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Player not found!"));
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Player.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Player player = (Player) target;
        if (findByEmail(player.getEmail()).isPresent()) {
            errors.rejectValue("email", "", "This email is already taken!");
        }
        if (findByName(player.getName()).isPresent()) {
            errors.rejectValue("name", "", "This name is already taken!");
        }

    }

    public Optional<Player> findById(long id) {
        return playerRepository.findById(id);
    }

    public Optional<Player> findByEmail(String email) {
        return playerRepository.findByEmail(email);
    }

    public Optional<Player> findByName(String name) {
        return playerRepository.findByName(name);
    }

    public List<Player> findAll() {
        return playerRepository.findAll();
    }
}
