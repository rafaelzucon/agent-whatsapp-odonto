package br.com.consultorio.odonto.adaptador.saida.persistencia.cripto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Converte colunas de dados pessoais: grava cifrado e lê em claro.
 * É um bean do Spring: o Hibernate obtém a instância pelo contêiner de beans do Spring.
 */
@Component
@Converter
public class TextoCriptografadoConverter implements AttributeConverter<String, String> {

    private final CriptografiaService criptografia;

    public TextoCriptografadoConverter(CriptografiaService criptografia) {
        this.criptografia = criptografia;
    }

    @Override
    public String convertToDatabaseColumn(String texto) {
        return criptografia.cifrar(texto);
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        return criptografia.decifrar(valor);
    }
}
