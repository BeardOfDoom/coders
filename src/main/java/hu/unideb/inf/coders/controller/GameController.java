package hu.unideb.inf.coders.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class GameController {

    @RequestMapping(path = "/game", method = RequestMethod.GET)
    public String loadPage() {
        return "game";
    }

}
