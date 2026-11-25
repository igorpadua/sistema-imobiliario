package io.github.igorpadua.imoveis.imovel;

final class Textos {
    private Textos() { }

    static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeDominioException("O campo " + campo + " é obrigatório e não pode ficar em branco.");
        }
        return valor.strip();
    }

    static String opcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.strip();
    }

}
