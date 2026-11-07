package io.github.igorpadua.imoveis.imovel;

public record Endereco(String logradouro, String numero, String complemento, String bairro, String cidade, Uf uf,
                       String cep, Double latitude, Double longitude) {

    public Endereco {
        boolean temLatitude = latitude != null;
        boolean temLongitude = longitude != null;
        if (temLatitude != temLongitude) {
            throw new RegraDeDominioException("Latitude e longitude devem ser informadas juntas (latitude: "
                    + latitude + ", longitude: " + longitude + ").");
        }

        if (temLatitude && (latitude < -90 || latitude > 90 || Double.isNaN(latitude))) {
            throw new RegraDeDominioException("Latitude deve estar entre -90 e 90 (informada: " + latitude + ").");
        }

        if (temLongitude && (longitude < -180 || longitude > 180 || Double.isNaN(longitude))) {
            throw new RegraDeDominioException("Longitude deve estar entre -180 e 180 (informada: " + longitude + ").");
        }

        logradouro = obrigatorio(logradouro, "logradouro");
        bairro = obrigatorio(bairro, "bairro");
        cidade = obrigatorio(cidade, "cidade");
        numero = obrigatorio(numero, "número");
        cep = obrigatorio(cep, "CEP");

        if (complemento != null) {
            complemento = complemento.strip();
            if (complemento.isEmpty()) {
                complemento = null;
            }
        }

        if (uf == null) {
            throw new RegraDeDominioException("O campo UF é obrigatório.");
        }

        boolean cepValido = cep.matches("\\d{5}-?\\d{3}");
        if (!cepValido) {
            throw new RegraDeDominioException("CEP deve ter 8 dígitos, no formato 00000000 ou 00000-000 (informado: " + cep + ").");
        }

        cep = cep.replace("-", "");
    }

    private static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeDominioException("O campo " + campo + " é obrigatório e não pode ficar em branco.");
        }
        return valor.strip();
    }

}
