package br.com.consultorio.odonto.adaptador.entrada.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Exige {@code Authorization: Bearer <ADMIN_TOKEN>} nas rotas {@code /api/admin/**}. Sem token configurado,
 * a API administrativa fica desligada (404). A comparação é feita em tempo constante.
 */
@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    static final String PREFIXO_ROTA = "/api/admin/";
    private static final String PREFIXO_BEARER = "Bearer ";

    private final ConfiguracaoAdmin configuracao;

    public AdminTokenFilter(ConfiguracaoAdmin configuracao) {
        this.configuracao = configuracao;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        String caminho = requisicao.getRequestURI().substring(requisicao.getContextPath().length());
        return !(caminho + "/").startsWith(PREFIXO_ROTA);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        if (!configuracao.habilitada()) {
            recusar(resposta, HttpStatus.NOT_FOUND, "API administrativa desativada: defina ADMIN_TOKEN.");
            return;
        }
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER) || !MessageDigest.isEqual(
                configuracao.token().getBytes(StandardCharsets.UTF_8),
                cabecalho.substring(PREFIXO_BEARER.length()).strip().getBytes(StandardCharsets.UTF_8))) {
            resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            recusar(resposta, HttpStatus.UNAUTHORIZED, "Token administrativo ausente ou inválido.");
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private static void recusar(HttpServletResponse resposta, HttpStatus status, String detalhe) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.getWriter().write("{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + detalhe + "\"}");
    }
}
