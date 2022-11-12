package ua.sl.igor.MyCasino.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.sl.igor.MyCasino.DTO.EditPlayerDTO;
import ua.sl.igor.MyCasino.domain.Player;
import ua.sl.igor.MyCasino.services.PlayerService;
import ua.sl.igor.MyCasino.util.exceptions.PlayerNotFoundException;

import javax.validation.Valid;
import java.util.Optional;

@Controller
public class PlayerController {

    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal Player player, Model model) {
        model.addAttribute("player", playerService.findById(player.getId()).orElseThrow(PlayerNotFoundException::new));
        return "profile";
    }

    @PostMapping("/add100")
    public String add100(@AuthenticationPrincipal Player player) {
        playerService.increasePlayerBalance(player.getId(), 100);
        return "redirect:/profile";
    }

    @GetMapping("/registration")
    public String registrationPage(@ModelAttribute("player") Player player) {
        return "registration";
    }

    @PostMapping("/registration")
    public String registerPlayer(@ModelAttribute("player") @Valid Player player, BindingResult bindingResult) {
        playerService.validate(player, bindingResult);
        if (bindingResult.hasErrors()) {
            return "registration";
        }
        playerService.register(player);
        return "redirect:/login";
    }

    @GetMapping("/editProfile")
    private String editProfile(@AuthenticationPrincipal Player player, @ModelAttribute("editPlayer") EditPlayerDTO editPlayerDTO, Model model) {
        model.addAttribute("player", playerService.findById(player.getId()).orElseThrow(PlayerNotFoundException::new));
        return "editProfile";
    }

    @PostMapping("/editProfile")
    public String editProfileConfirm(@AuthenticationPrincipal Player player, @ModelAttribute("editPlayer") @Valid EditPlayerDTO editPlayerDTO, BindingResult bindingResult, Model model) {
        String email = editPlayerDTO.getEmail();
        String name = editPlayerDTO.getName();
        player = playerService.findById(player.getId()).orElseThrow(PlayerNotFoundException::new);
        model.addAttribute("player", player);
        if (player.getName().equals(name) && player.getEmail().equals(email)) {
            return "redirect:/profile";
        } else {
            boolean isSomeIsTaken = false;
            String sameName = null;
            String sameEmail = null;
            Optional<Player> sameNamePlayer = playerService.findByName(name);
            Optional<Player> sameEmailPlayer = playerService.findByEmail(email);
            if (sameNamePlayer.isPresent()) {
                sameName = sameNamePlayer.get().getName();
            }
            if (sameEmailPlayer.isPresent()) {
                sameEmail = sameEmailPlayer.get().getEmail();
            }
            if (name.equals(sameName) && !(player.getName().equals(sameName))) {
                model.addAttribute("nameError", "This name is already taken!");
                isSomeIsTaken = true;
            }
            if (email.equals(sameEmail) && !(player.getEmail().equals(sameEmail))) {
                model.addAttribute("emailError", "This email is already taken!");
                isSomeIsTaken = true;
            }
            if (bindingResult.hasErrors()) {
                return "editProfile";
            }
            if (isSomeIsTaken) {
                return "editProfile";
            }
        }

        player.setName(name);
        player.setEmail(email);
        playerService.save(player);

        Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuth = new UsernamePasswordAuthenticationToken(player, oldAuth.getCredentials(), oldAuth.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return "redirect:/profile";
    }

    @PostMapping("/changePassword")
    public String editPassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, @AuthenticationPrincipal Player player, Model model) {
        model.addAttribute("player", player);
        if (playerService.changePassword(player, oldPassword, newPassword)) {
            return "redirect:/profile";
        }
        model.addAttribute("passwordError", "Passwords do not match!");
        EditPlayerDTO editPlayerDTO = new EditPlayerDTO(player.getName(), player.getEmail());
        model.addAttribute("editPlayer", editPlayerDTO);
        return "editProfile";
    }
}
