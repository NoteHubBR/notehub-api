package br.com.notehub.application.validation;

import br.com.notehub.application.validation.constraints.NoForbiddenWords;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NoForbiddenWordsValidator implements ConstraintValidator<NoForbiddenWords, String> {

    private static final List<String> DEFAULT_FORBIDDEN = List.of(
            "elelyon", "elshaddai", "adonai", "elohim", "yhwh", "yahweh", "hashem",
            "jehovah", "jeova", "jeová", "allah", "alá",
            "israel", "israeli", "aisraeli", "israelita", "umisraelita", "israelit", "aisraelit",
            "null", "undefined", "admin", "user", "guest",
            "toe", "terms", "termos", "policies", "policy", "cookies", "cookie", "privacy", "legal", "legals", "language", "idioma",
            "signup", "signin", "recover", "sent", "activate", "search", "settings", "new", "help", "changelog", "dashboard",
            "crf", "crfla", "crflamengo", "flamengo", "fla", "tjf", "t.j.f", "jovemfla", "jovem.fla", "jovem_fla",
            "ffc", "flufc", "fluminensefc", "fluminese", "flu", "tyf", "t.y.f", "youngflu", "young.flu", "young_flu",
            "tcp", "t.c.p", "tcpuro",
            "pcc", "p.c.c",
            "ada", "a.d.a",
            "cv", "c.v", "comandov", "comando.v", "cvermelho", "c.vermelho",
            "td2", "td.2", "tudo2", "td3", "td.3", "tudo3", "13", "1333", "13.3.3"
    );

    private List<Pattern> forbiddenPatterns;

    @Override
    public void initialize(NoForbiddenWords constraintAnnotation) {
        forbiddenPatterns = DEFAULT_FORBIDDEN.stream()
                .map(String::toLowerCase)
                .map(w -> Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String normalized = value.toLowerCase().replaceAll("\\s+", "");
        return forbiddenPatterns.stream().noneMatch(p -> p.matcher(normalized).find());
    }

}