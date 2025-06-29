package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {

    private static final Path BASE_PATH = Path.of("public");
    private static final int MAX_RECURSION_DEPTH = 10;

    public static String render(String filePath) {
        try {
            Path mainTemplate = Path.of(filePath).normalize();

            if (!Files.exists(mainTemplate)) {
                return errorMessage("Arquivo principal não encontrado: " + mainTemplate);
            }

            String content = Files.readString(mainTemplate);

            // Processa todas as diretivas no template principal
            content = processAll(content, 0);

            return content;

        } catch (IOException e) {
            return errorMessage("Erro ao processar template: " + e.getMessage());
        }
    }

    // Processa todas as diretivas, com controle de profundidade para evitar recursão infinita
    private static String processAll(String content, int depth) throws IOException {
        if (depth > MAX_RECURSION_DEPTH) {
            return errorMessage("Profundidade máxima de recursão atingida");
        }

        content = processSections(content, depth);
        content = processAssets(content);
        content = processScripts(content);
        content = processImages(content);
        content = processComponents(content, depth);

        return content;
    }

    // Substitui @asset("caminho") por link CSS
    private static String processAssets(String content) {
        return content.replaceAll(
                "@asset\\(\"([^\"]+)\"\\)",
                "<link rel=\"stylesheet\" href=\"$1\">"
        );
    }

    // Substitui @script("nome.js") por script tag com src em static/js
    private static String processScripts(String content) {
        return content.replaceAll(
                "@script\\(\"([^\"]+)\"\\)",
                "<script src=\"/static/js/$1\"></script>"
        );
    }

    // Substitui @img("nome.png", "classes") por tag img com src e class
    private static String processImages(String content) {
        // Com classes
        Pattern patternWithClass = Pattern.compile("@img\\(\"([^\"]+)\",\\s*\"([^\"]*)\"\\)");
        Matcher matcherWithClass = patternWithClass.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcherWithClass.find()) {
            String file = matcherWithClass.group(1);
            String classes = matcherWithClass.group(2);
            String tag = "<img src=\"static/img/" + file + "\" class=\"" + classes + "\">";
            matcherWithClass.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }
        matcherWithClass.appendTail(sb);
        content = sb.toString();

        // Sem classes
        Pattern patternNoClass = Pattern.compile("@img\\(\"([^\"]+)\"\\)");
        Matcher matcherNoClass = patternNoClass.matcher(content);
        sb = new StringBuffer();

        while (matcherNoClass.find()) {
            String file = matcherNoClass.group(1);
            String tag = "<img src=\"static/img/" + file + "\">";
            matcherNoClass.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }

        matcherNoClass.appendTail(sb);
        return sb.toString();
    }


    // Processa @component recursivamente, chamando processAll para processar diretivas internas
    private static String processComponents(String content, int depth) throws IOException {
        Pattern pattern = Pattern.compile("@component\\(\"([^\"]+)\"\\)");
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String relativeComponentPath = cleanComponentPath(matcher.group(1));
            Path componentPath = BASE_PATH.resolve(relativeComponentPath).normalize();

            String replacement = readComponent(componentPath);

            // Processa todo o conteúdo do componente para expandir diretivas internas, com profundidade incrementada
            replacement = processAll(replacement, depth + 1);
            replacement = Matcher.quoteReplacement(replacement);

            matcher.appendReplacement(sb, replacement);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    // Remove barra inicial para evitar erro de caminho
    private static String cleanComponentPath(String rawPath) {
        return rawPath.replaceFirst("^/+", "");
    }

    // Lê o conteúdo do componente ou retorna mensagem de erro
    private static String readComponent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "<!-- Componente não encontrado: " + path + " -->";
        }
    }

    /*
    private static String processSections(String content, int depth) throws IOException {
        // Detecta @extends("template.admin.html")
        Pattern extendsPattern = Pattern.compile("@extends\\(\"([^\"]+)\"\\)");
        Matcher extendsMatcher = extendsPattern.matcher(content);

        if (extendsMatcher.find()) {
            String parentTemplate = extendsMatcher.group(1);
            Path parentPath = BASE_PATH.resolve(parentTemplate).normalize();

            // Remove a linha @extends do filho
            content = extendsMatcher.replaceFirst("");

            // Extrai todas as sections do filho
            Pattern sectionPattern = Pattern.compile("@section\\(\"([^\"]+)\"\\)([\\s\\S]*?)@endsection");
            Matcher sectionMatcher = sectionPattern.matcher(content);

            // Mapeia sections
            java.util.Map<String, String> sections = new java.util.HashMap<>();
            while (sectionMatcher.find()) {
                String name = sectionMatcher.group(1);
                String value = sectionMatcher.group(2).trim();
                sections.put(name, value);
            }

            // Lê o template pai
            String parentContent = Files.readString(parentPath);

            // Substitui @yield("nome") pelos blocos do filho
            for (var entry : sections.entrySet()) {
                String yieldTag = "@yield\\(\"" + Pattern.quote(entry.getKey()) + "\"\\)";
                parentContent = parentContent.replaceAll(yieldTag, Matcher.quoteReplacement(entry.getValue()));
            }

            // Remove yields não preenchidos
            parentContent = parentContent.replaceAll("@yield\\(\"[^\"]+\"\\)", "");

            // Processa recursivamente o resultado
            return processAll(parentContent, depth + 1);
        }

        // Se não tem @extends, retorna o conteúdo original
        return content;
    } */
    private static String processSections(String content, int depth) throws IOException {
        // Detecta @extends("template", "Título")
        Pattern extendsPattern = Pattern.compile("@extends\\(\"([^\"]+)\"(?:,\\s*\"([^\"]+)\")?\\)");
        Matcher extendsMatcher = extendsPattern.matcher(content);

        if (extendsMatcher.find()) {
            String parentTemplate = extendsMatcher.group(1);
            String pageTitle = extendsMatcher.group(2); // pode ser null
            Path parentPath = BASE_PATH.resolve(parentTemplate).normalize();

            // Remove a linha @extends do filho
            content = extendsMatcher.replaceFirst("");

            // Extrai todas as sections do filho
            Pattern sectionPattern = Pattern.compile("@section\\(\"([^\"]+)\"\\)([\\s\\S]*?)@endsection");
            Matcher sectionMatcher = sectionPattern.matcher(content);

            java.util.Map<String, String> sections = new java.util.HashMap<>();
            while (sectionMatcher.find()) {
                String name = sectionMatcher.group(1);
                String value = sectionMatcher.group(2).trim();
                sections.put(name, value);
            }

            // Lê o template pai
            String parentContent = Files.readString(parentPath);

            // Substitui @title se o título foi passado
            if (pageTitle != null) {
                parentContent = parentContent.replaceAll("@title", Matcher.quoteReplacement(pageTitle));
            }

            // Substitui @yield("nome") pelos blocos do filho
            for (var entry : sections.entrySet()) {
                String yieldTag = "@yield\\(\"" + Pattern.quote(entry.getKey()) + "\"\\)";
                parentContent = parentContent.replaceAll(yieldTag, Matcher.quoteReplacement(entry.getValue()));
            }

            // Remove yields não preenchidos
            parentContent = parentContent.replaceAll("@yield\\(\"[^\"]+\"\\)", "");

            // Processa recursivamente o resultado
            return processAll(parentContent, depth + 1);
        }

        return content;
    }

    // Mensagem de erro padronizada
    private static String errorMessage(String message) {
        return "<!-- ERRO: " + message + " -->";
    }
}
