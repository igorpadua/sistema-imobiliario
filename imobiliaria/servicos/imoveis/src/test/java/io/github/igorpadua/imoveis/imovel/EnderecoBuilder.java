package io.github.igorpadua.imoveis.imovel;

class EnderecoBuilder {

    private String logradouro = "Rua 98";
    private String numero = "222";
    private String bairro = "Bairro";
    private String cidade = "São Paulo";
    private Uf uf = Uf.SP;
    private String cep = "04852005";
    private String complemento = "Ao lado do prédio X";
    private Double latitude = -23.76d;
    private Double longitude = -46.66d;

    static EnderecoBuilder umEndereco() {
        return new EnderecoBuilder();
    }

    EnderecoBuilder comCoordenadas(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        return this;
    }

    EnderecoBuilder comLogradouro(String logradouro) {
        this.logradouro = logradouro;
        return this;
    }

    EnderecoBuilder comBairro(String bairro) {
        this.bairro = bairro;
        return this;
    }

    EnderecoBuilder comCidade(String cidade) {
        this.cidade = cidade;
        return this;
    }

    EnderecoBuilder comNumero(String numero) {
        this.numero = numero;
        return this;
    }

    EnderecoBuilder semCoordenadas() {
        return comCoordenadas(null, null);
    }

    EnderecoBuilder comComplemento(String complemento) {
        this.complemento = complemento;
        return this;
    }

    EnderecoBuilder semComplemento() {
        return comComplemento(null);
    }

    EnderecoBuilder semUf() {
        this.uf = null;
        return this;
    }

    EnderecoBuilder comCep(String cep) {
        this.cep = cep;
        return this;
    }

    Endereco build() {
        return new Endereco(
                logradouro,
                numero,
                complemento,
                bairro,
                cidade,
                uf,
                cep,
                latitude,
                longitude
        );
    }

}
